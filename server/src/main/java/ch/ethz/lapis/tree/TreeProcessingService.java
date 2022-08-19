package ch.ethz.lapis.tree;

import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import info.bioinfweb.jphyloio.JPhyloIOEventReader;
import info.bioinfweb.jphyloio.ReadWriteParameterMap;
import info.bioinfweb.jphyloio.events.EdgeEvent;
import info.bioinfweb.jphyloio.events.JPhyloIOEvent;
import info.bioinfweb.jphyloio.events.NodeEvent;
import info.bioinfweb.jphyloio.events.type.EventContentType;
import info.bioinfweb.jphyloio.events.type.EventTopologyType;
import info.bioinfweb.jphyloio.factory.JPhyloIOReaderWriterFactory;
import info.bioinfweb.jphyloio.formats.JPhyloIOFormatIDs;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.javatuples.Pair;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Inspired by http://bioinfweb.info/Code/sventon/repos/JPhyloIO/show/trunk/demo/info.bioinfweb.jphyloio.demo.tree/src/info/bioinfweb/jphyloio/demo/tree/TreeReader.java
public class TreeProcessingService {

    private final ComboPooledDataSource dbPool;
    private final Path pathToNwkFile;
    private final Path pathToIdentifierMapping;

    public TreeProcessingService(ComboPooledDataSource dbPool, Path pathToNwkFile, Path pathToIdentifierMapping) {
        this.dbPool = dbPool;
        this.pathToNwkFile = pathToNwkFile;
        this.pathToIdentifierMapping = pathToIdentifierMapping;
    }

    private int i = 0;

    public void doWork() throws Exception {
        // Read mapping of the public IDs to GISAID IDs
        Map<String, String> accessionToGisaid = new HashMap<>();
        Map<String, String> strainToGisaid = new HashMap<>();
        try (CSVParser parser = CSVParser.parse(pathToIdentifierMapping, StandardCharsets.UTF_8, CSVFormat.TDF)) {
            for (CSVRecord record : parser) {
                String gisaid = record.get(0);
                if (gisaid == null || gisaid.isBlank()) {
                    continue;
                }
                String accession = record.get(1);
                String strain = record.get(2);
                if (accession != null && !accession.isBlank()) {
                    accessionToGisaid.put(accession, gisaid);
                }
                if (strain != null && !strain.isBlank()) {
                    strainToGisaid.put(strain, gisaid);
                }
            }
        }
        // Read tree from newick file
        JPhyloIOEventReader reader = new JPhyloIOReaderWriterFactory()
            .getReader(JPhyloIOFormatIDs.NEWICK_FORMAT_ID, pathToNwkFile.toFile(), new ReadWriteParameterMap());
        TreeBuildingContext context = new TreeBuildingContext();
        while (reader.hasNextEvent()) {
            JPhyloIOEvent event = reader.next();
            if (!event.getType().getTopologyType().equals(EventTopologyType.START)) {
                continue;
            }
            EventContentType eventType = event.getType().getContentType();
            switch (eventType) {
                case NODE -> readNode(event.asNodeEvent(), context);
                case ROOT_EDGE, EDGE -> readEdge(event.asEdgeEvent(), context);
            }
        }
        // Find the root node of the tree
        SimpleTreeNode root = context.idToNodeMap.values().stream().findAny().get();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        // Clean up node labels:
        //   1. replace " " with "_"
        //   2. For sequences, use the gisaid_epi_isl as label
        //   3. Collect labels that cannot be parsed
        root.traverseDFS(n -> {
            String name = n.getName().replace(' ', '_');
            if (!name.startsWith("node_")) {
                String[] split = name.split("\\|");
                for (String s : split) {
                    if (s.startsWith("EPI_ISL_")) {
                        // Parse the GISAID EPI ISL from the label
                        n.setName(s);
                        return;
                    } else if (accessionToGisaid.containsKey(s)) {
                        // Map the accession to the GISAID EPI ISL
                        n.setName(accessionToGisaid.get(s));
                        return;
                    } else if (strainToGisaid.containsKey(s)) {
                        // Map the strain to the GISAID EPI ISL
                        n.setName(strainToGisaid.get(s));
                        return;
                    }
                }
            }
        });
        // Create tree object
        SimpleTree tree = new SimpleTree(root);
        // Remove the leaves that we don't associate with a GISAID sequence
        List<String> gisaidEpiIsls = new ArrayList<>();
        tree.traverseBFS(n -> {
            if (n.getName().startsWith("EPI_ISL_")) {
                gisaidEpiIsls.add(n.getName());
            }
        });
        tree = tree.extractTreeWith(gisaidEpiIsls, true);
        // Save the compressed tree in the database
        byte[] serialized = tree.serializeToBytes();
        byte[] compressed = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE).compressBytes(serialized);
        String sql = """
            insert into y_tree (timestamp, bytes)
            values (extract(epoch from now())::bigint, ?);
            """;
        try (Connection conn = dbPool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setBytes(1, compressed);
                statement.execute();
            }
        }
        System.out.println("Finished");
    }


    /**
     * Reads the most recent version of the compressed tree from the database
     */
    public static Pair<Long, SimpleTree> getMostRecentTreeFromDatabase(ComboPooledDataSource dbPool) {
        String sql = """
            select timestamp, bytes
            from y_tree
            order by timestamp desc
            limit 1;
            """;
        try (Connection conn = dbPool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql)) {
                    if (!rs.next()) {
                        return null;
                    }
                    return new Pair<>(
                        rs.getLong("timestamp"),
                        SimpleTree.readFromBytes(new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE).decompressBytes(rs.getBytes("bytes")))
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void readNode(NodeEvent nodeEvent, TreeBuildingContext context) {
        if (i++ % 100000 == 0) {
            System.out.println("Node (per 100k) " + (i / 100000));
        }
        SimpleTreeNode treeNode = new SimpleTreeNode(nodeEvent.getLabel());
        context.idToNodeMap.put(nodeEvent.getID(), treeNode);
    }

    private void readEdge(EdgeEvent edgeEvent, TreeBuildingContext context) {
        SimpleTreeNode sourceNode = context.idToNodeMap.get(edgeEvent.getSourceID());
        SimpleTreeNode targetNode = context.idToNodeMap.get(edgeEvent.getTargetID());
        if (targetNode.getParent() == null) {
            if (sourceNode != null) {
                sourceNode.addChild(targetNode);
                targetNode.setParent(sourceNode);
            }
        } else {
            throw new RuntimeException("Multiple parent nodes were specified for the node \"" + edgeEvent.getTargetID() + "\".");
        }
    }

    private static class TreeBuildingContext {
        private final Map<String, SimpleTreeNode> idToNodeMap = new HashMap<>();
    }

}

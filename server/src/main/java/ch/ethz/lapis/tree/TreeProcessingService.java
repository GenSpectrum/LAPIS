package ch.ethz.lapis.tree;

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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Inspired by http://bioinfweb.info/Code/sventon/repos/JPhyloIO/show/trunk/demo/info.bioinfweb.jphyloio.demo.tree/src/info/bioinfweb/jphyloio/demo/tree/TreeReader.java
public class TreeProcessingService {

    private final Path pathToNwkFile;
    private final Path pathToIdentifierMapping;

    public TreeProcessingService(Path pathToNwkFile, Path pathToIdentifierMapping) {
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
        List<String> unparsable = new ArrayList<>();
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
                unparsable.add(name);
            }
        });
        // Create tree object
        SimpleTree tree = new SimpleTree(root);
        // Remove the leaves that we don't associate with a GISAID sequence
        // TODO
        System.out.println("Finished");
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

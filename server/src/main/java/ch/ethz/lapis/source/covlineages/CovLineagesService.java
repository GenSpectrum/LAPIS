package ch.ethz.lapis.source.covlineages;

import ch.ethz.lapis.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class CovLineagesService {

    private final ComboPooledDataSource databasePool;
    private final Path workdir;

    public CovLineagesService(
        ComboPooledDataSource databasePool,
        String workdir
    ) {
        this.databasePool = databasePool;
        this.workdir = Path.of(workdir);
    }

    /**
     * Pull pango assignments from https://github.com/cov-lineages/pangolin-assignment
     */
    public void pullGisaidPangoLineageAssignments() throws IOException, SQLException {
        // Download the most recent assignment file from GitHub
        String fileListApiUrl = "https://api.github.com/repos/cov-lineages/pangolin-assignment/git/trees/main";
        Map<String, Object> fileListJson1 = (Map<String, Object>)
            new ObjectMapper().readValue(new URL(fileListApiUrl), Object.class);
        List<Object> fileListJson2 = (List<Object>) fileListJson1.get("tree");
        String urlOfMostRecentFile = null;
        LocalDate dateOfMostRecentFile = LocalDate.MIN;
        for (Object o : fileListJson2) {
            Map<String, String> fileListJson3 = (Map<String, String>) o;
            String path = fileListJson3.get("path");
            if (path.matches("pango_lineages_\\d{4}-\\d{2}-\\d{2}\\.csv\\.gz")) {
                String dateString = path.substring(15, 25);
                LocalDate date = LocalDate.parse(dateString);
                if (date.isAfter(dateOfMostRecentFile)) {
                    urlOfMostRecentFile = "https://github.com/cov-lineages/pangolin-assignment/raw/main/" + path;
                    dateOfMostRecentFile = date;
                }
            }
        }
        if (urlOfMostRecentFile == null) {
            throw new RuntimeException("Cannot find data in the pangolin-assignment repository");
        }
        Path assignmentFilePath = workdir.resolve("pangolin-assignment.csv.gz");
        downloadFile(new URL(urlOfMostRecentFile), assignmentFilePath);

        // Delete old data
        String truncateSql = "truncate y_pangolin_assignment;";
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(truncateSql);
            }
        }

        // Import the file
        InputStream fileInputStream = new FileInputStream(assignmentFilePath.toFile());
        GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
        String insertSql = """
            insert into y_pangolin_assignment (gisaid_epi_isl, pango_lineage)
            values (?, ?);
        """;
        int i = 0;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
                for (PangolinAssignmentEntry entry : new PangolinAssignmentFileReader(gzipInputStream)) {
                    statement.setString(1, entry.getGisaidEpiIsl());
                    statement.setString(2, entry.getPangoLineage());
                    statement.addBatch();
                    if (i++ >= 5000) {
                        Utils.executeClearCommitBatch(conn, statement);
                        i = 0;
                    }
                }
                if (i > 0) {
                    Utils.executeClearCommitBatch(conn, statement);
                }
                conn.setAutoCommit(true);
            }
        }
    }

    private void downloadFile(URL url, Path outputFilePath) throws IOException {
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath.toFile());
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }

}

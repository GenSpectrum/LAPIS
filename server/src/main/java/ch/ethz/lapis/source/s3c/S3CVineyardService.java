package ch.ethz.lapis.source.s3c;

import ch.ethz.lapis.core.DatabaseConfig;
import ch.ethz.lapis.core.DatabaseService;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class S3CVineyardService {

    private final ComboPooledDataSource databasePool;
    private final DatabaseConfig s3cVineyardDatabaseConfig;

    public S3CVineyardService(ComboPooledDataSource databasePool, DatabaseConfig s3cVineyardDatabaseConfig) {
        this.databasePool = databasePool;
        this.s3cVineyardDatabaseConfig = s3cVineyardDatabaseConfig;
    }

    public void updateData() throws SQLException {
        // Load all additional metadata from S3C Vineyard
        ComboPooledDataSource s3cVineyardPool = DatabaseService.createDatabaseConnectionPool(s3cVineyardDatabaseConfig);
        String loadSql = """
            select
              si.gisaid_id,
              bm.hospitalisation_type = 'HOSPITALIZED' as hospitalized,
              coalesce(bm.pttod, false) as died,
              (case
                when bm.impfstatus = 'YES' and (bm.fall_dt - bm.impfdatum_dose2 >= 14) then true
                when bm.impfstatus = 'YES' or bm.impfstatus = 'NO' then false
              end) as fully_vaccinated
            from
              sequence_identifier si
              join viollier_test vt on si.ethid = vt.ethid
              join bag_meldeformular bm on vt.sample_number = bm.sample_number
            where si.gisaid_id is not null;
        """;
        List<S3CAdditionalMetadataEntry> data = new ArrayList<>();
        try (Connection s3cConn = s3cVineyardPool.getConnection()) {
            try (Statement statement = s3cConn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(loadSql)) {
                    while (rs.next()) {
                        data.add(new S3CAdditionalMetadataEntry(
                                rs.getString("gisaid_id"),
                                null,
                                rs.getObject("hospitalized", Boolean.class),
                                rs.getObject("died", Boolean.class),
                                rs.getObject("fully_vaccinated", Boolean.class)
                        ));
                    }
                }
            }
        }
        s3cVineyardPool.close();

        // Delete the existing additional data from the LAPIS database and insert
        String deleteExistingSql = """
            delete from y_s3c;
        """;
        String insertSql = """
            insert into y_s3c (gisaid_epi_isl, sra_accession, hospitalized, died, fully_vaccinated)
            values (?, ?, ?, ?, ?);
        """;
        try (Connection lapisConn = databasePool.getConnection()) {
            lapisConn.setAutoCommit(false);
            try (Statement statement = lapisConn.createStatement()) {
                statement.execute(deleteExistingSql);
            }
            try (PreparedStatement statement = lapisConn.prepareStatement(insertSql)) {
                for (S3CAdditionalMetadataEntry d : data) {
                    statement.setString(1, d.getGisaidEpiIsl());
                    statement.setString(2, d.getEnaId());
                    statement.setObject(3, d.getHospitalized());
                    statement.setObject(4, d.getDied());
                    statement.setObject(5, d.getFullyVaccinated());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            lapisConn.commit();
            lapisConn.setAutoCommit(true);
        }
    }

}

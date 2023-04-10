package ch.ethz.lapis.api.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SqlClientTest {

    @Test
    void testParse() {
        SqlClient sqlClient = new SqlClient();
        Query query = sqlClient.parse("""
            select country, count(*) as num_sequences
            from metadata
            where
              region = 'Europe'
              and lineage = 'B.1'
              and aa_S_501 = 'Y'
            group by country
            order by num_sequences desc
            limit 1
            offset 2;
            """);
        sqlClient.validateAndRewrite(query);
        // TODO check correctness
    }

    @Test
    void testParseUnsupportedQueryWithArithmetic() {
        SqlClient sqlClient = new SqlClient();
        assertThrows(UnsupportedSqlException.class, () -> {
            Query query = sqlClient.parse("""
            select country, 1 + count(*) as num_sequences
            from metadata
            where region = 'Europe'
            group by country;
            """);
            sqlClient.validateAndRewrite(query);
        });
    }

    @Test
    void testParseUnsupportedQueryWithWrongField() {
        SqlClient sqlClient = new SqlClient();
        assertThrows(UnsupportedSqlException.class, () -> {
            Query query = sqlClient.parse("""
            select does_not_exist, count(*)
            from metadata
            group by does_not_exist;
            """);
            sqlClient.validateAndRewrite(query);
        });
    }

    @Test
    void testParseUnsupportedQueryWithWrongGroupBy() {
        SqlClient sqlClient = new SqlClient();
        assertThrows(UnsupportedSqlException.class, () -> {
            Query query = sqlClient.parse("""
            select does_not_exist, count(*) as abc
            from metadata
            group by num_sequences;
            """);
            sqlClient.validateAndRewrite(query);
        });
    }

}

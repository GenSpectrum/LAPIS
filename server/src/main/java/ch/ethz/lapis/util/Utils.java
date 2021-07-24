package ch.ethz.lapis.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;


public class Utils {

    public static String nullableBlankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s;
    }

    public static Float nullableFloatValue(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }


    public static Integer nullableIntegerValue(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }


    public static LocalDate nullableLocalDateValue(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }


    public static Date nullableSqlDateValue(LocalDate d) {
        if (d == null) {
            return null;
        }
        return Date.valueOf(d);
    }


    public static String getStackTraceString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static void executeClearCommitBatch(Connection conn, PreparedStatement statement) throws SQLException {
        statement.executeBatch();
        statement.clearBatch();
        conn.commit();
    }


    public static String getReferenceSeq() {
        try {
            InputStream in = Utils.class.getResourceAsStream("/reference-dictionary.txt");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static String getGeneMapGff() {
        try {
            InputStream in = Utils.class.getResourceAsStream("/genemap.gff");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

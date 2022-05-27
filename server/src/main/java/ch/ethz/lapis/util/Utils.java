package ch.ethz.lapis.util;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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


    // This function casts a float value to an integer.
    public static Integer nullableForcedIntegerValue(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return (int) Float.parseFloat(s);
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


    public static Float nullableDoubleToFloat(Double d) {
        if (d == null) {
            return null;
        }
        return (float) ((double) d);
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


    /**
     * Examples for normalized amino acid mutation codes: S:501Y, ORF1a:123N Examples for not normalized codes: S:N501Y,
     * s:501Y, ORF1A:123N This function does not intend to catch invalid mutation codes.
     */
    public static String normalizeAAMutation(String mutationCode) {
        List<String> normalizedGenes = ReferenceGenomeData.getInstance().getGeneNames();
        Map<String, String> lowerCaseToNormalizedGene = new HashMap<>();
        for (String normalizedGene : normalizedGenes) {
            lowerCaseToNormalizedGene.put(normalizedGene.toLowerCase(), normalizedGene);
        }

        String[] split = mutationCode.split(":");
        String gene = split[0];
        String mut = split[1];
        if (!Character.isDigit(mut.charAt(0))) {
            mut = mut.substring(1);
        }
        if (!lowerCaseToNormalizedGene.containsKey(gene.toLowerCase())) {
            throw new RuntimeException("Unknown gene: " + gene);
        }
        return lowerCaseToNormalizedGene.get(gene.toLowerCase()) + ":" + mut.toUpperCase();
    }


    /**
     * Examples for normalized nucleotide mutation codes: 123C, 234T Examples for not normalized codes: T123C, 234t This
     * function does not intend to catch invalid mutation codes.
     */
    public static String normalizeNucMutation(String mutationCode) {
        if (!Character.isDigit(mutationCode.charAt(0))) {
            mutationCode = mutationCode.substring(1);
        }
        return mutationCode.toUpperCase();
    }


    public static String hashMd5(Object obj) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
            objectOut.writeObject(obj);
            objectOut.flush();
            byte[] bytes = byteOut.toByteArray();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static void compareFastaFiles(FastaFileReader file1, FastaFileReader file2) {
        Map<String, String> sequences1 = new HashMap<>();
        for (FastaEntry fastaEntry : file1) {
            sequences1.put(fastaEntry.getSampleName(), fastaEntry.getSeq());
        }
        Map<String, String> sequences2 = new HashMap<>();
        for (FastaEntry fastaEntry : file2) {
            sequences2.put(fastaEntry.getSampleName(), fastaEntry.getSeq());
        }
        System.out.println("Files are equal? " + sequences1.equals(sequences2));
        if (!sequences1.equals(sequences2)) {
            sequences2.forEach((name2, seq2) -> {
                if (!sequences1.containsKey(name2)) {
                    System.out.println("File1 does not have " + name2);
                } else {
                    if (!sequences1.get(name2).equals(seq2)) {
                        System.out.println("File1 contains a different sequence for " + name2);
                    }
                }
            });
        }
    }

}

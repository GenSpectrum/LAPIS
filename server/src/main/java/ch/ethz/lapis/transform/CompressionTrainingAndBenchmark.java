package ch.ethz.lapis.transform;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.util.DeflateSeqCompressor;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.javatuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.*;


// This class was used to briefly compare Deflate with Zstd. Results (from 2021-12-03):
//   1. For Zstd, using the reference genome as dictionary is just as good or even better than using the dictionary
//      that I trained from 100 sequences.
//   2. Zstd compression level 3 is good. Compression levels 6 and 9 somehow resulted in worse compression. Level 19
//      gave a great compression ratio but is too slow.
//   3. Zstd (level 3) is much faster than Deflate (level 9). Compression can be up to 100x faster.
//   4. Compression ratio, Zstd (level 3) vs. Deflate (level 9):
//        aligned: Zstd better
//        original: Deflate better
//        aa_columnar: Deflate better
//        nuc_columnar: Deflate better
//      The differences are not huge, only in the order of 50-100%.
//   5. For the (nuc and AA) columnar data, a pre-defined dictionary is irrelevant.
//
// The values from one run:
//    ==== Test compression ====
//    ---- aligned ----
//    zstdAA: t=60, average size=9396
//    deflateReference: t=173, average size=375
//    deflateAA: t=1690, average size=8852
//    deflateNuc: t=2406, average size=8849
//    zstdNone: t=34, average size=9398
//    zstdReference: t=27, average size=171
//    zstdNuc: t=35, average size=9394
//    deflateNone: t=1843, average size=8847
//    ---- original ----
//    zstdAA: t=27, average size=9798
//    deflateReference: t=429, average size=1156
//    deflateAA: t=2197, average size=9309
//    deflateNuc: t=1872, average size=9310
//    zstdNone: t=20, average size=9795
//    zstdReference: t=20, average size=1841
//    zstdNuc: t=28, average size=9796
//    deflateNone: t=1728, average size=9304
//    ---- nuc_columnar ----
//    zstdAA: t=411, average size=139142
//    deflateReference: t=33215, average size=104435
//    deflateAA: t=34250, average size=104437
//    deflateNuc: t=32168, average size=104441
//    zstdNone: t=396, average size=139177
//    zstdReference: t=419, average size=139133
//    zstdNuc: t=428, average size=139168
//    deflateNone: t=31895, average size=104431
//    ---- aa_columnar ----
//    zstdAA: t=396, average size=135416
//    deflateReference: t=27402, average size=101734
//    deflateAA: t=28423, average size=101739
//    deflateNuc: t=29523, average size=101735
//    zstdNone: t=392, average size=135446
//    zstdReference: t=463, average size=135411
//    zstdNuc: t=520, average size=135419
//    deflateNone: t=29587, average size=101730
//    ---- aa_seq ----
//    deflateReference: t=2800, average size=497
//    zstdReference: t=536, average size=465
//    zstdAAReference: t=81, average size=27
//    deflateAA: t=2773, average size=498
//    deflateNone: t=2627, average size=492
//    zstdNone: t=22, average size=472
//    deflateNuc: t=2676, average size=496
//    deflateAAReference: t=2611, average size=32
//
//
//    ==== Test decompression ====
//    ---- aligned ----
//    zstdAA: t=16
//    deflateReference: t=137
//    deflateAA: t=95
//    deflateNuc: t=89
//    zstdNone: t=9
//    zstdReference: t=7
//    zstdNuc: t=22
//    deflateNone: t=102
//    ---- original ----
//    zstdAA: t=20
//    deflateReference: t=106
//    deflateAA: t=76
//    deflateNuc: t=78
//    zstdNone: t=11
//    zstdReference: t=12
//    zstdNuc: t=16
//    deflateNone: t=61
//    ---- nuc_columnar ----
//    zstdAA: t=286
//    deflateReference: t=660
//    deflateAA: t=688
//    deflateNuc: t=864
//    zstdNone: t=275
//    zstdReference: t=268
//    zstdNuc: t=283
//    deflateNone: t=646
//    ---- aa_columnar ----
//    zstdAA: t=245
//    deflateReference: t=682
//    deflateAA: t=812
//    deflateNuc: t=619
//    zstdNone: t=268
//    zstdReference: t=263
//    zstdNuc: t=249
//    deflateNone: t=690
//    ---- aa_seq ----
//    deflateReference: t=2923
//    zstdReference: t=20
//    zstdAAReference: t=6
//    deflateAA: t=3983
//    deflateNone: t=2613
//    zstdNone: t=8
//    deflateNuc: t=2695
//    deflateAAReference: t=2923
public class CompressionTrainingAndBenchmark {

    public static ComboPooledDataSource dbPool = LapisMain.dbPool;

    // Note: The following code may need adoption, depending on how the data are currently compressed in the
    // database.
    private static final SeqCompressor seqCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.REFERENCE);
    private static final SeqCompressor nucColumnarCompressor = new DeflateSeqCompressor(
        DeflateSeqCompressor.DICT.ATCGNDEL);
    private static final SeqCompressor aaColumnarCompressor = new DeflateSeqCompressor(
        DeflateSeqCompressor.DICT.AACODONS);

    public static void createTrainingDataset(Path outputDir) throws SQLException, IOException {
        Files.createDirectories(outputDir.resolve("seq_aligned"));
        Files.createDirectories(outputDir.resolve("seq_original"));

        // Write some sequences to file so that we can use e.g. zstd --train to create a dictionary.
        // https://github.com/facebook/zstd#dictionary-compression-how-to
        String sql1 = """
            select seq_original_compressed, seq_aligned_compressed
            from y_main_sequence
            order by random()
            limit 100;
            """;
        int i = 0;
        try (Connection conn = dbPool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql1)) {
                    while (rs.next()) {
                        Files.writeString(outputDir.resolve("seq_original").resolve(i + ".fasta"),
                            seqCompressor.decompress(rs.getBytes("seq_original_compressed")));
                        Files.writeString(outputDir.resolve("seq_aligned").resolve(i + ".fasta"),
                            seqCompressor.decompress(rs.getBytes("seq_aligned_compressed")));
                        i++;
                    }
                }
            }
        }
    }


    public static void benchmark() throws SQLException {

        List<Pair<String, SeqCompressor>> compressors = new ArrayList<>() {{
            add(new Pair<>("zstdReference", new ZstdSeqCompressor(ZstdSeqCompressor.DICT.REFERENCE)));
//            add(new Pair<>("zstdNuc", new ZstdSeqCompressor(ZstdSeqCompressor.DICT.ATCGNDEL)));
//            add(new Pair<>("zstdAA", new ZstdSeqCompressor(ZstdSeqCompressor.DICT.AACODONS)));
//            add(new Pair<>("zstdOriginal", new ZstdSeqCompressor(ZstdSeqCompressor.DICT.SEQ_ORIGINAL)));
//            add(new Pair<>("zstdAligned", new ZstdSeqCompressor(ZstdSeqCompressor.DICT.SEQ_ALIGNED)));
            add(new Pair<>("zstdAAReference", new ZstdSeqCompressor(ZstdSeqCompressor.DICT.AA_REFERENCE)));
            add(new Pair<>("zstdNone", new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE)));
            add(new Pair<>("deflateReference", new DeflateSeqCompressor(DeflateSeqCompressor.DICT.REFERENCE)));
            add(new Pair<>("deflateAAReference", new DeflateSeqCompressor(DeflateSeqCompressor.DICT.AA_REFERENCE)));
            add(new Pair<>("deflateNuc", new DeflateSeqCompressor(DeflateSeqCompressor.DICT.ATCGNDEL)));
            add(new Pair<>("deflateAA", new DeflateSeqCompressor(DeflateSeqCompressor.DICT.AACODONS)));
            add(new Pair<>("deflateNone", new DeflateSeqCompressor(DeflateSeqCompressor.DICT.NONE)));
        }};

        List<Pair<String, List<String>>> datasets = new ArrayList<>();
        String sql1 = """
            select seq_original_compressed, seq_aligned_compressed
            from y_main_sequence
            order by random()
            limit 100;
            """;
        String sql2 = """
            select data_compressed
            from y_main_sequence_columnar
            order by random()
            limit 50;
            """;
        String sql3 = """
            select data_compressed
            from y_main_aa_sequence_columnar
            order by random()
            limit 50;
            """;
        String sql4 = """
            select aa_seq
            from y_main_aa_sequence
            order by random()
            limit 2000;
            """;
        try (Connection conn = dbPool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql1)) {
                    List<String> aligned = new ArrayList<>();
                    List<String> original = new ArrayList<>();
                    while (rs.next()) {
                        aligned.add(seqCompressor.decompress(rs.getBytes("seq_aligned_compressed")));
                        original.add(seqCompressor.decompress(rs.getBytes("seq_original_compressed")));
                    }
                    datasets.add(new Pair<>("aligned", aligned));
                    datasets.add(new Pair<>("original", original));
                }
                try (ResultSet rs = statement.executeQuery(sql2)) {
                    List<String> columnar = new ArrayList<>();
                    while (rs.next()) {
                        columnar.add(nucColumnarCompressor.decompress(rs.getBytes("data_compressed")));
                    }
                    datasets.add(new Pair<>("nuc_columnar", columnar));
                }
                try (ResultSet rs = statement.executeQuery(sql3)) {
                    List<String> columnar = new ArrayList<>();
                    while (rs.next()) {
                        columnar.add(aaColumnarCompressor.decompress(rs.getBytes("data_compressed")));
                    }
                    datasets.add(new Pair<>("aa_columnar", columnar));
                }
                try (ResultSet rs = statement.executeQuery(sql4)) {
                    List<String> aaSeq = new ArrayList<>();
                    while (rs.next()) {
                        aaSeq.add(rs.getString("aa_seq"));
                    }
                    datasets.add(new Pair<>("aa_seq", aaSeq));
                }
            }
        }

        System.out.println("==== Test compression ====");

        Collections.shuffle(compressors);
        List<Pair<String, Map<String, List<byte[]>>>> datasets2 = new ArrayList<>();
        for (Pair<String, List<String>> dataset : datasets) {
            System.out.println("---- " + dataset.getValue0() + " ----");
            Map<String, List<byte[]>> compressedMap = new HashMap<>();
            for (Pair<String, SeqCompressor> compressor : compressors) {
                Instant start = Instant.now();
                List<Integer> sizes = new ArrayList<>();
                List<byte[]> compressedList = new ArrayList<>();
                for (String s : dataset.getValue1()) {
                    byte[] compressed = compressor.getValue1().compress(s);
                    compressedList.add(compressed);
                    sizes.add(compressed.length);
                }
                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                int averageSize = (int) sizes.stream().mapToDouble(a -> a).average().getAsDouble();
                System.out.println(compressor.getValue0() + ": t=" + timeElapsed + ", average size=" + averageSize);
                compressedMap.put(compressor.getValue0(), compressedList);
            }
            datasets2.add(new Pair<>(dataset.getValue0(), compressedMap));
        }


        System.out.println("\n\n==== Test decompression ====");
        for (Pair<String, Map<String, List<byte[]>>> dataset : datasets2) {
            System.out.println("---- " + dataset.getValue0() + " ----");
            for (Pair<String, SeqCompressor> compressor : compressors) {
                List<byte[]> compressedList = dataset.getValue1().get(compressor.getValue0());
                Instant start = Instant.now();
                for (byte[] compressed : compressedList) {
                    compressor.getValue1().decompress(compressed);
                }
                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                System.out.println(compressor.getValue0() + ": t=" + timeElapsed);
            }
        }
    }

}

package ch.ethz.lapis.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class DeflateSeqCompressorTest {

    @Test
    public void test() {
        DeflateSeqCompressor deflateSeqCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.ATCGNDEL);

        String input = "-----TTAATCCTAAGATCCCCCNNNNN";
        assertEquals(1, 1);
        byte[] compressed = deflateSeqCompressor.compress(input);
        String decompressed = deflateSeqCompressor.decompress(compressed);
        assertEquals(input, decompressed);
    }

}

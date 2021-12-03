package ch.ethz.lapis.util;

import ch.ethz.lapis.VariableSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SeqCompressorTest {

    public static Stream<Arguments> arguments = Stream.of(
        Arguments.of(new DeflateSeqCompressor(DeflateSeqCompressor.DICT.ATCGNDEL)),
        Arguments.of(new ZstdSeqCompressor(ZstdSeqCompressor.DICT.REFERENCE))
    );

    @ParameterizedTest
    @VariableSource("arguments")
    public void test(SeqCompressor seqCompressor) {
        String input = "-----TTAATCCTAAGATCCCCCNNNNN";
        assertEquals(1, 1);
        byte[] compressed = seqCompressor.compress(input);
        String decompressed = seqCompressor.decompress(compressed);
        assertEquals(input, decompressed);
    }

}

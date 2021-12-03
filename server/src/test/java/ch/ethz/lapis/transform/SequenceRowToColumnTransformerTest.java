package ch.ethz.lapis.transform;

import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;


public class SequenceRowToColumnTransformerTest {

    @Test
    public void test() {
        SequenceRowToColumnTransformer transformer = new SequenceRowToColumnTransformer(
            2,
            5
        );
        SeqCompressor compressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE);
        List<String> sequences = new ArrayList<>() {{
            add("ABCDEFGHIJKLMNOP");
            add("abcdefghijklmnop");
            add("0123456789abcdef");
        }};
        List<byte[]> compressedSequences = sequences.stream().map(compressor::compress).collect(Collectors.toList());
        LinkedBlockingQueue<Pair<Integer, List<byte[]>>> resultQueue = new LinkedBlockingQueue<>();
        transformer.transform(
            compressedSequences,
            compressor::decompress,
            (pos, result) -> {
                resultQueue.add(new Pair<>(pos, result));
            },
            compressor::compress,
            '?'
        );
        List<String> results = resultQueue.stream()
            .map(p -> new Pair<>(
                p.getValue0(),
                p.getValue1().stream()
                    .map(compressor::decompress)
                    .collect(Collectors.toList()))
            )
            .sorted(Comparator.comparingInt(Pair::getValue0))
            .map(Pair::getValue1)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        Assertions.assertEquals(
            results,
            Arrays.asList("Aa0", "Bb1", "Cc2", "Dd3", "Ee4", "Ff5", "Gg6", "Hh7", "Ii8", "Jj9", "Kka", "Llb", "Mmc",
                "Nnd", "Ooe", "Ppf")
        );
    }

}

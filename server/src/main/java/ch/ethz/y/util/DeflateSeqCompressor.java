package ch.ethz.y.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class DeflateSeqCompressor implements SeqCompressor {

    public enum DICT {
        REFERENCE,
        ATCGNDEL,
        AASEQ,
        AACODONS
    }

    private static final int bufferSize = 5000000;
    private final byte[] dict;

    public DeflateSeqCompressor(DICT dictionary) {
        String name = switch (dictionary) {
            case REFERENCE -> "/reference-dictionary.txt";
            case ATCGNDEL -> "/simple-ATCGNdel-dictionary.txt";
            case AASEQ -> "/aa-seq-dictionary.txt";
            case AACODONS -> "/simple-AAcodons-dictionary.txt";
        };
        //Read File Content
        try {
            InputStream in = getClass().getResourceAsStream(name);
            this.dict = in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] compress(String seq) {
        byte[] input = seq.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[bufferSize];
        Deflater deflater = new Deflater(9);
        deflater.setInput(input);
        deflater.setDictionary(dict);
        deflater.finish();
        int compressedDataLength = deflater.deflate(output);
        return Arrays.copyOfRange(output, 0, compressedDataLength);
    }

    @Override
    public String decompress(byte[] compressed) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(compressed);
            byte[] result = new byte[bufferSize];
            inflater.inflate(result);
            inflater.setDictionary(dict);
            int resultLength = inflater.inflate(result);
            inflater.end();
            return new String(result, 0, resultLength, StandardCharsets.UTF_8);
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
    }

}

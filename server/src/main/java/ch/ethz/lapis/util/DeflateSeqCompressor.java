package ch.ethz.lapis.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class DeflateSeqCompressor implements SeqCompressor {

    public enum DICT {
        REFERENCE,
        ATCGNDEL,
        AASEQ,
        AACODONS,
        NONE
    }

    private static final int bufferSize = 5000000;
    private final byte[] dict;

    public DeflateSeqCompressor(DICT dictionary) {
        String name = switch (dictionary) {
            case REFERENCE -> "/reference-dictionary.txt";
            case ATCGNDEL -> "/simple-ATCGNdel-dictionary.txt";
            case AASEQ -> "/aa-seq-dictionary.txt";
            case AACODONS -> "/simple-AAcodons-dictionary.txt";
            case NONE -> null;
        };
        if (name == null) {
            this.dict = null;
            return;
        }
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
        byte[] buf = new byte[bufferSize];
        Deflater deflater = new Deflater(9);
        deflater.setInput(input);
        if (dict != null) {
            deflater.setDictionary(dict);
        }
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (!deflater.finished()) {
            int count = deflater.deflate(buf);
            out.write(buf, 0, count);
        }
        return out.toByteArray();
    }

    @Override
    public String decompress(byte[] compressed) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(compressed);
            byte[] buf = new byte[bufferSize];
            if (dict != null) {
                inflater.inflate(buf);
                inflater.setDictionary(dict);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (!inflater.finished()) {
                int count = inflater.inflate(buf);
                out.write(buf, 0, count);
            }
            inflater.end();
            return out.toString(StandardCharsets.UTF_8);
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
    }

}

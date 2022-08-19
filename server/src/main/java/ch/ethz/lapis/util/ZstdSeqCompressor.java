package ch.ethz.lapis.util;

import com.github.luben.zstd.Zstd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ZstdSeqCompressor implements SeqCompressor {

    private final byte[] dict;

    public ZstdSeqCompressor(DICT dictionary) {
        String name = switch (dictionary) {
            case REFERENCE -> "/reference-dictionary.txt";
            case AA_REFERENCE -> "/aa-seq-dictionary.txt";
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
        return compressBytes(seq.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decompress(byte[] compressed) {
        int decompressedSize = (int) Zstd.decompressedSize(compressed);
        byte[] decompressedBuffer = new byte[decompressedSize];
        long decompressionReturnCode;
        if (dict == null) {
            decompressionReturnCode = Zstd.decompress(decompressedBuffer, compressed);
        } else {
            decompressionReturnCode = Zstd.decompress(decompressedBuffer, compressed, dict);
        }
        if (Zstd.isError(decompressionReturnCode)) {
            throw new RuntimeException("Zstd decompression failed: error code " + decompressionReturnCode);
        }
        return new String(decompressedBuffer, 0, (int) decompressionReturnCode, StandardCharsets.UTF_8);
    }

    public byte[] compressBytes(byte[] input) {
        int compressBound = (int) Zstd.compressBound(input.length);
        byte[] outputBuffer = new byte[compressBound];
        long compressionReturnCode;
        if (dict == null) {
            compressionReturnCode = Zstd.compress(outputBuffer, input, 3);
        } else {
            compressionReturnCode = Zstd.compress(outputBuffer, input, dict, 3);
        }
        if (Zstd.isError(compressionReturnCode)) {
            throw new RuntimeException("Zstd compression failed: error code " + compressionReturnCode);
        }
        return Arrays.copyOfRange(outputBuffer, 0, (int) compressionReturnCode);
    }

    public byte[] decompressBytes(byte[] compressed) {
        int decompressedSize = (int) Zstd.decompressedSize(compressed);
        byte[] decompressedBuffer = new byte[decompressedSize];
        long decompressionReturnCode;
        if (dict == null) {
            decompressionReturnCode = Zstd.decompress(decompressedBuffer, compressed);
        } else {
            decompressionReturnCode = Zstd.decompress(decompressedBuffer, compressed, dict);
        }
        if (Zstd.isError(decompressionReturnCode)) {
            throw new RuntimeException("Zstd decompression failed: error code " + decompressionReturnCode);
        }
        return Arrays.copyOfRange(decompressedBuffer, 0, (int) decompressionReturnCode);
    }

    public enum DICT {
        REFERENCE,
        AA_REFERENCE,
        NONE
    }
}

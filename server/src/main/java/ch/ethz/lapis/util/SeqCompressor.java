package ch.ethz.lapis.util;

public interface SeqCompressor {

    byte[] compress(String seq);

    String decompress(byte[] compressed);

}

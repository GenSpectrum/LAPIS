package ch.ethz.y.util;

public interface SeqCompressor {

    byte[] compress(String seq);

    String decompress(byte[] compressed);

}

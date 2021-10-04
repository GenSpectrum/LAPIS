package ch.ethz.lapis.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

public class XZSeqCompressor implements SeqCompressor {

    @Override
    public byte[] compress(String seq) {
        try {
            if (seq == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XZOutputStream stream = new XZOutputStream(out, new LZMA2Options(LZMA2Options.PRESET_MAX));
            stream.write(seq.getBytes());
            stream.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String decompress(byte[] compressed) {
        try {
            if (compressed == null) {
                return null;
            }
            XZInputStream xzInputStream = new XZInputStream(new ByteArrayInputStream(compressed));
            byte firstByte = (byte) xzInputStream.read();
            byte[] buffer = new byte[xzInputStream.available()];
            buffer[0] = firstByte;
            xzInputStream.read(buffer, 1, buffer.length - 2);
            xzInputStream.close();
            return new String(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

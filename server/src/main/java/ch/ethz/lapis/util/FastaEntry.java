package ch.ethz.lapis.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public record FastaEntry(String sampleName, String sequence) {
    public void writeToStream(OutputStream outputStream) {
        try {
            outputStream.write(">".getBytes(StandardCharsets.UTF_8));
            outputStream.write(sampleName.getBytes(StandardCharsets.UTF_8));
            outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write(sequence.getBytes(StandardCharsets.UTF_8));
            outputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

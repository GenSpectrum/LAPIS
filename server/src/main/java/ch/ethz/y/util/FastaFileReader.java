package ch.ethz.y.util;

import org.tukaani.xz.XZInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;

public class FastaFileReader implements Iterator<FastaEntry>, Iterable<FastaEntry>, AutoCloseable {

    private final BufferedReader reader;
    private final BufferedInputStream fileIn;
    private final XZInputStream decompressedIn;
    private FastaEntry nextEntry;
    private String nextLine = "";

    public FastaFileReader(Path filePath, boolean isCompressed) {
        try {
            this.fileIn = new BufferedInputStream(new FileInputStream(filePath.toFile()));
            if (isCompressed) {
                this.decompressedIn = new XZInputStream(fileIn);
                this.reader = new BufferedReader(new InputStreamReader(decompressedIn, StandardCharsets.UTF_8));
            } else {
                this.decompressedIn = null;
                this.reader = new BufferedReader(new InputStreamReader(fileIn));
            }
            read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return nextEntry != null;
    }

    @Override
    public FastaEntry next() {
        try {
            FastaEntry entry = nextEntry;
            read();
            return entry;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void read() throws IOException {
        String sampleName = null;
        StringBuilder seq = new StringBuilder();
        while (true) {
            if (nextLine == null) {
                break;
            }
            if (nextLine.isBlank()) {
                nextLine = reader.readLine();
                continue;
            }
            if (nextLine.startsWith(">")) {
                if (sampleName == null) {
                    sampleName = nextLine.substring(1);
                } else {
                    break;
                }
            } else {
                seq.append(nextLine);
            }
            nextLine = reader.readLine();
        }
        if (sampleName == null) {
            nextEntry = null;
        } else {
            nextEntry = new FastaEntry(sampleName, seq.toString());
        }
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (fileIn != null) {
            fileIn.close();
        }
        if (decompressedIn != null) {
            decompressedIn.close();
        }
    }

    @Override
    public Iterator<FastaEntry> iterator() {
        return this;
    }
}

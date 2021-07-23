package ch.ethz.y.source;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NextstrainGenbankNextcladeFileReader
        implements Iterator<NextstrainGenbankNextcladeEntry>, Iterable<NextstrainGenbankNextcladeEntry>, AutoCloseable {

    private final Iterator<CSVRecord> iterator;
    private final InputStream in;

    public NextstrainGenbankNextcladeFileReader(InputStream in) {
        this.in = in;
        try {
            CSVFormat format = CSVFormat.TDF
                    .withFirstRecordAsHeader();
            CSVParser parser = CSVParser.parse(in, StandardCharsets.UTF_8, format);
            iterator = parser.iterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public NextstrainGenbankNextcladeEntry next() {
        CSVRecord csv = iterator.next();
        String aaMutations = "";
        String aaSubs = csv.get("aaSubstitutions");
        String aaDels = csv.get("aaDeletions");
        if (!aaSubs.isBlank() && !aaDels.isBlank()) {
            aaMutations = aaSubs + "," + aaDels;
        } else if (!aaSubs.isBlank() && aaDels.isBlank()) {
            aaMutations = aaSubs;
        } else if (aaSubs.isBlank() && !aaDels.isBlank()) {
            aaMutations = aaDels;
        }
        return new NextstrainGenbankNextcladeEntry()
                .setStrain(csv.get("seqName"))
                .setAaMutations(aaMutations)
                .setNucSubstitutions(csv.get("substitutions"))
                .setNucDeletions(csv.get("deletions"))
                .setNucInsertions(csv.get("insertions"));
    }

    @Override
    public Iterator<NextstrainGenbankNextcladeEntry> iterator() {
        return this;
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }
}

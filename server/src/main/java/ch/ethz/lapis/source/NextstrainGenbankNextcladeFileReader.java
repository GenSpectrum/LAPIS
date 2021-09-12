package ch.ethz.lapis.source;

import ch.ethz.lapis.util.ReferenceGenomeData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        ReferenceGenomeData refData = ReferenceGenomeData.getInstance();
        String nucDels = Arrays.stream(csv.get("deletions").split(","))
                .flatMap(nucDel -> {
                    // Nextclade reports the deletions either as a single position (e.g., 28254) or as a range
                    // (e.g., 22029-22034). We transform it into "22029-,22030-,22031-" ...
                    Stream<Integer> posStream;
                    if (!nucDel.contains("-")) {
                        int pos = Integer.parseInt(nucDel);
                        posStream = Stream.of(pos);
                    } else {
                        String[] range = nucDel.split("-");
                        int rangeFrom = Integer.parseInt(range[0]);
                        int rangeTo = Integer.parseInt(range[1]);
                        posStream = IntStream.range(rangeFrom, rangeTo + 1).boxed();
                    }
                    return posStream.map(pos -> Character.toString(refData.getNucleotideBase(pos)) + pos + "-");
                })
                .collect(Collectors.joining(","));
        return new NextstrainGenbankNextcladeEntry()
                .setStrain(csv.get("seqName"))
                .setAaMutations(aaMutations)
                .setNucSubstitutions(csv.get("substitutions"))
                .setNucDeletions(nucDels)
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

package ch.ethz.lapis.source.covlineages;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class PangolinAssignmentFileReader
    implements Iterator<PangolinAssignmentEntry>, Iterable<PangolinAssignmentEntry>, AutoCloseable{

    private final Iterator<CSVRecord> iterator;
    private final InputStream in;

    public PangolinAssignmentFileReader(InputStream in) {
        this.in = in;
        try {
            CSVFormat format = CSVFormat.DEFAULT
                .builder()
                .setHeader().setSkipHeaderRecord(true)
                .build();
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
    public PangolinAssignmentEntry next() {
        CSVRecord csv = iterator.next();
        String gisaidEpiIsl = csv.get("gisaid_accession");
        String pangoLineage = csv.get("lineage");
        return new PangolinAssignmentEntry()
            .setGisaidEpiIsl(gisaidEpiIsl)
            .setPangoLineage(pangoLineage);
    }

    @Override
    public Iterator<PangolinAssignmentEntry> iterator() {
        return this;
    }

    @Override
    public void close() throws Exception {
        if (in != null) {
            in.close();
        }
    }
}

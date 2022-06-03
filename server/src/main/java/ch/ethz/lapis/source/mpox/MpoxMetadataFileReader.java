package ch.ethz.lapis.source.mpox;

import ch.ethz.lapis.core.Utils;
import ch.ethz.lapis.util.ParsedDate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class MpoxMetadataFileReader
    implements Iterator<MpoxMetadataEntry>, Iterable<MpoxMetadataEntry>, AutoCloseable {

    private final Iterator<CSVRecord> iterator;
    private final InputStream in;

    public MpoxMetadataFileReader(InputStream in) {
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
    public MpoxMetadataEntry next() {
        CSVRecord csv = iterator.next();
        ParsedDate parsedDate = ParsedDate.parse(optionalGetFromCsv(csv, "date"));
        MpoxMetadataEntry entry = new MpoxMetadataEntry()
            .setStrain(cleanString(csv.get("strain")))
            .setSraAccession(cleanString(optionalGetFromCsv(csv, "accession")))
            .setDateOriginal(cleanString(optionalGetFromCsv(csv, "date")))
            .setDateSubmitted(Utils.nullableLocalDateValue(cleanString(optionalGetFromCsv(csv, "date_submitted"))))
            .setRegion(cleanString(optionalGetFromCsv(csv, "region")))
            .setCountry(cleanString(optionalGetFromCsv(csv, "country")))
            .setHost(cleanString(optionalGetFromCsv(csv, "host")))
            .setClade(cleanString(optionalGetFromCsv(csv, "clade")))
            .setAuthors(cleanString(optionalGetFromCsv(csv, "authors")));
        if (parsedDate != null) {
            entry
                .setDate(parsedDate.getDate())
                .setYear(parsedDate.getYear())
                .setMonth(parsedDate.getMonth())
                .setDay(parsedDate.getDay());
        }
        return entry;
    }

    @Override
    public Iterator<MpoxMetadataEntry> iterator() {
        return this;
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }

    private String cleanString(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        s = s.trim();
        if ("?".equals(s) || "unknown".equalsIgnoreCase(s) || "NA".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }

    private String optionalGetFromCsv(CSVRecord csvRecord, String field) {
        try {
            return csvRecord.get(field);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

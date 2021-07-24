package ch.ethz.lapis.source;

import ch.ethz.lapis.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NextstrainGenbankMetadataFileReader
        implements Iterator<NextstrainGenbankMetadataEntry>, Iterable<NextstrainGenbankMetadataEntry>, AutoCloseable {

    private final Iterator<CSVRecord> iterator;
    private final InputStream in;

    public NextstrainGenbankMetadataFileReader(InputStream in) {
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
    public NextstrainGenbankMetadataEntry next() {
        CSVRecord csv = iterator.next();
        return new NextstrainGenbankMetadataEntry()
                .setStrain(cleanString(csv.get("strain")))
                .setVirus(cleanString(csv.get("gisaid_epi_isl")))
                .setGisaidEpiIsl(cleanString(csv.get("gisaid_epi_isl")))
                .setGenbankAccession(cleanString(csv.get("genbank_accession")))
                .setSraAccession(cleanString(csv.get("sra_accession")))
                .setDateOriginal(cleanString(csv.get("date")))
                .setDate(Utils.nullableLocalDateValue(csv.get("date")))
                .setRegion(cleanString(csv.get("region")))
                .setCountry(cleanString(csv.get("country")))
                .setDivision(cleanString(csv.get("division")))
                .setLocation(cleanString(csv.get("location")))
                .setRegionExposure(cleanString(csv.get("region_exposure")))
                .setCountryExposure(cleanString(csv.get("country_exposure")))
                .setDivisionExposure(cleanString(csv.get("division_exposure")))
                .setHost(cleanString(csv.get("host")))
                .setAge(Utils.nullableIntegerValue(csv.get("age")))
                .setSex(cleanString(csv.get("sex")))
                .setNextstrainClade(cleanString(csv.get("Nextstrain_clade")))
                .setPangoLineage(cleanString(csv.get("pango_lineage")))
                .setGisaidClade(cleanString(csv.get("GISAID_clade")))
                .setOriginatingLab(cleanString(csv.get("originating_lab")))
                .setSubmittingLab(cleanString(csv.get("submitting_lab")))
                .setAuthors(cleanString(csv.get("authors")))
                .setDateSubmitted(Utils.nullableLocalDateValue(csv.get("date_submitted")))
                .setSamplingStrategy(cleanString(csv.get("sampling_strategy")));
    }

    @Override
    public Iterator<NextstrainGenbankMetadataEntry> iterator() {
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
        if ("?".equals(s) || "unknown".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }
}

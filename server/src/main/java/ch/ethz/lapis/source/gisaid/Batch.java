package ch.ethz.lapis.source.gisaid;

import java.util.List;


public class Batch {

    private final List<GisaidEntry> entries;

    public Batch(List<GisaidEntry> entries) {
        this.entries = entries;
    }

    public List<GisaidEntry> getEntries() {
        return entries;
    }
}

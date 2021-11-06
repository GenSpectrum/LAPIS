package ch.ethz.lapis.transform;

public class PangoLineageEntry {

    private String sequenceName;
    private String pangoLineage;
    private String pangoLineageUsher;

    public PangoLineageEntry() {
    }

    public PangoLineageEntry(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public PangoLineageEntry setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
        return this;
    }

    public String getPangoLineage() {
        return pangoLineage;
    }

    public PangoLineageEntry setPangoLineage(String pangoLineage) {
        this.pangoLineage = pangoLineage;
        return this;
    }

    public String getPangoLineageUsher() {
        return pangoLineageUsher;
    }

    public PangoLineageEntry setPangoLineageUsher(String pangoLineageUsher) {
        this.pangoLineageUsher = pangoLineageUsher;
        return this;
    }
}

package ch.ethz.lapis.api.entity.query;

public class PangoQuery implements VariantQueryExpr {

    private final String pangoLineage;

    private final boolean includeSubLineage;

    public PangoQuery(String pangoLineage, boolean includeSubLineage) {
        this.pangoLineage = pangoLineage;
        this.includeSubLineage = includeSubLineage;
    }

    public String getPangoLineage() {
        return pangoLineage;
    }

    public boolean isIncludeSubLineage() {
        return includeSubLineage;
    }

    @Override
    public String toString() {
        return "PangoQuery{" +
            "pangoLineage='" + pangoLineage + '\'' +
            ", includeSubLineage=" + includeSubLineage +
            '}';
    }
}

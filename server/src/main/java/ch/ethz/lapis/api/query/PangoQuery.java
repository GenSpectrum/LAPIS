package ch.ethz.lapis.api.query;

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

    @Override
    public boolean[] evaluate(DataStore dataStore) {
        // TODO Include sub-lineages if requested
        String pangoLineage = this.pangoLineage.toUpperCase();
        String[] data = dataStore.getPangoLineageArray();
        boolean[] result = new boolean[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = pangoLineage.equals(data[i]);
        }
        return result;
    }
}

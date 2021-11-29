package ch.ethz.lapis.api.query;

import ch.ethz.lapis.util.PangoLineageQueryConverter;

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
        String pangoLineage = this.pangoLineage.toUpperCase();
        if (includeSubLineage) {
            pangoLineage += "*";
        }
        PangoLineageQueryConverter queryConverter = dataStore.getPangoLineageQueryConverter();
        PangoLineageQueryConverter.PangoLineageQueryMatch match = queryConverter.convert(pangoLineage);

        String[] data = dataStore.getPangoLineageArray();
        boolean[] result = new boolean[data.length];

        for (int i = 0; i < result.length; i++) {
            boolean r = false;
            String d = data[i];
            if (d != null) {
                for (String s : match.getExact()) {
                    r = r || d.equals(s);
                }
                for (String s : match.getPrefix()) {
                    r = r || d.startsWith(s);
                }
            }
            result[i] = r;
        }
        return result;
    }
}

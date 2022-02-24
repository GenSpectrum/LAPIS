package ch.ethz.lapis.api.query;

import ch.ethz.lapis.util.PangoLineageQueryConverter;
import java.util.BitSet;

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
    public BitSet evaluate(Database database) {
        String pangoLineage = this.pangoLineage.toUpperCase();
        if (includeSubLineage) {
            pangoLineage += "*";
        }
        PangoLineageQueryConverter queryConverter = database.getPangoLineageQueryConverter();
        PangoLineageQueryConverter.PangoLineageQueryMatch match = queryConverter.convert(pangoLineage);

        String[] data = database.getStringColumn(Database.Columns.PANGO_LINEAGE);
        BitSet result = new BitSet(data.length);

        for (int i = 0; i < data.length; i++) {
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
            result.set(i, r);
        }
        return result;
    }
}

package ch.ethz.lapis.api.query;

import ch.ethz.lapis.util.PangoLineageQueryConverter;
import java.util.function.Consumer;

public class PangoQuery implements VariantQueryExpr {

    private final String pangoLineage;

    private final boolean includeSubLineage;

    private final String columnName;

    public PangoQuery(String pangoLineage, boolean includeSubLineage, String columnName) {
        this.pangoLineage = pangoLineage;
        this.includeSubLineage = includeSubLineage;
        if (!columnName.equals(Database.Columns.PANGO_LINEAGE)
            && !columnName.equals(Database.Columns.NEXTCLADE_PANGO_LINEAGE)) {
            throw new RuntimeException("Column " + columnName + " is not known for containing a Pango lineage.");
        }
        this.columnName = columnName;
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
    public boolean[] evaluate(Database database) {
        String pangoLineage = this.pangoLineage.toUpperCase();
        if (includeSubLineage) {
            pangoLineage += "*";
        }
        PangoLineageQueryConverter queryConverter = database.getPangoLineageQueryConverter();
        PangoLineageQueryConverter.PangoLineageQueryMatch match = queryConverter.convert(pangoLineage);

        String[] data = database.getStringColumn(columnName);
        boolean[] result = new boolean[data.length];

        for (int i = 0; i < result.length; i++) {
            boolean r = false;
            String d = data[i];
            if (d != null) {
                for (String s : match.exact()) {
                    r = r || d.equals(s);
                }
                for (String s : match.prefix()) {
                    r = r || d.startsWith(s);
                }
            }
            result[i] = r;
        }
        return result;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void traverseDFS(Consumer<QueryExpr> callback) {
        callback.accept(this);
    }
}

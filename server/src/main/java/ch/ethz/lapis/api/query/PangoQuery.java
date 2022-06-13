package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.exception.BadRequestException;
import ch.ethz.lapis.util.PangoLineageQueryConverter;

public class PangoQuery implements VariantQueryExpr {

    private final String pangoLineage;

    private final boolean includeSubLineage;

    private final String columnName;

    public PangoQuery(String pangoLineage, boolean includeSubLineage, String columnName) {
        throw new BadRequestException("This operation is not supported.");
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

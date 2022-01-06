package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.query2.Database;
import ch.ethz.lapis.api.query2.StringValue;

public class GisaidClade implements VariantQueryExpr {

    private final String clade;

    public GisaidClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

    @Override
    public boolean[] evaluate(DataStore dataStore) {
        String clade = this.clade.toUpperCase();
        String[] data = dataStore.getGisaidCladeArray();
        boolean[] result = new boolean[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = clade.equals(data[i]);
        }
        return result;
    }

    @Override
    public boolean[] evaluate2(Database database) {
        return new StringValue(clade, Database.Columns.GISAID_CLADE, false).evaluate2(database);
    }
}

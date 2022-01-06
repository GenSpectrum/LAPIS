package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.query2.Database;

public class Single implements VariantQueryExpr {
    private VariantQueryExpr value;

    @Override
    public void putValue(VariantQueryExpr value) {
        this.value = value;
    }

    @Override
    public boolean[] evaluate(DataStore dataStore) {
        return value.evaluate(dataStore);
    }

    @Override
    public boolean[] evaluate2(Database database) {
        return value.evaluate2(database);
    }

    public VariantQueryExpr getValue() {
        return value;
    }

}

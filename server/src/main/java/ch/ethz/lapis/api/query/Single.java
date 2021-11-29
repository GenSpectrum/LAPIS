package ch.ethz.lapis.api.query;

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

    public VariantQueryExpr getValue() {
        return value;
    }
}

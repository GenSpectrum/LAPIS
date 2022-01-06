package ch.ethz.lapis.api.query;

public class Single implements VariantQueryExpr {
    private VariantQueryExpr value;

    @Override
    public void putValue(VariantQueryExpr value) {
        this.value = value;
    }

    @Override
    public boolean[] evaluate(Database database) {
        return value.evaluate(database);
    }

    public VariantQueryExpr getValue() {
        return value;
    }

}

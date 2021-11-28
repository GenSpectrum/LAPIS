package ch.ethz.lapis.api.entity.query;

public class Single implements VariantQueryExpr {
    private VariantQueryExpr value;

    @Override
    public void putValue(VariantQueryExpr value) {
        this.value = value;
    }

    public VariantQueryExpr getValue() {
        return value;
    }
}

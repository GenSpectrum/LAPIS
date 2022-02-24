package ch.ethz.lapis.api.query;

import java.util.BitSet;

public class Single implements VariantQueryExpr {
    private VariantQueryExpr value;

    @Override
    public void putValue(VariantQueryExpr value) {
        this.value = value;
    }

    @Override
    public BitSet evaluate(Database database) {
        return value.evaluate(database);
    }

    public VariantQueryExpr getValue() {
        return value;
    }

}

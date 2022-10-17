package ch.ethz.lapis.api.query;

import java.util.function.Consumer;

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

    @Override
    public Object clone() throws CloneNotSupportedException {
        Single o = (Single) super.clone();
        o.value = (VariantQueryExpr) value.clone();
        return o;
    }

    @Override
    public void traverseDFS(Consumer<QueryExpr> callback) {
        callback.accept(this);
        value.traverseDFS(callback);
    }

    @Override
    public String toQueryString() {
        return getValue().toQueryString();
    }
}

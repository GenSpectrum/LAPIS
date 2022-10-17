package ch.ethz.lapis.api.query;

import java.util.function.Consumer;

public record GisaidClade(String clade) implements VariantQueryExpr {
    @Override
    public boolean[] evaluate(Database database) {
        return new StringValue(clade, Database.Columns.GISAID_CLADE, false).evaluate(database);
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

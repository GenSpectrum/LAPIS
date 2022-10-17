package ch.ethz.lapis.api.query;

import java.util.function.Consumer;

public record NextstrainClade(String clade) implements VariantQueryExpr {
    @Override
    public boolean[] evaluate(Database database) {
        return new StringValue(clade, Database.Columns.NEXTSTRAIN_CLADE, false).evaluate(database);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void traverseDFS(Consumer<QueryExpr> callback) {
        callback.accept(this);
    }

    @Override
    public String toQueryString() {
        return "nextstrainClade:" + clade;
    }
}

package ch.ethz.lapis.api.query;

public record NextstrainClade(String clade) implements VariantQueryExpr {
    @Override
    public boolean[] evaluate(Database database) {
        return new StringValue(clade, Database.Columns.NEXTSTRAIN_CLADE, false).evaluate(database);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

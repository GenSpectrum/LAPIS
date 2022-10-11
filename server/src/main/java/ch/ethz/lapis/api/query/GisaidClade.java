package ch.ethz.lapis.api.query;

public record GisaidClade(String clade) implements VariantQueryExpr {
    @Override
    public boolean[] evaluate(Database database) {
        return new StringValue(clade, Database.Columns.GISAID_CLADE, false).evaluate(database);
    }
}

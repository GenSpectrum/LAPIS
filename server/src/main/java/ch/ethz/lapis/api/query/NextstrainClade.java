package ch.ethz.lapis.api.query;

public class NextstrainClade implements VariantQueryExpr {

    private final String clade;

    public NextstrainClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

    @Override
    public boolean[] evaluate(Database database) {
        return new StringValue(clade, Database.Columns.NEXTSTRAIN_CLADE, false).evaluate(database);
    }
}

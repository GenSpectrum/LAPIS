package ch.ethz.lapis.api.query;

public class GisaidClade implements VariantQueryExpr {

    private final String clade;

    public GisaidClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

    @Override
    public boolean[] evaluate(Database database) {
        return new StringValue(clade, Database.Columns.GISAID_CLADE, false).evaluate(database);
    }
}

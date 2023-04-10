package ch.ethz.lapis.api.query;

public class Single implements QueryExpr {
    private QueryExpr value;

    @Override
    public void putValue(QueryExpr value) {
        this.value = value;
    }

    @Override
    public boolean[] evaluate(Database database) {
        return value.evaluate(database);
    }

    public QueryExpr getValue() {
        return value;
    }

}

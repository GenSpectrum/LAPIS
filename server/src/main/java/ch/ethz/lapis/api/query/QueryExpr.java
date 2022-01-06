package ch.ethz.lapis.api.query;

public interface QueryExpr {

    boolean[] evaluate(Database database);

}

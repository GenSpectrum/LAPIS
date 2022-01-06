package ch.ethz.lapis.api.query2;

public interface QueryExpr {

    boolean[] evaluate2(Database database);

}

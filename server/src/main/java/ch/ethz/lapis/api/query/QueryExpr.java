package ch.ethz.lapis.api.query;

public interface QueryExpr extends Cloneable {

    boolean[] evaluate(Database database);

    Object clone() throws CloneNotSupportedException;

}

package ch.ethz.lapis.api.query;

import java.util.function.Consumer;

public interface QueryExpr extends Cloneable {

    boolean[] evaluate(Database database);

    Object clone() throws CloneNotSupportedException;

    void traverseDFS(Consumer<QueryExpr> callback);

}

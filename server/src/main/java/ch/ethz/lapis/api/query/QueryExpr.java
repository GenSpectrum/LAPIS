package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.exception.MalformedVariantQueryException;

public interface QueryExpr {

    boolean[] evaluate(Database database);

    default void putValue(QueryExpr value) {
        throw new MalformedVariantQueryException();
    }

}

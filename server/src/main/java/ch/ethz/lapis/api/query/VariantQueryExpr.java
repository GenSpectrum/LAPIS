package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.exception.MalformedVariantQueryException;
import ch.ethz.lapis.api.query2.QueryExpr;

public interface VariantQueryExpr extends QueryExpr {

    default void putValue(VariantQueryExpr value) {
        throw new MalformedVariantQueryException();
    };

    boolean[] evaluate(DataStore dataStore);

}

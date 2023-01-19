package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.exception.MalformedVariantQueryException;

public interface VariantQueryExpr extends QueryExpr {

    default void putValue(VariantQueryExpr value) {
        throw new MalformedVariantQueryException();
    }

}

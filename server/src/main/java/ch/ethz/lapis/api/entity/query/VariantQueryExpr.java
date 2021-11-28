package ch.ethz.lapis.api.entity.query;

import ch.ethz.lapis.api.exception.MalformedVariantQueryException;

public interface VariantQueryExpr {

    default void putValue(VariantQueryExpr value) {
        throw new MalformedVariantQueryException();
    };

}

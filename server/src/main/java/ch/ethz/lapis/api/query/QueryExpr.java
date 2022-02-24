package ch.ethz.lapis.api.query;

import java.util.BitSet;

public interface QueryExpr {

    BitSet evaluate(Database database);

}

package ch.ethz.lapis.api.query;

import java.util.BitSet;

public class Negation extends Single {

    @Override
    public BitSet evaluate(Database database) {
        BitSet childEvaluated = super.evaluate(database);
        childEvaluated.flip(0, database.size());
        return childEvaluated;
    }
}

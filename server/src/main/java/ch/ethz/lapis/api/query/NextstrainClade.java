package ch.ethz.lapis.api.query;

import java.util.BitSet;

public class NextstrainClade implements VariantQueryExpr {

    private final String clade;

    public NextstrainClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

    @Override
    public BitSet evaluate(Database database) {
        return new StringValue(clade, Database.Columns.NEXTSTRAIN_CLADE, false).evaluate(database);
    }
}

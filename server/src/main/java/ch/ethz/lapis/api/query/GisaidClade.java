package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.exception.BadRequestException;

public class GisaidClade implements VariantQueryExpr {

    private final String clade;

    public GisaidClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

    @Override
    public boolean[] evaluate(Database database) {
        throw new BadRequestException("This operation is not supported.");
    }
}

package ch.ethz.lapis.api.entity.query;

public class NextstrainClade implements VariantQueryExpr {

    private final String clade;

    public NextstrainClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

}

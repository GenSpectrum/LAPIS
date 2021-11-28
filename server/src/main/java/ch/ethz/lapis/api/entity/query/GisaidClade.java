package ch.ethz.lapis.api.entity.query;

public class GisaidClade implements VariantQueryExpr {

    private final String clade;

    public GisaidClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

}

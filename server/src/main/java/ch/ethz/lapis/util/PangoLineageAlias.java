package ch.ethz.lapis.util;

public class PangoLineageAlias {

    private final String alias;
    private final String fullName;

    public PangoLineageAlias(String alias, String fullName) {
        this.alias = alias;
        this.fullName = fullName;
    }

    public String getAlias() {
        return alias;
    }

    public String getFullName() {
        return fullName;
    }
}

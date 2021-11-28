package ch.ethz.lapis.api.query;

public class NextstrainClade implements VariantQueryExpr {

    private final String clade;

    public NextstrainClade(String clade) {
        this.clade = clade;
    }

    public String getClade() {
        return clade;
    }

    @Override
    public boolean[] evaluate(DataStore dataStore) {
        String clade = this.clade.toUpperCase();
        String[] data = dataStore.getNextstrainCladeArray();
        boolean[] result = new boolean[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = clade.equals(data[i]);
        }
        return result;
    }
}

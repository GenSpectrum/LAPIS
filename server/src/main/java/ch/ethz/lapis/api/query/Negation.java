package ch.ethz.lapis.api.query;

public class Negation extends Single {

    @Override
    public boolean[] evaluate(DataStore dataStore) {
        boolean[] childEvaluated = super.evaluate(dataStore);
        boolean[] result = new boolean[childEvaluated.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = !childEvaluated[i];
        }
        return result;
    }
}

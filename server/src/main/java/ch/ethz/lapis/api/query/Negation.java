package ch.ethz.lapis.api.query;

public class Negation extends Single {

    @Override
    public boolean[] evaluate(Database database) {
        boolean[] childEvaluated = super.evaluate(database);
        boolean[] result = new boolean[childEvaluated.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = !childEvaluated[i];
        }
        return result;
    }

    @Override
    public String toQueryString() {
        return "!(" + getValue().toQueryString() + ")";
    }
}

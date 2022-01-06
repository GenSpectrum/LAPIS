package ch.ethz.lapis.api.query;

public class StringValue implements QueryExpr {

    private final String value;
    private final String columnName;
    private final boolean caseSensitive;

    public StringValue(String value, String columnName, boolean caseSensitive) {
        this.value = value;
        this.columnName = columnName;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean[] evaluate(Database database) {
        String[] data = database.getStringColumn(columnName);
        boolean[] result = new boolean[data.length];
        if (caseSensitive) {
            for (int i = 0; i < result.length; i++) {
                result[i] = value.equals(data[i]);
            }
        } else {
            for (int i = 0; i < result.length; i++) {
                result[i] = value.equalsIgnoreCase(data[i]);
            }
        }
        return result;
    }
}

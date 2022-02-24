package ch.ethz.lapis.api.query;

import java.util.BitSet;

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
    public BitSet evaluate(Database database) {
        String[] data = database.getStringColumn(columnName);
        BitSet result = new BitSet(data.length);
        if (caseSensitive) {
            for (int i = 0; i < data.length; i++) {
                result.set(i, value.equals(data[i]));
            }
        } else {
            for (int i = 0; i < data.length; i++) {
                result.set(i, value.equalsIgnoreCase(data[i]));
            }
        }
        return result;
    }
}

package ch.ethz.lapis.api.query;

import java.time.LocalDate;

public class DateCompare implements QueryExpr {

    public enum OpType {
        EQUAL,
        GREATER_THAN,
        GREATER_THAN_EQUAL,
        LESS_THAN,
        LESS_THAN_EQUAL
    }

    private final OpType opType;
    private final String leftColumn;
    private final LocalDate rightValue;

    public DateCompare(OpType opType, String leftColumn, LocalDate rightValue) {
        this.opType = opType;
        this.leftColumn = leftColumn;
        this.rightValue = rightValue;
    }

    @Override
    public boolean[] evaluate(Database database) {
        int dateValue = Database.dateToInt(rightValue);
        Integer[] data = database.getIntColumn(leftColumn);
        boolean[] result = new boolean[data.length];
        switch (opType) {
            case EQUAL -> {
                for (int i = 0; i < result.length; i++) {
                    if (data[i] != null) result[i] = data[i] == dateValue;
                }
            }
            case GREATER_THAN -> {
                for (int i = 0; i < result.length; i++) {
                    if (data[i] != null) result[i] = data[i] > dateValue;
                }
            }
            case GREATER_THAN_EQUAL -> {
                for (int i = 0; i < result.length; i++) {
                    if (data[i] != null) result[i] = data[i] >= dateValue;
                }
            }
            case LESS_THAN -> {
                for (int i = 0; i < result.length; i++) {
                    if (data[i] != null) result[i] = data[i] < dateValue;
                }
            }
            case LESS_THAN_EQUAL -> {
                for (int i = 0; i < result.length; i++) {
                    if (data[i] != null) result[i] = data[i] <= dateValue;
                }
            }
        }
        return result;
    }
}

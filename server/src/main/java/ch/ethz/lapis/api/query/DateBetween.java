package ch.ethz.lapis.api.query;

import java.time.LocalDate;

public class DateBetween implements QueryExpr {

    private final String leftColumn;
    private final LocalDate startValue;
    private final LocalDate endValue;

    public DateBetween(String leftColumn, LocalDate startValue, LocalDate endValue) {
        this.leftColumn = leftColumn;
        this.startValue = startValue;
        this.endValue = endValue;
    }

    @Override
    public boolean[] evaluate(Database database) {
        int dateValueStart = Database.dateToInt(startValue);
        int dateValueEnd = Database.dateToInt(endValue);
        Integer[] data = database.getIntColumn(leftColumn);
        boolean[] result = new boolean[data.length];
        for (int i = 0; i < result.length; i++) {
            Integer current = data[i];
            if (current != null) {
                result[i] = data[i] >= dateValueStart && data[i] <= dateValueEnd;
            }
        }
        return result;
    }
}

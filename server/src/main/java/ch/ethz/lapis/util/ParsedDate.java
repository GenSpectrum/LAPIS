package ch.ethz.lapis.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsedDate {

    private String originalString;
    private LocalDate date;
    private Integer year;
    private Integer month;
    private Integer day;

    public String getOriginalString() {
        return originalString;
    }

    public ParsedDate setOriginalString(String originalString) {
        this.originalString = originalString;
        return this;
    }

    public LocalDate getDate() {
        return date;
    }

    public ParsedDate setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public ParsedDate setYear(Integer year) {
        this.year = year;
        return this;
    }

    public Integer getMonth() {
        return month;
    }

    public ParsedDate setMonth(Integer month) {
        this.month = month;
        return this;
    }

    public Integer getDay() {
        return day;
    }

    public ParsedDate setDay(Integer day) {
        this.day = day;
        return this;
    }

    /**
     * A partial date string could look like "2022", "2022-05", "2022-XX-XX", "2022-05-XX"
     */
    public static ParsedDate parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        // Parse to LocalDate -> will only work if we have a complete date
        ParsedDate parsed = new ParsedDate()
            .setOriginalString(s);
        try {
            parsed.setDate(LocalDate.parse(s));
        } catch (DateTimeParseException ignored) {}
        // Parse date parts
        s = s.replaceAll("X", "0");
        Pattern pattern = Pattern.compile("(\\d{4})(-\\d{2})?(-\\d{2})?");
        Matcher m = pattern.matcher(s);
        if (m.matches()) {
            Integer g1 = m.group(1) != null ? Integer.parseInt(m.group(1)) : null;
            Integer g2 = m.group(2) != null ? Integer.parseInt(m.group(2).substring(1)) : null;
            Integer g3 = m.group(3) != null ? Integer.parseInt(m.group(3).substring(1)) : null;
            if (g1 != null && g1 > 0) {
                parsed.setYear(g1);
            }
            if (g2 != null && g2 > 0 && g2 <= 12) {
                parsed.setMonth(g2);
            }
            if (g3 != null && g3 > 0 && g3 <= 31) {
                parsed.setDay(g3);
            }
        }
        return parsed;
    }

    @Override
    public String toString() {
        return "ParsedDate{" +
            "originalString='" + originalString + '\'' +
            ", date=" + date +
            ", year=" + year +
            ", month=" + month +
            ", day=" + day +
            '}';
    }
}

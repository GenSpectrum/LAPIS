package ch.ethz.lapis.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class Utils {

    public static String nullableBlankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s;
    }

    public static Float nullableFloatValue(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }


    public static Integer nullableIntegerValue(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }


    public static LocalDate nullableLocalDateValue(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }


    /**
     * Code based on https://stackoverflow.com/a/29698326/8376759 (CC BY-SA 3.0 license)
     */
    public static Map nullableMapDeepMerge(Map original, Map newMap) {
        if (original == null) {
            return newMap;
        }
        if (newMap == null) {
            return original;
        }
        for (Object key : newMap.keySet()) {
            if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
                Map originalChild = (Map) original.get(key);
                Map newChild = (Map) newMap.get(key);
                original.put(key, nullableMapDeepMerge(originalChild, newChild));
            } else if (newMap.get(key) instanceof List && original.get(key) instanceof List) {
                List originalChild = (List) original.get(key);
                List newChild = (List) newMap.get(key);
                for (Object each : newChild) {
                    if (!originalChild.contains(each)) {
                        originalChild.add(each);
                    }
                }
            } else {
                original.put(key, newMap.get(key));
            }
        }
        return original;
    }


    public static String getStackTraceString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}

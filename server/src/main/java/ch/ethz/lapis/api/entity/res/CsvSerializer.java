package ch.ethz.lapis.api.entity.res;

import ch.ethz.lapis.api.entity.req.DataFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.javatuples.Pair;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CsvSerializer {

    public enum Delimiter {
        CSV,
        TSV
    }

    public static Delimiter getDelimiterFromDataFormat(DataFormat dataFormat) {
        return switch (dataFormat) {
            case CSV -> Delimiter.CSV;
            case TSV -> Delimiter.TSV;
            default -> throw new IllegalArgumentException();
        };
    }

    private final CSVFormat csvFormat;

    public CsvSerializer(Delimiter delimiter) {
        csvFormat = switch (delimiter) {
            case CSV -> CSVFormat.DEFAULT;
            case TSV -> CSVFormat.TDF;
        };
    }

    public <T> String serialize(List<T> objects, Class<T> c) {
        return serialize(objects, c, null);
    }

    public <T> String serialize(List<T> objects, Class<T> c, List<String> fields) {
        Map<String, Pair<String, Method>> allFields = new HashMap<>(); // The keys are lower-cased
        // Find out whether c is a Record.
        boolean isRecord = false;
        Class<?> currentClass = c;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            if (currentClass.equals(Record.class)) {
                isRecord = true;
                break;
            }
            currentClass = currentClass.getSuperclass();
        }
        // Find the field names by looking at the method names via reflection
        currentClass = c;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (Method method : currentClass.getDeclaredMethods()) {
                // Skip method if it is not public
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                String name = method.getName();
                String fieldName;
                if (!isRecord) {
                    // Skip method if it's not a getter function
                    if (!name.startsWith("get") && !name.startsWith("is")) {
                        continue;
                    }
                    String nameWithoutPrefix;
                    if (name.startsWith("get")) {
                        nameWithoutPrefix = name.substring(3);
                    } else {
                        nameWithoutPrefix = name.substring(2);
                    }
                    fieldName = Character.toLowerCase(nameWithoutPrefix.charAt(0)) + nameWithoutPrefix.substring(1);
                } else {
                    // Records don't have typical getters (i.e., methods that start with "get" or "is"). The value
                    // of the field x is returned by the method x() and not by getX(). Records declare exactly three
                    // implicit methods: equals(), toString(), and hashCode(). We will exclude them.
                    if (name.equals("equals") || name.equals("toString") || name.equals("hashCode")) {
                        continue;
                    }
                    fieldName = name;
                }
                allFields.put(fieldName.toLowerCase(), new Pair<>(fieldName, method));
            }
            currentClass = currentClass.getSuperclass();
        }
        // Check fields
        List<String> selectedFields = fields != null ?
            fields.stream().map(String::toLowerCase).toList() :
            allFields.keySet().stream().sorted().toList();
        if (fields != null) {
            for (String field : selectedFields) {
                if (!allFields.containsKey(field)) {
                    throw new RuntimeException("Field " + field + " does not exist.");
                }
            }
        }
        // Prepare header array for CSVPrinter
        String[] header = selectedFields.stream()
            .map(f -> allFields.get(f).getValue0())
            .toArray(String[]::new);
        // Write CSV
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, csvFormat.builder().setHeader(header).build())) {
            for (T obj : objects) {
                Object[] record = selectedFields.stream()
                    .map(f -> {
                        try {
                            return allFields.get(f).getValue1().invoke(obj);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(Object[]::new);
                printer.printRecord(record);
            }
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

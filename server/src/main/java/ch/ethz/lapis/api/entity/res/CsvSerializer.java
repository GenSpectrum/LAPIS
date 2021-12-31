package ch.ethz.lapis.api.entity.res;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.javatuples.Pair;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class CsvSerializer {

    public <T> String serialize(List<T> objects, Class<T> c) {
        List<Pair<String, Method>> fields = new ArrayList<>();
        // Find the getters via reflection and extract the field names
        Class<?> currentClass = c;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (Method method : currentClass.getDeclaredMethods()) {
                // Skip method if it is not public
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                // Skip method if it's not a getter function
                String name = method.getName();
                if (!name.startsWith("get") && !name.startsWith("is")) {
                    continue;
                }
                String nameWithoutPrefix;
                if (name.startsWith("get")) {
                    nameWithoutPrefix = name.substring(3);
                } else {
                    nameWithoutPrefix = name.substring(2);
                }
                String fieldName = Character.toLowerCase(nameWithoutPrefix.charAt(0)) + nameWithoutPrefix.substring(1);
                fields.add(new Pair<>(fieldName, method));
            }
            currentClass = currentClass.getSuperclass();
        }
        fields.sort(Comparator.comparing(Pair::getValue0));
        // Prepare header array for CSVPrinter
        String[] header = fields.stream()
            .map(Pair::getValue0)
            .toArray(String[]::new);
        // Write CSV
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(header))) {
            for (T obj : objects) {
                Object[] record = fields.stream()
                        .map(f -> {
                            try {
                                return f.getValue1().invoke(obj);
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

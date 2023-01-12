package ch.ethz.lapis.util;

import ch.ethz.lapis.VariableSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PangoLineageQueryConverterTest {

    private final PangoLineageQueryConverter converter = new PangoLineageQueryConverter(List.of(
        new PangoLineageAlias("C", "B.1.1.1"),
        new PangoLineageAlias("AY", "B.1.617.2"),
        new PangoLineageAlias("BA", "B.1.1.529"),
        new PangoLineageAlias("BE", "B.1.1.529.5.3.1"),
        new PangoLineageAlias("BF", "B.1.1.529.5.2.1")
    ));

    public static Stream<Arguments> arguments = Stream.of(
        Arguments.of("B.1.2.3", List.of("B.1.2.3"), List.of()),
        Arguments.of("B.1.2.3*", List.of("B.1.2.3"), List.of("B.1.2.3.")),
        Arguments.of("B.1.1.529", List.of("B.1.1.529"), List.of()),
        Arguments.of("B.1.1.529*", List.of("B.1.1.529", "BA", "BE", "BF"), List.of("B.1.1.529.", "BA.", "BE.", "BF.")),
        Arguments.of("BF", List.of("BA.5.2.1", "BF"), List.of()),
        Arguments.of("BF*", List.of("BA.5.2.1", "BF"), List.of("BA.5.2.1.", "BF."))
    );

    @ParameterizedTest
    @VariableSource("arguments")
    public void test(String query, List<String> expectedExact, List<String> expectedPrefix) {
        var actual = converter.convert(query);
        assertEquals(serializeStringList(expectedExact), serializeStringList(actual.exact()), query + " exact");
        assertEquals(serializeStringList(expectedPrefix), serializeStringList(actual.prefix()), query + " prefix");
    }

    private String serializeStringList(List<String> s) {
        return s.stream().sorted().collect(Collectors.joining(","));
    }
}

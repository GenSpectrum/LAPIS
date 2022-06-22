package ch.ethz.lapis.util;

import ch.ethz.lapis.VariableSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PangoLineageAliasResolverTest {

    private final PangoLineageAliasResolver resolver = new PangoLineageAliasResolver(List.of(
        new PangoLineageAlias("C", "B.1.1.1"),
        new PangoLineageAlias("AY", "B.1.617.2"),
        new PangoLineageAlias("BA", "B.1.1.529"),
        new PangoLineageAlias("BE", "B.1.1.529.5.3.1"),
        new PangoLineageAlias("BF", "B.1.1.529.5.2.1")
    ));

    public static Stream<Arguments> arguments = Stream.of(
        Arguments.of("B.1.2.3", List.of()),
        Arguments.of("B.1.1.1", List.of()),
        Arguments.of("B.1.1.1.1", List.of("C.1")),
        Arguments.of("B.1.1.529.5.3.1.2.1", List.of("BE.2.1")),
        Arguments.of("BA.5.3.1.2.1", List.of("BE.2.1")),
        Arguments.of("B.1.1.1*", List.of("C*")),
        Arguments.of("B.1*", List.of("C*", "AY*", "BA*", "BE*", "BF*")),
        Arguments.of("B.*", List.of("C*", "AY*", "BA*", "BE*", "BF*")),
        Arguments.of("BA.5*", List.of("BA.5*", "BE*", "BF*")),
        Arguments.of("B.1.1.529.5.3*", List.of("BA.5.3*", "BE*"))
    );

    @ParameterizedTest
    @VariableSource("arguments")
    public void test(String query, List<String> expected) {
        List<String> actual = resolver.findAlias(query);
        assertEquals(serializeStringList(expected), serializeStringList(actual), query);
    }

    private String serializeStringList(List<String> s) {
        return s.stream().sorted().collect(Collectors.joining(","));
    }
}

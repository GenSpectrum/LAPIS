package ch.ethz.lapis.api.entry;

import ch.ethz.lapis.api.entity.AAInsertion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AAInsertionTest {
    @ParameterizedTest
    @MethodSource("provideInsertions")
    public void serializeDeserialize (String insertionCode, AAInsertion mutation) {
        assertThat(AAInsertion.parse(insertionCode), is(mutation));
        assertThat(mutation.getInsertionCode(), is(insertionCode));
    }

    public static Stream<Arguments> provideInsertions() {
        return Stream.of(
            Arguments.of("ins_S:12:EN", new AAInsertion("S", 12, "EN")),
            Arguments.of("ins_ORRF1A:12:?NY?", new AAInsertion("ORRF1A", 12,"?NY?"))
        );
    }
}

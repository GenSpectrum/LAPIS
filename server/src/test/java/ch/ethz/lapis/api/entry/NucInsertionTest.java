package ch.ethz.lapis.api.entry;

import ch.ethz.lapis.api.entity.NucInsertion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NucInsertionTest {

    @ParameterizedTest
    @MethodSource("provideInsertions")
    public void serializeDeserialize (String insertionCode, NucInsertion mutation) {
        assertThat(NucInsertion.parse(insertionCode), is(mutation));
        assertThat(mutation.getInsertionCode(), is(insertionCode));
    }

    public static Stream<Arguments> provideInsertions() {
        return Stream.of(
            Arguments.of("ins_1234:ACT?GGT", new NucInsertion(1234, "ACT?GGT")),
            Arguments.of("ins_2345:?AAT?", new NucInsertion(2345,"?AAT?"))
        );
    }
}

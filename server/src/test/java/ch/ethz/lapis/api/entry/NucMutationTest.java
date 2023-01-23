package ch.ethz.lapis.api.entry;

import ch.ethz.lapis.api.entity.NucMutation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NucMutationTest {

    @Test
    public void testIsMaybeMatchingMutation() {
        // Found base is non-ambiguous
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('A', new NucMutation(913, 'A')));
        Assertions.assertFalse(NucMutation.isMaybeMatchingMutation('A', new NucMutation(913, 'T')));
        Assertions.assertFalse(NucMutation.isMaybeMatchingMutation('C', new NucMutation(913, null)));
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('A', new NucMutation(913, null)));
        Assertions.assertFalse(NucMutation.isMaybeMatchingMutation('G', new NucMutation(913, '.')));
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('C', new NucMutation(913, '.')));

        // Found base is ambiguous
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('N', new NucMutation(913, 'A')));
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('H', new NucMutation(913, 'A')));
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('W', new NucMutation(913, 'A')));
        Assertions.assertFalse(NucMutation.isMaybeMatchingMutation('B', new NucMutation(913, 'A')));
        Assertions.assertFalse(NucMutation.isMaybeMatchingMutation('S', new NucMutation(913, 'A')));
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('D', new NucMutation(913, null)));
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('N', new NucMutation(913, null)));
        Assertions.assertTrue(NucMutation.isMaybeMatchingMutation('M', new NucMutation(913, '.')));
        Assertions.assertFalse(NucMutation.isMaybeMatchingMutation('K', new NucMutation(913, '.')));
    }

    @ParameterizedTest
    @MethodSource("provideMutations")
    public void serializeDeserialize (String mutationCode, NucMutation mutation) {
        assertThat(NucMutation.parse(mutationCode), is(mutation));
        assertThat(mutation.getMutationCode(), is(mutationCode));
    }

    public static Stream<Arguments> provideMutations() {
        return Stream.of(
            Arguments.of("1234T", new NucMutation(1234, 'T')),
            Arguments.of("2345", new NucMutation(2345))
        );
    }

}

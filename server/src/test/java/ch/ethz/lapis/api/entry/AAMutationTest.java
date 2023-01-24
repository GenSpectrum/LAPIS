package ch.ethz.lapis.api.entry;

import ch.ethz.lapis.api.entity.AAMutation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AAMutationTest {

    @Test
    public void testIsMaybeMatchingMutation() {
        // Found base is non-ambiguous
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('Y', new AAMutation("S", 501, 'Y')));
        Assertions.assertFalse(AAMutation.isMaybeMatchingMutation('Y', new AAMutation("S", 501, 'S')));
        Assertions.assertFalse(AAMutation.isMaybeMatchingMutation('N', new AAMutation("S", 501, null)));
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('Y', new AAMutation("S", 501, null)));
        Assertions.assertFalse(AAMutation.isMaybeMatchingMutation('Y', new AAMutation("S", 501, '.')));
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('N', new AAMutation("S", 501, '.')));

        // Found base is ambiguous
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('X', new AAMutation("S", 501, 'Y')));
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('X', new AAMutation("S", 501, 'S')));
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('X', new AAMutation("S", 501, null)));
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('X', new AAMutation("S", 501, '.')));
        Assertions.assertTrue(AAMutation.isMaybeMatchingMutation('X', new AAMutation("S", 501, '.')));
    }

    @ParameterizedTest
    @MethodSource("provideMutations")
    public void serializeDeserialize (String mutationCode, AAMutation mutation) {
        assertThat(AAMutation.parse(mutationCode), is(mutation));
        assertThat(mutation.getMutationCode(), is(mutationCode));
    }

    public static Stream<Arguments> provideMutations() {
        return Stream.of(
            Arguments.of("S:501Y", new AAMutation("S", 501, 'Y')),
            Arguments.of("S:501", new AAMutation("S", 501))
        );
    }
}

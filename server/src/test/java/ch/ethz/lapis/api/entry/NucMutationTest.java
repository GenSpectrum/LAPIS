package ch.ethz.lapis.api.entry;

import ch.ethz.lapis.api.entity.NucMutation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

}

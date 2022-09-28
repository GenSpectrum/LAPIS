package ch.ethz.lapis.api.entry;

import ch.ethz.lapis.api.entity.AAMutation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

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

}

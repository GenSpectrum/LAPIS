package ch.ethz.lapis.source;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MutationFinderTest {

    @Test
    public void testFindNucUnknowns() {
        Assertions.assertEquals(
            List.of(3, 5, 6, 9),
            MutationFinder.findNucUnknowns("aaNcyyGGrtt"),
            "Non-empty case"
        );
        Assertions.assertEquals(
            List.of(),
            MutationFinder.findNucUnknowns(""),
            "Empty case"
        );
    }

    @Test
    public void testFindAAUnknowns() {
        Assertions.assertEquals(
            List.of(4, 5, 8),
            MutationFinder.findAAUnknowns("AArXXnDXCe"),
            "Non-empty case"
        );
        Assertions.assertEquals(
            List.of(),
            MutationFinder.findAAUnknowns(""),
            "Empty case"
        );
    }

    @Test
    public void testCompressPositionsAsStrings() {
        Assertions.assertEquals(
            List.of("1", "5-8", "20"),
            MutationFinder.compressPositionsAsStrings(List.of(1, 5, 6, 7, 8, 20)),
            "Multiple values"
        );
        Assertions.assertEquals(
            List.of(),
            MutationFinder.compressPositionsAsStrings(List.of()),
            "Empty case"
        );
        Assertions.assertEquals(
            List.of("3"),
            MutationFinder.compressPositionsAsStrings(List.of(3)),
            "Single value"
        );
    }

}

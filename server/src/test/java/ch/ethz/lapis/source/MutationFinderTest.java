package ch.ethz.lapis.source;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MutationFinderTest {

    @Test
    public void testFindNucUnknowns() {
        Assertions.assertEquals(
            List.of(3, 5, 6, 9),
            MutationFinder.findNucUnknowns("aaNcyyGGr-tt"),
            "Non-empty case"
        );
        Assertions.assertEquals(
            List.of(1, 2, 5, 7, 8, 11, 15, 16, 17),
            MutationFinder.findNucUnknowns("--aaNcyyGGr-tt---"),
            "Leading and tailing deletions"
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
            MutationFinder.findAAUnknowns("AArXXnDX-Ce"),
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

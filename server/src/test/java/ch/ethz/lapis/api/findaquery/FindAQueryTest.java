package ch.ethz.lapis.api.findaquery;

import ch.ethz.lapis.api.query.MutationStore;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FindAQueryTest {

    @Test
    public void testCalcSequenceDistance() {
        // Test data
        MutationStore mutationStore = new MutationStore(3);
        mutationStore.putEntry(0, List.of(
            new MutationStore.Mutation((short) 25, 'T'),
            new MutationStore.Mutation((short) 33, 'G'),
            new MutationStore.Mutation((short) 42, 'G')
        ), List.of(
            "1-10",
            "40",
            "41",
            "45-50"
        ));
        mutationStore.putEntry(1, List.of(
            new MutationStore.Mutation((short) 33, 'G')
        ), List.of(
            "1-30",
            "40-50"
        ));
        mutationStore.putEntry(2, List.of(
            new MutationStore.Mutation((short) 33, 'G'),
            new MutationStore.Mutation((short) 42, 'C')
        ), List.of(
            "1-20",
            "35-40",
            "45-50"
        ));

        // Execute tests
        FindAQuery findAQuery = new FindAQuery(mutationStore);
        // Different positions: 11-30, 42-44 -> 20 + 3 = 23
        Assertions.assertEquals(23, findAQuery.calcSequenceDistance(0, 1), "Distance seq0-seq1");
        // Different positions: 11-20, 25, 35-39, 41, 42 -> 10 + 1 + 5 + 1 + 1 = 18
        Assertions.assertEquals(18, findAQuery.calcSequenceDistance(0, 2), "Distance seq0-seq2");
        // Different positions: 21-30, 35-39, 41-44 -> 10 + 5 + 4 = 19
        Assertions.assertEquals(19, findAQuery.calcSequenceDistance(1, 2), "Distance seq1-seq2");
    }

}

package ch.ethz.lapis.api.query;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

public class MutationStoreTest {

    @Test
    public void test() {
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

        // Case 1: count single
        List<MutationStore.MutationCount> counts0 = mutationStore.countMutations(List.of(0));
        Assertions.assertEquals(
            new HashSet<>(List.of(
                new MutationStore.MutationCount(new MutationStore.Mutation((short) 25, 'T'), 1)
                    .setProportion(1),
                new MutationStore.MutationCount(new MutationStore.Mutation((short) 33, 'G'), 1)
                    .setProportion(1),
                new MutationStore.MutationCount(new MutationStore.Mutation((short) 42, 'G'), 1)
                    .setProportion(1)
            )),
            new HashSet<>(counts0),
            "Case 1"
        );

        // Case 2: count all
        List<MutationStore.MutationCount> counts1 = mutationStore.countMutations(List.of(0, 1, 2));
        Assertions.assertEquals(
            new HashSet<>(List.of(
                new MutationStore.MutationCount(new MutationStore.Mutation((short) 25, 'T'), 1)
                    .setProportion(0.5),
                new MutationStore.MutationCount(new MutationStore.Mutation((short) 33, 'G'), 3)
                    .setProportion(1),
                new MutationStore.MutationCount(new MutationStore.Mutation((short) 42, 'G'), 1)
                    .setProportion(0.5),
                new MutationStore.MutationCount(new MutationStore.Mutation((short) 42, 'C'), 1)
                    .setProportion(0.5)
            )),
            new HashSet<>(counts1),
            "Case 2"
        );
    }

}

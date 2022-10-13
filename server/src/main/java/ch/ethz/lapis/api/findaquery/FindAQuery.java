package ch.ethz.lapis.api.findaquery;

import ch.ethz.lapis.api.query.MutationStore;
import ch.ethz.lapis.api.query.MutationStore.InternalEntry;
import ch.ethz.lapis.util.tuples.MutableTriplet;
import org.javatuples.Pair;


public class FindAQuery {

    private final MutationStore mutationStore;

    public FindAQuery(MutationStore mutationStore) {
        this.mutationStore = mutationStore;
    }

    public int calcSequenceDistance(int seqId1, int seqId2) {
        var internalMutationData = mutationStore.getInternalData();
        var data1 = internalMutationData[seqId1];
        var data2 = internalMutationData[seqId2];
        return calcSequenceDistance(data1, data2);
    }

    private int calcSequenceDistance(InternalEntry data1, InternalEntry data2) {
        var internalMutationDict = mutationStore.getInternalMutationDict();

        int distance = 0;
        var mutations1 = data1.mutationIds();
        var unknowns1 = data1.unknownPositions();
        var unknownsIsRange1 = data1.unknownIsStartRange();
        var mutations2 = data2.mutationIds();
        var unknowns2 = data2.unknownPositions();
        var unknownsIsRange2 = data2.unknownIsStartRange();

        int indexMutations1 = 0;
        int indexUnknowns1 = 0;
        var currentMutation1 = takeMutation(mutations1, internalMutationDict, indexMutations1);
        var currentUnknown1 = takeUnknown(unknowns1, unknownsIsRange1, indexUnknowns1);
        int indexMutations2 = 0;
        int indexUnknowns2 = 0;
        var currentMutation2 = takeMutation(mutations2, internalMutationDict, indexMutations2);
        var currentUnknown2 = takeUnknown(unknowns2, unknownsIsRange2, indexUnknowns2);

        while (true) {
            // If we went through both sequences -> done
            if (currentMutation1 == null && currentUnknown1 == null
                && currentMutation2 == null && currentUnknown2 == null) {
                break;
            }
            // If we went through the data of only one of the sequences
            // -> we need to process the remaining of the other one and add the distances
            if (currentMutation1 == null && currentUnknown1 == null) {
                if (indexMutations2 < mutations2.length) {
                    distance += mutations2.length - indexMutations2;
                }
                while (currentUnknown2 != null) {
                    distance += currentUnknown2.getValue2() - currentUnknown2.getValue1() + 1;
                    indexUnknowns2 += currentUnknown2.getValue0() ? 2 : 1;
                    currentUnknown2 = takeUnknown(unknowns2, unknownsIsRange2, indexUnknowns2);
                }
                break;
            }
            if (currentMutation2 == null && currentUnknown2 == null) {
                if (indexMutations1 < mutations1.length) {
                    distance += mutations1.length - indexMutations1;
                }
                while (currentUnknown1 != null) {
                    distance += currentUnknown1.getValue2() - currentUnknown1.getValue1() + 1;
                    indexUnknowns1 += currentUnknown1.getValue0() ? 2 : 1;
                    currentUnknown1 = takeUnknown(unknowns1, unknownsIsRange1, indexUnknowns1);
                }
                break;
            }
            // For both sequences, we need to determine whether we are looking at the mutations or at the unknowns
            boolean lookingAtMutations1 = doesMutationHaveASmallerPosition(currentMutation1, currentUnknown1);
            boolean lookingAtMutations2 = doesMutationHaveASmallerPosition(currentMutation2, currentUnknown2);
            // Compare
            Pair<Integer, ConsumedStatus> distanceAndConsumption;
            if (lookingAtMutations1 && lookingAtMutations2) {
                distanceAndConsumption = calculateDistance(currentMutation1, currentMutation2);
            } else if (lookingAtMutations1) {
                distanceAndConsumption = calculateDistance(currentMutation1, currentUnknown2);
            } else if (lookingAtMutations2) {
                distanceAndConsumption = calculateDistance(currentUnknown1, currentMutation2);
            } else {
                distanceAndConsumption = calculateDistance(currentUnknown1, currentUnknown2);
            }
            distance += distanceAndConsumption.getValue0();
            // Advance the indices
            var consumption = distanceAndConsumption.getValue1();
            if (consumption == ConsumedStatus.VALUE1_CONSUMED || consumption == ConsumedStatus.BOTH_CONSUMED) {
                if (lookingAtMutations1) {
                    indexMutations1++;
                    currentMutation1 = takeMutation(mutations1, internalMutationDict, indexMutations1);
                } else {
                    indexUnknowns1 += currentUnknown1.getValue0() ? 2 : 1;
                    currentUnknown1 = takeUnknown(unknowns1, unknownsIsRange1, indexUnknowns1);
                }
            }
            if (consumption == ConsumedStatus.VALUE2_CONSUMED ||  consumption == ConsumedStatus.BOTH_CONSUMED) {
                if (lookingAtMutations2) {
                    indexMutations2++;
                    currentMutation2 = takeMutation(mutations2, internalMutationDict, indexMutations2);
                } else {
                    indexUnknowns2 += currentUnknown2.getValue0() ? 2 : 1;
                    currentUnknown2 = takeUnknown(unknowns2, unknownsIsRange2, indexUnknowns2);
                }
            }

        }

        return distance;
    }

    private MutationStore.Mutation takeMutation(int[] mutations, MutationStore.MutationDict mutationDict, int index) {
        return index >= mutations.length ? null : mutationDict.idToMutation(mutations[index]);
    }

    private
    MutableTriplet <Boolean, Short, Short> takeUnknown(short[] unknowns, boolean[] unknownsIsRange, int index) {
        return index == unknowns.length ? null :
            (unknownsIsRange[index] ?
                new MutableTriplet<>(true, unknowns[index], unknowns[index + 1]) :
                new MutableTriplet<>(false, unknowns[index], unknowns[index]));
    }


    /**
     * This function expects that AT LEAST one of the two arguments is not null.
     */
    private static boolean doesMutationHaveASmallerPosition(
        MutationStore.Mutation mutation,
        MutableTriplet<Boolean, Short, Short> unknown
    ) {
        if (mutation == null) {
            return false;
        }
        if (unknown == null) {
            return true;
        }
        return mutation.position < unknown.getValue1();
    }

    private enum ConsumedStatus {
        VALUE1_CONSUMED,
        VALUE2_CONSUMED,
        BOTH_CONSUMED
    }

    private static Pair<Integer, ConsumedStatus> calculateDistance(
        MutationStore.Mutation value1, MutationStore.Mutation value2
    ) {
        if (value1.position < value2.position) {
            return new Pair<>(1, ConsumedStatus.VALUE1_CONSUMED);
        }
        if (value1.position > value2.position) {
            return new Pair<>(1, ConsumedStatus.VALUE2_CONSUMED);
        }
        return new Pair<>(value1.mutationTo == value2.mutationTo ? 0 : 1, ConsumedStatus.BOTH_CONSUMED);
    }

    private static Pair<Integer, ConsumedStatus> calculateDistance(
        MutationStore.Mutation value1, MutableTriplet<Boolean, Short, Short> value2
    ) {
        short startPos2 = value2.getValue1();
        short endPos2 = value2.getValue2();

        if (value1.position < startPos2) {
            return new Pair<>(1, ConsumedStatus.VALUE1_CONSUMED);
        }
        if (value1.position < endPos2) {
            int numberConsumed = value1.position - startPos2 + 1;
            value2.setValue1((short) (startPos2 + numberConsumed));
            return new Pair<>(numberConsumed, ConsumedStatus.VALUE1_CONSUMED);
        }
        if (value1.position > endPos2) {
            return new Pair<>(endPos2 - startPos2 + 1, ConsumedStatus.VALUE2_CONSUMED);
        }
        return new Pair<>(endPos2 - startPos2 + 1, ConsumedStatus.BOTH_CONSUMED);
    }

    private static Pair<Integer, ConsumedStatus> calculateDistance(
        MutableTriplet<Boolean, Short, Short> value1, MutationStore.Mutation value2
    ) {
        return flipConsumedStatus(calculateDistance(value2, value1));
    }

    private static Pair<Integer, ConsumedStatus> calculateDistance(
        MutableTriplet<Boolean, Short, Short> value1, MutableTriplet<Boolean, Short, Short> value2
    ) {
        short startPos1 = value1.getValue1();
        short startPos2 = value2.getValue1();
        if (startPos1 > startPos2) {
            return flipConsumedStatus(calculateDistance(value2, value1));
        }

        short endPos1 = value1.getValue2();
        short endPos2 = value2.getValue2();

        if (endPos1 < startPos2) {
            // No overlap
            // value1 is fully consumed
            // we have mismatch for the whole range of value1
            return new Pair<>(endPos1 - startPos1 + 1, ConsumedStatus.VALUE1_CONSUMED);
        }
        if (endPos1 < endPos2) {
            // value1 is fully consumed
            // we have mismatch for the non-overlapping range
            int distance = startPos2 - startPos1;
            int value2Consumed = endPos1 - startPos2 + 1;
            value2.setValue1((short) (startPos2 + value2Consumed));
            return new Pair<>(distance, ConsumedStatus.VALUE1_CONSUMED);
        }
        if (endPos1 == endPos2) {
            // value2 is a proper or non-proper subset of value1
            // both sets are fully consumed
            // we have mismatch for the non-overlapping range
            int distance = startPos2 - startPos1;
            return new Pair<>(distance, ConsumedStatus.BOTH_CONSUMED);
        }
        // value2 is a proper subset of value1
        // value2 is fully consumed
        // we have mismatch for the non-overlapping range until endPos2
        int distance = (startPos2 - startPos1);
        int value1Consumed = endPos2 - startPos1 + 1;
        value1.setValue1((short) (startPos1 + value1Consumed));
        return new Pair<>(distance, ConsumedStatus.VALUE2_CONSUMED);
    }

    private static Pair<Integer, ConsumedStatus> flipConsumedStatus(Pair<Integer, ConsumedStatus> value) {
        int distance = value.getValue0();
        var consumed = value.getValue1();
        if (consumed == ConsumedStatus.VALUE1_CONSUMED) {
            return new Pair<>(distance, ConsumedStatus.VALUE2_CONSUMED);
        }
        if (consumed == ConsumedStatus.VALUE2_CONSUMED) {
            return new Pair<>(distance, ConsumedStatus.VALUE1_CONSUMED);
        }
        return value;
    }

}

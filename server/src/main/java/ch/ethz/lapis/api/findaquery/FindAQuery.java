package ch.ethz.lapis.api.findaquery;

import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.MutationStore;
import ch.ethz.lapis.api.query.MutationStore.InternalEntry;
import ch.ethz.lapis.api.query.MutationStore.Mutation;
import ch.ethz.lapis.api.query.MutationStore.MutationCount;
import ch.ethz.lapis.api.query.QueryEngine;
import ch.ethz.lapis.source.MutationFinder;
import ch.ethz.lapis.util.tuples.MutableTriplet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.javatuples.Pair;


public class FindAQuery {

    private final MutationStore mutationStore;
    private final QueryEngine queryEngine = new QueryEngine();

    public FindAQuery(MutationStore mutationStore) {
        this.mutationStore = mutationStore;
    }

    public String proposeQuery(Database database, Set<Integer> wantedSeqs) {
        Set<Integer> unwantedSeqs = new HashSet<>(queryEngine.filterIds(database, new SampleDetailRequest()));
        unwantedSeqs.removeAll(wantedSeqs);

        // Iteration 1
        var p1 = proposeAndNotMaybeQuery(database, wantedSeqs, unwantedSeqs,
            new HashSet<>(), new ArrayList<>());
        if (p1.unwantedSequences.isEmpty()) {
            return String.join(" & ", p1.newComponents);
        }

        // Iteration 2 - with two clusters
        var baseComponents = p1.newComponents;
        var clusters = kMeans(new ArrayList<>(p1.wantedSequences), 2);

        var p20 = proposeAndNotMaybeQuery(database,
            new HashSet<>(clusters.get(0)), p1.unwantedSequences, wantedSeqs, baseComponents);
        var p21 = proposeAndNotMaybeQuery(database,
            new HashSet<>(clusters.get(1)), p1.unwantedSequences, wantedSeqs, baseComponents);

        return String.join(" & ", p1.newComponents) +
            " & ((" + String.join(" & ", p20.newComponents) + ") | (" + String.join(" & ", p21.newComponents) + "))";
    }

    public ProposedQueryAndQueriedSequences proposeAndNotMaybeQuery(
        Database database,
        Set<Integer> wantedSeqs,
        Set<Integer> unwantedSeqs,
        Set<Integer> acceptedSeqs,
        List<String> baseQueryComponents
    ) {
        List<String> queryComponents = new ArrayList<>(baseQueryComponents);
        List<String> newQueryComponents = new ArrayList<>();
        for (int i = 0; i < 8; i++) { // TODO The max. number of iterations should not be fixed?
            String component = proposeOneComponent(wantedSeqs, unwantedSeqs);
            if (component == null) {
                break;
            }
            queryComponents.add(component);
            newQueryComponents.add(component);
            String newQuery = String.join(" & ", queryComponents);
            Set<Integer> queriedSeqs = new HashSet<>(queryEngine.filterIds(database, new SampleDetailRequest()
                .setVariantQuery(newQuery)));
            // TODO We are still loosing sequences which, I believe, is due to rounding inaccuracies of "proportion"
//            for (int wantedSeq : wantedSeqs) {
//                if (!queriedSeqs.contains(wantedSeq)) {
//                    throw new RuntimeException("The query " + newQuery + " lost the sequence " + wantedSeq);
//                }
//            }
            unwantedSeqs = new HashSet<>(queriedSeqs);
            unwantedSeqs.removeAll(wantedSeqs);
            unwantedSeqs.removeAll(acceptedSeqs);

            int numberWantedSeqs = wantedSeqs.size();
            int numberUnwantedSeqs = unwantedSeqs.size();
            double precision = (double) numberWantedSeqs / (numberWantedSeqs + numberUnwantedSeqs);
            System.out.println(newQuery + " - precision=" + (double) Math.round(precision * 10000) / 10000);
            if (precision == 1) {
                break;
            }
        }

        return new ProposedQueryAndQueriedSequences(
            newQueryComponents,
            wantedSeqs, // This is inaccurate as we do not remove the sequences that we lost by accident.
            unwantedSeqs
        );
    }

    public String proposeOneComponent(
        Set<Integer> wantedSeqs,
        Set<Integer> unwantedSeqs
    ) {
        int numberWantedSeqs = wantedSeqs.size();
        int numberUnwantedSeqs = unwantedSeqs.size();

        // Fetch mutation distribution of both sets of sequences
        List<MutationCount> wantedSeqsMuts = mutationStore.countMutations(wantedSeqs);
        List<MutationCount> unwantedSeqsMuts = mutationStore.countMutations(unwantedSeqs);

        Map<String, MutationCount> wantedSeqsMutsMap = new HashMap<>();
        Set<String> wantedSeqsMutsSet = new HashSet<>();
        wantedSeqsMuts.forEach(m -> {
            String mutStr = m.getMutation().toString();
            wantedSeqsMutsMap.put(mutStr, m);
            wantedSeqsMutsSet.add(mutStr);
        });
        Map<String, MutationCount> unwantedSeqsMutsMap = new HashMap<>();
        Set<String> unwantedSeqsMutsSet = new HashSet<>();
        unwantedSeqsMuts.forEach(m -> {
            String mutStr = m.getMutation().toString();
            unwantedSeqsMutsMap.put(mutStr, m);
            unwantedSeqsMutsSet.add(mutStr);
        });
        Set<String> allMutsSet = new HashSet<>(wantedSeqsMutsSet);
        allMutsSet.addAll(unwantedSeqsMutsSet);

        // Find the mutations that are (1) definitely in all wantedSeqs and not in all unwantedSeqs, (2) in none of the
        // wantedSeqs and in some unwantedSeqs, or (3) maybe in all wantedSeqs and not maybe in all unwantedSeqs.
        // We also calculate the proportion difference to evaluate the effect of including a query component.
        List<Pair<String, Double>> queryComponentsAndProportionDifferences = new ArrayList<>();
        for (String mutStr : allMutsSet) {
            MutationCount wantedCount = wantedSeqsMutsMap.get(mutStr);
            MutationCount unwantedCount = unwantedSeqsMutsMap.get(mutStr);
            if (wantedCount == null) {
                // Case (2)
                queryComponentsAndProportionDifferences.add(new Pair<>(
                    "!" + mutStr,
                    (double) unwantedCount.getCount() / numberUnwantedSeqs
                ));
            } else if (wantedCount.getProportion() == 1) {
                // TODO Due to rounding inaccuracies, I think that this can still loose sequences
                if (wantedCount.getCount() == numberWantedSeqs) {
                    if (unwantedCount == null || unwantedCount.getCount() < numberUnwantedSeqs) {
                        // Case (1)
                        queryComponentsAndProportionDifferences.add(new Pair<>(
                            mutStr,
                            unwantedCount != null ?
                                1 - (double) unwantedCount.getCount() / numberUnwantedSeqs :
                                1
                        ));
                    }
                } else if (unwantedCount == null || unwantedCount.getProportion() < 1) {
                    // Case (3)
                    queryComponentsAndProportionDifferences.add(new Pair<>(
                        "maybe(" + mutStr + ")",
                        unwantedCount != null ?
                            1 - unwantedCount.getProportion() :
                            1
                    ));
                }
            }
        }

        // Return the best component if there's one available
        queryComponentsAndProportionDifferences.sort((a, b) -> b.getValue1().compareTo(a.getValue1()));
        if (queryComponentsAndProportionDifferences.isEmpty() ||
            queryComponentsAndProportionDifferences.get(0).getValue1() < 0.02) { // TODO Don't fix the min. difference?
            return null;
        }
        return queryComponentsAndProportionDifferences.get(0).getValue0();
    }

    public List<List<Integer>> kMeans(List<Integer> seqIds, int k) {
        InternalEntry[] internalData = mutationStore.getInternalData();

        // TODO Check border cases. E.g., can a cluster get zero sequences assigned? What if |seqIds| < k?
        // Init: split the sequences into k clusters of roughly the same size
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            clusters.add(new ArrayList<>());
        }
        for (int i = 0; i < seqIds.size(); i++) {
            clusters.get(i % k).add(seqIds.get(i));
        }

        for (int iter = 0; iter < 10; iter++) { // TODO define proper abort conditions
            // Calculate the centroid of every cluster
            List<InternalEntry> clusterCentroids = new ArrayList<>();
            for (List<Integer> cluster : clusters) {
                clusterCentroids.add(calcCentroid(cluster));
            }

            // For every sequence, calculate the distance to every centroid and assign the sequences to their closest
            // cluster.
            List<List<Integer>> newClusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                newClusters.add(new ArrayList<>());
            }
            for (Integer seqId : seqIds) {
                List<Integer> distances = clusterCentroids.stream()
                    .map(cc -> calcSequenceDistance(cc, internalData[seqId]))
                    .toList();
                int lowestDistance = Integer.MAX_VALUE;
                int closestClusterIndex = -1;
                for (int i = 0; i < distances.size(); i++) {
                    int distance = distances.get(i);
                    if (distance < lowestDistance) {
                        lowestDistance = distance;
                        closestClusterIndex = i;
                    }
                }
                newClusters.get(closestClusterIndex).add(seqId);
            }
            clusters = newClusters;
        }

        return clusters;
    }

    public int calcSequenceDistance(int seqId1, int seqId2) {
        var internalMutationData = mutationStore.getInternalData();
        var data1 = internalMutationData[seqId1];
        var data2 = internalMutationData[seqId2];
        return calcSequenceDistance(data1, data2);
    }

    public InternalEntry calcCentroid(Collection<Integer> seqIds) {
        var internalMutationData = mutationStore.getInternalData();
        return calcCentroid(seqIds.stream().map(id -> internalMutationData[id]).toList());
    }

    private InternalEntry calcCentroid(List<InternalEntry> dataList) {
        var internalMutationDict = mutationStore.getInternalMutationDict();

        // For every position, count the number of each mutation/unknown
        Map<Integer, Map<Character, Integer>> positionCharacterCount = new HashMap<>();
        for (InternalEntry data : dataList) {
            for (int mutId : data.mutationIds()) {
                Mutation mut = internalMutationDict.idToMutation(mutId);
                incrementCharacterCount(positionCharacterCount, mut.position, mut.mutationTo);
            }
            int i = 0;
            while (i < data.unknownPositions().length) {
                if (!data.unknownIsStartRange()[i]) {
                    incrementCharacterCount(positionCharacterCount, data.unknownPositions()[i], 'N');
                    i++;
                } else {
                    short start = data.unknownPositions()[i];
                    short end = data.unknownPositions()[i + 1];
                    for (int j = start; j <= end; j++) {
                        incrementCharacterCount(positionCharacterCount, j, 'N');
                    }
                    i += 2;
                }
            }
        }

        // For each position, find the character that occurs most
        int totalSequences = dataList.size();
        List<Integer> mutationIds = new ArrayList<>();
        List<Short> unknownPositions = new ArrayList<>();
        positionCharacterCount.forEach((position, characterCount) -> {
            final char[] charWithMaxCount = {'?'};
            final int[] maxCount = {0};
            final int[] totalNonWildtypeCount = {0};
            characterCount.forEach((c, count) -> {
                totalNonWildtypeCount[0] += count;
                if (count > maxCount[0]) { // If two characters have the same count, we take the first
                    charWithMaxCount[0] = c;
                    maxCount[0] = count;
                }
            });
            if (maxCount[0] > totalSequences - totalNonWildtypeCount[0]) {
                if (charWithMaxCount[0] != 'N') {
                    mutationIds.add(internalMutationDict.mutationToId(
                        new Mutation(position.shortValue(), charWithMaxCount[0])));
                } else {
                    unknownPositions.add(position.shortValue());
                }
            }
        });
        // Compress the unknown positions
        unknownPositions.sort(Short::compareTo);
        Pair<short[], boolean[]> unknownsCompressed = MutationFinder.compressPositionsAsArrays(unknownPositions);

        int[] mutationIdsArr = new int[mutationIds.size()];
        for (int i = 0; i < mutationIds.size(); i++) {
            mutationIdsArr[i] = mutationIds.get(i);
        }
        return new InternalEntry(mutationIdsArr, unknownsCompressed.getValue0(), unknownsCompressed.getValue1());
    }

    private static void incrementCharacterCount(
        Map<Integer, Map<Character, Integer>> positionCharacterCount,
        int position,
        char c
    ) {
        var characterCount = positionCharacterCount.computeIfAbsent(position, (x) -> new HashMap<>());
        characterCount.put(c, characterCount.getOrDefault(c, 0) + 1);
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

    private record ProposedQueryAndQueriedSequences(
        List<String> newComponents,
        Set<Integer> wantedSequences,
        Set<Integer> unwantedSequences) {
    }

}

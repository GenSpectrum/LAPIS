package ch.ethz.lapis.api.query;

import java.util.*;


public class MutationStore {

    private final InternalEntry[] data;
    private final MutationDict mutationDict = new MutationDict();

    /**
     * The largest position at which a mutation or unknown was found. Following the conventions, the position counter
     * starts with 1.
     */
    private int maxPosition = 0;


    public MutationStore(int capacity) {
        data = new InternalEntry[capacity];
    }


    /**
     *
     * @param id The id is at the same time also the index in the array; it holds 0 <= id < capacity
     * @param unknownsCompressedPositions As produced by MutationFinder.compressPositionsAsStrings(); e.g. 1,5-8,20
     */
    public void putEntry(
        int id,
        Collection<Mutation> mutations,
        Collection<String> unknownsCompressedPositions
    ) {
        // Encode the mutations using the MutationDict
        int[] mutationIds = new int[mutations.size()];
        int i = 0;
        for (Mutation mutation : mutations) {
            mutationIds[i] = mutationDict.mutationToId(mutation);
            i++;
            if (mutation.position > maxPosition) {
                maxPosition = mutation.position;
            }
        }
        // Parse the compressed unknown position strings
        List<Integer> unknownPositionsList = new ArrayList<>();
        List<Boolean> unknownIsStartRangeList = new ArrayList<>();
        for (String s : unknownsCompressedPositions) {
            if (!s.contains("-")) {
                // It's a single value
                int position = Integer.parseInt(s);
                unknownPositionsList.add(position);
                unknownIsStartRangeList.add(false);
                if (position > maxPosition) {
                    maxPosition = position;
                }
            } else {
                // It's a range
                String[] parts = s.split("-");
                int rangeStart = Integer.parseInt(parts[0]);
                int rangeEnd = Integer.parseInt(parts[1]);
                unknownPositionsList.add(rangeStart);
                unknownPositionsList.add(rangeEnd);
                unknownIsStartRangeList.add(true);
                unknownIsStartRangeList.add(false);
                if (rangeEnd > maxPosition) {
                    maxPosition = rangeEnd;
                }
            }
        }
        int[] unknownPositions = new int[unknownPositionsList.size()];
        boolean[] unknownIsStartRange = new boolean[unknownPositionsList.size()];
        for (int j = 0; j < unknownPositionsList.size(); j++) {
            unknownPositions[j] = unknownPositionsList.get(j);
            unknownIsStartRange[j] = unknownIsStartRangeList.get(j);
        }
        // Create and store entry
        InternalEntry entry = new InternalEntry(
            mutationIds,
            unknownPositions,
            unknownIsStartRange
        );
        data[id] = entry;
    }


    public List<MutationCount> countMutations(Collection<Integer> ids) {
        // We could also use an int[] to store the counts. It would need more space but also be faster.
        Map<Integer, int[]> mutationIdCounts = new HashMap<>();
        // To avoid too many annoying add-1 and minus-1 operations, we will leave out the first position in the array
        // so that the "position" corresponds to the index.
        int[] unknownCounts = new int[maxPosition + 1];
        for (int id : ids) {
            InternalEntry entry = data[id];
            // Count mutations
            for (int mutationId : entry.mutationIds) {
                mutationIdCounts.compute(mutationId, (k, v) -> v == null ?
                    new int[] { 0 } : v)[0]++;
            }
            // Count unknowns
            for (int i = 0; i < entry.unknownPositions.length; i++) {
                boolean isStartRange = entry.unknownIsStartRange[i];
                if (!isStartRange) {
                    int position = entry.unknownPositions[i];
                    unknownCounts[position]++;
                } else {
                    int startRange = entry.unknownPositions[i];
                    int endRange = entry.unknownPositions[i+1];
                    for (int position = startRange; position <= endRange; position++) {
                        unknownCounts[position]++;
                    }
                    i++; // Skip the end range value
                }
            }
        }
        // Translate the mutations back
        List<MutationCount> mutationCounts = new ArrayList<>();
        mutationIdCounts.forEach((mutationId, count) -> mutationCounts.add(new MutationCount(
            mutationDict.idToMutation(mutationId),
            count[0]
        )));
        // Calculate the proportions: as denominator, we need to subtract the unknowns from the total number of
        // entries.
        int totalEntries = ids.size();
        for (MutationCount mutationCount : mutationCounts) {
            int denominator = totalEntries - unknownCounts[mutationCount.getMutation().position];
            mutationCount.setProportion(((double) mutationCount.getCount()) / denominator);
        }
        return mutationCounts;
    }


    public static class Mutation {
        public final int position;
        public final char mutationTo;

        public Mutation(int position, char mutationTo) {
            this.position = position;
            this.mutationTo = mutationTo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Mutation mutation = (Mutation) o;
            return position == mutation.position && mutationTo == mutation.mutationTo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, mutationTo);
        }

        @Override
        public String toString() {
            return position + "" + mutationTo;
        }

        /**
         * This expects a well-formatted input of the shape "A1234B"
         */
        public static Mutation parse(String code) {
            return new Mutation(Integer.parseInt(code.substring(1, code.length() - 1)), code.charAt(code.length() - 1));
        }
    }


    public static class MutationCount {
        private final Mutation mutation;
        private final int count;
        private double proportion;

        public MutationCount(Mutation mutation, int count) {
            this.mutation = mutation;
            this.count = count;
        }

        public Mutation getMutation() {
            return mutation;
        }

        public int getCount() {
            return count;
        }

        public double getProportion() {
            return proportion;
        }

        public MutationCount setProportion(double proportion) {
            this.proportion = proportion;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MutationCount that = (MutationCount) o;
            return count == that.count
                && Double.compare(that.proportion, proportion) == 0
                && Objects.equals(mutation, that.mutation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mutation, count, proportion);
        }

        @Override
        public String toString() {
            return "MutationCount{" +
                "mutation=" + mutation +
                ", count=" + count +
                ", proportion=" + proportion +
                '}';
        }
    }


    private static class MutationDict {
        private final Map<Mutation, Integer> mutationToIdMap = new HashMap<>();
        private final List<Mutation> mutations = new ArrayList<>();
        private int nextId = 0;

        public int mutationToId(Mutation mutation) {
            Integer prev = mutationToIdMap.putIfAbsent(mutation, nextId);
            if (prev == null) {
                // The mutation is new
                mutations.add(mutation);
                nextId++;
                return nextId - 1;
            } else {
                // The mutation is already known
                return prev;
            }
        }

        /**
         * It is required that the id exists.
         */
        public Mutation idToMutation(Integer id) {
            return mutations.get(id);
        }
    }


    private static class InternalEntry {
        private final int[] mutationIds;
        private final int[] unknownPositions;
        private final boolean[] unknownIsStartRange;

        public InternalEntry(int[] mutationIds, int[] unknownPositions, boolean[] unknownIsStartRange) {
            this.mutationIds = mutationIds;
            this.unknownPositions = unknownPositions;
            this.unknownIsStartRange = unknownIsStartRange;
        }
    }

}

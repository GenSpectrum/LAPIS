package ch.ethz.lapis.source;

import ch.ethz.lapis.util.ReferenceGenomeData;
import java.util.ArrayList;
import java.util.List;
import org.javatuples.Pair;


public class MutationFinder {

    public static List<MutationNuc> findNucMutations(String alignedSeq) {
        char[] ref = ReferenceGenomeData.getInstance().getNucleotideSequenceArr();
        char[] seq = alignedSeq.toUpperCase().toCharArray();

        // Masking leading and tailing deletions because they are often actually unknowns but appear here as
        // deletions due to aligning.
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] != '-') {
                break;
            }
            seq[i] = 'N';
        }
        for (int i = seq.length - 1; i >= 0; i--) {
            if (seq[i] != '-') {
                break;
            }
            seq[i] = 'N';
        }

        List<MutationNuc> mutations = new ArrayList<>();
        for (int i = 0; i < ref.length; i++) {
            int pos = i + 1;
            char refBase = ref[i];
            char seqBase = seq[i];
            if (seqBase != 'C' && seqBase != 'T' && seqBase != 'A' && seqBase != 'G' && seqBase != '-') {
                continue;
            }
            if (seqBase != refBase) {
                mutations.add(new MutationNuc(pos, Character.toString(seqBase)));
            }
        }
        return mutations;
    }


    public static List<MutationAA> findAAMutations(String gene, String aaSeq) {
        char[] ref = ReferenceGenomeData.getInstance().getGeneAASequencesArr().get(gene);
        char[] seq = aaSeq.toUpperCase().toCharArray();

        List<MutationAA> mutations = new ArrayList<>();
        for (int i = 0; i < ref.length; i++) {
            int pos = i + 1;
            char refBase = ref[i];
            char seqBase = seq[i];
            if (seqBase == 'X') {
                continue;
            }
            if (seqBase != refBase) {
                mutations.add(new MutationAA(gene, pos, Character.toString(seqBase)));
            }
        }
        return mutations;
    }


    /**
     * Everything that is not A, T, C, G or - is considered as unknown.
     */
    public static List<Integer> findNucUnknowns(String aaSeq) {
        char[] seq = aaSeq.toUpperCase().toCharArray();

        // Masking leading and tailing deletions because they are often actually unknowns but appear here as
        // deletions due to aligning.
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] != '-') {
                break;
            }
            seq[i] = 'N';
        }
        for (int i = seq.length - 1; i >= 0; i--) {
            if (seq[i] != '-') {
                break;
            }
            seq[i] = 'N';
        }

        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] != 'A' && seq[i] != 'T' && seq[i] != 'C' && seq[i] != 'G' && seq[i] != '-') {
                positions.add(i + 1);
            }
        }
        return positions;
    }


    /**
     * X is considered as unknown.
     */
    public static List<Integer> findAAUnknowns(String alignedSeq) {
        char[] seq = alignedSeq.toUpperCase().toCharArray();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] == 'X') {
                positions.add(i + 1);
            }
        }
        return positions;
    }


    /**
     * This takes a list of position integers and introduces range representations if appropriate. Example:
     * 1,5,6,7,8,20 will be transformed to 1,5-8,20
     */
    public static List<String> compressPositionsAsStrings(List<Integer> positions) {
        List<String> result = new ArrayList<>();
        Integer rangeStart = null;
        Integer rangeEnd = null;

        for (int pos : positions) {
            if (rangeStart == null) {
                // Initial case
                rangeStart = pos;
                rangeEnd = pos;
            } else if (pos == rangeEnd + 1) {
                // If the range is being continued
                rangeEnd = pos;
            } else {
                // If the range ended
                if (rangeEnd - rangeStart > 1) {
                    // If there is at least one number in between
                    result.add(rangeStart + "-" + rangeEnd);
                } else if (rangeEnd - rangeStart > 0) {
                    // If there are two different numbers
                    result.add(rangeStart.toString());
                    result.add(rangeEnd.toString());
                } else {
                    // If there is a single number
                    result.add(rangeStart.toString());
                }
                rangeStart = pos;
                rangeEnd = pos;
            }
        }
        if (rangeStart != null) {
            // Finishing up - same code like above
            if (rangeEnd - rangeStart > 1) {
                // If there is at least one number in between
                result.add(rangeStart + "-" + rangeEnd);
            } else if (rangeEnd - rangeStart > 0) {
                // If there are two different numbers
                result.add(rangeStart.toString());
                result.add(rangeEnd.toString());
            } else {
                // If there is a single number
                result.add(rangeStart.toString());
            }
        }
        return result;
    }

    /**
     * This takes a list of position integers and introduces range representations if appropriate. Example:
     * 1,5,6,7,8,20 will be transformed to ([1, 5, 8,  20], [false, true, false, false])
     * The first entry in the tuple are the positions. The second entry in the tuple say whether the corresponding
     * position defines the start of a range.
     */
    public static Pair<short[], boolean[]> compressPositionsAsArrays(List<Short> positions) {
        List<Short> resultPositionsList = new ArrayList<>();
        List<Boolean> resultIsStartRangeList = new ArrayList<>();
        Short rangeStart = null;
        Short rangeEnd = null;

        for (short pos : positions) {
            if (rangeStart == null) {
                // Initial case
                rangeStart = pos;
                rangeEnd = pos;
            } else if (pos == rangeEnd + 1) {
                // If the range is being continued
                rangeEnd = pos;
            } else {
                // If the range ended
                if (rangeEnd - rangeStart > 1) {
                    // If there is at least one number in between
                    resultPositionsList.add(rangeStart);
                    resultPositionsList.add(rangeEnd);
                    resultIsStartRangeList.add(true);
                    resultIsStartRangeList.add(false);
                } else if (rangeEnd - rangeStart > 0) {
                    // If there are two different numbers
                    resultPositionsList.add(rangeStart);
                    resultPositionsList.add(rangeEnd);
                    resultIsStartRangeList.add(false);
                    resultIsStartRangeList.add(false);
                } else {
                    // If there is a single number
                    resultPositionsList.add(rangeStart);
                    resultIsStartRangeList.add(false);
                }
                rangeStart = pos;
                rangeEnd = pos;
            }
        }
        if (rangeStart != null) {
            // Finishing up - same code like above
            if (rangeEnd - rangeStart > 1) {
                // If there is at least one number in between
                resultPositionsList.add(rangeStart);
                resultPositionsList.add(rangeEnd);
                resultIsStartRangeList.add(true);
                resultIsStartRangeList.add(false);
            } else if (rangeEnd - rangeStart > 0) {
                // If there are two different numbers
                resultPositionsList.add(rangeStart);
                resultPositionsList.add(rangeEnd);
                resultIsStartRangeList.add(false);
                resultIsStartRangeList.add(false);
            } else {
                // If there is a single number
                resultPositionsList.add(rangeStart);
                resultIsStartRangeList.add(false);
            }
        }
        short[] resultPositions = new short[resultPositionsList.size()];
        boolean[] resultIsStartRange = new boolean[resultPositionsList.size()];
        for (int j = 0; j < resultPositionsList.size(); j++) {
            resultPositions[j] = resultPositionsList.get(j);
            resultIsStartRange[j] = resultIsStartRangeList.get(j);
        }
        return new Pair<>(resultPositions, resultIsStartRange);
    }
}

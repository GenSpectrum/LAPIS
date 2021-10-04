package ch.ethz.lapis.source;

import ch.ethz.lapis.util.ReferenceGenomeData;
import java.util.ArrayList;
import java.util.List;


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
}

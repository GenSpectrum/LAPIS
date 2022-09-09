package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.query.VariantQueryExpr;
import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.Utils;

public class AAMutation implements VariantQueryExpr {

    private static final ReferenceGenomeData referenceGenome = ReferenceGenomeData.getInstance();

    private String gene;

    private int position;

    private Character mutation;

    /**
     * If true, unknowns will be mapped to true during the evaluation. If false, unknowns will be mapped to false.
     */
    private boolean applyMaybe = false;

    public AAMutation() {
    }

    public AAMutation(String gene, int position) {
        this.gene = gene;
        this.position = position;
    }

    public AAMutation(String gene, int position, Character mutation) {
        this.gene = gene;
        this.position = position;
        this.mutation = mutation;
    }

    /**
     * @param mutationCode Examples: "S:501Y", "S:501"
     */
    public static AAMutation parse(String mutationCode) {
        String[] split = Utils.normalizeAAMutation(mutationCode).split(":");
        String gene = split[0];
        if (!Character.isDigit(split[1].charAt(split[1].length() - 1))) {
            return new AAMutation(
                gene,
                Integer.parseInt(split[1].substring(0, split[1].length() - 1)),
                split[1].charAt(split[1].length() - 1)
            );
        } else {
            return new AAMutation(gene, Integer.parseInt(split[1]));
        }
    }

    public String getGene() {
        return gene;
    }

    public AAMutation setGene(String gene) {
        this.gene = gene;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public AAMutation setPosition(int position) {
        this.position = position;
        return this;
    }

    public Character getMutation() {
        return mutation;
    }

    public AAMutation setMutation(Character mutation) {
        this.mutation = mutation;
        return this;
    }

    public boolean isApplyMaybe() {
        return applyMaybe;
    }

    public AAMutation setApplyMaybe(boolean applyMaybe) {
        this.applyMaybe = applyMaybe;
        return this;
    }

    @Override
    public String toString() {
        return "AAMutation{" +
            "gene='" + gene + '\'' +
            ", position=" + position +
            ", mutation=" + mutation +
            '}';
    }

    @Override
    public boolean[] evaluate(Database database) {
        char[] data = database.getAAArray(gene, position);
        boolean[] result = new boolean[data.length];
        if (!applyMaybe) {
            for (int i = 0; i < result.length; i++) {
                result[i] = isMatchingMutation(data[i], this);
            }
        } else {
            for (int i = 0; i < result.length; i++) {
                result[i] = isMaybeMatchingMutation(data[i], this);
            }
        }
        return result;
    }

    public static boolean isMatchingMutation(Character foundBase, AAMutation searchedMutation) {
        Character mutationBase = searchedMutation.getMutation();
        if (mutationBase == null) {
            // Check whether the base is mutated, i.e., not equal the base of the reference genome and not unknown (X)
            return foundBase != 'X' && foundBase != referenceGenome.getGeneAABase(searchedMutation.getGene(),
                searchedMutation.getPosition());
        } else if (mutationBase == '.') {
            // Check whether the base is not mutated, i.e., equals the base of the reference genome
            return foundBase == referenceGenome.getGeneAABase(searchedMutation.getGene(),
                searchedMutation.getPosition());
        } else {
            return foundBase == mutationBase;
        }
    }

    public static boolean isMaybeMatchingMutation(Character foundBase, AAMutation searchedMutation) {
        if (!isAmbiguityCode(foundBase)) {
            return isMatchingMutation(foundBase, searchedMutation);
        }
        // There is only a code for "unknown" for AAs. Thus, this function always returns true if the found base is
        // ambiguous.
        return true;
    }

    public static boolean isAmbiguityCode(Character base) {
        return base == 'X';
    }
}

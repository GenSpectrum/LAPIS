package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.query.VariantQueryExpr;
import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.Utils;

public class NucMutation implements VariantQueryExpr {

    private static final ReferenceGenomeData referenceGenome = ReferenceGenomeData.getInstance();

    private int position;

    private Character mutation;

    public NucMutation() {
    }

    public NucMutation(int position) {
        this.position = position;
    }

    public NucMutation(int position, Character mutation) {
        this.position = position;
        this.mutation = mutation;
    }

    /**
     * @param mutationCode Examples: "1234T", "2345"
     */
    public static NucMutation parse(String mutationCode) {
        mutationCode = Utils.normalizeNucMutation(mutationCode);
        if (!Character.isDigit(mutationCode.charAt(mutationCode.length() - 1))) {
            return new NucMutation(
                Integer.parseInt(mutationCode.substring(0, mutationCode.length() - 1)),
                mutationCode.charAt(mutationCode.length() - 1)
            );
        } else {
            return new NucMutation(Integer.parseInt(mutationCode));
        }
    }

    public int getPosition() {
        return position;
    }

    public NucMutation setPosition(int position) {
        this.position = position;
        return this;
    }

    public Character getMutation() {
        return mutation;
    }

    public NucMutation setMutation(Character mutation) {
        this.mutation = mutation;
        return this;
    }

    @Override
    public String toString() {
        return "NucMutation{" +
            "position=" + position +
            ", mutation=" + mutation +
            '}';
    }

    @Override
    public boolean[] evaluate(Database database) {
        char[] data = database.getNucArray(position);
        boolean[] result = new boolean[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = isMatchingMutation(data[i], this);
        }
        return result;
    }

    public static boolean isMatchingMutation(Character foundBase, NucMutation searchedMutation) {
        Character mutationBase = searchedMutation.getMutation();
        if (searchedMutation.getMutation() == null) {
            // Check whether the base is mutated, i.e., not equal the base of the reference genome and not unknown (N)
            return foundBase != 'N' && foundBase != referenceGenome.getNucleotideBase(searchedMutation.getPosition());
        } else if (mutationBase == '.') {
            // Check whether the base is not mutated, i.e., equals the base of the reference genome
            return foundBase == referenceGenome.getNucleotideBase(searchedMutation.getPosition());
        } else {
            return foundBase == searchedMutation.getMutation();
        }
    }
}

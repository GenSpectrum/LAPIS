package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.VariantQueryExpr;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.Utils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NucMutation implements VariantQueryExpr {

    private static final ReferenceGenomeData referenceGenome = ReferenceGenomeData.getInstance();
    private static final Map<Character, Set<Character>> ambiguityCodes = new HashMap<>() {{
       put('M', Set.of('A', 'C'));
       put('R', Set.of('A', 'G'));
       put('W', Set.of('A', 'T'));
       put('S', Set.of('C', 'G'));
       put('Y', Set.of('C', 'T'));
       put('K', Set.of('G', 'T'));
       put('V', Set.of('A', 'C', 'G'));
       put('H', Set.of('A', 'C', 'T'));
       put('D', Set.of('A', 'G', 'T'));
       put('B', Set.of('C', 'G', 'T'));
       put('N', Set.of('G', 'A', 'T', 'C', '-'));
       put('X', Set.of('G', 'A', 'T', 'C', '-'));
    }};

    private int position;

    private Character mutation;

    /**
     * If true, unknowns/ambiguity will be mapped to true during the evaluation. If false, unknowns/ambiguity will be
     * mapped to false.
     */
    private boolean applyMaybe = false;

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

    public boolean isApplyMaybe() {
        return applyMaybe;
    }

    public NucMutation setApplyMaybe(boolean applyMaybe) {
        this.applyMaybe = applyMaybe;
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

    public static boolean isMatchingMutation(Character foundBase, NucMutation searchedMutation) {
        Character mutationBase = searchedMutation.getMutation();
        if (mutationBase == null) {
            // Check whether the base is mutated, i.e., not equal the base of the reference genome and not unknown (N)
            return foundBase != 'N' && foundBase != referenceGenome.getNucleotideBase(searchedMutation.getPosition());
        } else if (mutationBase == '.') {
            // Check whether the base is not mutated, i.e., equals the base of the reference genome
            return foundBase == referenceGenome.getNucleotideBase(searchedMutation.getPosition());
        } else if (mutationBase == '~') {
            // Check whether the base is unknown/ambiguous, i.e., it is not A, T, C or G
            return !(foundBase == 'A' || foundBase == 'T' || foundBase == 'C' || foundBase == 'G');
        } else {
            return foundBase == searchedMutation.getMutation();
        }
    }

    public static boolean isMaybeMatchingMutation(Character foundBase, NucMutation searchedMutation) {
        Character mutationBase = searchedMutation.getMutation();
        if (!isAmbiguityCode(foundBase)) {
            return isMatchingMutation(foundBase, searchedMutation);
        }
        var foundPossibleBases = ambiguityCodes.get(foundBase);
        if (mutationBase == null) {
            // Check whether the base is maybe mutated. As the found base is ambiguous, this has to be true.
            return true;
        } else if (mutationBase == '.') {
            // Check whether the base is maybe not mutated
            return foundPossibleBases.contains(referenceGenome.getNucleotideBase(searchedMutation.getPosition()));
        } else if (mutationBase == '~') {
            // Check whether the base is unknown/ambiguous
            return true;
        } else {
            return foundPossibleBases.contains(searchedMutation.getMutation());
        }
    }

    public static boolean isAmbiguityCode(Character base) {
        return ambiguityCodes.containsKey(base);
    }
}

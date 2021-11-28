package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.entity.query.VariantQueryExpr;
import ch.ethz.lapis.util.Utils;

public class NucMutation implements VariantQueryExpr {

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
}

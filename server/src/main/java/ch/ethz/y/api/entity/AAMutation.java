package ch.ethz.y.api.entity;

public class AAMutation {

    private String gene;

    private int position;

    private Character mutation;

    public AAMutation(String gene, int position) {
        this.gene = gene;
        this.position = position;
    }

    public AAMutation(String gene, int position, Character mutation) {
        this.gene = gene;
        this.position = position;
        this.mutation = mutation;
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

    /**
     * @param mutationCode Examples: "S:501Y", "S:501"
     */
    public static AAMutation parse(String mutationCode) {
        String[] split = mutationCode.split(":");
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
}

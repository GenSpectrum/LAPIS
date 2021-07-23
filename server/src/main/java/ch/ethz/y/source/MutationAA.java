package ch.ethz.y.source;

public class MutationAA {

    private String gene;
    private int position;
    private String mutation;

    MutationAA() {
    }

    public String getGene() {
        return gene;
    }

    public MutationAA setGene(String gene) {
        this.gene = gene;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public MutationAA setPosition(int position) {
        this.position = position;
        return this;
    }

    public String getMutation() {
        return mutation;
    }

    public MutationAA setMutation(String mutation) {
        this.mutation = mutation;
        return this;
    }

    /**
     *
     * @param s In the format that Nextclade produces: e.g., ORF1a:F3677-, S:N501Y
     */
    public static MutationAA parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] split = s.split(":");
        String gene = split[0];
        int position = Integer.parseInt(split[1].substring(1, split[1].length() - 1));
        String mutation = split[1].substring(split[1].length() - 1);
        return new MutationAA()
                .setGene(gene)
                .setPosition(position)
                .setMutation(mutation);
    }
}

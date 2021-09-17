package ch.ethz.lapis.source;

public class MutationNuc {

    private int position;
    private String mutation;

    MutationNuc() {
    }

    public MutationNuc(int position, String mutation) {
        this.position = position;
        this.mutation = mutation;
    }

    public int getPosition() {
        return position;
    }

    public MutationNuc setPosition(int position) {
        this.position = position;
        return this;
    }

    public String getMutation() {
        return mutation;
    }

    public MutationNuc setMutation(String mutation) {
        this.mutation = mutation;
        return this;
    }

    /**
     *
     * @param s E.g., 241T, 6954C, 11288-
     */
    public static MutationNuc parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        int position = Integer.parseInt(s.substring(0, s.length() - 1));
        String mutation = s.substring(s.length() - 1);
        return new MutationNuc()
                .setPosition(position)
                .setMutation(mutation);
    }
}

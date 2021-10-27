package ch.ethz.lapis.api.entity.res;

import java.util.ArrayList;
import java.util.Collection;

public class SampleMutationsResponse extends ArrayList<SampleMutationsResponse.MutationEntry> {

    public SampleMutationsResponse() {
    }

    public SampleMutationsResponse(Collection<? extends MutationEntry> c) {
        super(c);
    }

    public static class MutationEntry {

        private final String mutation;
        private final double proportion;
        private final int count;

        public MutationEntry(String mutation, double proportion, int count) {
            this.mutation = mutation;
            this.proportion = proportion;
            this.count = count;
        }

        public String getMutation() {
            return mutation;
        }

        public double getProportion() {
            return proportion;
        }

        public int getCount() {
            return count;
        }
    }
}

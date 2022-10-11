package ch.ethz.lapis.api.entity.res;

import java.util.ArrayList;
import java.util.Collection;

public class SampleMutationsResponse extends ArrayList<SampleMutationsResponse.MutationEntry> {

    public SampleMutationsResponse() {
    }

    public SampleMutationsResponse(Collection<? extends MutationEntry> c) {
        super(c);
    }

    public record MutationEntry(String mutation, double proportion, int count) {
    }
}

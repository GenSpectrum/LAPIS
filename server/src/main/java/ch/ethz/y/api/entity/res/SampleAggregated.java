package ch.ethz.y.api.entity.res;

public class SampleAggregated extends SampleMetadata<SampleAggregated> {

    private int count;

    public int getCount() {
        return count;
    }

    public SampleAggregated setCount(int count) {
        this.count = count;
        return this;
    }
}

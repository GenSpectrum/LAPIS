package ch.ethz.lapis.api.entity.req;


public class MutationRequest extends SampleDetailRequest {

    private float minProportion = 0.05f;

    public float getMinProportion() {
        return minProportion;
    }

    public MutationRequest setMinProportion(float minProportion) {
        this.minProportion = minProportion;
        return this;
    }
}

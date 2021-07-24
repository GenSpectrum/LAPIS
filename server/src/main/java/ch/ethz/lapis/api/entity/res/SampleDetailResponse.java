package ch.ethz.lapis.api.entity.res;

import java.util.ArrayList;
import java.util.Collection;

public class SampleDetailResponse extends ArrayList<SampleDetail> {

    public SampleDetailResponse(Collection<? extends SampleDetail> c) {
        super(c);
    }
}

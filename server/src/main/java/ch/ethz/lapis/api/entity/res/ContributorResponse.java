package ch.ethz.lapis.api.entity.res;

import java.util.ArrayList;
import java.util.Collection;

public class ContributorResponse extends ArrayList<Contributor> {

    public ContributorResponse(Collection<? extends Contributor> c) {
        super(c);
    }
}

package ch.ethz.lapis.api.entity.res;

public class NextcladeDatasetInfoResponse {

    private final String tag;
    private final String name;

    public NextcladeDatasetInfoResponse(String tag, String name) {
        this.tag = tag;
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }
}

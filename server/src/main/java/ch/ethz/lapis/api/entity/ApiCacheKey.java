package ch.ethz.lapis.api.entity;


public class ApiCacheKey {

    private String endpointName;

    private Object requestObject;

    public ApiCacheKey() {
    }

    public ApiCacheKey(String endpointName, Object requestObject) {
        this.endpointName = endpointName;
        this.requestObject = requestObject;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public ApiCacheKey setEndpointName(String endpointName) {
        this.endpointName = endpointName;
        return this;
    }

    public Object getRequestObject() {
        return requestObject;
    }

    public ApiCacheKey setRequestObject(Object requestObject) {
        this.requestObject = requestObject;
        return this;
    }
}

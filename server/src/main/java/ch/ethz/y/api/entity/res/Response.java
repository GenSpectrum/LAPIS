package ch.ethz.y.api.entity.res;

import java.util.ArrayList;
import java.util.List;

public class Response<Payload> {

    private Payload payload;
    private List<Object> errors = new ArrayList<>();
    private Information info = new Information();

    public Response() {
    }

    public Response(Payload payload) {
        this.payload = payload;
    }

    public Payload getPayload() {
        return payload;
    }

    public Response<Payload> setPayload(Payload payload) {
        this.payload = payload;
        return this;
    }

    public List<Object> getErrors() {
        return errors;
    }

    public Response<Payload> setErrors(List<Object> errors) {
        this.errors = errors;
        return this;
    }

    public Information getInfo() {
        return info;
    }

    public Response<Payload> setInfo(Information info) {
        this.info = info;
        return this;
    }
}

package ch.ethz.lapis.api.entity.res;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

public class V0Response<Payload> {

    private Payload payload;
    private List<Object> errors = new ArrayList<>();
    private Information info = new Information()
            .setApiVersion(0)
            .setDeprecationDate(LocalDate.of(2021, Month.AUGUST, 25))
            .setDeprecationInfo("This is a beta version. Data are not being updated. It will soon be replaced with" +
                    " the official release (version 1). Please send us feedback through" +
                    " https://github.com/cevo-public/LAPIS/issues.");

    public V0Response() {
    }

    public V0Response(Payload payload) {
        this.payload = payload;
    }

    public Payload getPayload() {
        return payload;
    }

    public V0Response<Payload> setPayload(Payload payload) {
        this.payload = payload;
        return this;
    }

    public List<Object> getErrors() {
        return errors;
    }

    public V0Response<Payload> setErrors(List<Object> errors) {
        this.errors = errors;
        return this;
    }

    public Information getInfo() {
        return info;
    }

    public V0Response<Payload> setInfo(Information info) {
        this.info = info;
        return this;
    }
}

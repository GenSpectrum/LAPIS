package ch.ethz.lapis.api.entity.res;

import java.util.ArrayList;
import java.util.List;

public class V1Response<Data> {

    private Data data;
    private List<ErrorEntry> errors = new ArrayList<>();
    private Information info = new Information()
        .setApiVersion(1)
        .setDeprecationDate(null)
        .setDeprecationInfo(null);

    public V1Response() {
    }

    public V1Response(Data data, long dataVersion) {
        this.data = data;
        this.info.setDataVersion(dataVersion);
    }

    public Data getData() {
        return data;
    }

    public V1Response<Data> setData(Data data) {
        this.data = data;
        return this;
    }

    public List<ErrorEntry> getErrors() {
        return errors;
    }

    public V1Response<Data> setErrors(List<ErrorEntry> errors) {
        this.errors = errors;
        return this;
    }

    public Information getInfo() {
        return info;
    }

    public V1Response<Data> setInfo(Information info) {
        this.info = info;
        return this;
    }
}

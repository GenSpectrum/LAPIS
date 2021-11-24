package ch.ethz.lapis.api.entity.res;

import ch.ethz.lapis.api.entity.OpennessLevel;
import java.util.ArrayList;
import java.util.List;

public class V1Response<Data> {

    private List<ErrorEntry> errors = new ArrayList<>();
    private Information info = new Information()
        .setApiVersion(1)
        .setDeprecationDate(null)
        .setDeprecationInfo(null)
        .setAcknowledgement(null);
    private Data data;

    public V1Response() {
    }

    public V1Response(Data data, long dataVersion, OpennessLevel opennessLevel) {
        this.data = data;
        this.info.setDataVersion(dataVersion);
        if (opennessLevel == OpennessLevel.GISAID) {
            this.info.setAcknowledgement("The data are obtained from GISAID and remain subject to GISAID’s Terms "
                + "and Conditions (http://gisaid.org/daa). Please reference GISAID and adhere to GISAID's Terms and "
                + "Conditions if you use the data from this API.");
        }
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

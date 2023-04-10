package ch.ethz.lapis.api.sql;

import lombok.Data;

@Data
public class EndpointResponse {

    public static final String PLACEHOLDER = "\"THIS_IS_THE_PLACEHOLDER_WHERE_THE_JSON_DATA_WILL_BE_INSERTED\"";

    private final String data = "THIS_IS_THE_PLACEHOLDER_WHERE_THE_JSON_DATA_WILL_BE_INSERTED";
    private String error;

}

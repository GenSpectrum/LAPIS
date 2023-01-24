package ch.ethz.lapis.source.gisaid;

import ch.ethz.lapis.core.Config;
import lombok.Data;

@Data
public class GisaidApiConfig implements Config {
    private String url;
    private String username;
    private String password;
}

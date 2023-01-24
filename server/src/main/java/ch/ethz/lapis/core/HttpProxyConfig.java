package ch.ethz.lapis.core;

import lombok.Data;

@Data
public class HttpProxyConfig implements Config {
    private Boolean activated;
    private String host;
    private Integer port;
}

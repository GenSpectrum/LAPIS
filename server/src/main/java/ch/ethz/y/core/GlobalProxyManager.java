package ch.ethz.y.core;

public class GlobalProxyManager {
    public static void setProxyFromConfig(HttpProxyConfig config) {
        if (config.getActivated()) {
            System.setProperty("http.proxyHost", config.getHost());
            System.setProperty("http.proxyPort", String.valueOf(config.getPort()));
            System.setProperty("https.proxyHost", config.getHost());
            System.setProperty("https.proxyPort", String.valueOf(config.getPort()));
        }
    }
}

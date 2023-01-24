package ch.ethz.lapis.util;

import org.springframework.stereotype.Component;

@Component
public class TimeFactory {
    public long now() {
        return System.currentTimeMillis();
    }
}

package ch.ethz.lapis.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TimeFactory {
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}

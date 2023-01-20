package ch.ethz.lapis.core;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class NapiNotification {
    /**
     * Sends the report to the "cs-ingest-default" channel of our instance of
     * <a href="https://github.com/chaoran-chen/napi">napi</a>
     */
    public static void sendNotification(String authKey, String level, String subject, String body) {
        try {
            if (authKey == null || authKey.isBlank()) {
                log.info("No notification will be sent due to missing authentication key");
                return;
            }
            String url = "https://dev.cov-spectrum.org/notification/send";

            // Send
            var restTemplate = new RestTemplate();
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var jsonBody = new JSONObject();
            jsonBody.put("auth_key", authKey);
            jsonBody.put("channel", "cs-ingest-default");
            jsonBody.put("level", level);
            jsonBody.put("subject", subject);
            jsonBody.put("body", body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody.toString(), headers);
            restTemplate.postForObject(url, request, String.class);
            log.info("Notification sent");
        } catch (Exception e) {
            // We don't consider notifications as extremely essential, so we won't crash the program if it fails.
            log.error("Notification could not be sent." + e.getMessage(), e);
        }
    }
}

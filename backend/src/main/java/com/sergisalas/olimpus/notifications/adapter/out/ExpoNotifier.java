package com.sergisalas.olimpus.notifications.adapter.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergisalas.olimpus.notifications.domain.Notice;
import com.sergisalas.olimpus.notifications.domain.Notifier;
import com.sergisalas.olimpus.notifications.domain.PushTokenRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends through Expo's push service, which is what reaches both iPhone and
 * Android without dealing with Apple and Google separately.
 *
 * <p>Nothing here is allowed to break anything else: a failed send is logged and
 * that is the end of it. A notice that does not arrive is a nuisance; a round
 * that does not run because a notice failed would be a real problem.
 */
@Component
public class ExpoNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(ExpoNotifier.class);

    private static final URI EXPO = URI.create("https://exp.host/--/api/v2/push/send");

    /** Expo accepts up to a hundred messages per request. */
    private static final int BATCH = 100;

    private final PushTokenRepository tokens;
    private final ObjectMapper json;
    private final boolean enabled;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public ExpoNotifier(
            PushTokenRepository tokens,
            ObjectMapper json,
            @Value("${olimpus.notifications.enabled:true}") boolean enabled) {
        this.tokens = tokens;
        this.json = json;
        this.enabled = enabled;
    }

    @Override
    public void send(List<Notice> notices) {
        List<Map<String, Object>> messages = new ArrayList<>();

        for (Notice notice : notices) {
            for (String token : tokens.tokensOf(notice.accountId())) {
                Map<String, Object> message = new LinkedHashMap<>();
                message.put("to", token);
                message.put("title", notice.title());
                message.put("body", notice.body());
                message.put("sound", "default");
                message.put("data", Map.of("kind", notice.kind().name(),
                        "deepLink", notice.deepLink() == null ? "" : notice.deepLink()));
                messages.add(message);
            }
        }

        if (messages.isEmpty()) return;

        if (!enabled) {
            log.info("Notifications are off: {} would have been sent.", messages.size());
            return;
        }

        for (int from = 0; from < messages.size(); from += BATCH) {
            postQuietly(messages.subList(from, Math.min(from + BATCH, messages.size())));
        }
    }

    private void postQuietly(List<Map<String, Object>> batch) {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder(EXPO)
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(20))
                            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(batch)))
                            .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                log.warn("Push service answered {}: {}", response.statusCode(), response.body());
            } else {
                log.info("Sent {} notifications.", batch.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Could not send notifications: {}", e.toString());
        }
    }
}

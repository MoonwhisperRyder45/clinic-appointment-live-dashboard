package clinic.dashboard.infrai;

import clinic.dashboard.config.DashboardConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Infrai {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ERROR_CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\\\"(?:message|hint)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final DashboardConfig config;
    private final HttpClient http;

    public Infrai(DashboardConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void metricsBatch(List<Map<String, Object>> points, String operationId) {
        post("/v1/metrics/batch", Map.of("points", points, "idempotency_key", operationId), operationId);
    }

    public void realtimePublish(String event, Map<String, Object> data, String operationId) {
        post("/v1/realtime/publish", Map.of(
                "channel", config.channel(),
                "event", event,
                "data", data,
                "account_id", config.accountId()), operationId);
    }

    private void post(String path, Map<String, Object> payload, String operationId) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", operationId)
                .method("POST", HttpRequest.BodyPublishers.ofString(Json.encode(payload)))
                .build();
        send(request);
    }

    private void send(HttpRequest request) {
        for (int attempt = 1; attempt <= 4; attempt++) {
            HttpResponse<String> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException exception) {
                if (attempt == 4) throw new IllegalStateException("Infrai transport failed", exception);
                pause(250L * (1L << (attempt - 1)));
                continue;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Request interrupted", exception);
            }

            Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
            if (response.statusCode() == 429 && attempt < 4) {
                long fallback = 250L * (1L << (attempt - 1));
                long delay = response.headers().firstValue("Retry-After")
                        .map(Infrai::retryAfterMillis).orElse(fallback);
                pause(delay);
                continue;
            }
            if (!envelope.ok()) throw new InfraiException(envelope.code(), envelope.detail(), response.statusCode());
            if (response.statusCode() >= 500) throw new IllegalStateException("Infrai request failed");
            return;
        }
    }

    private static Envelope decodeEnvelope(String body, int status) {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) throw new IllegalStateException("Invalid Infrai response envelope (HTTP " + status + ")");
        if (Boolean.parseBoolean(ok.group(1))) return new Envelope(true, "", "");
        return new Envelope(false, match(ERROR_CODE, body, "REQUEST_REJECTED"),
                match(ERROR_MESSAGE, body, "Request rejected"));
    }

    private static String match(Pattern pattern, String body, String fallback) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static long retryAfterMillis(String value) {
        try { return Long.parseLong(value) * 1000L; }
        catch (NumberFormatException ignored) { return 1000L; }
    }

    private static void pause(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", exception);
        }
    }

    private record Envelope(boolean ok, String code, String detail) {}
}

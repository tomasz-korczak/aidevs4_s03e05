package pl.tomaszko.s03e05.llm;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;
import pl.tomaszko.s03e05.config.AppProperties;

public final class OpenRouterRetry {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterRetry.class);

    private final AppProperties.Retry retry;

    public OpenRouterRetry(AppProperties.Retry retry) {
        this.retry = retry;
    }

    public <T> T execute(Supplier<T> action) {
        int maxAttempts = Math.max(1, retry.getMaxAttempts());
        Duration backoff = retry.getInitialBackoff();
        Duration maxBackoff = retry.getMaxBackoff();
        double multiplier = retry.getMultiplier() < 1.0 ? 1.0 : retry.getMultiplier();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                last = ex;
                if (!isRetryable(ex) || attempt == maxAttempts) {
                    throw ex;
                }
                log.warn(
                        "OpenRouter attempt {}/{} failed ({}). Retrying in {}",
                        attempt,
                        maxAttempts,
                        rootMessage(ex),
                        backoff);
                sleep(backoff);
                long nextMillis = (long) (backoff.toMillis() * multiplier);
                backoff = Duration.ofMillis(Math.min(nextMillis, maxBackoff.toMillis()));
            }
        }
        throw last;
    }

    static boolean isRetryable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof RestClientResponseException rest
                    && isRetryableStatus(rest.getStatusCode())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && messageLooksRetryable(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isRetryableStatus(HttpStatusCode status) {
        int value = status.value();
        return value == 429 || value == 502 || value == 503 || value == 504 || value == 529;
    }

    private static boolean messageLooksRetryable(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("rate limit") || lower.contains("too many requests")) {
            return true;
        }
        return lower.matches("(?s).*\\b(429|502|503|504|529)\\b.*");
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        String message = ex.getMessage();
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message != null ? message : ex.getClass().getSimpleName();
    }

    private static void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry OpenRouter", ex);
        }
    }
}

package pl.tomaszko.s03e05.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import pl.tomaszko.s03e05.config.AppProperties;

class OpenRouterRetryTest {

    @Test
    void retries429ThenSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        Object result = new Object();
        OpenRouterRetry retry = new OpenRouterRetry(fastRetry(3));

        Object actual = retry.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new RuntimeException("429: Provider returned error");
            }
            return result;
        });

        assertSame(result, actual);
        org.junit.jupiter.api.Assertions.assertEquals(2, attempts.get());
    }

    @Test
    void doesNotRetryClientErrors() {
        AtomicInteger attempts = new AtomicInteger();
        RuntimeException failure = new RuntimeException("400: bad request");
        OpenRouterRetry retry = new OpenRouterRetry(fastRetry(4));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> retry.execute(failing(attempts, failure)));

        assertSame(failure, thrown);
        org.junit.jupiter.api.Assertions.assertEquals(1, attempts.get());
    }

    @Test
    void exhaustedRetriesRethrow() {
        AtomicInteger attempts = new AtomicInteger();
        RuntimeException failure = new RuntimeException("429: Provider returned error");
        OpenRouterRetry retry = new OpenRouterRetry(fastRetry(2));

        assertThrows(RuntimeException.class, () -> retry.execute(failing(attempts, failure)));
        org.junit.jupiter.api.Assertions.assertEquals(2, attempts.get());
    }

    @Test
    void detectsRetryableMessages() {
        assertTrue(OpenRouterRetry.isRetryable(new RuntimeException("429: Provider returned error")));
        assertTrue(OpenRouterRetry.isRetryable(new RuntimeException("HTTP 503")));
        assertFalse(OpenRouterRetry.isRetryable(new RuntimeException("400: Provider returned error")));
    }

    private static Supplier<Object> failing(AtomicInteger attempts, RuntimeException failure) {
        return () -> {
            attempts.incrementAndGet();
            throw failure;
        };
    }

    private static AppProperties.Retry fastRetry(int maxAttempts) {
        AppProperties.Retry retry = new AppProperties.Retry();
        retry.setMaxAttempts(maxAttempts);
        retry.setInitialBackoff(Duration.ofMillis(1));
        retry.setMaxBackoff(Duration.ofMillis(5));
        retry.setMultiplier(2);
        return retry;
    }
}

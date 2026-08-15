package pl.tomaszko.s03e05.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import pl.tomaszko.s03e05.TestAppProperties;
import pl.tomaszko.s03e05.config.AppProperties;
import pl.tomaszko.s03e05.hub.HubCallResult;
import pl.tomaszko.s03e05.hub.HubClient;
import pl.tomaszko.s03e05.logging.SecretRedactor;
import pl.tomaszko.s03e05.logging.ToolExecutionLogger;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;

class DiscoverToolDelayTest {

    @Test
    void spacesHubCallsByConfiguredDelay() {
        AppProperties properties = TestAppProperties.create();
        properties.getTools().setDiscoverDelay(Duration.ofMillis(80));
        HubClient hubClient = mock(HubClient.class);
        when(hubClient.discover(anyString(), anyString())).thenReturn(HubCallResult.ok("ok"));
        DiscoverTool tool = new DiscoverTool(
                hubClient,
                properties,
                new ToolBudget(properties),
                new GameSession(),
                reporter(properties),
                logger());

        long started = System.nanoTime();
        tool.discover("first", null);
        tool.discover("second", "/api/maps");
        long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

        assertTrue(elapsedMs >= 80, "elapsed " + elapsedMs + " ms");
    }

    private static OperatorReporter reporter(AppProperties properties) {
        SecretRedactor redactor = mock(SecretRedactor.class);
        when(redactor.redact(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        return new OperatorReporter(redactor, new ToolBudget(properties));
    }

    private static ToolExecutionLogger logger() {
        SecretRedactor redactor = mock(SecretRedactor.class);
        when(redactor.redact(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        return new ToolExecutionLogger(redactor);
    }
}

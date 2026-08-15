package pl.tomaszko.s03e05.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import pl.tomaszko.s03e05.TestAppProperties;
import pl.tomaszko.s03e05.config.AppProperties;
import pl.tomaszko.s03e05.hub.HubCallResult;
import pl.tomaszko.s03e05.hub.HubClient;
import pl.tomaszko.s03e05.logging.SecretRedactor;
import pl.tomaszko.s03e05.logging.ToolExecutionLogger;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;

class ToolBudgetConsumptionTest {

    @Test
    void discoverDecrementsOnSuccessAndOnUnreadable() {
        AppProperties properties = TestAppProperties.create();
        properties.getTools().setDiscoverLimit(3);
        HubClient hubClient = mock(HubClient.class);
        when(hubClient.discover(anyString(), anyString()))
                .thenReturn(HubCallResult.ok("ok"))
                .thenReturn(HubCallResult.failed("Hub unreachable or timed out"));
        ToolBudget budget = new ToolBudget(properties);
        GameSession session = new GameSession();
        DiscoverTool tool = new DiscoverTool(
                hubClient, properties, budget, session, reporter(budget), logger());

        tool.discover("q1", null);
        assertEquals(2, budget.getDiscoverRemaining());
        tool.discover("q2", "/api/notes");
        assertEquals(1, budget.getDiscoverRemaining());
    }

    @Test
    void verifyDecrementsOnSuccessAndOnUnreadable() {
        AppProperties properties = TestAppProperties.create();
        properties.getTools().setVerifyLimit(3);
        HubClient hubClient = mock(HubClient.class);
        when(hubClient.verify(anyList()))
                .thenReturn(HubCallResult.ok("nope"))
                .thenReturn(HubCallResult.failed("Empty hub response"));
        ToolBudget budget = new ToolBudget(properties);
        GameSession session = new GameSession();
        VerifyTool tool = new VerifyTool(hubClient, budget, session, reporter(budget), logger());

        tool.verify(new String[] {"right"});
        assertEquals(2, budget.getVerifyRemaining());
        tool.verify(new String[] {"left"});
        assertEquals(1, budget.getVerifyRemaining());
        verify(hubClient, times(2)).verify(anyList());
    }

    private static OperatorReporter reporter(ToolBudget budget) {
        SecretRedactor redactor = mock(SecretRedactor.class);
        when(redactor.redact(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        return new OperatorReporter(redactor, budget);
    }

    private static ToolExecutionLogger logger() {
        SecretRedactor redactor = mock(SecretRedactor.class);
        when(redactor.redact(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        return new ToolExecutionLogger(redactor);
    }
}

package pl.tomaszko.s03e05.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import pl.tomaszko.s03e05.TestAppProperties;
import pl.tomaszko.s03e05.hub.HubClient;
import pl.tomaszko.s03e05.logging.SecretRedactor;
import pl.tomaszko.s03e05.logging.ToolExecutionLogger;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;

class VerifyToolLocalRejectTest {

    @Test
    void nullAnswerDoesNotCallHubOrConsumeBudget() {
        assertLocalReject(null);
    }

    @Test
    void emptyAnswerDoesNotCallHubOrConsumeBudget() {
        assertLocalReject(new String[0]);
    }

    private static void assertLocalReject(String[] answer) {
        HubClient hubClient = mock(HubClient.class);
        ToolBudget budget = new ToolBudget(TestAppProperties.create());
        int before = budget.getVerifyRemaining();
        GameSession session = new GameSession();
        VerifyTool tool = new VerifyTool(hubClient, budget, session, reporter(budget), logger());

        String result = tool.verify(answer);

        assertTrue(result.toLowerCase().contains("answer"));
        assertEquals(before, budget.getVerifyRemaining());
        verify(hubClient, never()).verify(anyList());
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

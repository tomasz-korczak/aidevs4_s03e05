package pl.tomaszko.s03e05.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

class FlagAcquisitionTest {

    @Test
    void verifyFlagSetsSessionFlag() {
        HubClient hubClient = mock(HubClient.class);
        when(hubClient.verify(anyList())).thenReturn(HubCallResult.ok("ok {FLG:TESTFLAG} done"));
        AppProperties properties = TestAppProperties.create();
        ToolBudget budget = new ToolBudget(properties);
        GameSession session = new GameSession();
        VerifyTool verifyTool = new VerifyTool(hubClient, budget, session, reporter(budget), logger());

        verifyTool.verify(new String[] {"right"});

        assertTrue(session.hasFlag());
        assertEquals("{FLG:TESTFLAG}", session.getFlag());
        assertTrue(session.shouldStop());
    }

    @Test
    void discoverBodyWithFlagDoesNotSetSessionFlag() {
        HubClient hubClient = mock(HubClient.class);
        when(hubClient.discover(anyString(), anyString()))
                .thenReturn(HubCallResult.ok("notes {FLG:SHOULDIGNORE}"));
        AppProperties properties = TestAppProperties.create();
        ToolBudget budget = new ToolBudget(properties);
        GameSession session = new GameSession();
        DiscoverTool discoverTool = new DiscoverTool(
                hubClient, properties, budget, session, reporter(budget), logger());

        discoverTool.discover("notes", null);

        assertFalse(session.hasFlag());
        assertFalse(session.shouldStop());
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

package pl.tomaszko.s03e05.tools;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tomaszko.s03e05.TestAppProperties;
import pl.tomaszko.s03e05.config.AppProperties;
import pl.tomaszko.s03e05.hub.HubCallResult;
import pl.tomaszko.s03e05.hub.HubClient;
import pl.tomaszko.s03e05.logging.SecretRedactor;
import pl.tomaszko.s03e05.logging.ToolExecutionLogger;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;

class DiscoverToolFirstPathTest {

    private HubClient hubClient;
    private DiscoverTool tool;

    @BeforeEach
    void setUp() {
        AppProperties properties = TestAppProperties.create();
        hubClient = mock(HubClient.class);
        when(hubClient.discover(anyString(), anyString())).thenReturn(HubCallResult.ok("paths"));
        ToolBudget budget = new ToolBudget(properties);
        GameSession session = new GameSession();
        SecretRedactor redactor = mock(SecretRedactor.class);
        when(redactor.redact(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        OperatorReporter reporter = new OperatorReporter(redactor, budget);
        tool = new DiscoverTool(
                hubClient, properties, budget, session, reporter, new ToolExecutionLogger(redactor));
    }

    @Test
    void firstCallIgnoresModelPathAndUsesToolsearch() {
        tool.discover("find paths", "/api/other");
        verify(hubClient).discover("/api/toolsearch", "find paths");
    }

    @Test
    void laterCallUsesModelPath() {
        tool.discover("find paths", "/api/other");
        tool.discover("map notes", "/api/map");
        verify(hubClient).discover("/api/map", "map notes");
    }
}

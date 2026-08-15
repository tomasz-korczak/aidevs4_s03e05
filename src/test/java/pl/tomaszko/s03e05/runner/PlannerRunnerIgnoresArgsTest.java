package pl.tomaszko.s03e05.runner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.DefaultApplicationArguments;
import pl.tomaszko.s03e05.logging.SecretRedactor;
import pl.tomaszko.s03e05.prompt.SystemPromptFactory;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;
import pl.tomaszko.s03e05.tools.ToolBudget;
import pl.tomaszko.s03e05.TestAppProperties;

class PlannerRunnerIgnoresArgsTest {

    @Test
    void runDoesNotDependOnCommandLineArgs() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn("done");
        SystemPromptFactory factory = mock(SystemPromptFactory.class);
        when(factory.build()).thenReturn("system");
        GameSession session = new GameSession();
        SecretRedactor redactor = mock(SecretRedactor.class);
        when(redactor.redact(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        OperatorReporter reporter = new OperatorReporter(redactor, new ToolBudget(TestAppProperties.create()));
        PlannerRunner runner = new PlannerRunner(chatClient, factory, session, reporter);

        runner.run(new DefaultApplicationArguments("--flag", "value", "positional"));

        verify(factory).build();
        org.mockito.Mockito.verify(chatClient, org.mockito.Mockito.atLeastOnce()).prompt();
    }

    @Test
    void providerRateLimitBecomesStdoutStopReason() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("429: Provider returned error"));
        SystemPromptFactory factory = mock(SystemPromptFactory.class);
        when(factory.build()).thenReturn("system");
        GameSession session = new GameSession();
        SecretRedactor redactor = mock(SecretRedactor.class);
        when(redactor.redact(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        OperatorReporter reporter = new OperatorReporter(redactor, new ToolBudget(TestAppProperties.create()));
        PlannerRunner runner = new PlannerRunner(chatClient, factory, session, reporter);

        runner.run(new DefaultApplicationArguments());

        org.junit.jupiter.api.Assertions.assertEquals("429: Provider returned error", session.getStopReason());
        org.junit.jupiter.api.Assertions.assertEquals(1, runner.getExitCode());
        org.junit.jupiter.api.Assertions.assertTrue(session.shouldStop());
    }
}

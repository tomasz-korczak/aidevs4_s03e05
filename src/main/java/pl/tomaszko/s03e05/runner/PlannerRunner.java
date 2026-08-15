package pl.tomaszko.s03e05.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.tomaszko.s03e05.prompt.SystemPromptFactory;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;
import pl.tomaszko.s03e05.session.RunOutcome;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PlannerRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(PlannerRunner.class);

    private final ChatClient chatClient;
    private final SystemPromptFactory promptFactory;
    private final GameSession session;
    private final OperatorReporter reporter;
    private int exitCode = 1;

    public PlannerRunner(
            ChatClient chatClient,
            SystemPromptFactory promptFactory,
            GameSession session,
            OperatorReporter reporter) {
        this.chatClient = chatClient;
        this.promptFactory = promptFactory;
        this.session = session;
        this.reporter = reporter;
    }

    @Override
    public void run(ApplicationArguments args) {
        session.markPlanning();
        try {
            chatClient.prompt()
                    .system(promptFactory.build())
                    .user("Start now. Call discoverTool first. Do not wait for a human.")
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            if (!session.shouldStop()) {
                String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "Planning failed";
                log.warn("Planning stopped: {}", message);
                session.planningEnded(message);
            }
        }
        if (!session.shouldStop()) {
            session.planningEnded("Planning ended without a flag");
        }
        RunOutcome outcome = session.toOutcome();
        this.exitCode = outcome.exitCode();
        if (outcome.flag() != null) {
            return;
        }
        if (outcome.stopReason() != null) {
            reporter.stopReason(outcome.stopReason());
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}

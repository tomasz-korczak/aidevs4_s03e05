package pl.tomaszko.s03e05.tools;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import pl.tomaszko.s03e05.hub.HubCallResult;
import pl.tomaszko.s03e05.hub.HubClient;
import pl.tomaszko.s03e05.logging.ToolExecutionLogger;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;

@Component
public class VerifyTool {

    private static final Pattern FLAG = Pattern.compile("\\{FLG:[^}]+\\}");

    private final HubClient hubClient;
    private final ToolBudget budget;
    private final GameSession session;
    private final OperatorReporter reporter;
    private final ToolExecutionLogger executionLogger;

    public VerifyTool(
            HubClient hubClient,
            ToolBudget budget,
            GameSession session,
            OperatorReporter reporter,
            ToolExecutionLogger executionLogger) {
        this.hubClient = hubClient;
        this.budget = budget;
        this.session = session;
        this.reporter = reporter;
        this.executionLogger = executionLogger;
    }

    @Tool(name = "verifyTool", description = "Submit a trip. Success body contains {FLG:...}.")
    public String verify(
            @ToolParam(description = "Commands only. Optional leading vehicle name.") String[] answer) {
        String parameters = "answer=" + Arrays.toString(answer);
        if (answer == null || answer.length == 0) {
            String message = "answer must be a non-empty list of command strings";
            executionLogger.log("verifyTool", parameters, message);
            return message;
        }
        if (budget.isVerifyExhausted()) {
            session.verifyLimitReached();
            String message = "Verify limit reached";
            reporter.verifySubmitted(message);
            reporter.stopReason(session.getStopReason());
            executionLogger.log("verifyTool", parameters, message);
            return message;
        }
        if (!budget.tryConsumeVerify()) {
            session.verifyLimitReached();
            String message = "Verify limit reached";
            reporter.verifySubmitted(message);
            reporter.stopReason(session.getStopReason());
            executionLogger.log("verifyTool", parameters, message);
            return message;
        }
        List<String> commands = Arrays.asList(answer);
        session.markPlanning();
        session.setLastVerifyAnswer(commands);
        HubCallResult result = hubClient.verify(commands);
        String text = result.textForModel();
        Matcher matcher = FLAG.matcher(text);
        if (result.success() && matcher.find()) {
            String flag = matcher.group();
            session.acquireFlag(flag);
            session.setLastVerifyOutcome("success");
            reporter.verifySubmitted("flag found");
            reporter.flag(flag);
            executionLogger.log("verifyTool", parameters, text);
            return text;
        }
        session.setLastVerifyOutcome(text);
        reporter.verifySubmitted(text);
        if (budget.isVerifyExhausted() && !session.shouldStop()) {
            session.verifyLimitReached();
            reporter.stopReason(session.getStopReason());
        }
        executionLogger.log("verifyTool", parameters, text);
        return text;
    }
}

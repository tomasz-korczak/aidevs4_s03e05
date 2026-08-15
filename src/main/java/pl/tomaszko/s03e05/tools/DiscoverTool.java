package pl.tomaszko.s03e05.tools;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import pl.tomaszko.s03e05.config.AppProperties;
import pl.tomaszko.s03e05.hub.HubCallResult;
import pl.tomaszko.s03e05.hub.HubClient;
import pl.tomaszko.s03e05.logging.ToolExecutionLogger;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.session.OperatorReporter;

@Component
public class DiscoverTool {

    private final HubClient hubClient;
    private final AppProperties properties;
    private final ToolBudget budget;
    private final GameSession session;
    private final OperatorReporter reporter;
    private final ToolExecutionLogger executionLogger;
    private final Object hubGate = new Object();
    private long lastHubCallNanos;

    public DiscoverTool(
            HubClient hubClient,
            AppProperties properties,
            ToolBudget budget,
            GameSession session,
            OperatorReporter reporter,
            ToolExecutionLogger executionLogger) {
        this.hubClient = hubClient;
        this.properties = properties;
        this.budget = budget;
        this.session = session;
        this.reporter = reporter;
        this.executionLogger = executionLogger;
    }

    @Tool(name = "discoverTool", description = "Discover game rules, paths, map, notes, vehicles, costs, commands")
    public String discover(
            @ToolParam(description = "English question or keywords") String query,
            @ToolParam(required = false, description = "Hub path after the first call. Ignored on the first call.")
                    String path) {
        String parameters = "query=" + query + " path=" + path;
        if (budget.isDiscoverExhausted()) {
            session.discoverLimitReached();
            String message = "Discover limit reached";
            reporter.discover(query, message);
            reporter.stopReason(session.getStopReason());
            executionLogger.log("discoverTool", parameters, message);
            return message;
        }
        String usedPath = session.isToolsearchCompleted()
                ? normalizePath(path)
                : properties.getHub().getToolsearchPath();
        if (!budget.tryConsumeDiscover()) {
            session.discoverLimitReached();
            String message = "Discover limit reached";
            reporter.discover(query, message);
            reporter.stopReason(session.getStopReason());
            executionLogger.log("discoverTool", parameters, message);
            return message;
        }
        session.markPlanning();
        session.setLastDiscoverQuery(query);
        session.setLastDiscoverPath(usedPath);
        HubCallResult result;
        synchronized (hubGate) {
            awaitDiscoverDelay();
            result = hubClient.discover(usedPath, query);
            lastHubCallNanos = System.nanoTime();
        }
        session.markToolsearchCompleted();
        String text = result.textForModel();
        reporter.discover(query, text);
        if (budget.isDiscoverExhausted() && !session.shouldStop()) {
            session.discoverLimitReached();
            reporter.stopReason(session.getStopReason());
        }
        executionLogger.log("discoverTool", parameters, text);
        return text;
    }

    private void awaitDiscoverDelay() {
        Duration delay = properties.getTools().getDiscoverDelay();
        if (delay == null || delay.isZero() || delay.isNegative() || lastHubCallNanos == 0L) {
            return;
        }
        long waitNanos = delay.toNanos() - (System.nanoTime() - lastHubCallNanos);
        if (waitNanos <= 0) {
            return;
        }
        try {
            TimeUnit.NANOSECONDS.sleep(waitNanos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return properties.getHub().getToolsearchPath();
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}

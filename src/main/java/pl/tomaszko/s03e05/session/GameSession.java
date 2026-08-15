package pl.tomaszko.s03e05.session;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameSession {

    public enum State {
        STARTED,
        PLANNING,
        FLAG_ACQUIRED,
        DISCOVER_LIMIT,
        VERIFY_LIMIT,
        STARTUP_FAILED,
        PLANNING_ENDED
    }

    private State state = State.STARTED;
    private boolean toolsearchCompleted;
    private String flag;
    private String stopReason;
    private String lastDiscoverQuery;
    private String lastDiscoverPath;
    private List<String> lastVerifyAnswer;
    private String lastVerifyOutcome;

    public State getState() {
        return state;
    }

    public void markPlanning() {
        if (state == State.STARTED) {
            state = State.PLANNING;
        }
    }

    public boolean isToolsearchCompleted() {
        return toolsearchCompleted;
    }

    public void markToolsearchCompleted() {
        this.toolsearchCompleted = true;
    }

    public String getFlag() {
        return flag;
    }

    public void acquireFlag(String flag) {
        this.flag = flag;
        this.state = State.FLAG_ACQUIRED;
        this.stopReason = "Flag acquired";
    }

    public void discoverLimitReached() {
        this.state = State.DISCOVER_LIMIT;
        this.stopReason = "Discover limit reached";
    }

    public void verifyLimitReached() {
        this.state = State.VERIFY_LIMIT;
        this.stopReason = "Verify limit reached";
    }

    public void planningEnded(String reason) {
        if (!shouldStop()) {
            this.state = State.PLANNING_ENDED;
            this.stopReason = reason;
        }
    }

    public String getStopReason() {
        return stopReason;
    }

    public String getLastDiscoverQuery() {
        return lastDiscoverQuery;
    }

    public void setLastDiscoverQuery(String lastDiscoverQuery) {
        this.lastDiscoverQuery = lastDiscoverQuery;
    }

    public String getLastDiscoverPath() {
        return lastDiscoverPath;
    }

    public void setLastDiscoverPath(String lastDiscoverPath) {
        this.lastDiscoverPath = lastDiscoverPath;
    }

    public List<String> getLastVerifyAnswer() {
        return lastVerifyAnswer;
    }

    public void setLastVerifyAnswer(List<String> lastVerifyAnswer) {
        this.lastVerifyAnswer = lastVerifyAnswer;
    }

    public String getLastVerifyOutcome() {
        return lastVerifyOutcome;
    }

    public void setLastVerifyOutcome(String lastVerifyOutcome) {
        this.lastVerifyOutcome = lastVerifyOutcome;
    }

    public boolean shouldStop() {
        return state == State.FLAG_ACQUIRED
                || state == State.DISCOVER_LIMIT
                || state == State.VERIFY_LIMIT
                || state == State.STARTUP_FAILED
                || state == State.PLANNING_ENDED;
    }

    public boolean hasFlag() {
        return flag != null && !flag.isBlank();
    }

    public RunOutcome toOutcome() {
        if (state == State.FLAG_ACQUIRED && flag != null) {
            return new RunOutcome(0, flag, stopReason);
        }
        if (state == State.STARTUP_FAILED) {
            return new RunOutcome(2, null, stopReason);
        }
        String reason = stopReason != null ? stopReason : "Planning ended without a flag";
        return new RunOutcome(1, null, reason);
    }
}

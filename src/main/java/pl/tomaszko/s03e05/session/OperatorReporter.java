package pl.tomaszko.s03e05.session;

import org.springframework.stereotype.Component;
import pl.tomaszko.s03e05.logging.SecretRedactor;
import pl.tomaszko.s03e05.tools.ToolBudget;

@Component
public class OperatorReporter {

    private final SecretRedactor redactor;
    private final ToolBudget budget;

    public OperatorReporter(SecretRedactor redactor, ToolBudget budget) {
        this.redactor = redactor;
        this.budget = budget;
    }

    public void discover(String query, String outcomeSummary) {
        System.out.println("discover: " + redact(query));
        System.out.println("outcome: " + summarize(outcomeSummary));
        remaining();
    }

    public void verifySubmitted(String outcomeSummary) {
        System.out.println("verify: trip check submitted");
        System.out.println("outcome: " + summarize(outcomeSummary));
        remaining();
    }

    public void remaining() {
        System.out.println(
                "remaining discover=" + budget.getDiscoverRemaining()
                        + " verify=" + budget.getVerifyRemaining());
    }

    public void flag(String flag) {
        System.out.println(redact(flag));
    }

    public void stopReason(String reason) {
        System.out.println(redact(reason));
    }

    public void startupError(String reason) {
        System.err.println(redact(reason));
    }

    private String summarize(String text) {
        String redacted = redact(text);
        if (redacted == null) {
            return "";
        }
        if (redacted.length() > 240) {
            return redacted.substring(0, 240) + "...";
        }
        return redacted;
    }

    private String redact(String text) {
        return redactor.redact(text);
    }
}

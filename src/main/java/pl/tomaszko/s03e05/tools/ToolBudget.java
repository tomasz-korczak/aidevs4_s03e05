package pl.tomaszko.s03e05.tools;

import org.springframework.stereotype.Component;
import pl.tomaszko.s03e05.config.AppProperties;

@Component
public class ToolBudget {

    private int discoverRemaining;
    private int verifyRemaining;

    public ToolBudget(AppProperties properties) {
        this.discoverRemaining = properties.getTools().getDiscoverLimit();
        this.verifyRemaining = properties.getTools().getVerifyLimit();
    }

    public int getDiscoverRemaining() {
        return discoverRemaining;
    }

    public int getVerifyRemaining() {
        return verifyRemaining;
    }

    public boolean isDiscoverExhausted() {
        return discoverRemaining == 0;
    }

    public boolean isVerifyExhausted() {
        return verifyRemaining == 0;
    }

    public boolean tryConsumeDiscover() {
        if (discoverRemaining <= 0) {
            return false;
        }
        discoverRemaining--;
        return true;
    }

    public boolean tryConsumeVerify() {
        if (verifyRemaining <= 0) {
            return false;
        }
        verifyRemaining--;
        return true;
    }
}

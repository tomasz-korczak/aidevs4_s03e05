package pl.tomaszko.s03e05;

import java.net.URI;
import java.time.Duration;
import pl.tomaszko.s03e05.config.AppProperties;

public final class TestAppProperties {

    private TestAppProperties() {}

    public static AppProperties create() {
        AppProperties properties = new AppProperties();
        properties.getHub().setApiKey("test-hub-key");
        properties.getHub().setBaseUrl(URI.create("https://hub.ag3nts.org"));
        properties.getHub().setToolsearchPath("/api/toolsearch");
        properties.getHub().setVerifyPath("/verify");
        properties.getHub().setTask("savethem");
        properties.getOpenrouter().setBaseUrl(URI.create("https://openrouter.ai/api/v1"));
        properties.getLlm().setModel("inclusionai/ling-3.0-flash");
        properties.getTools().setDiscoverLimit(30);
        properties.getTools().setVerifyLimit(10);
        properties.getTools().setDiscoverDelay(Duration.ZERO);
        properties.getBriefing().setDestination("Skolwin");
        properties.getBriefing().setMapWidth(10);
        properties.getBriefing().setMapHeight(10);
        properties.getBriefing().setStartingFuel(10);
        properties.getBriefing().setStartingFood(10);
        return properties;
    }
}

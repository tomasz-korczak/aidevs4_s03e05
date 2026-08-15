package pl.tomaszko.s03e05.prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import pl.tomaszko.s03e05.TestAppProperties;
import pl.tomaszko.s03e05.config.AppProperties;

class SystemPromptFactoryTest {

    @Test
    void interpolatesLimitsUrlsDestinationAndBriefing() {
        AppProperties properties = TestAppProperties.create();
        SystemPromptFactory factory = new SystemPromptFactory(properties);
        String prompt = factory.build();
        assertTrue(prompt.contains("Skolwin"));
        assertTrue(prompt.contains("30"));
        assertTrue(prompt.contains("10"));
        assertTrue(prompt.contains("https://hub.ag3nts.org"));
        assertTrue(prompt.contains("/api/toolsearch"));
        assertFalse(prompt.contains("${destination}"));
        assertFalse(prompt.contains("${discoverLimit}"));
        assertFalse(prompt.contains("${hubBaseUrl}"));
        assertFalse(prompt.contains("${mapWidth}"));
        assertFalse(prompt.contains("${startingFuel}"));
    }
}

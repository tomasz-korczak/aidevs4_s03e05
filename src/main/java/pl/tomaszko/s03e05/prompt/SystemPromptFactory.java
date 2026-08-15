package pl.tomaszko.s03e05.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.tomaszko.s03e05.config.AppProperties;

@Component
public class SystemPromptFactory {

    static final String DEFAULT_CLASSPATH = "prompts/system.txt";

    private final AppProperties properties;

    public SystemPromptFactory(AppProperties properties) {
        this.properties = properties;
    }

    public String build() {
        String template = properties.getPrompt() != null && notBlank(properties.getPrompt().getSystem())
                ? properties.getPrompt().getSystem()
                : readDefault();
        return interpolate(template);
    }

    String interpolate(String template) {
        AppProperties.Briefing briefing = properties.getBriefing();
        AppProperties.Hub hub = properties.getHub();
        AppProperties.Tools tools = properties.getTools();
        return template
                .replace("${destination}", briefing.getDestination())
                .replace("${mapWidth}", Integer.toString(briefing.getMapWidth()))
                .replace("${mapHeight}", Integer.toString(briefing.getMapHeight()))
                .replace("${startingFuel}", Integer.toString(briefing.getStartingFuel()))
                .replace("${startingFood}", Integer.toString(briefing.getStartingFood()))
                .replace("${hubBaseUrl}", hub.getBaseUrl().toString())
                .replace("${toolsearchPath}", hub.getToolsearchPath())
                .replace("${discoverLimit}", Integer.toString(tools.getDiscoverLimit()))
                .replace("${verifyLimit}", Integer.toString(tools.getVerifyLimit()));
    }

    private static String readDefault() {
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_CLASSPATH);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Missing " + DEFAULT_CLASSPATH, ex);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

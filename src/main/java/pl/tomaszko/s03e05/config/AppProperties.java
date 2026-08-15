package pl.tomaszko.s03e05.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @Valid
    @NotNull
    private Hub hub = new Hub();

    @Valid
    @NotNull
    private Openrouter openrouter = new Openrouter();

    @Valid
    @NotNull
    private Llm llm = new Llm();

    @Valid
    @NotNull
    private Tools tools = new Tools();

    private Prompt prompt = new Prompt();

    @Valid
    @NotNull
    private Briefing briefing = new Briefing();

    public Hub getHub() {
        return hub;
    }

    public void setHub(Hub hub) {
        this.hub = hub;
    }

    public Openrouter getOpenrouter() {
        return openrouter;
    }

    public void setOpenrouter(Openrouter openrouter) {
        this.openrouter = openrouter;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public Tools getTools() {
        return tools;
    }

    public void setTools(Tools tools) {
        this.tools = tools;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public void setPrompt(Prompt prompt) {
        this.prompt = prompt;
    }

    public Briefing getBriefing() {
        return briefing;
    }

    public void setBriefing(Briefing briefing) {
        this.briefing = briefing;
    }

    public static class Hub {
        private String apiKey;
        @NotNull
        private URI baseUrl;
        @NotBlank
        private String toolsearchPath = "/api/toolsearch";
        @NotBlank
        private String verifyPath = "/verify";
        @NotBlank
        private String task = "savethem";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public URI getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getToolsearchPath() {
            return toolsearchPath;
        }

        public void setToolsearchPath(String toolsearchPath) {
            this.toolsearchPath = toolsearchPath;
        }

        public String getVerifyPath() {
            return verifyPath;
        }

        public void setVerifyPath(String verifyPath) {
            this.verifyPath = verifyPath;
        }

        public String getTask() {
            return task;
        }

        public void setTask(String task) {
            this.task = task;
        }
    }

    public static class Openrouter {
        @NotNull
        private URI baseUrl;

        public URI getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Llm {
        @NotBlank
        private String model;

        @Valid
        @NotNull
        private Retry retry = new Retry();

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }
    }

    public static class Retry {
        @Min(1)
        private int maxAttempts = 6;

        @NotNull
        private Duration initialBackoff = Duration.ofSeconds(2);

        @NotNull
        private Duration maxBackoff = Duration.ofSeconds(30);

        @DecimalMin("1.0")
        private double multiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialBackoff() {
            return initialBackoff;
        }

        public void setInitialBackoff(Duration initialBackoff) {
            this.initialBackoff = initialBackoff;
        }

        public Duration getMaxBackoff() {
            return maxBackoff;
        }

        public void setMaxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    public static class Tools {
        @Min(1)
        private int discoverLimit = 30;
        @Min(1)
        private int verifyLimit = 10;
        @NotNull
        private Duration discoverDelay = Duration.ofSeconds(1);

        public int getDiscoverLimit() {
            return discoverLimit;
        }

        public void setDiscoverLimit(int discoverLimit) {
            this.discoverLimit = discoverLimit;
        }

        public int getVerifyLimit() {
            return verifyLimit;
        }

        public void setVerifyLimit(int verifyLimit) {
            this.verifyLimit = verifyLimit;
        }

        public Duration getDiscoverDelay() {
            return discoverDelay;
        }

        public void setDiscoverDelay(Duration discoverDelay) {
            this.discoverDelay = discoverDelay;
        }
    }

    public static class Prompt {
        private String system;

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }
    }

    public static class Briefing {
        @NotBlank
        private String destination = "Skolwin";
        @Min(1)
        private int mapWidth = 10;
        @Min(1)
        private int mapHeight = 10;
        @Min(1)
        private int startingFuel = 10;
        @Min(1)
        private int startingFood = 10;

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public int getMapWidth() {
            return mapWidth;
        }

        public void setMapWidth(int mapWidth) {
            this.mapWidth = mapWidth;
        }

        public int getMapHeight() {
            return mapHeight;
        }

        public void setMapHeight(int mapHeight) {
            this.mapHeight = mapHeight;
        }

        public int getStartingFuel() {
            return startingFuel;
        }

        public void setStartingFuel(int startingFuel) {
            this.startingFuel = startingFuel;
        }

        public int getStartingFood() {
            return startingFood;
        }

        public void setStartingFood(int startingFood) {
            this.startingFood = startingFood;
        }
    }
}

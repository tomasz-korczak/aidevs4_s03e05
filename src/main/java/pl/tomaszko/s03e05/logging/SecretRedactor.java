package pl.tomaszko.s03e05.logging;

import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecretRedactor {

    private static final Pattern APIKEY_JSON = Pattern.compile("(?i)(\"apikey\"\\s*:\\s*\")([^\"]*)(\")");
    private static final String REDACTED = "***";

    private final Environment environment;

    public SecretRedactor(Environment environment) {
        this.environment = environment;
    }

    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String redacted = APIKEY_JSON.matcher(text).replaceAll("$1" + REDACTED + "$3");
        redacted = replaceSecret(redacted, environment.getProperty("HUB_API_KEY"));
        redacted = replaceSecret(redacted, environment.getProperty("OPENROUTER_API_KEY"));
        redacted = replaceSecret(redacted, environment.getProperty("app.hub.api-key"));
        redacted = replaceSecret(redacted, environment.getProperty("spring.ai.openai.api-key"));
        return redacted;
    }

    private static String replaceSecret(String text, String secret) {
        if (secret == null || secret.isBlank()) {
            return text;
        }
        return text.replace(secret, REDACTED);
    }
}

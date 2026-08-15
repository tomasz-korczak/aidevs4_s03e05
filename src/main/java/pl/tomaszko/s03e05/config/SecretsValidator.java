package pl.tomaszko.s03e05.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecretsValidator implements ApplicationRunner {

    private final Environment environment;

    public SecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isBlank(first("OPENROUTER_API_KEY", "spring.ai.openai.api-key"))
                || isBlank(first("HUB_API_KEY", "app.hub.api-key"))) {
            throw new MissingSecretsException();
        }
    }

    private String first(String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static final class MissingSecretsException extends RuntimeException implements ExitCodeGenerator {
        public MissingSecretsException() {
            super("Missing OPENROUTER_API_KEY or HUB_API_KEY");
        }

        @Override
        public int getExitCode() {
            return 2;
        }
    }
}

package pl.tomaszko.s03e05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ExitCodeGenerator;
import pl.tomaszko.s03e05.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class S03e05Application {

    public static void main(String[] args) {
        try {
            System.exit(SpringApplication.exit(SpringApplication.run(S03e05Application.class)));
        } catch (Exception ex) {
            int code = exitCodeFrom(ex);
            if (code == 2) {
                System.err.println(messageFrom(ex));
            } else {
                System.err.println(messageFrom(ex));
            }
            System.exit(code);
        }
    }

    static int exitCodeFrom(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ExitCodeGenerator generator) {
                return generator.getExitCode();
            }
            current = current.getCause();
        }
        return 1;
    }

    static String messageFrom(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                if (current instanceof ExitCodeGenerator) {
                    return current.getMessage();
                }
            }
            current = current.getCause();
        }
        return ex.getMessage() != null ? ex.getMessage() : "Planning failed";
    }
}

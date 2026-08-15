package pl.tomaszko.s03e05.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutionLogger {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionLogger.class);

    private final SecretRedactor redactor;

    public ToolExecutionLogger(SecretRedactor redactor) {
        this.redactor = redactor;
    }

    public void log(String name, String parameters, String result) {
        log.info(
                "tool={} parameters={} result={}",
                name,
                redactor.redact(parameters),
                redactor.redact(result));
    }
}

package pl.tomaszko.s03e05.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class SecretRedactorTest {

    @Test
    void stripsEnvKeysAndApikeyJsonField() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("HUB_API_KEY")).thenReturn("hub-secret-value");
        when(environment.getProperty("OPENROUTER_API_KEY")).thenReturn("or-secret-value");
        SecretRedactor redactor = new SecretRedactor(environment);

        String redacted = redactor.redact(
                "open=or-secret-value hub=hub-secret-value body={\"apikey\":\"hub-secret-value\"}");

        assertFalse(redacted.contains("hub-secret-value"));
        assertFalse(redacted.contains("or-secret-value"));
        assertTrue(redacted.contains("***"));
    }
}

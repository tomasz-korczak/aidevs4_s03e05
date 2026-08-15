package pl.tomaszko.s03e05.llm;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import pl.tomaszko.s03e05.config.AppProperties;

class RetryingChatModelTest {

    @Test
    void replacesIncompatiblePromptOptionsWithDelegateOptions() {
        ChatModel delegate = mock(ChatModel.class);
        ChatOptions delegateOptions = mock(ChatOptions.class);
        ChatResponse response = mock(ChatResponse.class);
        when(delegate.getDefaultOptions()).thenReturn(delegateOptions);
        when(delegate.call(any(Prompt.class))).thenReturn(response);

        ChatResponse actual = new RetryingChatModel(delegate, new OpenRouterRetry(fastRetry(1))).call(new Prompt("go"));

        assertSame(response, actual);
        verify(delegate).call(argThat((Prompt prompt) -> prompt.getOptions() == delegateOptions));
    }

    @Test
    void delegatesGetOptionsSoChatClientKeepsProviderOptions() {
        ChatModel delegate = mock(ChatModel.class);
        ChatOptions options = mock(ChatOptions.class);
        when(delegate.getOptions()).thenReturn(options);
        when(delegate.getDefaultOptions()).thenReturn(options);

        RetryingChatModel retrying = new RetryingChatModel(delegate, new OpenRouterRetry(fastRetry(1)));

        assertSame(options, retrying.getOptions());
        assertSame(options, retrying.getDefaultOptions());
    }

    private static AppProperties.Retry fastRetry(int maxAttempts) {
        AppProperties.Retry retry = new AppProperties.Retry();
        retry.setMaxAttempts(maxAttempts);
        retry.setInitialBackoff(Duration.ofMillis(1));
        retry.setMaxBackoff(Duration.ofMillis(5));
        retry.setMultiplier(2);
        return retry;
    }
}

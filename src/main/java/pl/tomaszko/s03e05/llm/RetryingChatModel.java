package pl.tomaszko.s03e05.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public final class RetryingChatModel implements ChatModel {

    private final ChatModel delegate;
    private final OpenRouterRetry retry;

    public RetryingChatModel(ChatModel delegate, OpenRouterRetry retry) {
        this.delegate = delegate;
        this.retry = retry;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Prompt toSend = withDelegateOptions(prompt);
        return retry.execute(() -> delegate.call(toSend));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(withDelegateOptions(prompt));
    }

    @Override
    public ChatOptions getOptions() {
        return delegate.getOptions();
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return delegate.getDefaultOptions();
    }

    private Prompt withDelegateOptions(Prompt prompt) {
        ChatOptions defaults = delegate.getDefaultOptions();
        if (defaults == null) {
            return prompt;
        }
        ChatOptions incoming = prompt.getOptions();
        if (incoming != null && defaults.getClass().isInstance(incoming)) {
            return prompt;
        }
        return new Prompt(prompt.getInstructions(), defaults);
    }
}

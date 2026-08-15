package pl.tomaszko.s03e05.config;

import java.util.Arrays;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.tomaszko.s03e05.llm.ContextCompactionAdvisor;
import pl.tomaszko.s03e05.llm.OpenRouterRetry;
import pl.tomaszko.s03e05.llm.RetryingChatModel;
import pl.tomaszko.s03e05.logging.ModelCommunicationAdvisor;
import pl.tomaszko.s03e05.logging.ToolExecutionLogger;
import pl.tomaszko.s03e05.session.GameSession;
import pl.tomaszko.s03e05.tools.DiscoverTool;
import pl.tomaszko.s03e05.tools.SessionStopToolCallback;
import pl.tomaszko.s03e05.tools.VerifyTool;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(
            ChatModel chatModel,
            AppProperties properties,
            DiscoverTool discoverTool,
            VerifyTool verifyTool,
            ModelCommunicationAdvisor modelCommunicationAdvisor,
            ContextCompactionAdvisor contextCompactionAdvisor,
            ToolExecutionLogger toolExecutionLogger,
            GameSession session) {
        ToolCallback[] callbacks = Arrays.stream(ToolCallbacks.from(discoverTool, verifyTool))
                .map(callback -> (ToolCallback) new SessionStopToolCallback(callback, session))
                .toArray(ToolCallback[]::new);
        ChatModel retrying = new RetryingChatModel(chatModel, new OpenRouterRetry(properties.getLlm().getRetry()));
        return ChatClient.builder(retrying)
                .defaultAdvisors(contextCompactionAdvisor, modelCommunicationAdvisor)
                .defaultTools((Object[]) callbacks)
                .build();
    }
}

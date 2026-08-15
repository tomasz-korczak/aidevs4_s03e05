package pl.tomaszko.s03e05.llm;

import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class ContextCompactionAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Prompt prompt = request.prompt();
        List<Message> compacted = ContextCompactor.compact(prompt.getInstructions());
        ChatClientRequest next = request.mutate().prompt(new Prompt(compacted, prompt.getOptions())).build();
        return chain.nextCall(next);
    }

    @Override
    public String getName() {
        return "ContextCompactionAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }
}

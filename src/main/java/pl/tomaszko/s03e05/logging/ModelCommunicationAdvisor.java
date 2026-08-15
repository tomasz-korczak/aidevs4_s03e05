package pl.tomaszko.s03e05.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class ModelCommunicationAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ModelCommunicationAdvisor.class);

    private final SecretRedactor redactor;

    public ModelCommunicationAdvisor(SecretRedactor redactor) {
        this.redactor = redactor;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("model request system/user/tools: {}", redactor.redact(String.valueOf(request.prompt())));
        ChatClientResponse response = chain.nextCall(request);
        log.info("model response: {}", redactor.redact(String.valueOf(response.chatResponse())));
        return response;
    }

    @Override
    public String getName() {
        return "ModelCommunicationAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}

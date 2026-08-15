package pl.tomaszko.s03e05.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.chat.model.ToolContext;
import pl.tomaszko.s03e05.session.GameSession;

public class SessionStopToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final GameSession session;

    public SessionStopToolCallback(ToolCallback delegate, GameSession session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().returnDirect(session.shouldStop()).build();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return delegate.call(toolInput, toolContext);
    }
}

package pl.tomaszko.s03e05.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

class ContextCompactorTest {

    @Test
    void stripsReasoningMetadataAndKeepsFinishReason() {
        AssistantMessage original = AssistantMessage.builder()
                .content("")
                .properties(Map.of(
                        "reasoningContent", "long chain of thought about the map",
                        "finishReason", "TOOL_CALLS",
                        "role", "assistant"))
                .toolCalls(List.of(discoverCall("c1", "/api/maps", "legend")))
                .build();

        AssistantMessage compacted = (AssistantMessage) ContextCompactor.compact(List.of(original)).getFirst();

        assertFalse(compacted.getMetadata().containsKey("reasoningContent"));
        assertEquals("TOOL_CALLS", compacted.getMetadata().get("finishReason"));
        assertEquals("c1", compacted.getToolCalls().getFirst().id());
    }

    @Test
    void keepsLastDuplicateFailureAndStubsEarlierOnes() {
        String notFound = "Hub HTTP 404 application/json: {\"code\":-716,\"message\":\"I don't have maps for such a city.\"}";
        List<Message> messages = List.of(
                new SystemMessage("sys"),
                new UserMessage("start"),
                assistantDiscover("c1", "/api/maps", "legend"),
                discoverResult("c1", notFound),
                assistantDiscover("c2", "/api/maps", "start position"),
                discoverResult("c2", notFound),
                assistantDiscover("c3", "/api/maps", "Skolwin"),
                discoverResult("c3", "{\"code\":241,\"message\":\"Map found.\"}"));

        List<Message> compacted = ContextCompactor.compact(messages);

        assertEquals(
                ContextCompactor.DUPLICATE_FAILURE_STUB.formatted("/api/maps"),
                discoverBody(compacted, "c1"));
        assertEquals(notFound, discoverBody(compacted, "c2"));
        assertEquals("{\"code\":241,\"message\":\"Map found.\"}", discoverBody(compacted, "c3"));
    }

    @Test
    void keepsLastSuccessPerPathAndAllToolsearchHits() {
        List<Message> messages = List.of(
                assistantDiscover("s1", "/api/toolsearch", "map"),
                discoverResult("s1", "{\"code\":210,\"tools\":[{\"url\":\"/api/maps\"}]}"),
                assistantDiscover("s2", "/api/toolsearch", "notes"),
                discoverResult("s2", "{\"code\":210,\"tools\":[{\"url\":\"/api/books\"}]}"),
                assistantDiscover("m1", "/api/maps", "wrong"),
                discoverResult("m1", "{\"code\":241,\"message\":\"Map found.\",\"cityName\":\"Old\"}"),
                assistantDiscover("m2", "/api/maps", "Skolwin"),
                discoverResult("m2", "{\"code\":241,\"message\":\"Map found.\",\"cityName\":\"Skolwin\"}"));

        List<Message> compacted = ContextCompactor.compact(messages);

        assertTrue(discoverBody(compacted, "s1").contains("/api/maps"));
        assertTrue(discoverBody(compacted, "s2").contains("/api/books"));
        assertEquals(
                ContextCompactor.SUPERSEDED_SUCCESS_STUB.formatted("/api/maps"),
                discoverBody(compacted, "m1"));
        assertTrue(discoverBody(compacted, "m2").contains("Skolwin"));
    }

    @Test
    void doesNotCompactVerifyResults() {
        ToolResponseMessage verify = ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(
                        "v1", "verifyTool", "out of fuel")))
                .build();

        List<Message> compacted = ContextCompactor.compact(List.of(verify, verify));

        assertEquals("out of fuel", ((ToolResponseMessage) compacted.getFirst()).getResponses().getFirst().responseData());
        assertEquals("out of fuel", ((ToolResponseMessage) compacted.get(1)).getResponses().getFirst().responseData());
    }

    private static AssistantMessage assistantDiscover(String id, String path, String query) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(discoverCall(id, path, query)))
                .build();
    }

    private static AssistantMessage.ToolCall discoverCall(String id, String path, String query) {
        String arguments = "{\"path\":\"" + path + "\",\"query\":\"" + query + "\"}";
        return new AssistantMessage.ToolCall(id, "function", "discoverTool", arguments);
    }

    private static ToolResponseMessage discoverResult(String id, String body) {
        return ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(id, "discoverTool", body)))
                .build();
    }

    private static String discoverBody(List<Message> messages, String id) {
        for (Message message : messages) {
            if (message instanceof ToolResponseMessage toolMessage) {
                for (ToolResponseMessage.ToolResponse response : toolMessage.getResponses()) {
                    if (id.equals(response.id())) {
                        return response.responseData();
                    }
                }
            }
        }
        throw new AssertionError("missing discover id " + id);
    }
}

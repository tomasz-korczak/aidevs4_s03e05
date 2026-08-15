package pl.tomaszko.s03e05.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

public final class ContextCompactor {

    static final String DUPLICATE_FAILURE_STUB = "Duplicate failed discover on %s omitted (same error).";
    static final String SUPERSEDED_SUCCESS_STUB =
            "Earlier successful discover on %s omitted; a later result is in this conversation.";

    private static final Pattern PATH_JSON = Pattern.compile("\"path\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTTP_STATUS = Pattern.compile("hub http (\\d+)", Pattern.CASE_INSENSITIVE);
    private static final String TOOLSEARCH = "/api/toolsearch";
    private static final String DISCOVER = "discoverTool";

    private ContextCompactor() {}

    public static List<Message> compact(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        Map<String, String> callIdToPath = indexDiscoverPaths(messages);
        Set<String> keepFull = selectDiscoverBodiesToKeep(messages, callIdToPath);
        List<Message> compacted = new ArrayList<>(messages.size());
        for (Message message : messages) {
            compacted.add(rewrite(message, callIdToPath, keepFull));
        }
        return compacted;
    }

    static boolean isDiscoverFailure(String body) {
        if (body == null || body.isBlank()) {
            return true;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("hub http 4")
                || lower.contains("hub http 5")
                || lower.contains("no matching tools found")
                || lower.contains("unreadable hub")
                || lower.contains("hub unreachable")
                || lower.contains("hub request failed")
                || lower.contains("empty hub")
                || lower.contains("i don't have maps")
                || lower.contains("does not exist");
    }

    static String pathFromArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return TOOLSEARCH;
        }
        Matcher matcher = PATH_JSON.matcher(arguments);
        if (matcher.find()) {
            String path = matcher.group(1);
            return path.isBlank() ? TOOLSEARCH : path;
        }
        return TOOLSEARCH;
    }

    static String failureSignature(String body) {
        if (body == null) {
            return "empty";
        }
        String lower = body.toLowerCase(Locale.ROOT);
        if (lower.contains("i don't have maps")) {
            return "no-map";
        }
        if (lower.contains("does not exist")) {
            return "no-module";
        }
        if (lower.contains("no matching tools")) {
            return "no-tools";
        }
        if (lower.contains("unreadable")) {
            return "unreadable";
        }
        if (lower.contains("unreachable") || lower.contains("timed out")) {
            return "timeout";
        }
        Matcher http = HTTP_STATUS.matcher(lower);
        if (http.find()) {
            return "http-" + http.group(1);
        }
        return Integer.toHexString(body.strip().hashCode());
    }

    private static Map<String, String> indexDiscoverPaths(List<Message> messages) {
        Map<String, String> callIdToPath = new HashMap<>();
        for (Message message : messages) {
            if (message instanceof AssistantMessage assistant) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    if (DISCOVER.equals(call.name())) {
                        callIdToPath.put(call.id(), pathFromArguments(call.arguments()));
                    }
                }
            }
        }
        return callIdToPath;
    }

    private static Set<String> selectDiscoverBodiesToKeep(
            List<Message> messages, Map<String, String> callIdToPath) {
        record DiscoverHit(String id, String path, boolean failure, String signature) {}
        List<DiscoverHit> hits = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof ToolResponseMessage toolMessage) {
                for (ToolResponseMessage.ToolResponse response : toolMessage.getResponses()) {
                    if (!DISCOVER.equals(response.name())) {
                        continue;
                    }
                    String path = callIdToPath.getOrDefault(response.id(), TOOLSEARCH);
                    boolean failure = isDiscoverFailure(response.responseData());
                    hits.add(new DiscoverHit(
                            response.id(),
                            path,
                            failure,
                            failure ? failureSignature(response.responseData()) : ""));
                }
            }
        }
        Set<String> keepFull = new HashSet<>();
        Set<String> seenFailure = new HashSet<>();
        Set<String> seenSuccessPath = new HashSet<>();
        for (int i = hits.size() - 1; i >= 0; i--) {
            DiscoverHit hit = hits.get(i);
            if (hit.failure()) {
                if (seenFailure.add(hit.path() + "|" + hit.signature())) {
                    keepFull.add(hit.id());
                }
            } else if (isToolsearch(hit.path())) {
                keepFull.add(hit.id());
            } else if (seenSuccessPath.add(hit.path())) {
                keepFull.add(hit.id());
            }
        }
        return keepFull;
    }

    private static boolean isToolsearch(String path) {
        return path == null || path.isBlank() || path.contains("toolsearch");
    }

    private static Message rewrite(
            Message message, Map<String, String> callIdToPath, Set<String> keepFull) {
        if (message instanceof AssistantMessage assistant) {
            return stripReasoning(assistant);
        }
        if (message instanceof ToolResponseMessage toolMessage) {
            return compactDiscoverResponses(toolMessage, callIdToPath, keepFull);
        }
        return message;
    }

    private static AssistantMessage stripReasoning(AssistantMessage message) {
        Map<String, Object> metadata = message.getMetadata();
        if (metadata == null || metadata.isEmpty() || !hasReasoning(metadata)) {
            return message;
        }
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!isReasoningKey(entry.getKey())) {
                cleaned.put(entry.getKey(), entry.getValue());
            }
        }
        return AssistantMessage.builder()
                .content(message.getText())
                .properties(cleaned)
                .toolCalls(message.getToolCalls())
                .media(message.getMedia())
                .build();
    }

    private static boolean hasReasoning(Map<String, Object> metadata) {
        for (String key : metadata.keySet()) {
            if (isReasoningKey(key)) {
                return true;
            }
        }
        return false;
    }

    static boolean isReasoningKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return lower.equals("reasoningcontent")
                || lower.equals("reasoning")
                || lower.equals("thinking")
                || lower.equals("reasoningdetails")
                || lower.equals("reasoningtext");
    }

    private static ToolResponseMessage compactDiscoverResponses(
            ToolResponseMessage message, Map<String, String> callIdToPath, Set<String> keepFull) {
        List<ToolResponseMessage.ToolResponse> original = message.getResponses();
        boolean changed = false;
        List<ToolResponseMessage.ToolResponse> rewritten = new ArrayList<>(original.size());
        for (ToolResponseMessage.ToolResponse response : original) {
            if (!DISCOVER.equals(response.name()) || keepFull.contains(response.id())) {
                rewritten.add(response);
                continue;
            }
            changed = true;
            String path = callIdToPath.getOrDefault(response.id(), TOOLSEARCH);
            String stub = isDiscoverFailure(response.responseData())
                    ? DUPLICATE_FAILURE_STUB.formatted(path)
                    : SUPERSEDED_SUCCESS_STUB.formatted(path);
            rewritten.add(new ToolResponseMessage.ToolResponse(response.id(), response.name(), stub));
        }
        if (!changed) {
            return message;
        }
        return ToolResponseMessage.builder().responses(rewritten).metadata(message.getMetadata()).build();
    }
}

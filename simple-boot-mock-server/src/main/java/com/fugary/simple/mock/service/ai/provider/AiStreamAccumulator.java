package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fugary.simple.mock.service.ai.AiProvider;

import java.io.IOException;
import java.util.Set;

/** 只汇总测试所需的正文和用量，复用各供应商已有的响应解析。 */
class AiStreamAccumulator {
    private static final Set<String> FINISH_REASONS = Set.of(
        "stop", "tool_calls", "end_turn", "tool_use", "stop_sequence", "STOP");
    private final AiProvider provider;
    private final ObjectMapper mapper;
    private final AiStreamSession stream;
    private final ObjectNode root;
    private boolean completed;
    private String finishReason;

    AiStreamAccumulator(AiProvider provider, ObjectMapper mapper, AiStreamSession stream) {
        this.provider = provider;
        this.mapper = mapper;
        this.stream = stream;
        root = mapper.createObjectNode();
    }

    void accept(String data) throws IOException {
        stream.check();
        if (provider == AiProvider.OPENAI && "[DONE]".equals(data)) {
            completed = true;
            return;
        }
        JsonNode event = mapper.readTree(data);
        if (event == null || !event.isObject()) throw new IOException("AI 流式响应格式无效");
        if (event.hasNonNull("error")) throw new IOException("AI upstream error");
        switch (provider) {
            case OPENAI: openAi(event); break;
            case ANTHROPIC: anthropic(event); break;
            case GEMINI: gemini(event); break;
            default: throw new IOException("AI 供应商无效");
        }
    }

    private void openAi(JsonNode event) {
        if (event.hasNonNull("usage")) root.set("usage", event.get("usage"));
        for (JsonNode choice : event.path("choices")) {
            if (choice.path("index").asInt() != 0) continue;
            text(choice.path("delta").path("content"));
            if (choice.hasNonNull("finish_reason")) finishReason = choice.get("finish_reason").asText();
        }
    }

    private void anthropic(JsonNode event) {
        switch (event.path("type").asText()) {
            case "message_start":
                if (event.path("message").path("usage").isObject()) root.set("usage", event.path("message").get("usage"));
                break;
            case "content_block_start":
                JsonNode block = event.path("content_block");
                if ("text".equals(block.path("type").asText())) text(block.path("text"));
                break;
            case "content_block_delta":
                JsonNode delta = event.path("delta");
                if ("text_delta".equals(delta.path("type").asText())) text(delta.path("text"));
                break;
            case "message_delta":
                if (event.path("usage").isObject()) root.with("usage").setAll((ObjectNode) event.get("usage"));
                if (event.path("delta").hasNonNull("stop_reason")) finishReason = event.path("delta").get("stop_reason").asText();
                break;
            case "message_stop": completed = true; break;
            default: break;
        }
    }

    private void gemini(JsonNode event) {
        if (event.hasNonNull("usageMetadata")) root.set("usageMetadata", event.get("usageMetadata"));
        for (JsonNode candidate : event.path("candidates")) {
            if (candidate.path("index").asInt() != 0) continue;
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (!part.path("thought").asBoolean()) text(part.path("text"));
            }
            if (candidate.hasNonNull("finishReason")) {
                finishReason = candidate.get("finishReason").asText();
                completed = true;
            }
        }
    }

    private void text(JsonNode value) {
        if (value.isTextual()) stream.text(value.asText());
    }

    boolean isTerminal() { return completed && provider != AiProvider.GEMINI; }

    JsonNode result() throws IOException {
        stream.check();
        if (!completed) throw new IOException("AI 流式响应中断，未收到完整结束事件");
        if (finishReason != null && !FINISH_REASONS.contains(finishReason)) {
            throw new IOException("AI 未完整生成回答: " + finishReason);
        }
        String content = stream.getText();
        switch (provider) {
            case OPENAI:
                root.putArray("choices").addObject().putObject("message").put("content", content);
                break;
            case ANTHROPIC:
                root.putArray("content").addObject().put("type", "text").put("text", content);
                break;
            case GEMINI:
                root.putArray("candidates").addObject().putObject("content").putArray("parts").addObject().put("text", content);
                break;
            default: break;
        }
        return root;
    }
}

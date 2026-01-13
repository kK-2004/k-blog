package com.kk.kblog.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

@Service
public class GlmAiService {
    private static final Logger log = LoggerFactory.getLogger(GlmAiService.class);
    private final GlmProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GlmAiService(GlmProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, props.getTimeoutSeconds())))
                .build();
    }

    public boolean isEnabled() {
        return props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    public String summarize(String content) {
        if (!isEnabled()) {
            log.info("GLM disabled, using fallback summary");
            return fallbackSummary(content);
        }

        var reqBody = buildChatRequest(content, false);
        var request = baseRequest()
                .POST(HttpRequest.BodyPublishers.ofString(reqBody, StandardCharsets.UTF_8))
                .build();

        try {
            log.info("GLM summarize request: model={}, url={}", props.getModel(), props.getBaseUrl());
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IOException("GLM API error: HTTP " + response.statusCode() + " body=" + safeBodySnippet(response.body()));
            }
            var root = objectMapper.readTree(response.body());
            var choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                var msg = choices.get(0).path("message").path("content").asText("");
                if (!msg.isBlank()) return msg;
            }
            return "";
        } catch (Exception e) {
            log.warn("GLM summarize failed, fallback used", e);
            return fallbackSummary(content);
        }
    }

    public void summarizeStream(String content, Consumer<String> onDelta) throws IOException, InterruptedException {
        if (!isEnabled()) {
            log.info("GLM disabled, streaming fallback summary");
            onDelta.accept(fallbackSummary(content));
            return;
        }

        var reqBody = buildChatRequest(content, true);
        var request = baseRequest()
                .POST(HttpRequest.BodyPublishers.ofString(reqBody, StandardCharsets.UTF_8))
                .build();

        log.info("GLM stream request: model={}, url={}", props.getModel(), props.getBaseUrl());
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            log.warn("GLM stream error: HTTP {}", response.statusCode());
            throw new IOException("GLM API error: HTTP " + response.statusCode());
        }

        try (var is = response.body();
             var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (!line.startsWith("data:")) continue;

                var data = line.substring("data:".length()).trim();
                if (data.isEmpty()) continue;
                if ("[DONE]".equals(data)) break;

                try {
                    var root = objectMapper.readTree(data);
                    var delta = extractDeltaContent(root);
                    if (delta != null && !delta.isEmpty()) {
                        onDelta.accept(delta);
                    }
                } catch (Exception ignored) {
                    // best-effort: ignore malformed chunks
                }
            }
        }
    }

    private String extractDeltaContent(JsonNode root) {
        var choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) return null;
        var choice0 = choices.get(0);

        var deltaContent = choice0.path("delta").path("content");
        if (deltaContent.isTextual()) return deltaContent.asText();

        var msgContent = choice0.path("message").path("content");
        if (msgContent.isTextual()) return msgContent.asText();

        return null;
    }

    private String fallbackSummary(String content) {
        var safe = content == null ? "" : content.trim();
        if (safe.isBlank()) return "";
        return safe.length() <= 120 ? safe : safe.substring(0, 120) + "...";
    }

    private HttpRequest.Builder baseRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl()))
                .timeout(Duration.ofSeconds(Math.max(1, props.getTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.getApiKey());
    }

    private String buildChatRequest(String content, boolean stream) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", props.getModel());
        root.put("stream", stream);

        ArrayNode messages = root.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "你是一个文章摘要助手。请基于用户提供的内容生成中文摘要：\n1) 4-8 行\n2) 重点突出、信息密度高\n3) 不要编造不存在的事实\n4) 可以使用项目符号");
        messages.addObject()
                .put("role", "user")
                .put("content", content == null ? "" : content);

        return root.toString();
    }

    private String safeBodySnippet(String body) {
        if (body == null) return "";
        var s = body.replaceAll("\\s+", " ").trim();
        return s.length() <= 400 ? s : s.substring(0, 400) + "...";
    }
}

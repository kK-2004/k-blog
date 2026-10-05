package com.kk.kblog.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String summaryPrompt;

    public AiService(
            AiProperties props,
            ObjectMapper objectMapper,
            @Value("${app.ai.summary_prompt:}") String summaryPrompt
    ) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.summaryPrompt = summaryPrompt;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, props.getTimeoutSeconds())))
                .build();
    }

    public boolean isEnabled() {
        return props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    /**
     * 调用 LLM 生成摘要；未启用、请求失败或返回为空时抛异常
     */
    public String summarize(String content) throws IOException, InterruptedException {
        if (!isEnabled()) {
            throw new IllegalStateException("AI disabled: app.ai.api-key is empty");
        }

        var reqBody = buildChatRequest(content, false);
        var request = baseRequest()
                .POST(HttpRequest.BodyPublishers.ofString(reqBody, StandardCharsets.UTF_8))
                .build();

        log.info("AI summarize request: model={}, url={}, promptLen={}, contentLen={}",
                props.getModel(),
                props.getBaseUrl(),
                summaryPrompt.length(),
                content == null ? 0 : content.length());
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("AI API error: HTTP " + response.statusCode() + " body=" + safeBodySnippet(response.body()));
        }
        var root = objectMapper.readTree(response.body());
        var choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            var msg = choices.get(0).path("message").path("content").asText("");
            if (!msg.isBlank()) return msg.trim();
        }
        throw new IOException("AI API returned empty summary");
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
        if (!summaryPrompt.isBlank()) {
            messages.addObject()
                    .put("role", "system")
                    .put("content", summaryPrompt);
        }

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

package com.kk.kblog.controller.ai;

import com.kk.kblog.service.ai.AiService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    public record SummaryRequest(@NotBlank String content) {
    }

    public record SummaryResponse(String summary) {
    }

    @PostMapping("/summary")
    public SummaryResponse summary(@Valid @RequestBody SummaryRequest request) {
        log.info("AI summary request: contentLength={}, aiEnabled={}",
                request.content() == null ? 0 : request.content().length(),
                aiService.isEnabled());
        var summary = aiService.summarize(request.content());
        log.info("AI summary done: summaryLength={}", summary == null ? 0 : summary.length());
        return new SummaryResponse(summary);
    }

    @PostMapping(value = "/summary/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> summaryStream(@Valid @RequestBody SummaryRequest request) {
        StreamingResponseBody body = outputStream -> {
            var total = new StringBuilder();
            log.info("AI summary stream start: contentLength={}, aiEnabled={}",
                    request.content() == null ? 0 : request.content().length(),
                    aiService.isEnabled());
            try {
                aiService.summarizeStream(request.content(), delta -> {
                    try {
                        total.append(delta);
                        outputStream.write(delta.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    } catch (Exception ignored) {
                        log.warn("AI summary stream write failed: writtenLength={}", total.length());
                    }
                });
                log.info("AI summary stream done: totalLength={}", total.length());
            } catch (Exception e) {
                log.warn("AI summary stream failed: totalLength={}", total.length(), e);
                if (total.isEmpty()) {
                    var fallback = aiService.summarize(request.content());
                    outputStream.write(fallback.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    log.info("AI summary stream fallback sent: fallbackLength={}", fallback == null ? 0 : fallback.length());
                }
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }
}

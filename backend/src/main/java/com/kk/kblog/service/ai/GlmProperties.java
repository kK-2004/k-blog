package com.kk.kblog.service.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.ai.glm")
public class GlmProperties {
    /**
     * BigModel (GLM) API Key, e.g. from env `GLM_API_KEY`.
     */
    private String apiKey;

    /**
     * Chat Completions endpoint.
     */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    /**
     * Model name, e.g. `glm-4-flash` / `glm-4`.
     */
    private String model = "glm-4.5-flash";

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 60;

}


package org.example.aisvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "langchain4j.deepseek")
public class DeepSeekProperties {

    /**
     * DeepSeek API key, usually provided by environment variable DEEPSEEK_API_KEY.
     */
    private String apiKey;

    /**
     * OpenAI-compatible base URL of DeepSeek API.
     */
    private String baseUrl = "https://api.deepseek.com/v1";

    /**
     * Default chat model for common text generation.
     */
    private String chatModel = "deepseek-chat";

    /**
     * Sampling temperature.
     */
    private Double temperature = 0.2D;
}


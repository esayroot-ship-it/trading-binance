package org.example.aisvc.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(DeepSeekProperties.class)
public class LangChain4jConfig {

    @Bean
    public ChatModel deepSeekChatModel(DeepSeekProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DeepSeek API key is empty. Please set DEEPSEEK_API_KEY.");
        }

        return OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getChatModel())
                .temperature(properties.getTemperature())
                .build();
    }
}

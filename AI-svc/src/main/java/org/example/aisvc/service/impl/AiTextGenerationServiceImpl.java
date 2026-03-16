package org.example.aisvc.service.impl;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.example.aisvc.service.AiTextGenerationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiTextGenerationServiceImpl implements AiTextGenerationService {

    private final ChatModel chatModel;

    @Override
    public String summarizeNews(String newsContent) {
        if (!StringUtils.hasText(newsContent)) {
            throw new IllegalArgumentException("newsContent is required");
        }

        String prompt = """
                You are a professional financial editor.
                Please summarize the following news in Chinese.
                Requirements:
                1) Keep the length around 80-120 Chinese characters.
                2) Include key event, affected assets, and potential market impact.
                3) Do not fabricate facts.
                News content:
                %s
                """.formatted(newsContent.trim());

        return chatModel.chat(prompt);
    }

    @Override
    public String analyzeNews(String newsContent, String title, String source) {
        if (!StringUtils.hasText(newsContent)) {
            throw new IllegalArgumentException("newsContent is required");
        }

        String safeTitle = StringUtils.hasText(title) ? title.trim() : "UNKNOWN";
        String safeSource = StringUtils.hasText(source) ? source.trim() : "UNKNOWN";
        String prompt = """
                You are a professional market research assistant.
                Based on the given financial news body, provide a brief analysis in Chinese.
                Requirements:
                1) Explain the core event in one short paragraph.
                2) Analyze possible short-term impact on market sentiment and related assets.
                3) Give one risk reminder sentence.
                4) Keep the output concise and practical.

                News title: %s
                News source: %s
                News body:
                %s
                """.formatted(safeTitle, safeSource, newsContent.trim());
        return chatModel.chat(prompt);
    }

    @Override
    public String analyzeKline(String klineJson, String symbol, String timeframe) {
        if (!StringUtils.hasText(klineJson)) {
            throw new IllegalArgumentException("klineJson is required");
        }

        String prompt = """
                You are a quantitative trading analysis assistant.
                Based on the given kline data, provide a brief analysis in Chinese.
                Requirements:
                1) Identify current trend (uptrend/downtrend/sideways).
                2) Provide key support and resistance levels.
                3) Give one risk reminder sentence.
                Symbol: %s
                Timeframe: %s
                Kline JSON:
                %s
                """.formatted(
                StringUtils.hasText(symbol) ? symbol.trim() : "UNKNOWN",
                StringUtils.hasText(timeframe) ? timeframe.trim() : "UNKNOWN",
                klineJson.trim()
        );

        return chatModel.chat(prompt);
    }
}

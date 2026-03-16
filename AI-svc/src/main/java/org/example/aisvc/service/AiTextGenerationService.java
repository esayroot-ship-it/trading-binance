package org.example.aisvc.service;

public interface AiTextGenerationService {

    String summarizeNews(String newsContent);

    String analyzeNews(String newsContent, String title, String source);

    String analyzeKline(String klineJson, String symbol, String timeframe);
}

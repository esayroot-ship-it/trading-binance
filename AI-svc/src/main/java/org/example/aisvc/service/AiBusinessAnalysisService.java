package org.example.aisvc.service;

import org.example.aisvc.dto.AiKlineAnalysisResponse;
import org.example.aisvc.dto.AiNewsAnalysisResponse;

public interface AiBusinessAnalysisService {

    AiNewsAnalysisResponse analyzeNewsById(Long newsId);

    AiKlineAnalysisResponse analyzeKlineFromCache(String symbol, String timeframe);
}

package org.example.aisvc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.aisvc.dto.AiKlineAnalysisResponse;
import org.example.aisvc.dto.AiNewsAnalysisResponse;
import org.example.aisvc.mapper.NewsArticleMapper;
import org.example.aisvc.service.AiBusinessAnalysisService;
import org.example.aisvc.service.AiTextGenerationService;
import org.example.common.entity.NewsArticle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiBusinessAnalysisServiceImpl implements AiBusinessAnalysisService {

    private static final String KLINE_KEY_PREFIX = "market:kline:";
    private static final String DEFAULT_TIMEFRAME = "1m";

    private final NewsArticleMapper newsArticleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final AiTextGenerationService aiTextGenerationService;
    private final ObjectMapper objectMapper;

    @Override
    public AiNewsAnalysisResponse analyzeNewsById(Long newsId) {
        if (newsId == null || newsId <= 0) {
            throw new IllegalArgumentException("newsId is required");
        }
        NewsArticle article = newsArticleMapper.selectVisibleById(newsId);
        if (article == null) {
            throw new IllegalArgumentException("news not found or not visible");
        }
        if (!StringUtils.hasText(article.getContent())) {
            throw new IllegalArgumentException("news content is empty");
        }

        String analysis = aiTextGenerationService.analyzeNews(
                article.getContent(),
                article.getTitle(),
                article.getSource());

        AiNewsAnalysisResponse response = new AiNewsAnalysisResponse();
        response.setNewsId(article.getId());
        response.setTitle(article.getTitle());
        response.setSource(article.getSource());
        response.setAnalysis(analysis);
        response.setAnalyzeTime(System.currentTimeMillis());
        return response;
    }

    @Override
    public AiKlineAnalysisResponse analyzeKlineFromCache(String symbol, String timeframe) {
        String safeSymbol = normalizeSymbol(symbol);
        String safeTimeframe = normalizeTimeframe(timeframe);
        String cacheKey = klineCacheKey(safeSymbol, safeTimeframe);
        String klineJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!StringUtils.hasText(klineJson)) {
            throw new IllegalArgumentException("kline cache not found");
        }

        String analysis = aiTextGenerationService.analyzeKline(klineJson, safeSymbol, safeTimeframe);

        AiKlineAnalysisResponse response = new AiKlineAnalysisResponse();
        response.setSymbol(safeSymbol);
        response.setTimeframe(safeTimeframe);
        response.setCacheKey(cacheKey);
        response.setCandleCount(extractCandleCount(klineJson));
        response.setLastKlineTime(extractLastKlineTime(klineJson));
        response.setAnalysis(analysis);
        response.setAnalyzeTime(System.currentTimeMillis());
        return response;
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new IllegalArgumentException("symbol is required");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTimeframe(String timeframe) {
        if (!StringUtils.hasText(timeframe)) {
            return DEFAULT_TIMEFRAME;
        }
        return timeframe.trim();
    }

    private String klineCacheKey(String symbol, String timeframe) {
        return KLINE_KEY_PREFIX + symbol + ":" + timeframe;
    }

    private Integer extractCandleCount(String klineJson) {
        try {
            JsonNode root = objectMapper.readTree(klineJson);
            if (!root.isArray()) {
                return 0;
            }
            return root.size();
        } catch (Exception ex) {
            log.warn("提取蜡烛图数量失败", ex);
            return 0;
        }
    }

    private Long extractLastKlineTime(String klineJson) {
        try {
            JsonNode root = objectMapper.readTree(klineJson);
            if (!root.isArray() || root.size() == 0) {
                return null;
            }
            JsonNode last = root.get(root.size() - 1);
            if (!last.isArray()) {
                return null;
            }
            if (last.size() > 6) {
                return last.get(6).asLong();
            }
            return last.get(0).asLong();
        } catch (Exception ex) {
            log.warn("提取最后蜡烛图时间失败", ex);
            return null;
        }
    }
}

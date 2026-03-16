package org.example.aisvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.aisvc.dto.AiKlineAnalysisResponse;
import org.example.aisvc.dto.AiNewsAnalysisResponse;
import org.example.aisvc.service.AiBusinessAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

/**
 * AI分析控制器。
 */
@RestController
@RequestMapping("/api/ai/analysis")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiBusinessAnalysisService aiBusinessAnalysisService;

    /**
     * 分析新闻。
     *
     * @param newsId 新闻ID
     * @return 新闻分析结果
     */
    @GetMapping("/news/{newsId}")
    public R<AiNewsAnalysisResponse> analyzeNews(@PathVariable Long newsId) {
        try {
            return R.ok("analyze success", aiBusinessAnalysisService.analyzeNewsById(newsId));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            return R.fail("ai service error: " + ex.getMessage());
        }
    }

    /**
     * 分析K线。
     *
     * @param symbol 标的代码
     * @param timeframe 时间范围
     * @return K线分析结果
     */
    @GetMapping("/kline/{symbol}")
    public R<AiKlineAnalysisResponse> analyzeKline(
            @PathVariable String symbol,
            @RequestParam(required = false) String timeframe) {
        try {
            return R.ok("analyze success", aiBusinessAnalysisService.analyzeKlineFromCache(symbol, timeframe));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            return R.fail("ai service error: " + ex.getMessage());
        }
    }
}

package org.example.aisvc.dto;

import lombok.Data;

/**
 * AI 新闻分析结果响应。
 */
@Data
public class AiNewsAnalysisResponse {

    /** 新闻ID。 */
    private Long newsId;

    /** 新闻标题。 */
    private String title;

    /** 新闻来源。 */
    private String source;

    /** AI 输出的分析文本。 */
    private String analysis;

    /** 分析生成时间戳（毫秒）。 */
    private Long analyzeTime;
}

package com.loan.agent.rag.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档检索结果
 *
 * <p>包含匹配的文档片段、来源信息和相似度分数。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /** 文档 ID */
    private String documentId;

    /** 文档标题 */
    private String title;

    /** 匹配的文档片段（截取相关部分） */
    private String excerpt;

    /** 文档分类 */
    private String category;

    /** 相似度分数 (0.0 ~ 1.0)，越高越匹配 */
    private double similarity;
}

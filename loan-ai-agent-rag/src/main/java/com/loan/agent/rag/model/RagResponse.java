package com.loan.agent.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 问答响应
 *
 * <p>包含 LLM 生成的答案、引用来源和检索到的文档片段。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {

    /** 用户原始问题 */
    private String question;

    /** LLM 生成的回答 */
    private String answer;

    /** 引用来源列表 */
    private List<SourceInfo> sources;

    /** 处理耗时，如 "1.2s" */
    private String processingTime;

    /**
     * 引用来源信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceInfo {

        /** 文档标题 */
        private String title;

        /** 匹配的文档片段 */
        private String excerpt;

        /** 相似度分数 (0.0 ~ 1.0) */
        private double similarity;

        /** 文档分类 */
        private String category;
    }
}

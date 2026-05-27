package com.loan.agent.rag.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 问题解析器 — 对用户问题进行标准化预处理
 *
 * <p>职责：
 * <ol>
 *   <li>去除冗余空白和标点</li>
 *   <li>提取关键词（供日志/调试使用）</li>
 *   <li>统一问题格式</li>
 * </ol>
 *
 * <p>这是一个简化的解析器。生产环境可以升级为：
 * <ul>
 *   <li>意图识别（询问 vs 投诉 vs 闲聊）</li>
 *   <li>实体抽取（人名、金额、日期）</li>
 *   <li>问题改写（Query Rewriting）提升检索效果</li>
 * </ul>
 */
@Component
public class QueryParser {

    private static final Logger log = LoggerFactory.getLogger(QueryParser.class);

    /**
     * 解析并标准化用户问题
     *
     * @param rawQuestion 用户输入的原始问题
     * @return 标准化后的问题文本
     */
    public String parse(String rawQuestion) {
        // 1. 去除首尾空白和多余空格
        String cleaned = rawQuestion.trim().replaceAll("\\s+", " ");

        // 2. 去除末尾多余的标点
        cleaned = cleaned.replaceAll("[？！。，、]+$", "");

        log.info("问题解析: 原始=\"{}\" → 标准化=\"{}\"", rawQuestion, cleaned);
        return cleaned;
    }
}

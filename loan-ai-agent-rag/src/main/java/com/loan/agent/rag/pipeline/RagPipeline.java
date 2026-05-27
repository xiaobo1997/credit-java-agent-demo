package com.loan.agent.rag.pipeline;

import com.loan.agent.rag.model.RagResponse;
import com.loan.agent.rag.retrieval.DocumentRetriever;
import com.loan.agent.rag.retrieval.SearchResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 完整流程编排器
 *
 * <h3>四步流程</h3>
 * <ol>
 *   <li><b>解析问题</b> — 调用 {@link QueryParser} 标准化用户输入</li>
 *   <li><b>检索文档</b> — 调用 {@link DocumentRetriever} 查找相关文档片段</li>
 *   <li><b>组装 Prompt</b> — 将检索到的文档作为上下文注入 System Prompt</li>
 *   <li><b>LLM 生成</b> — 调用 DeepSeek 生成带引用的回答</li>
 * </ol>
 *
 * <h3>RAG 是什么？—— 开卷考试类比</h3>
 * <p>LLM 是考生，知识库是参考书。普通对话 = 闭卷考试（LLM 凭记忆回答），
 * RAG = 开卷考试（先帮你翻到正确的页码，再让 LLM 基于那一页回答）。</p>
 * <p>所以 RAG 的关键不是 LLM 有多聪明，而是"翻书"翻得准不准——即检索质量。</p>
 */
@Service
public class RagPipeline {

    private static final Logger log = LoggerFactory.getLogger(RagPipeline.class);

    private final QueryParser queryParser;
    private final DocumentRetriever documentRetriever;
    private final ChatLanguageModel chatModel;

    public RagPipeline(QueryParser queryParser,
                       DocumentRetriever documentRetriever,
                       ChatLanguageModel chatModel) {
        this.queryParser = queryParser;
        this.documentRetriever = documentRetriever;
        this.chatModel = chatModel;
    }

    /**
     * 执行完整的 RAG 问答流程
     *
     * @param rawQuestion 用户原始问题
     * @return 包含答案和引用来源的响应
     */
    public RagResponse execute(String rawQuestion) {
        long startTime = System.currentTimeMillis();
        log.info("========== RAG Pipeline 开始 ==========");

        // ===== 第 1 步：解析问题 =====
        String question = queryParser.parse(rawQuestion);
        log.info("[步骤1/4] 问题解析完成: \"{}\"", question);

        // ===== 第 2 步：检索相关文档 =====
        List<SearchResult> searchResults = documentRetriever.search(question);
        log.info("[步骤2/4] 文档检索完成: 命中 {} 篇", searchResults.size());

        if (searchResults.isEmpty()) {
            // 没有匹配到知识库内容时，回退到纯 LLM 回答
            log.warn("未检索到相关文档，使用纯 LLM 回答");
            String answer = chatModel.generate(
                    "你是贷款知识助手。用户问：" + question + "\n请根据你的知识回答，并提示这不在当前知识库范围内。");
            long elapsed = System.currentTimeMillis() - startTime;
            return RagResponse.builder()
                    .question(question)
                    .answer(answer)
                    .sources(List.of())
                    .processingTime(formatTime(elapsed))
                    .build();
        }

        // ===== 第 3 步：组装 Prompt（RAG 的核心！） =====
        String prompt = buildPrompt(question, searchResults);
        log.info("[步骤3/4] Prompt 组装完成 ({} 字符)", prompt.length());

        // ===== 第 4 步：调用 LLM 生成回答 =====
        String answer = chatModel.generate(prompt);
        log.info("[步骤4/4] LLM 回答生成完成 ({} 字符)", answer.length());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("========== RAG Pipeline 结束 ({}ms) ==========", elapsed);

        // 构建响应
        List<RagResponse.SourceInfo> sources = searchResults.stream()
                .map(r -> RagResponse.SourceInfo.builder()
                        .title(r.getTitle())
                        .excerpt(r.getExcerpt())
                        .similarity(r.getSimilarity())
                        .category(r.getCategory())
                        .build())
                .collect(Collectors.toList());

        return RagResponse.builder()
                .question(question)
                .answer(answer)
                .sources(sources)
                .processingTime(formatTime(elapsed))
                .build();
    }

    /**
     * 构建 RAG Prompt
     *
     * <p>Prompt 结构：
     * <pre>
     * System: 角色设定 + 行为约束 + 知识库内容
     * User: 用户原始问题
     * </pre>
     *
     * <p>关键是让 LLM 知道：回答必须基于提供的知识库内容，不要编造。</p>
     */
    private String buildPrompt(String question, List<SearchResult> results) {
        StringBuilder knowledgeContext = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            knowledgeContext.append(String.format("""
                    【知识片段 %d】来源：%s（分类：%s）
                    %s

                    """, i + 1, r.getTitle(), r.getCategory(), r.getExcerpt()));
        }

        return String.format("""
                你是一个专业的贷款知识智能助手。请严格基于以下知识库内容回答用户问题。

                ## 知识库内容
                %s

                ## 回答规则
                1. 如果知识库中有相关内容，请基于知识库回答，并在答案末尾标注引用来源
                2. 如果知识库中没有相关内容，请明确说"当前知识库中暂无相关信息"
                3. 不要编造知识库中没有的内容
                4. 回答要简洁、准确，用通俗易懂的语言

                ## 用户问题
                %s

                ## 你的回答
                """, knowledgeContext.toString(), question);
    }

    /**
     * 格式化耗时
     */
    private String formatTime(long millis) {
        if (millis < 1000) return millis + "ms";
        return String.format("%.1fs", millis / 1000.0);
    }
}

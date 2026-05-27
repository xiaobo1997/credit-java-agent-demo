package com.loan.agent.rag.retrieval;

import com.loan.agent.rag.model.KnowledgeDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档检索器 — 基于 TF-IDF + 余弦相似度
 *
 * <h3>为什么不用向量数据库？</h3>
 * <p>这是教学 Demo，核心目的是让 Java 程序员理解 RAG 的检索原理。
 * TF-IDF 是 NLP 中最经典的文本相似度算法，纯 Java 实现，零外部依赖。
 * 理解了这个，再换成 Elasticsearch / Milvus 等向量数据库就很容易。</p>
 *
 * <h3>算法流程</h3>
 * <ol>
 *   <li>构建索引：对所有文档分词 → 计算 TF-IDF → 生成文档向量</li>
 *   <li>查询：用户问题分词 → 转 TF-IDF 向量 → 与所有文档向量计算余弦相似度 → 返回 Top-K</li>
 * </ol>
 *
 * <h3>生产环境升级路径</h3>
 * <p>将 TF-IDF 替换为 Embedding 模型（如 text-embedding-3-small），
 * 将内存向量存储替换为向量数据库（Milvus / Elasticsearch），
 * 检索接口不变。</p>
 */
@Service
public class DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(DocumentRetriever.class);

    @Value("${rag.retrieval.top-k:3}")
    private int topK;

    @Value("${rag.retrieval.min-score:0.1}")
    private double minScore;

    /** 中文常见停用词 */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一",
            "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着",
            "没有", "看", "好", "自己", "这", "他", "她", "它", "们", "那", "些",
            "什么", "怎么", "如何", "哪个", "吗", "呢", "吧", "啊", "哦", "嗯"
    );

    /** 文档向量存储：docId → (term → tfidf) */
    private Map<String, Map<String, Double>> documentVectors = new HashMap<>();

    /** 词汇表 → IDF 值 */
    private Map<String, Double> idfScores = new HashMap<>();

    /** 文档元信息 */
    private Map<String, KnowledgeDocument> docMeta = new LinkedHashMap<>();

    /**
     * 构建 TF-IDF 检索索引
     *
     * @param documents 所有知识文档
     */
    public void buildIndex(List<KnowledgeDocument> documents) {
        log.info("开始构建 TF-IDF 索引，文档数: {}", documents.size());

        // 1. 保存文档元信息
        docMeta.clear();
        for (KnowledgeDocument doc : documents) {
            docMeta.put(doc.getId(), doc);
        }

        // 2. 对所有文档分词 → 词频统计
        List<Map<String, Integer>> termFrequencies = new ArrayList<>();
        for (KnowledgeDocument doc : documents) {
            Map<String, Integer> tf = computeTermFrequency(doc.getContent());
            termFrequencies.add(tf);
        }

        // 3. 计算 IDF（逆文档频率）
        idfScores.clear();
        int totalDocs = documents.size();
        for (int i = 0; i < totalDocs; i++) {
            for (String term : termFrequencies.get(i).keySet()) {
                // 统计包含该词的文档数
                int docCount = 0;
                for (Map<String, Integer> tfMap : termFrequencies) {
                    if (tfMap.containsKey(term)) docCount++;
                }
                // IDF = log(总文档数 / 包含该词的文档数)
                idfScores.put(term, Math.log((double) totalDocs / docCount));
            }
        }

        // 4. 生成每篇文档的 TF-IDF 向量
        documentVectors.clear();
        for (int i = 0; i < totalDocs; i++) {
            String docId = documents.get(i).getId();
            Map<String, Integer> tf = termFrequencies.get(i);
            Map<String, Double> tfidf = new HashMap<>();
            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                String term = entry.getKey();
                double tfValue = entry.getValue();
                double idfValue = idfScores.getOrDefault(term, 0.0);
                tfidf.put(term, tfValue * idfValue);
            }
            documentVectors.put(docId, tfidf);
        }

        log.info("TF-IDF 索引构建完成: {} 篇文档, {} 个词汇",
                documentVectors.size(), idfScores.size());
    }

    /**
     * 检索与问题最相关的 Top-K 文档
     *
     * @param query 用户问题
     * @return 按相似度降序排列的检索结果
     */
    public List<SearchResult> search(String query) {
        if (documentVectors.isEmpty()) {
            log.warn("检索索引为空，请先调用 buildIndex()");
            return Collections.emptyList();
        }

        // 1. 将查询转为 TF-IDF 向量（使用已计算的 IDF）
        Map<String, Integer> queryTF = computeTermFrequency(query);
        Map<String, Double> queryVector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : queryTF.entrySet()) {
            String term = entry.getKey();
            double idf = idfScores.getOrDefault(term, 0.0);
            queryVector.put(term, entry.getValue() * idf);
        }

        // 2. 计算与所有文档的余弦相似度
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : documentVectors.entrySet()) {
            String docId = entry.getKey();
            Map<String, Double> docVector = entry.getValue();
            double similarity = cosineSimilarity(queryVector, docVector);

            if (similarity >= minScore) {
                KnowledgeDocument doc = docMeta.get(docId);
                results.add(SearchResult.builder()
                        .documentId(docId)
                        .title(doc.getTitle())
                        .excerpt(truncate(doc.getContent(), 200))
                        .category(doc.getCategory())
                        .similarity(Math.round(similarity * 100.0) / 100.0)
                        .build());
            }
        }

        // 3. 按相似度降序排列，返回 Top-K
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        List<SearchResult> topResults = results.stream()
                .limit(topK)
                .collect(Collectors.toList());

        log.info("检索完成: 查询=\"{}\", 命中={}篇, 返回Top-{}",
                query, results.size(), topResults.size());
        return topResults;
    }

    // ========== 私有方法 ==========

    /**
     * 分词 + 词频统计
     * <p>简单实现：按非中文字符分割，过滤停用词和单字。</p>
     */
    private Map<String, Integer> computeTermFrequency(String text) {
        Map<String, Integer> tf = new HashMap<>();
        // 按非中文字符和标点分割
        String[] words = text.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
        for (String word : words) {
            if (word.length() < 2) continue;            // 过滤单字
            if (STOP_WORDS.contains(word)) continue;     // 过滤停用词
            tf.merge(word.toLowerCase(), 1, Integer::sum);
        }
        return tf;
    }

    /**
     * 计算两个稀疏向量的余弦相似度
     *
     * <p>余弦相似度 = (A·B) / (|A| × |B|)</p>
     * <p>值域 [0, 1]，越接近 1 表示越相似。</p>
     */
    private double cosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        // 点积: A·B = Σ(A[i] × B[i])
        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : vecA.entrySet()) {
            Double bValue = vecB.get(entry.getKey());
            if (bValue != null) {
                dotProduct += entry.getValue() * bValue;
            }
        }

        // 模长: |A| = √Σ(A[i]²)
        double magnitudeA = 0.0;
        for (double v : vecA.values()) {
            magnitudeA += v * v;
        }
        magnitudeA = Math.sqrt(magnitudeA);

        double magnitudeB = 0.0;
        for (double v : vecB.values()) {
            magnitudeB += v * v;
        }
        magnitudeB = Math.sqrt(magnitudeB);

        if (magnitudeA == 0 || magnitudeB == 0) return 0.0;
        return dotProduct / (magnitudeA * magnitudeB);
    }

    /**
     * 截断文本，保留前 n 个字符
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}

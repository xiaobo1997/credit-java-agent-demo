package com.loan.agent.rag.knowledge;

import com.loan.agent.rag.model.KnowledgeDocument;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 知识库管理器 — 纯内存存储
 *
 * <p>职责：加载知识文档、提供文档列表和统计信息。
 * 实际数据从 JSON 文件加载，由 {@link KnowledgeInitializer} 在启动时调用。</p>
 */
@Component
public class KnowledgeBase {

    /** 内存文档存储，key=文档ID */
    private final Map<String, KnowledgeDocument> documents = new LinkedHashMap<>();

    /**
     * 批量加载文档到内存
     */
    public void load(List<KnowledgeDocument> docs) {
        documents.clear();
        for (KnowledgeDocument doc : docs) {
            documents.put(doc.getId(), doc);
        }
    }

    /**
     * 获取所有文档
     */
    public List<KnowledgeDocument> getAllDocuments() {
        return new ArrayList<>(documents.values());
    }

    /**
     * 按 ID 获取文档
     */
    public KnowledgeDocument getById(String id) {
        return documents.get(id);
    }

    /**
     * 文档总数
     */
    public int size() {
        return documents.size();
    }

    /**
     * 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Long> categoryCount = new LinkedHashMap<>();
        for (KnowledgeDocument doc : documents.values()) {
            categoryCount.merge(doc.getCategory(), 1L, Long::sum);
        }
        return Map.of(
                "totalDocuments", documents.size(),
                "categoryDistribution", categoryCount
        );
    }
}

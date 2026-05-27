package com.loan.agent.rag.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.agent.rag.model.KnowledgeDocument;
import com.loan.agent.rag.retrieval.DocumentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 知识库初始化器 — 应用启动时自动加载知识库并预热检索索引
 *
 * <p>实现 {@link ApplicationRunner}，在 Spring Boot 启动完成后执行：
 * <ol>
 *   <li>从 classpath:knowledge/loan-faq.json 加载知识文档</li>
 *   <li>将文档加载到 {@link KnowledgeBase}</li>
 *   <li>调用 {@link DocumentRetriever} 构建 TF-IDF 索引</li>
 * </ol>
 */
@Component
public class KnowledgeInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeInitializer.class);

    private final KnowledgeBase knowledgeBase;
    private final DocumentRetriever documentRetriever;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeInitializer(KnowledgeBase knowledgeBase, DocumentRetriever documentRetriever) {
        this.knowledgeBase = knowledgeBase;
        this.documentRetriever = documentRetriever;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========== 知识库初始化开始 ==========");

        // 1. 从 JSON 文件加载文档
        ClassPathResource resource = new ClassPathResource("knowledge/loan-faq.json");
        try (InputStream is = resource.getInputStream()) {
            List<KnowledgeDocument> documents = objectMapper.readValue(is,
                    new TypeReference<List<KnowledgeDocument>>() {});
            knowledgeBase.load(documents);
            log.info("加载知识文档: {} 篇", documents.size());
        }

        // 2. 构建 TF-IDF 检索索引
        documentRetriever.buildIndex(knowledgeBase.getAllDocuments());
        log.info("TF-IDF 检索索引构建完成");

        // 3. 打印统计
        log.info("知识库统计: {}", knowledgeBase.getStats());
        log.info("========== 知识库初始化完成 ==========");
    }
}

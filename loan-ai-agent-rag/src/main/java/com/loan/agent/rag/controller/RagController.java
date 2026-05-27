package com.loan.agent.rag.controller;

import com.loan.agent.rag.model.RagRequest;
import com.loan.agent.rag.model.RagResponse;
import com.loan.agent.rag.pipeline.RagPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 问答 REST 接口
 *
 * <p>提供 HTTP API 供前端或其他服务调用。</p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagPipeline ragPipeline;

    public RagController(RagPipeline ragPipeline) {
        this.ragPipeline = ragPipeline;
    }

    /**
     * RAG 问答接口
     *
     * <p>接收自然语言问题，经过"检索→增强→生成"流程返回答案。</p>
     *
     * <pre>
     * POST /api/rag/ask
     * Content-Type: application/json
     *
     * { "question": "等额本息和等额本金有什么区别？" }
     * </pre>
     */
    @PostMapping("/ask")
    public RagResponse ask(@RequestBody RagRequest request) {
        log.info("收到 RAG 问答请求: \"{}\"", request.getQuestion());
        return ragPipeline.execute(request.getQuestion());
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "RAG Module OK";
    }
}

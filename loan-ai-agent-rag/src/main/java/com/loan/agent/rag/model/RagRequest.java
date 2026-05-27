package com.loan.agent.rag.model;

import lombok.Data;

/**
 * RAG 问答请求
 */
@Data
public class RagRequest {

    /** 用户自然语言问题 */
    private String question;
}

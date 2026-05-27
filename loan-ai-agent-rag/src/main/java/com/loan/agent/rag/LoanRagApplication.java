package com.loan.agent.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 贷款知识智能小助手 — RAG 模块启动类
 *
 * <p>独立运行（端口 8081），与 chat 模块互不干扰。
 * <p>启动时自动加载知识库并预热检索索引。
 */
@SpringBootApplication
public class LoanRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanRagApplication.class, args);
    }
}

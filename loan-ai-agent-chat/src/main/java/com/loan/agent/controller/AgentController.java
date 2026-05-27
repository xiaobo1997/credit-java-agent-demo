package com.loan.agent.controller;

import com.loan.agent.agent.CreditReviewAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Agent 接口：支持多轮对话记忆。
 * 同一 memoryId 的请求共享上下文——Agent 会记住之前的审核记录和追问。
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final CreditReviewAgent creditReviewAgent;

    @PostMapping("/review")
    public Map<String, Object> review(@RequestBody Map<String, String> request) {
        String memoryId = request.getOrDefault("memoryId", UUID.randomUUID().toString());
        String name = request.getOrDefault("name", "未知");
        String idCard = request.getOrDefault("idCard", "");
        String amount = request.getOrDefault("amount", "0");
        String purpose = request.getOrDefault("purpose", "未说明");
        String question = request.get("question");

        String application;
        if (question != null && !question.isBlank()) {
            // 追问模式：不再重复申请信息
            application = question;
        } else {
            application = String.format(
                    "【借款申请】姓名: %s, 身份证号: %s, 申请金额: %s元, 用途: %s",
                    name, idCard, amount, purpose
            );
        }

        String result = creditReviewAgent.review(memoryId, application);
        return Map.of("memoryId", memoryId, "application", application, "result", result);
    }
}

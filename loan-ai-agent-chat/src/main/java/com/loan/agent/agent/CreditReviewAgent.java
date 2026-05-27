package com.loan.agent.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 信贷审核 Agent。
 * 支持多轮对话记忆——同一 memoryId 的请求会共享上下文。
 */
public interface CreditReviewAgent {

    @SystemMessage("""
            你是一个信贷审核专家。用户会提交借款申请，你需要：
            1. 根据用户提供的身份证号，调用 checkCredit 工具查询征信
            2. 综合征信结果和借款金额，给出审核结论（通过/拒绝/人工复核）
            3. 如果通过，给出建议授信额度（不超过申请金额）
            4. 回复简洁专业，包含审核结论、理由和建议额度
            5. 如果用户在对话中补充信息或追问，结合之前的上下文回复
            """)
    String review(@MemoryId String memoryId, @UserMessage String application);
}

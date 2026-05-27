package com.loan.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 征信查询工具（Mock 实现）。
 * LLM 会根据用户意图自动决定是否调用此工具。
 */
@Slf4j
@Component
public class CreditCheckTool {

    private final Random random = new Random();

    @Tool("查询用户的征信评分。输入身份证号，返回征信分数（300-850）和信用等级（优秀/良好/一般/较差）")
    public String checkCredit(String idCard) {
        log.info("征信查询工具被调用: idCard={}", idCard);

        // Mock：根据身份证尾号模拟不同分数，便于验证 Agent 是否真的调用了工具
        int score;
        try {
            int lastDigit = Integer.parseInt(idCard.substring(idCard.length() - 1));
            score = 300 + lastDigit * 55;
        } catch (Exception e) {
            score = 650;
        }

        String level;
        if (score >= 750) level = "优秀";
        else if (score >= 650) level = "良好";
        else if (score >= 550) level = "一般";
        else level = "较差";

        return String.format("征信分数: %d, 信用等级: %s", score, level);
    }
}

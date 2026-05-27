package com.loan.agent.controller;

import com.loan.agent.evaluate.AgentEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评测接口：运行全部测试用例，返回通过率和详细结果。
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class EvaluateController {

    private final AgentEvaluator evaluator;

    @GetMapping("/evaluate")
    public AgentEvaluator.EvaluationResult evaluate() {
        return evaluator.evaluate();
    }
}

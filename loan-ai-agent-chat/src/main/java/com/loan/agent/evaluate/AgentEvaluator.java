package com.loan.agent.evaluate;

import com.loan.agent.agent.CreditReviewAgent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 评测服务。
 * 用预定义的测试用例跑 Agent，检查输出是否包含期望关键词，统计通过率。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEvaluator {

    private final CreditReviewAgent creditReviewAgent;

    /**
     * 预定义测试用例（5个，覆盖通过/拒绝/人工复核）。
     * 身份证尾号 → 征信分数：0=300 1=355 2=410 3=465 4=520 5=575 6=630 7=685 8=740 9=795
     */
    private static final List<TestCase> TEST_CASES = List.of(
            new TestCase("优秀-小额借款", "王五", "320102199001011239", "10000", "医疗", "通过", 795),
            new TestCase("良好-大额借款", "赵六", "320102199001011238", "200000", "购房", "通过", 740),
            new TestCase("较差-任意借款", "孙七", "320102199001011234", "30000", "旅游", "拒绝", 520),
            new TestCase("一般-中等借款", "周八", "320102199001011236", "50000", "教育", "人工复核", 630),
            new TestCase("极差-小额借款", "吴九", "320102199001011230", "5000", "日常消费", "拒绝", 300)
    );

    /**
     * 运行全部评测用例，返回结果。
     */
    public EvaluationResult evaluate() {
        List<CaseResult> caseResults = new ArrayList<>();

        for (TestCase testCase : TEST_CASES) {
            String application = String.format(
                    "【借款申请】姓名: %s, 身份证号: %s, 申请金额: %s元, 用途: %s",
                    testCase.getApplicantName(), testCase.getIdCard(),
                    testCase.getAmount(), testCase.getPurpose()
            );

            String memoryId = "eval-" + UUID.randomUUID();
            log.info("评测用例 [{}]: idCard={}", testCase.getName(), testCase.getIdCard());

            String result = creditReviewAgent.review(memoryId, application);

            boolean hasKeyword = result.contains(testCase.getExpectedKeyword());
            boolean hasScore = result.contains(String.valueOf(testCase.getExpectedScore()));

            caseResults.add(new CaseResult(
                    testCase.getName(),
                    testCase.getExpectedKeyword(),
                    hasKeyword,
                    hasKeyword && hasScore,
                    result.substring(0, Math.min(200, result.length()))
            ));

            log.info("  期望: {}, 命中: {}, 分数校验: {}", testCase.getExpectedKeyword(), hasKeyword, hasScore);
        }

        long passed = caseResults.stream().filter(CaseResult::isPassed).count();
        double accuracy = (double) passed / caseResults.size() * 100;

        return new EvaluationResult(caseResults, (int) passed, caseResults.size(), accuracy);
    }

    @Data
    @AllArgsConstructor
    public static class CaseResult {
        private String caseName;
        private String expectedKeyword;
        private boolean keywordMatched;
        private boolean passed;
        private String resultPreview;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EvaluationResult {
        private List<CaseResult> cases;
        private int passed;
        private int total;
        private double accuracy;
    }
}

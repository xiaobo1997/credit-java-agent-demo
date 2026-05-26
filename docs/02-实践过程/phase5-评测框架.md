# Phase 5: 评测框架

## 目标

用预定义测试用例自动跑 Agent，检查输出是否符合预期，量化 Agent 的决策准确性。

## 为什么需要评测

Agent 不像传统代码——没有"单元测试"可以验证。Agent 的输出是自然语言，而且是概率性的（同一输入可能不同输出）。评测框架解决的就是"怎么判断 Agent 做对了"。

## 评测策略

### 两级校验

| 级别 | 检查什么 | 为什么 |
|------|---------|--------|
| 关键词命中 | 结果包含"通过"/"拒绝"/"人工复核" | 判断 Agent 决策方向是否正确 |
| 分数校验 | 结果包含正确的征信分数（如 795） | 判断 Agent 是否真的调用了工具 |

两级都通过才算该用例 PASS。

### 5 个用例覆盖决策边界

```
尾号0(300) → 极差 → 期望拒绝
尾号4(520) → 较差 → 期望拒绝
尾号6(630) → 一般 → 期望人工复核  ← 边界！
尾号8(740) → 良好 → 期望通过
尾号9(795) → 优秀 → 期望通过
```

## 实现

```java
@Test
public class AgentEvaluator {
    // 预定义测试用例
    List<TestCase> cases = List.of(
        new TestCase("优秀", "王五", "尾号9", "10000", "通过", 795),
        new TestCase("良好", "赵六", "尾号8", "200000", "通过", 740),
        new TestCase("一般", "周八", "尾号6", "50000", "人工复核", 630),
        new TestCase("较差", "孙七", "尾号4", "30000", "拒绝", 520),
        new TestCase("极差", "吴九", "尾号0", "5000", "拒绝", 300)
    );

    public EvaluationResult evaluate() {
        for (TestCase tc : cases) {
            String result = agent.review(memoryId, buildApplication(tc));
            boolean keywordOk = result.contains(tc.expectedKeyword);
            boolean scoreOk = result.contains(String.valueOf(tc.expectedScore));
            // ... 统计通过率
        }
    }
}
```

## 验证结果

```
5/5 全部通过，准确率 100%

优秀-小额(795) → 通过 ✅
良好-大额(740) → 通过 ✅
一般-中等(630) → 人工复核 ✅  ← 边界准确
较差-任意(520) → 拒绝 ✅
极差-小额(300) → 拒绝 ✅
```

Agent 的决策边界和预期完全一致。

## 扩展方向

生产级的 Agent 评测需要：

1. **更多用例**：覆盖各种边界条件
2. **语义评测**：不是关键词匹配，而是用另一个 LLM 评判"回复是否合理"
3. **回归测试**：每次调 Prompt 或换模型后自动跑全量
4. **A/B 对比**：同一个用例跑两个版本的 Agent，对比结果

# Phase 3: Agent + Tool Calling

## 目标

Agent 收到借款申请后，自动调用征信查询工具，给出审核结论。

## 核心机制

```
@Tool 注解                 @AiService 接口
     │                          │
     ▼                          ▼
CreditCheckTool          CreditReviewAgent
     │                          │
     └────── AiServices ────────┘
           .tools(creditCheckTool)
           .build()
              │
              ▼
        LangChain4j 生成代理类
        拦截 review() 调用
        构建 SystemMessage + UserMessage
        注册工具列表（从 @Tool 扫描）
        发给 LLM
        LLM 返回 tool_calls →
        反射调用 checkCredit()
        结果再发给 LLM →
        生成最终回复
```

## 代码

### Tool 定义

```java
@Component
public class CreditCheckTool {
    @Tool("查询用户的征信评分。输入身份证号，返回征信分数和信用等级")
    public String checkCredit(String idCard) {
        int score = 300 + lastDigit * 55; // Mock：身份证尾号决定分数
        return String.format("征信分数: %d, 信用等级: %s", score, level);
    }
}
```

**关键设计**：Mock 分数由身份证尾号决定（0→300, 9→795），方便验证 Agent 是否真的调用了工具。

### Agent 接口

```java
public interface CreditReviewAgent {
    @SystemMessage("""
        你是信贷审核专家。用户提交借款申请，你需要：
        1. 调用 checkCredit 查询征信
        2. 结合分数给出审核结论（通过/拒绝/人工复核）
        """)
    String review(@UserMessage String application);
}
```

### 配置注册

```java
@Bean
public CreditReviewAgent creditReviewAgent(
        ChatLanguageModel model, CreditCheckTool tool) {
    return AiServices.builder(CreditReviewAgent.class)
        .chatLanguageModel(model)
        .tools(tool)          // 把 Tool 注入 Agent
        .build();
}
```

## 验证结果

```
输入：张三，身份证 320102199001011234（尾号4→分数520），借款5万
输出：审核结论：拒绝。征信分数 520，等级较差，建议改善信用后再申请。
```

Agent 自主完成了：理解申请 → 调用征信工具 → 综合征信结果 → 给出审核结论。

## 设计要点

- **@Tool 描述是 Prompt 的一部分**：LLM 通过描述判断何时调用，描述要精确
- **Mock 分值是验证关键**：尾号决定分数 → 可预测结果 → 方便测试
- **SystemMessage 约束行为**："通过/拒绝/人工复核"三个结论，避免 LLM 自由发挥

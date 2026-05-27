# AI Prompt 和 Skill 开发入门

> 面向 Java 程序员的 AI 开发新技能——学会"和 LLM 对话"以及"封装可复用的 AI 能力"。

## 为什么要学 Prompt 和 Skill

作为 Java 程序员，你可能已经习惯了"需求 → 代码 → 测试"的开发模式。但在 AI 时代，多了一种新模式：

```
传统开发：你写代码告诉计算机"怎么做"
Prompt 开发：你用自然语言告诉 LLM"做什么"
Skill 开发：你把 Prompt + Tool 打包成一个可复用的 AI 能力模块
```

这不是替代关系，而是**互补**——你的 Java 代码负责"确定性逻辑"（查数据库、调接口、做计算），Prompt 和 Skill 负责"非确定性决策"（理解意图、综合判断、动态推理）。

## 第一部分：Prompt 开发

### 1.1 什么是 Prompt

```
Prompt = 你给 LLM 的指令

就像你给新同事交代任务：
  - 交代他的角色（"你是我们团队的后端开发"）
  - 告诉他任务（"帮我排查这个接口为什么慢"）
  - 给他必要的上下文（"数据库是 MySQL，QPS 2000"）
  - 指定输出格式（"输出一个表格，列出慢查询和优化建议"）
```

在 LangChain4j 中，Prompt 的三种形式：

| 类型 | 注解 | 作用 | 示例 |
|------|------|------|------|
| 系统提示词 | `@SystemMessage` | 设定 Agent 角色和行为边界 | "你是信贷审核专家，严格遵守审核规则" |
| 用户消息 | `@UserMessage` | 用户的具体输入 | "审核：张三，借款 5 万" |
| 模板变量 | `{{变量名}}` | 动态插入数据 | "申请人：{{name}}，金额：{{amount}}" |

### 1.2 写好 Prompt 的四要素

一个有效的 Prompt 包含四个部分：

```
┌─────────────────────────────────────────┐
│ 1. 角色设定    你是谁？你的专业领域？      │  ← 建立上下文
├─────────────────────────────────────────┤
│ 2. 任务描述    你要做什么？具体要求？      │  ← 核心指令
├─────────────────────────────────────────┤
│ 3. 约束条件    不能做什么？边界在哪？      │  ← 防止跑偏
├─────────────────────────────────────────┤
│ 4. 输出格式    用什么格式返回？JSON/表格？  │  ← 方便解析
└─────────────────────────────────────────┘
```

**实战对比**：

```java
// ❌ 太模糊
@SystemMessage("审核借款申请")

// ✅ 四要素齐全
@SystemMessage("""
    你是一个信贷审核专家，拥有 10 年银行风控经验。【角色】
    收到借款申请后，先调用 checkCredit 查询征信，再给出审核结论。【任务】
    严格遵守：征信 < 600 直接拒绝，600-700 标记人工复核，> 700 可放款。【约束】
    输出格式：
    {
      "结论": "通过/拒绝/人工复核",
      "征信分数": 数字,
      "理由": "一句话理由",
      "建议额度": "金额（如通过）"
    }【格式】""")
```

### 1.3 在 Java 代码里写 Prompt

#### 静态 Prompt（写死在注解里）

```java
@AiService
public interface CreditReviewAgent {
    @SystemMessage("你是信贷审核专家...")
    String review(@MemoryId String memoryId, @UserMessage String application);
}
```

#### 动态 Prompt（运行时拼接）

```java
@SystemMessage(fromResource = "prompts/credit-review-system.txt")
public interface AgentWithFilePrompt { ... }
```

或完全在代码中构建：

```java
String systemPrompt = """
    你是%s的客服助手。
    当前时间：%s
    业务规则：%s
    """.formatted(companyName, LocalDateTime.now(), rulesJson);

ChatMessage systemMsg = SystemMessage.from(systemPrompt);
ChatMessage userMsg = UserMessage.from(userInput);

Response<AiMessage> response = model.generate(systemMsg, userMsg);
```

#### 变量模板（混合模式）

```java
@SystemMessage("""
    你是{{company}}的客服助手。
    你能处理的业务：{{services}}""")
String chat(
    @V("company") String company,
    @V("services") String services,
    @UserMessage String question
);
```

### 1.4 Few-Shot Prompting（给例子）

LLM 很擅长"模仿"。给它几个输入输出例子，它能学会你想要的模式：

```java
@SystemMessage("""
    你是一个情感分析助手。分析用户评论的情感倾向。

    示例 1：
    评论："这个 App 太棒了，界面很漂亮"
    分析：正面

    示例 2：
    评论："登录经常失败，体验很差"
    分析：负面

    示例 3：
    评论："还行吧，能用"
    分析：中性

    现在请分析以下评论：
    {{comment}}""")
```

这就是 Few-Shot——用 3-5 个例子教会 LLM 怎么干活。

### 1.5 常见 Prompt 技巧速查

| 技巧 | 说明 | 适用场景 |
|------|------|---------|
| **思维链 (Chain of Thought)** | 让 LLM 先说推理过程再给答案 | 复杂推理、数学计算 |
| **角色扮演** | "你是一个 XXX 专家" | 需要专业知识的场景 |
| **格式约束** | "输出纯 JSON，不要有其他文字" | 需要程序解析输出的场景 |
| **分步骤** | "第一步...第二步...第三步..." | 多阶段任务 |
| **负面约束** | "不要说 XXX，不要问 XXX" | 限制 Agent 行为边界 |
| **温度控制** | temperature=0 更精确，=1 更有创意 | 精确回答 vs 内容生成 |

## 第二部分：Skill 开发

### 2.1 什么是 Skill

```
Skill = Prompt（行为定义）+ Tool（执行能力）+ 知识（领域认知）

类比 Java 里的：
  Skill ≈ 一个有状态的 Service 类
  Prompt ≈ 类的职责描述
  Tool ≈ 类的方法
  知识 ≈ 类持有的数据/规则
```

**一句话：Skill 是把 AI 能力封装成"可复用的专业模块"。**

### 2.2 Skill 的组成结构

```java
// 一个完整的 Skill 包含三样东西：

@Component
public class CreditReviewSkill {  // ← Skill 类

    // 1. 知识：Skill 持有哪些规则/数据
    private static final Map<Integer, String> CREDIT_RULES = Map.of(
        750, "征信优秀，可直接放款",
        650, "征信中等，需人工复核",
        500, "征信较差，拒绝放款"
    );

    // 2. Tool：Skill 提供哪些可执行能力
    @Tool("查询用户征信评分。输入身份证号，返回分数和等级")
    public String checkCredit(String idCard) {
        int score = creditService.query(idCard);  // 你的 Java 代码
        String level = score >= 750 ? "优秀" : score >= 650 ? "良好" : "较差";
        return String.format("征信分数: %d, 等级: %s", score, level);
    }

    @Tool("查询用户银行流水。输入身份证号，返回近 6 个月月均流水")
    public String checkBankStatement(String idCard) {
        return bankService.getMonthlyAverage(idCard);
    }

    // 3. Prompt 定义在 @AiService 接口里
}

// Agent 接口关联 Skill 的 Tools
@AiService
public interface LoanAgent {
    @SystemMessage("""
        你是信贷审核专家。
        审核规则：
        - 征信 >= 750 → 通过
        - 征信 650-749 → 人工复核
        - 征信 < 650 → 拒绝
        - 银行流水 > 借款金额 2 倍 → 可以提额
        先用 checkCredit 查征信，再根据情况决定是否查流水。""")
    String review(@MemoryId String memoryId, @UserMessage String application);
}
```

### 2.3 从零设计一个 Skill

假设你要做一个"代码审查 Skill"：

```
步骤 1：明确 Skill 的职责
  → 审查 Java 代码，找出潜在问题，给出修改建议

步骤 2：分解成 Tool（可执行的能力）
  ├── analyzeCode(code)       → 静态分析（复杂度、命名规范）
  ├── checkSecurity(code)     → 安全检查（SQL 注入、XSS）
  └── suggestOptimize(code)   → 性能优化建议

步骤 3：编写 System Prompt（行为定义）
  → "你是资深 Java 代码审查员。审查代码时关注：
     1. 安全性（SQL 注入、敏感信息泄露）
     2. 性能（N+1 查询、不必要的对象创建）
     3. 可读性（命名规范、注释质量）
     每次审查按以上三个维度输出报告。"

步骤 4：注册为 Spring Bean
  → 把 Tool 注册到 AiServices 中
```

### 2.4 Skill 的 Java 实现模板

```java
// ============ Skill 定义 ============

@Component
public class CodeReviewSkill {

    private final CodeAnalysisService analysisService;

    @Tool("分析代码复杂度。输入代码片段，返回圈复杂度、行数、方法数")
    public String analyzeComplexity(String code) {
        ComplexityResult result = analysisService.analyze(code);
        return String.format("""
            圈复杂度: %d
            代码行数: %d
            方法数量: %d
            风险等级: %s
            """, result.complexity(), result.lines(),
                result.methods(), result.riskLevel());
    }

    @Tool("检查代码安全问题。输入代码片段，返回安全漏洞列表")
    public String checkSecurity(String code) {
        List<SecurityIssue> issues = securityScanner.scan(code);
        if (issues.isEmpty()) return "未发现安全问题";
        return issues.stream()
            .map(i -> String.format("[%s] %s (第%d行)", i.severity(), i.description(), i.line()))
            .collect(Collectors.joining("\n"));
    }
}

// ============ Agent 接口 ============

@AiService
public interface CodeReviewAgent {

    @SystemMessage("""
        你是资深 Java 代码审查员。
        审查步骤：
        1. 先调用 analyzeComplexity 检查代码复杂度
        2. 再调用 checkSecurity 检查安全问题
        3. 综合两个工具的结果，输出审查报告

        报告格式：
        ## 代码审查报告
        ### 复杂度分析
        ...
        ### 安全检查
        ...
        ### 改进建议
        ...""")
    String review(@MemoryId String memoryId, @UserMessage String code);
}

// ============ Spring 注册 ============

@Configuration
public class AiConfig {
    @Bean
    public CodeReviewAgent codeReviewAgent(
            ChatLanguageModel model,
            CodeReviewSkill skill) {
        return AiServices.builder(CodeReviewAgent.class)
            .chatLanguageModel(model)
            .tools(skill)  // 整个 Skill 的 @Tool 方法自动注册
            .build();
    }
}
```

### 2.5 设计 Skill 的原则

| 原则 | 说明 | 反例 |
|------|------|------|
| **单一职责** | 一个 Skill 只做一件事 | 把代码审查和部署放到一个 Skill |
| **Tool 粒度适中** | 一个 Tool 一个明确功能 | 一个 Tool 做"全部分析" |
| **描述即文档** | @Tool 的 value 就是给 LLM 的 API 文档 | `@Tool("doSomething")` |
| **返回结构化** | Tool 返回 LLM 能理解的结构 | 返回 JSON 裸字符串不说明字段含义 |
| **错误友好** | Tool 异常时返回有意义的错误信息 | 直接抛异常让 LLM 懵掉 |

### 2.6 Skill 的粒度层级

```
L1: 原子 Tool ──── 一个 @Tool 方法（如 checkCredit）
                       │
L2: 技能 Skill ─── 一组相关 Tool + 领域知识（如 CreditReviewSkill）
                       │
L3: 角色 Agent ─── 多个 Skill + 完整 Prompt（如 信贷审核 Agent）
                       │
L4: 多 Agent 协作 ─ 多个 Agent 编排（如 风控Agent → 审核Agent → 放款Agent）
```

## 第三部分：从 Java 程序员视角理解

### 类比：面向对象 → 面向 Skill

| 面向对象 | 面向 AI | 关键差异 |
|----------|---------|---------|
| `class` | `Skill` | Skill 不仅有方法还有"行为描述" |
| `method` | `@Tool` | Tool 需要自然语言描述，LLM 决定调用 |
| `interface` | `@AiService` | 接口不写实现，LLM 运行时"实现" |
| `@Autowired` | `AiServices.builder()` | 代理对象不是你的代码，是 LLM |
| `单元测试` | `评测用例` | 测的不是代码逻辑，是 AI 决策质量 |
| `日志` | `Memory` | 不是控制台输出，是上下文注入 |

### 你已有的能力怎么复用

```java
// 你写了 6 年的这段代码：
@Service
public class CreditService {
    public CreditReport query(String idCard) { ... }
    public AntiFraudResult checkFraud(String idCard) { ... }
    public IncomeReport analyzeIncome(String idCard) { ... }
}

// 只需要包一层：
@Component
public class CreditSkill {
    private final CreditService creditService;  // ← 复用！

    @Tool("查询征信报告")
    public String checkCredit(String idCard) {
        return creditService.query(idCard).toPromptString();
    }

    @Tool("反欺诈检查")
    public String checkFraud(String idCard) {
        return creditService.checkFraud(idCard).toSummary();
    }
}
```

**你的所有 Java 后端经验，都是 Skill 的"肌肉"，AI 只是加了一个"大脑"。**

## 常见陷阱

### 1. 把 Prompt 当成"万能咒语"

```java
// ❌ 不断加长 Prompt，期望 LLM 理解一切
@SystemMessage("你是一个专家...你要做A...也要做B...还要做C..." + 5000 字)

// ✅ 复杂任务拆成多个 Skill，每个 Skill 的 Prompt 精简短小
```

### 2. Tool 描述太简略

```java
// ❌ LLM 不知道什么时候该调
@Tool("查征信")

// ✅ LLM 清楚知道输入输出
@Tool("查询用户征信评分。输入身份证号（18位），返回格式：征信分数: 数字, 等级: 优秀/良好/较差")
```

### 3. 忘了 LLM 的输出不可控

```java
String result = agent.review(memoryId, application);
// result 可能是 JSON、纯文本、Markdown...取决于 LLM

// ✅ 在 Prompt 中约束，在代码中验证
@SystemMessage("...输出必须是纯 JSON，不要有其他文字")
// 然后
try {
    JSONObject json = new JSONObject(result);  // 验证格式
} catch (JSONException e) {
    // 格式不符合预期，可能需要重试
}
```

### 4. 把所有逻辑都交给 LLM

```java
// ❌ LLM 不擅长精确计算
@SystemMessage("计算这个借款的等额本息月还款额")

// ✅ 精确计算留在 Java 代码里
@Tool("计算等额本息月还款额")
public String calculateMonthlyPayment(double principal, double rate, int months) {
    double monthly = Math.round(principal * rate * Math.pow(1 + rate, months)
                    / (Math.pow(1 + rate, months) - 1));
    return String.format("月还款额: %.2f 元", monthly);
}
```

## 学习路径建议

```
第 1 步：理解 Prompt 四要素 → 写一个简单的 @SystemMessage
第 2 步：学会 Few-Shot → 给 LLM 做几个例子看效果
第 3 步：掌握 @Tool 开发 → 把一个现有 Service 包成 Tool
第 4 步：设计一个完整 Skill → 2-3 个 Tool + 专业 Prompt
第 5 步：搭建评测用例 → 验证 Skill 在各种输入下的表现
```

## 延伸阅读

- [什么是 AI Agent？](./agent是什么.md) — 理解 Agent 四要素
- [Function Calling 机制](./function-calling.md) — Tool 调用的底层原理
- [LangChain4j 入门](./langchain4j入门.md) — 怎么在 Java 里实现
- [OpenAI Prompt Engineering Guide](https://platform.openai.com/docs/guides/prompt-engineering) — Prompt 进阶

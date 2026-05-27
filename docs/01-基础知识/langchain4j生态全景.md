# LangChain4j 生态全景

> 不只是"Java 版 LangChain"——这是一个让 Java 程序员进入 AI 世界的完整生态。

## 一句话定位

**LangChain4j 是 Java 生态中构建 LLM 应用的主力框架**，它在 Java 世界里复刻了 LangChain 的核心思想（链式调用、Agent、RAG），但用了更符合 Java 习惯的方式——类型安全、声明式注解、Spring 原生集成。

## 生态全景图

```
┌─────────────────────────────────────────────────────────────┐
│                    LangChain4j 生态                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  核心模块     │  │  模型集成     │  │  向量存储     │      │
│  │              │  │              │  │              │      │
│  │ langchain4j  │  │ open-ai      │  │ elasticsearch│      │
│  │  - 核心抽象   │  │ anthropic    │  │ pinecone     │      │
│  │  - AiServices│  │ ollama       │  │ milvus       │      │
│  │  - Tool呼叫  │  │ vertex-ai    │  │ chroma       │      │
│  │  - Memory   │  │ azure-openai │  │ weaviate     │      │
│  │  - Chain    │  │ hugging-face │  │ redis        │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  文档加载     │  │  嵌入模型     │  │  社区与工具   │      │
│  │              │  │              │  │              │      │
│  │ pdf          │  │ open-ai      │  │ Spring Boot  │      │
│  │ tika         │  │ all-minilm   │  │  Starter     │      │
│  │ apache-poi   │  │ bge-small    │  │ Quarkus      │      │
│  │              │  │ voyage       │  │  Extension   │      │
│  └──────────────┘  └──────────────┘  │ Micronaut    │      │
│                                      │  Extension   │      │
│                                      └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 模块分类

| 分类 | 模块 | 作用 | 本项目用到的 |
|------|------|------|-------------|
| **核心** | `langchain4j` | 核心抽象：ChatLanguageModel、AiServices、Tool、Memory | ✅ |
| **模型集成** | `langchain4j-open-ai` | 对接 OpenAI / DeepSeek / 千问等 OpenAI 兼容端点 | ✅ |
| | `langchain4j-ollama` | 对接本地 Ollama 模型 | - |
| | `langchain4j-anthropic` | 对接 Claude | - |
| **向量存储** | `langchain4j-elasticsearch` | ES 做向量检索 | - |
| | `langchain4j-redis` | Redis 向量检索（低延迟场景） | - |
| **文档加载** | `langchain4j-document-loader-*` | PDF/Word/网页 文档解析 | - |
| **嵌入模型** | `langchain4j-embeddings-*` | 文本转向量 | - |
| **框架集成** | `langchain4j-spring-boot-starter` | Spring Boot 自动配置 | - |
| | `langchain4j-quarkus` | Quarkus 集成 | - |

## 架构设计

### 核心分层

```
┌──────────────────────────────────────────────────────┐
│                    用户代码层                          │
│  你的 @AiService 接口 / @Tool 方法 / Controller       │
├──────────────────────────────────────────────────────┤
│                  AiServices 代理层                    │
│  ┌──────────────────────────────────────────────┐   │
│  │ JDK 动态代理 → 拦截方法调用 → 组装 Prompt      │   │
│  │ → 发送 LLM → 解析 tool_calls → 反射调用 Tool   │   │
│  │ → 结果回传 LLM → 返回最终结果                  │   │
│  └──────────────────────────────────────────────┘   │
├──────────────────────────────────────────────────────┤
│                   模型适配层                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │ OpenAI   │ │ Anthropic│ │ Ollama   │ ...        │
│  │ Adapter  │ │ Adapter  │ │ Adapter  │            │
│  └──────────┘ └──────────┘ └──────────┘            │
├──────────────────────────────────────────────────────┤
│                   基础设施层                          │
│  ChatMemoryStore │ EmbeddingStore │ DocumentLoader   │
└──────────────────────────────────────────────────────┘
```

### 三条核心数据流

**流 1：普通对话**
```
用户输入 → @UserMessage 模板渲染 → 组装 SystemMessage + UserMessage
→ 发送 LLM → 返回文本 → 直接返回给用户
```

**流 2：Agent + Tool Calling（关键！）**
```
用户输入 → 组装 Prompt + Tools 列表 → 发送 LLM
→ LLM 返回 function_call { name: "checkCredit", args: { idCard: "xxx" } }
→ LangChain4j 反射调用 CreditCheckTool.checkCredit("xxx")
→ 拿到结果 "征信分数: 795"
→ 再次发送 LLM（带 Tool 返回结果）
→ LLM 综合判断 → "审核通过，建议额度 5 万"
```

**流 3：RAG（检索增强生成）**
```
用户问题 → 转向量 → 向量数据库检索 Top-K 相关文档
→ 文档片段 + 用户问题 → 组装 Prompt → 发送 LLM → 返回基于文档的回答
```

## 设计理念

### 1. Java 原生，不魔改

LangChain4j 没有试图在 Java 里复刻 Python 的动态特性。它用了 Java 最擅长的方式：

```java
// Python LangChain：动态，灵活，但类型不安全
chain = prompt | llm | output_parser
result = chain.invoke({"input": "hello"})

// Java LangChain4j：静态类型，编译期检查
@AiService
public interface MyAgent {
    @SystemMessage("你是客服助手")
    String chat(@UserMessage String message);  // ← 类型安全
}
```

### 2. 声明式 > 命令式

能用注解解决的问题，不写代码：

| 想做的事 | 加什么注解 | 背后原理 |
|----------|-----------|---------|
| 定义 Agent 行为 | `@SystemMessage` | 注入到每次 LLM 请求 |
| 给 LLM 工具 | `@Tool("描述")` | 扫描注解 → 生成 ToolSpecification |
| 多轮记忆 | `@MemoryId` | 自动从 ChatMemoryStore 读写 |
| 指定用户输入 | `@UserMessage` | 参数绑定 + 模板渲染 |

### 3. 渐进式复杂度

你不需要一开始就理解所有概念：

```
入门：只 import langchain4j + langchain4j-open-ai，2 行代码发请求
进阶：加 @Tool，让 LLM 调你的方法
深入：加 Memory、RAG、Chain，构建复杂 Agent
```

每个阶段只加必需的依赖，不引入额外复杂度。

### 4. Spring 友好

和 Spring Boot 的集成就像用 `@Service`、`@Repository` 一样自然：

```java
@Configuration
public class AiConfig {
    @Bean
    public ChatLanguageModel model() { ... }          // 像 DataSource

    @Bean
    public MyAgent agent(ChatLanguageModel model) {   // 像注入 Service
        return AiServices.builder(MyAgent.class)
            .chatLanguageModel(model)
            .tools(myTool)
            .build();
    }
}
```

## 对 Java 程序员的影响

### 你在团队中的角色会变

```
传统 Java 后端：
  需求 → 写 if-else → 调 RPC → 写 SQL → 返回 JSON

AI 时代的 Java 后端：
  需求 → 设计 Agent 行为 → 写 @Tool → 调 Prompt → Agent 自主决策
```

你不是在写"执行逻辑"，而是在写"决策框架"。

### 新的技能树

```
                      Java AI 工程师
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    传统后端功底       AI 框架能力      领域理解
          │                │                │
    Spring Boot      LangChain4j       Prompt 工程
    MySQL/Redis      Function Calling   评测框架
    分布式/微服务     RAG 架构         Tool 设计
    并发/性能        Agent 编排        Token 成本
```

好消息是：**Java 后端功底仍然是底座**。Tool 里还是写你熟悉的 Java 代码——查数据库、调 RPC、做业务逻辑。AI 框架只是一层新的"胶水"。

### 从"翻译需求"到"设计智能体"

```
传统方式：PM 说"审核借款要查征信、算额度" → 你写 if-else
Agent 方式：PM 说"审核借款" → 你设计一个 Agent，给它 Tool，
            让它自己决定查什么、怎么审
```

对 Java 程序员来说，这不是颠覆而是**能力升维**——你的价值从"写代码"变成"设计智能体行为"。

## 能做什么

### 1. 智能对话（最低门槛）

```java
ChatLanguageModel model = OpenAiChatModel.builder()
    .baseUrl("https://api.deepseek.com/v1")
    .apiKey("sk-xxx")
    .modelName("deepseek-chat")
    .build();

String reply = model.generate("解释一下分布式事务");
```

一行代码，LLM 已经能回答技术问题了。

### 2. AI Agent（Tool Calling）

这是 LangChain4j 的核心价值——让 LLM 学会"做事"：

```java
@Tool("查询用户征信评分。输入身份证号，返回分数和等级")
public String checkCredit(String idCard) {
    // ← 这里是你写了 6 年的 Java 代码
    return creditService.query(idCard);
}
```

Agent 场景举例：

| 场景 | Tools | 效果 |
|------|-------|------|
| 信贷审核 | 征信查询 + 反欺诈 + 流水分析 | Agent 自动判断通过/拒绝 |
| 客服助手 | 订单查询 + 退款 + FAQ检索 | 一句话处理完整售后流程 |
| DevOps Agent | kubectl + 日志查询 + 告警 | "帮我看下 payment 服务为什么慢了" |
| 数据分析 | SQL执行 + 图表生成 | "上个月放款趋势怎么样" |

### 3. RAG（检索增强生成）

让 LLM 能回答它没学过的东西（你的内部文档、业务知识库）：

```
用户："我们的放款流程有哪些风控节点？"
     │
     ▼
1. 问题转向量 → [0.23, 0.87, ...]
2. 向量数据库检索 → 找到"放款流程文档"片段
3. 组装 Prompt："请根据以下文档回答：{文档片段}\n\n问题：{用户问题}"
4. LLM 基于文档生成精准回答
```

### 4. 评测体系（Agent Evaluation）

Agent 不是"写完就完了"——你怎么知道它真的做对了？

```java
@Test
public void 征信优秀应通过() {
    String result = agent.review("eval-1", "张三, 尾号9, 10000元, 购车");
    assertThat(result).contains("通过");          // 决策方向正确
    assertThat(result).contains("795");           // 工具真的被调用了
}
```

### 5. 多 Agent 协作（进阶）

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ 风控 Agent   │ ──→ │ 审核 Agent   │ ──→ │ 放款 Agent   │
│ (查征信/反欺诈)│    │ (综合评分)    │     │ (执行放款)    │
└─────────────┘     └─────────────┘     └─────────────┘
```

## 与竞品对比

| 维度 | LangChain4j | Spring AI | Python LangChain |
|------|------------|-----------|-----------------|
| 语言 | Java | Java | Python |
| 成熟度 | ⭐⭐⭐⭐ 活跃 | ⭐⭐⭐ 追赶中 | ⭐⭐⭐⭐⭐ 最成熟 |
| Spring 集成 | 手动 + Starter | 原生集成 | N/A |
| 文档质量 | 好 | 一般 | 最好 |
| 概念覆盖 | Planning/Memory/Tool/RAG | 基本覆盖 | 全覆盖 |
| 上手难度 | 中等 | 低（自动配置多） | 低 |
| 适合谁 | 要理解底层+可控性 | 快速原型 | AI 研究/快速实验 |

**选择建议**：
- 想快速原型 → Spring AI 自动配置
- 想深入理解 + 生产级可控 → LangChain4j 手动配置
- 做 AI 研究/实验 → Python LangChain

## 版本与未来

| 时间 | 版本 | 里程碑 |
|------|------|--------|
| 2023 Q3 | 0.1.x | 核心抽象 + OpenAI 集成 |
| 2024 Q1 | 0.26.x | AiServices 成熟 + 多模型支持 |
| 2024 Q3 | 0.35.x | RAG 完善 + Spring Boot Starter |
| 2025+ | 1.0 | 计划发布稳定版 |

趋势：Java AI 开发正在从"实验性"走向"生产就绪"。现在入门 LangChain4j，就是抢占了 Java AI 开发的先发优势。

## 延伸阅读

- 官方文档：https://docs.langchain4j.dev
- GitHub：https://github.com/langchain4j/langchain4j
- 本项目入门教程：[LangChain4j 入门](./langchain4j入门.md)
- 本项目 Agent 实战：[Phase 3 - Agent + Tool Calling](../02-实践过程/phase3-agent-tool.md)

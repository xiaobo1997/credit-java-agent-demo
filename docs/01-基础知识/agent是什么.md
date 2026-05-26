# 什么是 AI Agent？

## 一句话定义

**Agent = LLM + 工具 + 记忆 + 决策能力**

一个能理解自然语言、自主调用工具完成任务、记住上下文的智能体。

## 和传统后端的区别

| | 传统后端 | AI Agent |
|---|---|---|
| 输入 | 结构化数据（JSON/表单） | 自然语言 |
| 逻辑 | 人写的 if-else / 规则引擎 | LLM 推理 + 决策 |
| 扩展 | 加接口、改代码 | 加 Tool、调 Prompt |
| 容错 | 抛异常、返回错误码 | 自主重试、换方案 |

一句话：**传统后端是人写规则让代码执行，Agent 是人给目标让 LLM 自己想办法执行**。

## Agent 的核心四要素

```
┌──────────────────────────────────┐
│            AI Agent              │
│                                  │
│  ┌──────────┐   ┌──────────┐    │
│  │ Planning │   │  Memory  │    │
│  │ 任务拆解  │   │ 上下文记  │    │
│  └──────────┘   └──────────┘    │
│                                  │
│  ┌──────────┐   ┌──────────┐    │
│  │   LLM    │   │  Tools   │    │
│  │ 大语言模  │   │ 工具调用  │    │
│  └──────────┘   └──────────┘    │
└──────────────────────────────────┘
```

### 1. LLM（大脑）

负责理解用户意图、推理、生成回复。本项目用 DeepSeek。

### 2. Tools（手脚）

LLM 不能直接操作数据库、调API——需要 Tool 作为"手"。
你在 Java 方法上加 `@Tool` 注解并描述功能，LLM 自己决定什么时候调用哪个。

### 3. Memory（记忆）

没有 Memory 的 Agent 是金鱼——每次对话都从零开始。
Memory 存储历史消息，每次新请求时注入到 Prompt 中。

### 4. Planning（规划）

复杂任务需要拆解成多步。比如审核借款：
"先查征信 → 征信差直接拒绝 → 征信好再查流水 → 综合评分"

## ReAct 模式

最经典的 Agent 推理模式：

```
Thought: 我需要查这个人的征信
Action: 调用 CreditCheckTool(idCard="320102...")
Observation: 征信分数 795，等级优秀
Thought: 征信优秀，可以放款
Action: 返回审核结论：通过
```

## 本项目中的 Agent

```java
// 这就是一个 Agent——一个接口，三个要素
public interface CreditReviewAgent {
    String review(
        @MemoryId String memoryId,    // ← Memory
        @UserMessage String application  // ← 用户输入
    );
}
// LLM: DeepSeek
// Tools: CreditCheckTool（@Tool 注解）
// Memory: InMemoryChatMemoryStore
```

# Memory 对话记忆

## 为什么需要 Memory

LLM 本身是无状态的——每次请求都是全新的。没有 Memory 的 Agent：

```
用户: "查张三的征信"
Agent: "征信分数 795，通过"
用户: "那他的额度建议多少？"
Agent: "谁的额度？请提供申请人信息"  ← 忘了上一句
```

## 原理

每次请求时，把历史对话注入到 Prompt 中：

```
第 1 轮请求：
System: 你是信贷审核专家...
User: 查张三的征信，身份证 XXX

第 2 轮请求（有 Memory）：
System: 你是信贷审核专家...
User: 查张三的征信，身份证 XXX         ← 第 1 轮
Assistant: 征信分数 795，通过           ← 第 1 轮回复
User: 那他的额度建议多少？              ← 当前问题
```

LLM 看到了历史对话，就能理解"他"指的是张三。

## LangChain4j 中的 Memory

### 核心组件

```
ChatMemoryProvider（记忆提供者）
    │
    ▼
MessageWindowChatMemory（消息窗口记忆）
    │
    ├── id: memoryId（唯一标识一个会话）
    ├── maxMessages: 10（最多保留多少条）
    │
    ▼
ChatMemoryStore（存储后端）
    ├── InMemoryChatMemoryStore（内存，本项目用）
    └── 可替换为 Redis / DB
```

### 代码实现

```java
// 1. 创建存储
InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();

// 2. 绑定到 Agent
AiServices.builder(CreditReviewAgent.class)
    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
        .id(memoryId)
        .maxMessages(10)
        .chatMemoryStore(memoryStore)
        .build())
    .build();

// 3. Agent 接口用 @MemoryId 区分会话
public interface CreditReviewAgent {
    String review(@MemoryId String memoryId, @UserMessage String msg);
}
```

### memoryId 的作用

- 不同 memoryId → 不同会话 → 记忆隔离
- 同一 memoryId → 同一会话 → 共享上下文
- 前端生成 UUID 传给后端，或后端自动生成返回给前端

## 消息窗口策略

| 策略 | 说明 | 适用 |
|------|------|------|
| MessageWindow | 保留最近 N 条 | 简单场景，本项目 |
| TokenWindow | 按 Token 数截断 | 控制成本 |
| 永久保留 | 全量存储 | 配合向量检索 |

## 生产环境怎么办

内存实现在重启后丢失。生产环境替换为：

- **Redis**：高性能，适合分布式
- **数据库**：持久化，可审计
- **LangChain4j 自带**：`dev.langchain4j:langchain4j-redis`

# Phase 4: 多轮记忆

## 目标

同一 memoryId 的两轮请求，第二轮能引用第一轮的上下文。

## 实现

三个改动：

### 1. Agent 接口加 @MemoryId

```java
public interface CreditReviewAgent {
    String review(@MemoryId String memoryId, @UserMessage String application);
}
```

### 2. 注册 ChatMemoryProvider

```java
InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();

AiServices.builder(CreditReviewAgent.class)
    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
        .id(memoryId)
        .maxMessages(10)          // 只保留最近10条
        .chatMemoryStore(memoryStore)
        .build())
    .build();
```

### 3. Controller 支持 memoryId 传入

```java
String memoryId = request.getOrDefault("memoryId", UUID.randomUUID().toString());
// 首轮自动生成，追问时前端带回同一个 memoryId
```

## 验证结果

```
第 1 轮：提交李四的借款申请（征信795分，通过）
第 2 轮：追问 "刚才那个客户的征信分数是多少？"
Agent 回答："客户李四的征信分数为 795 分..."
```

Agent 准确回忆了第一轮的客户姓名、征信分数、金额——Memory 生效。

## 关键决策

- **窗口大小 10 条**：足够覆盖"申请→审核→追问→确认"的典型流程，也防止 Token 爆掉
- **内存存储**：学习阶段够用，生产换成 Redis
- **memoryId 自动生成**：减少前端对接成本

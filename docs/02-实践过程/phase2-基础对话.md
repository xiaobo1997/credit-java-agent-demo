# Phase 2: 基础对话

## 目标

REST API 调用 DeepSeek，验证 LLM 连接正常。

## 实现

一行核心代码：

```java
String reply = chatModel.generate(request.getMessage());
```

POST /api/chat → ChatLanguageModel.generate() → 返回 LLM 回复。

## 踩坑：Anthropic vs OpenAI 端点

这是整个项目最关键的踩坑。

### 第一版：Anthropic 端点（失败）

```java
AnthropicChatModel.builder()
    .baseUrl("https://api.deepseek.com/anthropic")
    .modelName("deepseek-v4-pro")
    .build();
```

请求发过去了，但响应反序列化时报错：
```
EnumDeserializer.deserialize ... stop_reason 字段解析失败
```

**原因**：DeepSeek 的 Anthropic 兼容端点返回的 `stop_reason` 值和 langchain4j-anthropic 期望的枚举不匹配。

### 第二版：OpenAI 端点（成功）

```java
OpenAiChatModel.builder()
    .baseUrl("https://api.deepseek.com/v1")
    .modelName("deepseek-chat")
    .build();
```

| 端点 | 协议 | 模块 | 结果 |
|------|------|------|------|
| api.deepseek.com/anthropic | Anthropic | langchain4j-anthropic | ❌ 序列化不兼容 |
| api.deepseek.com/v1 | OpenAI | langchain4j-open-ai | ✅ 完美兼容 |

同一个 API Key 在两个端点上都可以用，只是协议格式不同。

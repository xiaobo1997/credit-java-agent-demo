# LangChain4j 入门

## 是什么

LangChain4j 是 LangChain 的 Java 移植版——Java 生态中做 AI Agent 的主力框架。

Java 生态还有 Spring AI，但 LangChain4j 社区更活跃，文档更全，三个 JD 里提到的概念（Planning/Memory/Function Calling/Tool Use）它全都有。

## 核心模块

| 模块 | 作用 | 本项目用到的 |
|------|------|-------------|
| langchain4j-core | 核心抽象（ChatLanguageModel 接口等） | ✅ |
| langchain4j-open-ai | OpenAI/DeepSeek 连接 | ✅ |
| langchain4j-anthropic | Anthropic/Claude 连接 | ❌（尝试过但不兼容） |
| langchain4j-spring-boot-starter | Spring Boot 自动配置 | ❌（手动配置更可控） |

## 最小示例

```xml
<!-- pom.xml -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.36.2</version>
</dependency>
```

```java
// 创建模型
ChatLanguageModel model = OpenAiChatModel.builder()
    .baseUrl("https://api.deepseek.com/v1")
    .apiKey("sk-xxx")
    .modelName("deepseek-chat")
    .build();

// 对话
String reply = model.generate("你好");
```

## AiServices：LangChain4j 的核心魔法

```java
// 定义接口
public interface CreditReviewAgent {
    String review(@MemoryId String memoryId, @UserMessage String msg);
}

// LangChain4j 自动生成实现
AiServices.builder(CreditReviewAgent.class)
    .chatLanguageModel(model)
    .tools(creditCheckTool)          // 注册工具
    .chatMemoryProvider(memoryProvider) // 注册记忆
    .build();
```

你定义接口 → LangChain4j 生成代理类 → 拦截方法调用 → 发给 LLM → 解析 tool_calls → 反射调用 → 返回结果。

## 和 Spring Boot 集成

两种方式：

### 方式 1：自动配置（langchain4j-spring-boot-starter）
```yaml
langchain4j:
  open-ai:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: https://api.deepseek.com/v1
```
优点：零代码。缺点：黑盒，出问题难排查。

### 方式 2：手动配置（本项目采用）
```java
@Configuration
public class DeepSeekConfig {
    @Bean
    public ChatLanguageModel chatLanguageModel() { ... }
}
```
优点：完全可控，完全可控。缺点：多写几行代码。

## 关键注解

| 注解 | 作用 | 位置 |
|------|------|------|
| `@AiService` | 标记 Agent 接口 | 接口类 |
| `@SystemMessage` | 系统提示词 | 方法上 |
| `@UserMessage` | 用户消息模板 | 参数上 |
| `@MemoryId` | 标记会话 ID | 参数上 |
| `@Tool` | 声明可调用工具 | 方法上 |

## 本项目踩过的坑

1. **Anthropic 端点不兼容**：DeepSeek 的 Anthropic 兼容端点返回格式和 langchain4j-anthropic 不完全一致（enum 反序列化失败），换成 OpenAI 端点解决
2. **@Tool 描述要精确**：LLM 通过描述判断何时调用，描述模糊会导致调错或不调
3. **手动配置 > 自动配置**：手动配置比自动配置更能理解底层机制

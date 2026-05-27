# Credit AI Agent Demo

基于 **Spring Boot 3 + LangChain4j + DeepSeek** 的信贷 AI Agent 学习工程。

两个子模块：
- **loan-ai-agent-chat** — 信贷审核 Agent（Agent + Tool Calling + Memory + 评测）
- **loan-ai-agent-rag** — 贷款知识智能小助手（RAG 检索增强生成）

## 一句话理解

```
传统后端：人写规则 → 代码执行
AI Agent：人给目标 → LLM 自己想办法执行
```

Agent 就是一个"会思考的微服务"——你用自然语言告诉它做什么，它自己决定调哪些工具来完成。

## 核心概念

| 概念 | 对应技术 | 本项目实现 |
|------|---------|-----------|
| **LLM** | 大语言模型 | DeepSeek（OpenAI 兼容端点） |
| **Tool Calling** | 工具调用 | CreditCheckTool 征信查询 |
| **Memory** | 对话记忆 | InMemoryChatMemoryStore |
| **Agent** | 自主决策体 | CreditReviewAgent |
| **Evaluation** | 评测体系 | AgentEvaluator 5 个用例 |
| **RAG** | 检索增强生成 | 贷款知识小助手（TF-IDF + LLM） |

## 技术栈

| 层 | 技术 |
|----|------|
| 框架 | Spring Boot 3.3.5 + Java 17 |
| AI 框架 | LangChain4j 0.36.2 |
| LLM | DeepSeek（OpenAI 兼容端点） |
| 记忆 | langchain4j ChatMemory（内存实现） |
| 检索 | TF-IDF + 余弦相似度（纯 Java） |
| 构建 | Maven 多模块 |

## 快速开始

### 前置条件

- JDK 17+
- DeepSeek API Key（配置在 `~/tools/moon-bridge/config.yml`）

### 启动 Chat 模块（信贷审核 Agent）

```bash
export JAVA_HOME=/path/to/jdk17
cd loan-ai-agent
mvn spring-boot:run -pl loan-ai-agent-chat
```

应用启动在 `http://localhost:8080`

### 启动 RAG 模块（贷款知识小助手）

```bash
mvn spring-boot:run -pl loan-ai-agent-rag
```

应用启动在 `http://localhost:8081`

### API 测试

**基础对话**（Chat 模块）
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"用一句话介绍你自己"}'
```

**信贷审核（Agent + Tool Calling）**（Chat 模块）
```bash
curl -X POST http://localhost:8080/api/agent/review \
  -H "Content-Type: application/json" \
  -d '{"name":"张三","idCard":"320102199001011234","amount":"50000","purpose":"购车"}'
```

**知识问答（RAG）**（RAG 模块）
```bash
curl -X POST http://localhost:8081/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"等额本息和等额本金有什么区别？"}'
```

## 项目结构

```
loan-ai-agent/
├── pom.xml                                  # 父 POM（版本管理）
├── loan-ai-agent-chat/                      # Chat 模块（信贷 Agent）
│   └── src/main/java/com/loan/agent/
│       ├── config/DeepSeekConfig.java
│       ├── agent/CreditReviewAgent.java
│       ├── tool/CreditCheckTool.java
│       ├── controller/{Chat,Agent,Evaluate}Controller.java
│       └── evaluate/{TestCase,AgentEvaluator}.java
├── loan-ai-agent-rag/                       # RAG 模块（知识小助手）
│   └── src/main/java/com/loan/agent/rag/
│       ├── config/RagConfiguration.java
│       ├── knowledge/{KnowledgeBase,KnowledgeInitializer}.java
│       ├── retrieval/DocumentRetriever.java   # TF-IDF 检索器
│       ├── pipeline/{RagPipeline,QueryParser}.java
│       ├── controller/RagController.java
│       └── model/{KnowledgeDocument,RagRequest,RagResponse}.java
└── docs/                                    # 学习文档
```

## 学习路径

```
Phase 1: 骨架搭建 ──── Spring Boot + LangChain4j + DeepSeek 跑通
Phase 2: 基础对话 ──── POST /api/chat，验证 LLM 连接
Phase 3: Agent ──────── @Tool + @AiService，LLM 自主调工具
Phase 4: Memory ────── @MemoryId，多轮对话上下文记忆
Phase 5: 评测 ──────── 5 个用例自动化评测，100% 通过
Phase 6: RAG ───────── 检索增强生成，知识问答小助手
```

详细文档见 [GitHub Wiki](https://github.com/xiaobo1997/credit-java-agent-demo/wiki)

## 设计原则

- **从简**：每个 Phase 只加必要代码，不搞抽象
- **可验证**：每个 Phase 都有明确的验证步骤
- **渐进式**：6 个 Phase 从小到大，概念层层递进
- **安全**：API Key 读仓库外配置，.gitignore 防护，零泄露

---

*最后更新：2026-05-28*

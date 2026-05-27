# Credit AI Agent Demo

基于 **Spring Boot 3 + LangChain4j + DeepSeek** 的信贷审核 AI Agent 学习工程。

模拟真实信贷场景——Agent 收到借款申请后，自动调用征信查询工具，综合评估给出审核结论。

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

## 技术栈

| 层 | 技术 |
|----|------|
| 框架 | Spring Boot 3.3.5 + Java 17 |
| AI 框架 | LangChain4j 0.36.2 |
| LLM | DeepSeek（OpenAI 兼容端点） |
| 记忆 | langchain4j ChatMemory（内存实现） |
| 构建 | Maven |

## 快速开始

### 前置条件

- JDK 17+
- DeepSeek API Key（配置在 `~/tools/moon-bridge/config.yml`）

### 启动

```bash
export JAVA_HOME=/path/to/jdk17
cd loan-ai-agent
mvn spring-boot:run
```

应用启动在 `http://localhost:8080`

### API 测试

**基础对话**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"用一句话介绍你自己"}'
```

**信贷审核（Agent + Tool Calling）**
```bash
curl -X POST http://localhost:8080/api/agent/review \
  -H "Content-Type: application/json" \
  -d '{"name":"张三","idCard":"320102199001011234","amount":"50000","purpose":"购车"}'
```

**多轮对话（带记忆）**
```bash
# 第 1 轮：提交申请（记录返回的 memoryId）
curl -X POST http://localhost:8080/api/agent/review \
  -H "Content-Type: application/json" \
  -d '{"name":"李四","idCard":"320102199001011239","amount":"80000","purpose":"装修"}'

# 第 2 轮：追问（使用同一个 memoryId）
curl -X POST http://localhost:8080/api/agent/review \
  -H "Content-Type: application/json" \
  -d '{"memoryId":"<上轮返回的memoryId>","question":"那他的征信分数是多少？"}'
```

**运行评测**
```bash
curl http://localhost:8080/api/agent/evaluate
```

## 项目结构

```
src/main/java/com/loan/agent/
├── LoanAiAgentApplication.java    # 启动类
├── config/
│   └── DeepSeekConfig.java        # DeepSeek 配置 + Bean 注册
├── agent/
│   └── CreditReviewAgent.java     # @AiService 审核 Agent 接口
├── tool/
│   └── CreditCheckTool.java       # @Tool 征信查询（Mock）
├── controller/
│   ├── ChatController.java        # /api/chat 基础对话
│   ├── AgentController.java       # /api/agent/review 信贷审核
│   └── EvaluateController.java    # /api/agent/evaluate 评测
└── evaluate/
    ├── TestCase.java              # 评测用例模型
    └── AgentEvaluator.java        # 评测执行引擎
```

## 学习路径

```
Phase 1: 骨架搭建 ──── Spring Boot + LangChain4j + DeepSeek 跑通
Phase 2: 基础对话 ──── POST /api/chat，验证 LLM 连接
Phase 3: Agent ──────── @Tool + @AiService，LLM 自主调工具
Phase 4: Memory ────── @MemoryId，多轮对话上下文记忆
Phase 5: 评测 ──────── 5 个用例自动化评测，100% 通过
```

详细文档见 [GitHub Wiki](https://github.com/xiaobo1997/credit-java-agent-demo/wiki)

## 设计原则

- **从简**：每个 Phase 只加必要代码，不搞抽象
- **可验证**：每个 Phase 都有明确的验证步骤
- **渐进式**：5 个 Phase 从小到大，概念层层递进
- **安全**：API Key 读仓库外配置，.gitignore 防护，零泄露

---

*最后更新：2026-05-27*

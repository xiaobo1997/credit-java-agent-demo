# Phase 6: RAG 智能小助手

## 目标

新增 `loan-ai-agent-rag` 子模块，展示完整的 RAG（检索增强生成）流程——用户提问 → 问题解析 → 知识检索 → LLM 生成回答。

同时改造项目为 Maven 多模块结构，chat 和 rag 两个模块独立运行、互不干扰。

## RAG 是什么

```
普通 LLM 对话 = 闭卷考试：LLM 凭记忆回答，可能出错或编造
RAG 对话     = 开卷考试：先帮你翻到正确的页码，再让 LLM 基于那一页回答
```

RAG 的核心价值在于"先检索，再生成"。LLM 不需要知道所有知识，只需要能把检索到的知识转化成自然语言回答。

## 四步流程

```
POST /api/rag/ask  { "question": "等额本息是什么？" }
         │
         ▼
┌─────────────────────────────────────────────────┐
│ 第 1 步：问题解析 (QueryParser)                  │
│ "等额本息是什么意思？？" → "等额本息是什么意思"  │
│ 去重标点、统一格式                                │
└─────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│ 第 2 步：文档检索 (DocumentRetriever)            │
│ 问题分词 → TF-IDF 向量 → 余弦相似度 → Top-3      │
│ 命中: faq-001(0.85), faq-004(0.62), faq-008(0.31)│
└─────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│ 第 3 步：Prompt 组装 (RagPipeline.buildPrompt)    │
│ System: 角色 + 知识库片段 + 回答规则              │
│ User: 用户原始问题                                │
└─────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│ 第 4 步：LLM 生成                                 │
│ DeepSeek 基于检索文档生成带引用的回答              │
└─────────────────────────────────────────────────┘
         │
         ▼
响应: { question, answer, sources: [...], processingTime }
```

## 多模块架构

```
loan-ai-agent/                          (父POM，管理版本)
├── loan-ai-agent-chat/                 (Credit Agent)
│   └── 端口 8080，审核+对话+评测
└── loan-ai-agent-rag/                  (RAG 小助手)
    └── 端口 8081，知识问答
```

改造要点：
- 父 POM 使用 `<dependencyManagement>` 统一版本，子模块只声明需要的依赖
- 添加 `maven-compiler-plugin` 显式指定 Java 17
- chat 模块代码一个字节没改，只是移动了目录位置

## 检索核心：TF-IDF + 余弦相似度

### 为什么不用向量数据库？

这是教学 Demo，目标是让 Java 程序员**理解检索原理**。TF-IDF 是 NLP 最经典的文本相似度算法，纯 Java 实现，零外部依赖。

### 算法步骤

```java
// 1. 分词 + 词频统计
Map<String, Integer> tf = computeTermFrequency("等额本息是指每月还款金额固定...");
// → {"等额本息": 1, "每月": 1, "还款": 2, "金额": 1, "固定": 1, ...}

// 2. 计算 IDF
// IDF = log(总文档数 / 包含该词的文档数)
// 只在 2 篇文档中出现的词 → IDF 高 → 区分度强
// 在 8 篇文档中都出现的词 → IDF 低 → 区分度弱（如"贷款"）

// 3. 生成 TF-IDF 向量
// TF-IDF = TF × IDF
Map<String, Double> vector = { "等额本息": 2.08, "固定": 0.69, ... }

// 4. 余弦相似度
// similarity = (A·B) / (|A| × |B|)
// 值域 [0, 1]，越接近 1 越相似
```

### 生产升级路径

```
Demo:  TF-IDF + 内存 Map           ← 当前
  ↓    零依赖，适合学习
Prod: Embedding模型 + 向量数据库    ← 升级
      准确性提升 10-30%
```

换 embedding 时，`DocumentRetriever` 的接口不变，只需替换内部实现。

## 知识库数据

8 条贷款领域通用知识，纯教科书级内容：

| # | 标题 | 分类 | 关键词 |
|---|------|------|--------|
| 1 | 等额本息与等额本金的区别 | 还款方式 | 等额本息、等额本金 |
| 2 | 征信查询规则 | 征信 | 征信、硬查询、逾期 |
| 3 | 贷款审批流程 | 流程 | 审批、面签、人工复核 |
| 4 | 贷款利率计算方式 | 利率 | LPR、年化、计息 |
| 5 | 提前还款规则 | 还款方式 | 违约金、预约 |
| 6 | 逾期处理与征信影响 | 征信 | 罚息、展期 |
| 7 | 担保方式介绍 | 流程 | 抵押、信用贷款 |
| 8 | LPR 利率机制说明 | 利率 | 重定价、市场化 |

数据文件：`loan-ai-agent-rag/src/main/resources/knowledge/loan-faq.json`

## 怎么跑

```bash
# 编译全部模块
cd loan-ai-agent
export JAVA_HOME=~/myworkspace/tools/java17/amazon-corretto-17.jdk/Contents/Home
mvn clean compile

# 启动 RAG 模块（端口 8081）
mvn spring-boot:run -pl loan-ai-agent-rag

# 提问测试
curl -X POST http://localhost:8081/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"等额本息和等额本金有什么区别？"}'

# 返回示例
{
  "question": "等额本息和等额本金有什么区别",
  "answer": "等额本息是每月还款金额固定，前期利息多本金少...（引用来源：faq-001）",
  "sources": [
    {
      "title": "等额本息与等额本金的区别",
      "excerpt": "等额本息是指每月还款金额固定...",
      "similarity": 0.85,
      "category": "还款方式"
    }
  ],
  "processingTime": "1.2s"
}
```

## 项目结构

```
loan-ai-agent-rag/
├── pom.xml
└── src/main/
    ├── java/com/loan/agent/rag/
    │   ├── LoanRagApplication.java         启动类
    │   ├── config/
    │   │   └── RagConfiguration.java         DeepSeek 连接配置
    │   ├── knowledge/
    │   │   ├── KnowledgeBase.java            知识库内存存储
    │   │   └── KnowledgeInitializer.java     启动时加载+索引构建
    │   ├── retrieval/
    │   │   ├── DocumentRetriever.java        TF-IDF 检索器（核心）
    │   │   └── SearchResult.java             检索结果模型
    │   ├── pipeline/
    │   │   ├── RagPipeline.java              RAG 流程编排器
    │   │   └── QueryParser.java              问题标准化
    │   ├── controller/
    │   │   └── RagController.java            REST 接口
    │   └── model/
    │       ├── KnowledgeDocument.java        知识文档实体
    │       ├── RagRequest.java               问答请求
    │       └── RagResponse.java              问答响应
    └── resources/
        ├── application.yml
        └── knowledge/loan-faq.json           知识库数据
```

## 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 检索算法 | TF-IDF（纯 Java） | 零依赖，Java 程序员看得懂 |
| 知识存储 | 内存 Map | 数据量小，启动快 |
| 数据格式 | JSON 文件 | 可读、可编辑、可版本管理 |
| LLM 调用 | DeepSeek Chat | 和 chat 模块共用配置 |
| 启动预热 | ApplicationRunner | 启动时自动加载，不用手动初始化 |
| 模块端口 | 8081 | 和 chat 的 8080 不冲突 |

## 踩坑记录

1. **多模块改造后 Java 版本问题**：父 POM 没了 `spring-boot-starter-parent` 的直接继承，`maven-compiler-plugin` 默认用 Java 8，导致文本块语法报错。解决：父 POM 显式配置 compiler 插件 source/target=17。

2. **Jackson 依赖**：`KnowledgeInitializer` 用 `ObjectMapper` 读 JSON，需要在 pom.xml 中显式添加 `jackson-databind` 依赖（Spring Boot Web 自带但不保证版本一致性）。

3. **Lombok 内部类注解**：`RagResponse.SourceInfo` 是静态内部类，使用 `@Data`、`@Builder` 等注解需要显式 import，不会自动继承外部类的 import。

## 学完这个 Phase 你能回答

- RAG 和普通 LLM 对话有什么区别？
- TF-IDF 的 TF 和 IDF 分别是什么？怎么计算？
- 余弦相似度怎么算？为什么用余弦而不是欧氏距离？
- RAG 的 Prompt 应该怎么写？和普通 Agent 的 Prompt 有什么不同？
- 怎么把 TF-IDF 升级为 Embedding 模型？
- 检索不到相关文档时怎么处理？

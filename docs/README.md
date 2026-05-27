# Wiki：信贷 AI Agent 学习工程

从零搭建一个 AI Agent 的完整学习记录——不仅写代码，更理解背后的原理。

## 适合谁看

- Java 后端开发，想了解 AI Agent 是什么、怎么用 Java 实现
- 对 LangChain4j 感兴趣，需要实战参考

## 文档导航

### 01 基础知识

先理解概念，再写代码。

- [什么是 AI Agent？](./01-基础知识/agent是什么.md)
- [Function Calling 机制](./01-基础知识/function-calling.md)
- [Memory 对话记忆](./01-基础知识/memory机制.md)
- [LangChain4j 入门](./01-基础知识/langchain4j入门.md)
- [LangChain4j 生态全景](./01-基础知识/langchain4j生态全景.md)
- [Prompt 和 Skill 开发入门](./01-基础知识/prompt和skill开发.md)

### 02 实践过程

每个 Phase 的踩坑记录和关键决策。

- [Phase 1: 骨架搭建](./02-实践过程/phase1-骨架搭建.md)
- [Phase 2: 基础对话](./02-实践过程/phase2-基础对话.md)
- [Phase 3: Agent + Tool Calling](./02-实践过程/phase3-agent-tool.md)
- [Phase 4: 多轮记忆](./02-实践过程/phase4-多轮记忆.md)
- [Phase 5: 评测框架](./02-实践过程/phase5-评测框架.md)
- [Phase 6: RAG 智能小助手](./02-实践过程/phase6-RAG模块.md) 🆕

### 03 架构设计

从整体视角理解系统。

- [系统架构](./03-架构设计/系统架构.md)

## 学习路线

```
第 1 步：读「基础知识」→ 理解 Agent 是什么
第 2 步：读「基础知识」LangChain4j 生态全景 → 了解全貌
第 3 步：读「基础知识」Prompt 和 Skill 开发 → 掌握 AI 开发核心技能
第 4 步：读「实践过程」Phase 1-3 → Agent 核心能力
第 5 步：读「实践过程」Phase 4-5 → Memory + 评测
第 6 步：读「实践过程」Phase 6 → RAG 检索增强生成 🆕
第 7 步：读「架构设计」→ 形成整体认知
第 8 步：自己加一个 Tool → 真正吃透
```

---

_完整工程见 [credit-ai-agent-demo](https://github.com/xiaobo1997/credit-java-agent-demo)_  
_Wiki 地址：[GitHub Wiki](https://github.com/xiaobo1997/credit-java-agent-demo/wiki)_

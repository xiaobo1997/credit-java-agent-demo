# Function Calling 机制

## 是什么

**Function Calling = LLM 自动决定调用哪个 Java 方法**

你给 LLM 一份"菜单"（可用工具列表），LLM 根据用户意图从菜单里选，告诉你要调哪个、传什么参数。

## 完整流程

```
用户: "查一下张三的征信，身份证 320102199001011234"

第 1 步：LangChain4j 把请求 + 工具列表发给 LLM
┌─────────────────────────────────────┐
│ POST https://api.deepseek.com/v1    │
│ {                                   │
│   "messages": [{"role":"user", ...}],│
│   "tools": [{                       │
│     "type": "function",             │
│     "function": {                   │
│       "name": "checkCredit",        │
│       "description": "查询征信...",  │
│       "parameters": {               │
│         "idCard": {"type":"string"} │
│       }                             │
│     }                               │
│   }]                                │
│ }                                   │
└─────────────────────────────────────┘

第 2 步：LLM 返回 tool_calls
┌─────────────────────────────────────┐
│ {                                   │
│   "choices": [{                     │
│     "message": {                    │
│       "tool_calls": [{              │
│         "function": {               │
│           "name": "checkCredit",    │
│           "arguments": "{\"idCard\" │
│             :\"320102199001011234\"}"│
│         }                           │
│       }]                            │
│     }                               │
│   }]                                │
│ }                                   │
└─────────────────────────────────────┘

第 3 步：LangChain4j 反射调用 Java 方法
checkCredit("320102199001011234") → "征信分数: 520, 等级: 较差"

第 4 步：把结果再发给 LLM，生成最终回复
LLM → "审核结论：拒绝。征信分数 520 分，信用等级较差..."
```

## 关键点

1. **工具注册是自动的**：你只需要在方法上加 `@Tool` 注解，LangChain4j 自动扫描参数和描述
2. **LLM 决定调不调**：LLM 判断用户意图，自己决定是否需要调工具
3. **参数是 LLM 填的**：你不需要写参数提取逻辑，LLM 自己从对话中提取
4. **多轮调用**：如果 LLM 觉得信息不够，会多次调用工具

## @Tool 注解怎么写

```java
@Tool("查询用户的征信评分。输入身份证号，返回征信分数（300-850）和信用等级")
public String checkCredit(String idCard) {
    // 实现逻辑
    return String.format("征信分数: %d, 信用等级: %s", score, level);
}
```

关键：**描述要精确**，LLM 通过描述判断什么时候该调用。

## 和传统 RPC 的区别

| | 传统 RPC | Function Calling |
|---|---|---|
| 谁决定调 | 程序员写代码决定 | LLM 自己判断 |
| 参数提取 | 手动从 request 取 | LLM 从对话中提取 |
| 扩展方式 | 改代码、加接口 | 加 @Tool 方法 |
| 容错 | 异常处理 | LLM 可以换方案 |

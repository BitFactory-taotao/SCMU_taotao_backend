# Campus Trading Agent V1 — 产品需求文档（PRD）

> **版本**：V1.0  
> **日期**：2026-07-18  
> **作者**：sangnas

---

## 1. 项目背景与现状分析

### 1.1 项目现状

SCMU_taotao（校园淘淘）是一个功能完善的校园二手交易平台，基于 Spring Boot + MyBatis-Plus + MySQL + Redis，已具备：

- **18 张数据表**，覆盖用户、商品、交易、评价、收藏、黑名单、聊天、推荐、管理审计等完整业务域
- **REST API**：`/api/v1/campus-taotao` 下约 60+ 个接口
- **STOMP WebSocket**：用户间即时通讯（`/ws/messages`）
- **推荐系统**：双轨策略（个性化加权 + 冷启动热门），含浏览记录追踪和定时缓存刷新
- **管理后台**：商品审核、反馈管理、风险账号审核、解决事项总台
- **基础设施**：S3 对象存储（阿里云 OSS）、敏感词过滤、Token + Redis 认证

### 1.2 核心问题

当前平台采用传统菜单式操作——用户需要找到对应页面、填写表单、手动完成每个步骤。例如发布商品需要：选择分类 → 填写标题 → 编写描述 → 上传图片 → 定价 → 提交。对于不熟悉平台的用户，流程繁琐且容易出错。

### 1.3 解决思路

引入一个 **AI Agent** 作为统一自然语言交互入口。用户只需用自然语言表达需求（"我要卖一台 MacBook，九成新"），Agent 自动完成意图识别、工作流编排、工具调用和结果返回。

**核心设计理念**：Intent-Driven Interaction（意图驱动交互）——用户不再学习平台操作流程，直接表达需求；系统负责理解意图、规划流程并调用工具完成任务。

---

## 2. V1 产品目标

构建一个统一的 **Campus Trading Agent**，支持以下核心能力：

| 能力 | 说明 | 优先级 |
|------|------|--------|
| 智能商品发布 | 上传图片 → 图片分析 → 自动填表 → 用户确认 → 发布 | P0 |
| 智能商品搜索 | 自然语言语义搜索，非关键词匹配 | P0 |
| 平台助手 | 规则咨询、个人数据查询、售后指引 | P1 |
| Agent 对话界面 | 独立前端页面，聊天式交互入口 | P0 |

**V1 不做**：多 Agent 协作、超长推理链、用户记忆（Memory）、RAG 知识库、自动循环修正。

---

## 3. 系统架构

### 3.1 总体架构

```
┌──────────────────────────────────────────────────────────┐
│                Agent 前端（独立页面，新增）                  │
│  Vue / 纯 HTML — 独立部署，不集成原前端                     │
│  SSE 流式对话 + 图片上传 + 确认卡片                         │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTP/SSE（直连 Agent Service）
                         ▼
┌──────────────────────────────────────────────────────────┐
│              Agent Service（Python，新增）                  │
│  FastAPI（端口 8100）+ LangGraph                          │
│  • /chat — SSE 对话接口（前端直连）                         │
│  • /confirm — 确认指令                                    │
│  • 静态文件服务 — Agent 前端页面                            │
│  • Token 透传校验 — 调用 Java Backend 验证                  │
└────────┬──────────────────────────┬──────────────────────┘
         │ LLM API                 │ HTTP（Tool Calling）
         ▼                         ▼
┌─────────────────┐    ┌───────────────────────────────────┐
│  LLM Provider   │    │     Java Backend（Spring Boot）     │
│  OpenAI / Qwen  │    │  ┌─────────────────────────────┐  │
│  / DeepSeek     │    │  │ AgentInternalController（新增）│  │
│                 │    │  │ /internal/*（仅 127.0.0.1）   │  │
│                 │    │  └─────────────────────────────┘  │
│                 │    │  ┌─────────────────────────────┐  │
│                 │    │  │ 现有 Controller（不变）        │  │
│                 │    │  │ /goods /user /trade ...      │  │
│                 │    │  └─────────────────────────────┘  │
└─────────────────┘    └──────────────┬────────────────────┘
                                      ▼
                           ┌───────────────────┐
                           │  MySQL / Redis     │
                           └───────────────────┘
```

### 3.2 关键设计决策

**决策 1：Agent 作为独立 Python 服务，而非嵌入 Java**

理由：
- LangGraph 生态原生 Python，嵌入 Java 需 GraalVM/Jython，复杂且不稳定
- 独立部署便于独立扩缩容、LLM 调用管理、故障隔离
- 技术栈清晰：Java 负责业务 + 数据，Python 负责 Agent 编排 + LLM

**决策 2：Agent 前端独立部署，直连 Agent Service**

理由：
- 原前端代码不可控，Agent 需要一个完全独立的展示页面
- 前端直连 Agent Service 的 SSE 接口，避免 Java 中转增加延迟
- Agent Service 负责 Token 校验：接收用户 Token → 调 Java `/internal/auth/verify` 验证 → 获取 userId
- Java Backend 仅负责 Tool 执行（`/internal/*`），不做对话代理

**决策 3：Agent 通过 HTTP 调用 Java 内部 API 操作数据**

Agent 不直连数据库。所有业务操作（查商品、发商品、查用户）通过 Java 暴露的 `/internal/*` 接口完成。这些接口：
- 仅在 127.0.0.1 可访问，不接受外部请求
- Agent Service 调用时携带 `X-Internal-Token` 头部，Java 侧校验后识别调用来源
- 由 Java Service 层实现，复用已有业务逻辑、敏感词过滤、权限校验

**决策 4：对话通道使用 SSE，不复用 STOMP WebSocket**

理由：
- LLM 流式输出天然适配 SSE（Server-Sent Events）
- STOMP WebSocket 已有用户间聊天用途，混用会增加协议复杂度
- SSE 实现简单，前端只需 `EventSource` 或 `fetch` + `ReadableStream`

---

## 4. Agent 工作流设计

### 4.1 总体流程

```
用户输入（文字 / 文字+图片）
        │
        ▼
┌──────────────────┐
│  Intent Router    │  ← LangGraph node: LLM 分类
│  意图识别          │
└──────┬───────────┘
       │
  ┌────┼────┬────────┐
  ▼    ▼    ▼        ▼
发布  搜索  助手    闲聊
  │    │    │        │
  ▼    ▼    ▼        ▼
┌──────────────────────────────────────┐
│        对应 Workflow 执行             │
│  (LangGraph StateGraph)              │
│  • 多步编排                           │
│  • Tool Calling (HTTP → Java)        │
│  • LLM 总结                          │
└──────────────────────────────────────┘
       │
       ▼
  SSE 流式返回结果给前端
```

### 4.2 Workflow 1：智能商品发布（P0 核心）

这是 V1 最核心、最能体现 Agent 价值的 Workflow。用户上传一张商品图片（或直接描述），Agent 自动完成信息提取、补全和发布。

**流程详述**：

```
Step 0: 接收输入
  ├─ 用户消息（文字描述 + 可选图片）
  └─ 图片以 base64 传给 Agent，Agent 决定是否先上传 OSS 再做分析

Step 1: 图片分析（有图片时）
  ├─ Tool: analyze_image
  ├─ 调用多模态 LLM（GPT-4o / Qwen-VL）
  ├─ 提取：品类、品牌、型号、成色、配件
  └─ 输出：结构化商品特征

Step 2: 信息补全与推荐
  ├─ Tool: search_similar_goods → 查询同类商品（价格参考）
  ├─ Tool: get_categories → 获取可用分类列表
  ├─ LLM 综合图片分析 + 同类商品数据
  └─ 生成：
       • 推荐标题（1-3 个选项）
       • 推荐描述
       • 推荐分类
       • 建议价格区间
       • 建议交易地点（校园默认）

Step 3: 用户确认（人机协作节点）
  ├─ Agent 将生成的信息以结构化卡片展示
  ├─ 用户可修改任意字段
  ├─ 用户确认 → 继续；用户取消 → 结束
  └─ 这里 LangGraph 使用 interrupt 机制等待用户输入

Step 4: 执行发布
  ├─ Tool: publish_goods
  ├─ 调用 Java POST /internal/goods
  ├─ 传递：名称、描述、价格、分类、图片URL、类型
  └─ 返回：商品ID + 商品详情链接

Step 5: 结果展示
  └─ "商品已发布！点击查看：[商品链接]"
```

**无图片时的降级路径**：跳过 Step 1，用户纯文字描述（如 "卖一台 iPhone 14，128G，紫色，9成新，4500元"），Agent 直接从 Step 2 开始。

### 4.3 Workflow 2：智能商品搜索（P0）

用户用自然语言描述需求，Agent 做语义级搜索，而非简单的 SQL `LIKE` 匹配。

**流程**：

```
Step 1: 理解搜索意图
  ├─ LLM 提取：品类、预算区间、用途场景、偏好标签
  └─ 示例："预算4000，适合写Java的笔记本"
        → { category: "电子产品/电脑", priceMax: 4000, usage: "编程/Java开发", tags: ["高性能", "大内存"] }

Step 2: 多策略搜索
  ├─ Tool: search_goods (关键词 + 分类 + 价格区间)
  ├─ Tool: get_recommendations (利用现有推荐系统，传入 userId)
  ├─ 合并去重
  └─ 按语义相关性重排序（LLM 评分）

Step 3: 结果解释
  ├─ 对 Top 3-5 结果，LLM 生成推荐理由
  ├─ 例："这台 ThinkPad X1 Carbon（3500元）内存16G，适合跑 IDE；
  │       这台 MacBook Pro 2019（4000元）性能足够编译大型 Java 项目"
  └─ 返回结构化商品列表 + 推荐理由
```

### 4.4 Workflow 3：平台助手（P1）

统一处理规则咨询、数据查询、售后指引，作为 Fallback Workflow。

**支持的问题类型**：

| 类型 | 示例 | 实现方式 |
|------|------|----------|
| 规则咨询 | "怎么实名认证？""违规商品有哪些？" | LLM 直接回答（System Prompt 包含规则摘要） |
| 个人数据 | "我的商品有哪些？""我的收藏呢？" | Tool: get_my_goods / get_my_favorites → Java API |
| 订单查询 | "我的订单呢？""卖出的东西" | Tool: get_my_trades → Java API |
| 售后指引 | "怎么退款？""什么时候到账？" | LLM 直接回答（规则摘要） |

V1 的规则咨询不依赖 RAG，而是将核心平台规则写进 Agent 的 System Prompt 中。规则量不大（< 20 条），Prompt 内嵌足够。

---

## 5. Tool 定义

Agent 通过 Tool 与 Java Backend 交互。每个 Tool 封装一个 `/internal/*` HTTP 调用。

### 5.1 Agent 侧 Tool 清单

| Tool 名称 | 功能 | 对应 Java API | V1 |
|-----------|------|---------------|-----|
| `analyze_image` | 多模态 LLM 分析商品图片 | 无（纯 LLM 调用） | ✅ |
| `search_similar_goods` | 按关键字/分类搜索已有商品 | `GET /internal/goods/search` | ✅ |
| `get_categories` | 获取可用分类列表 | `GET /internal/categories` | ✅ |
| `publish_goods` | 发布商品 | `POST /internal/goods` | ✅ |
| `search_goods` | 语义搜索商品 | `GET /internal/goods/search` | ✅ |
| `get_goods_detail` | 获取商品详情 | `GET /internal/goods/{id}` | ✅ |
| `get_recommendations` | 获取推荐商品（复用推荐系统） | `GET /internal/recommend` | ✅ |
| `get_my_goods` | 查询当前用户的商品 | `GET /internal/user/goods` | ✅ |
| `get_my_favorites` | 查询当前用户收藏 | `GET /internal/user/favorites` | ✅ |
| `get_my_trades` | 查询当前用户交易 | `GET /internal/user/trades` | ✅ |
| `get_user_info` | 查询用户信息 | `GET /internal/user/info` | P1 |
| `get_platform_rules` | 查询平台规则（暂用 Prompt） | — | P1 |

### 5.2 Java 侧新增接口（`/internal/*`）

所有 `/internal/*` 接口仅监听 `127.0.0.1`，或通过 `X-Internal-Secret` 请求头校验。

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/internal/auth/verify` | Token 校验，返回 userId + userRole |
| GET | `/internal/categories` | 返回分类列表 |
| GET | `/internal/goods/search` | 商品搜索（复用已有搜索逻辑） |
| GET | `/internal/goods/{id}` | 商品详情 |
| POST | `/internal/goods` | 发布商品 |
| GET | `/internal/recommend` | 获取推荐（复用推荐服务） |
| GET | `/internal/user/goods` | 当前用户的商品列表 |
| GET | `/internal/user/favorites` | 当前用户的收藏列表 |
| GET | `/internal/user/trades` | 当前用户的交易列表 |

---

## 6. Agent Service 技术设计

### 6.1 技术栈

| 组件 | 选型 | 理由 |
|------|------|------|
| 框架 | FastAPI | 异步原生，SSE 支持好，与 LangGraph 集成友好 |
| Agent 编排 | LangGraph | StateGraph + ToolNode + interrupt（人机协作） |
| LLM | 通用接口（OpenAI / Qwen / DeepSeek 可切换） | 不绑定单一厂商 |
| 多模态 | GPT-4o 或 Qwen-VL | 图片分析 |
| HTTP 客户端 | httpx (async) | 调用 Java Backend |
| 运行环境 | Python 3.11+ | LangGraph 最低要求 |

### 6.2 LangGraph 状态设计

```python
class AgentState(TypedDict):
    # 会话信息
    session_id: str
    user_id: str                # 由 Agent 校验 Token 后获取
    # 消息历史
    messages: list[BaseMessage] # LangChain 消息格式
    # 意图路由
    intent: str                 # "publish" | "search" | "assist" | "chat"
    # 发布流程状态
    publish_images: list[str]   # 临时图片 base64 或 OSS URL
    publish_analysis: dict      # 图片分析结果
    publish_draft: dict         # 待确认的草稿
    publish_confirmed: bool     # 用户是否确认
    # 搜索流程状态
    search_query: dict          # 结构化搜索条件
    search_results: list[dict]  # 搜索结果
    # 通用
    final_response: str         # 最终返回给用户的内容
```

### 6.3 人机协作（Human-in-the-Loop）

商品发布 Workflow 在生成草稿后需要用户确认，使用 LangGraph 的 `interrupt` 机制：

```python
# 伪代码
def draft_step(state: AgentState) -> AgentState:
    draft = build_draft(state["publish_analysis"])
    state["publish_draft"] = draft
    return state

# Graph 定义
graph.add_node("draft", draft_step)
graph.add_node("confirm", human_confirm_node)  # interrupt 在这里
graph.add_node("publish", publish_step)
graph.add_edge("draft", "confirm")
graph.add_conditional_edges("confirm", lambda s: "publish" if s["publish_confirmed"] else END)
```

当执行到 `confirm` 节点时，Agent 暂停并将草稿返回前端，等待用户确认后继续。

### 6.4 Java Backend 层职责

Java Backend 不代理对话流量。前端直连 Agent Service，Java 仅提供 **Tool 执行接口** 和 **Token 校验接口**：

**AgentInternalController**（新增，仅 127.0.0.1 可访问）：

```java
@RestController
@RequestMapping("/internal")
public class AgentInternalController {

    // Token 校验接口：Agent Service 拿用户 Token 来验证并获取 userId
    @PostMapping("/auth/verify")
    public Result verifyToken(@RequestBody TokenVerifyRequest req) {
        // 复用 TokenUtil 校验，返回 userId + userRole
    }

    // 以下是 Tool 调用的接口，Agent Service 传入 userId 和参数
    // 每个接口内部通过 Service 层操作，不绕过业务逻辑
    @GetMapping("/categories")
    @GetMapping("/goods/search")
    @GetMapping("/goods/{id}")
    @PostMapping("/goods")
    @GetMapping("/recommend")
    @GetMapping("/user/goods")
    @GetMapping("/user/favorites")
    @GetMapping("/user/trades")
}
```

**Agent Service 侧鉴权流程**：

```
前端（token 在 Authorization header）
  │
  ▼
Agent Service 接收请求
  │
  ├─ 提取 Authorization header
  ├─ POST http://127.0.0.1:8080/api/v1/campus-taotao/internal/auth/verify
  │   携带 { "token": "<user-token>" }
  ├─ 校验通过 → 获取 userId, userRole
  ├─ 校验失败 → 返回 401 给前端
  └─ 后续 Tool 调用均携带 userId
```

### 6.5 会话管理

- 每个用户与 Agent 的对话对应一个 LangGraph `thread_id`
- `thread_id = userId`（V1 简化，每用户单会话）
- LangGraph 内置 checkpoint 机制持久化状态（内存或 SQLite）
- V1 不保留跨会话记忆（刷新页面后对话重置）

---

## 7. 前端设计

### 7.1 部署与登录

Agent 前端为 **独立单页面应用**，不嵌入原 Vue 前端。用户可以单独打开此页面进行 AI 对话交互。

**部署方式**（二选一）：
- **方案 A（推荐开发期）**：独立 Vite 开发服务器，端口 5173，配置 CORS 允许与 Agent Service 通信
- **方案 B（演示/交付）**：Agent Service 的 FastAPI 直接 serve 前端静态文件，访问 `http://localhost:8100` 即用

**登录方式**：
- 页面顶部提供 Token 输入框，用户粘贴从平台获取的 Token
- 支持 `?token=xxx` URL 参数自动填充（方便跳转）
- Test profile 下可使用 `/test-only/auth/token` 获取测试 Token

**页面布局**：

```text
┌──────────────────────────────────────────────────┐
│  🔑 Token: [________________________] [验证]     │  ← 登录栏（验证后隐藏）
├──────────────────────────────────────────────────┤
│  Campus Trading Agent                            │
│                                                  │
│  ┌──────────────────────────────────────────────┐│
│  │                对话消息区域                    ││
│  │  🤖 你好！我是校园淘淘助手，                   ││
│  │     可以帮你发布商品、搜索好物、               ││
│  │     解答平台问题。试试看？                     ││
│  │                                              ││
│  └──────────────────────────────────────────────┘│
│                                                  │
│  ┌──────────────────────────────────────────────┐│
│  │ 描述你想做的事，或上传图片...           📎 → ││
│  └──────────────────────────────────────────────┘│
│  [📷 发布商品]  [🔍 找东西]  [📖 帮助]           │
└──────────────────────────────────────────────────┘
```

### 7.2 对话界面

对话界面特性：

- 消息流：用户消息靠右，Agent 回复靠左，支持 Markdown + 商品卡片
- 图片上传：支持粘贴 / 拖拽 / 文件选择，前端将图片转为 base64 传给 Agent Service
- 确认卡片：发布流程中 Agent 返回结构化草稿卡片，用户可编辑并确认
- SSE 流式：Agent 回复逐字显示
- 错误处理：网络断开提示、超时重试、友好错误消息

### 7.3 关键 UI 状态

| 状态 | 表现 |
|------|------|
| 思考中 | 三个跳动的点 + "正在分析..." |
| 返回结果 | 流式打字效果 |
| 等待确认 | 显示确认卡片，输入框变为确认/取消按钮 |
| 错误 | 红色提示 + 重试按钮 |

---

## 8. 非功能需求

### 8.1 性能

| 指标 | 目标 | 备注 |
|------|------|------|
| 简单问答响应 | < 3 秒 | 无 Tool 调用的纯 LLM 回复 |
| 搜索工作流 | < 5 秒 | 含 2-3 个 Tool 调用 |
| 发布工作流（无图片） | < 5 秒 | 纯文本分析 + Tool 调用 |
| 发布工作流（含图片） | < 15 秒 | 含多模态 LLM 调用 |
| SSE 首字节 | < 1 秒 | 用户感知"开始回复了" |

### 8.2 可靠性

- Agent Service 不可用时，前端显示"服务暂不可用"并禁用输入
- LLM 调用失败时，Agent 返回友好错误，不暴露技术细节
- 图片上传失败时，提示用户重试或跳过图片仅用文字描述
- LangGraph checkpoint 保证流程中断后可恢复（同 session 内）

### 8.3 安全

- `/internal/*` 接口仅监听 `127.0.0.1`，不接受外部请求
- Agent 传给 Java 的 `userId` 来自 Token 校验结果，不可伪造
- Tool 调用携带会话级限流（同一用户每分钟最多 20 次工具调用）
- 敏感词过滤在 Java 侧 `publish_goods` 中已有，Agent 侧不额外处理

---

## 9. 项目结构

```
SCMU_taotao/
├── src/main/java/com/bit/scmu_taotao/
│   ├── controller/
│   │   └── agent/
│   │       └── AgentInternalController.java   # Agent → Java 内部 API + Token 校验
│   ├── dto/agent/
│   │   ├── TokenVerifyRequest.java
│   │   └── AgentInternalResponse.java
│   └── config/
│       └── AgentProperties.java              # Agent Service 地址等配置
│
├── agent/                                     # Python Agent Service
│   ├── pyproject.toml
│   ├── requirements.txt
│   ├── src/
│   │   ├── main.py                            # FastAPI 入口
│   │   ├── graph/
│   │   │   ├── state.py                       # AgentState 定义
│   │   │   ├── router.py                      # Intent Router
│   │   │   ├── publish_workflow.py            # Workflow 1
│   │   │   ├── search_workflow.py             # Workflow 2
│   │   │   └── assist_workflow.py             # Workflow 3
│   │   ├── tools/
│   │   │   ├── __init__.py
│   │   │   ├── vision.py                      # analyze_image
│   │   │   ├── goods.py                       # 商品相关 tools
│   │   │   ├── user.py                        # 用户相关 tools
│   │   │   └── client.py                      # httpx → Java Backend
│   │   └── config.py                          # 配置（LLM、URL 等）
│   └── tests/
│       └── test_workflows.py
│
├── agent-frontend/                            # Agent 独立前端（新增）
│   ├── index.html
│   ├── src/
│   │   ├── main.js                            # Vue/原生 JS 入口
│   │   ├── App.vue
│   │   ├── components/
│   │   │   ├── ChatPanel.vue                  # 对话面板
│   │   │   ├── MessageBubble.vue              # 消息气泡
│   │   │   ├── ConfirmCard.vue                # 确认卡片
│   │   │   ├── ImageUploader.vue              # 图片上传
│   │   │   └── TokenBar.vue                   # Token 登录栏
│   │   ├── api/
│   │   │   └── agent.js                       # SSE 连接 + HTTP 请求
│   │   └── utils/
│   │       └── sse.js                         # SSE 解析工具
│   └── vite.config.js
│
└── docs/
    └── PRD-V1.md                              # 本文档
```

---

## 10. V1 交付清单

| 编号 | 交付物 | 说明 |
|------|--------|------|
| D1 | Agent Service 基础框架 | FastAPI + LangGraph 骨架，可用空 Workflow 联通 |
| D2 | Intent Router | LLM 分类 → 选择 Workflow |
| D3 | 发布 Workflow | 完整链路：图片分析 → 草稿 → 确认 → 发布 |
| D4 | 搜索 Workflow | 语义搜索 + 推荐理由 |
| D5 | 助手 Workflow | 规则咨询 + 个人数据查询 |
| D6 | Java AgentInternalController | `/internal/*` 接口 + Token 校验接口 |
| D7 | Agent 前端 | 独立聊天页面：Token 登录 + 对话 + 图片上传 + 确认卡片 + SSE 流式 |
| D8 | Agent ↔ Java 联调 | Agent Service Tool Calling → Java Backend 全链路 |
| D9 | 配置文件与环境变量 | LLM API Key、Java Backend 地址、Internal Secret |

---

## 11. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 多模态 LLM 图片分析不准 | 发布信息错误 | 提供用户确认环节，用户可手动修正所有字段 |
| LLM 调用延迟高 | 用户体验差 | SSE 流式输出首字节快速响应；设置超时降级 |
| LangGraph 学习曲线 | 开发进度慢 | V1 使用简单的 StateGraph，不涉及复杂分支/循环 |
| Python 服务部署运维 | 增加运维复杂度 | V1 本地单机部署，与 Java 同机；后续再容器化 |
| 用户输入不可控 | Agent 执行错误操作 | 关键操作（发布）强制人机确认 + Java 侧校验 |

---

## 12. 后续版本规划

### V2 — 记忆与个性化
- 用户偏好记忆（常卖品类、描述风格）
- 浏览/收藏历史融入推荐
- RAG 知识库（平台规则、帮助文档向量化）

### V3 — 主动服务
- 商品自动补全信息
- 价格偏高/偏低提醒
- 长期未售出商品提醒
- 多标题自动生成

### V4 — 外部生态
- MCP 接入（日历、邮件、物流）
- 第三方比价
- 同城交易地图服务

### V5 — Multi-Agent（按需）
- 仅在单 Agent 复杂度不可维护时引入
- 按职责拆分：Vision Agent、Pricing Agent、Copywriting Agent 等

---

## 附录 A：与原始草案的主要变更

| 原始草案 | 优化后 | 变更理由 |
|----------|--------|----------|
| 未定义前端来源 | Agent 前端为独立页面，不嵌入原 Vue | 原前端代码不可控，独立部署便于单独展示和开发 |
| 前端经 Java 代理 Agent | 前端直连 Agent Service | 减少一跳延迟；Java 仅做 Tool 执行，不做对话中继 |
| 未定义 Token 传递方式 | Agent Service 校验 Token 时调 Java `/internal/auth/verify` | Token 验证逻辑仍在 Java 侧，Agent 不持有校验密钥 |
| 未定义架构 | 明确定义三层架构（前端→Agent→Java） | 确保工程可行性 |
| Vision/OCR 并列 | 合并为单一多模态 LLM 调用 | OCR 是 Vision 子集，现代多模态模型同时处理 |
| "查询历史成交价格" | 改为"搜索同类商品" | 项目无历史成交价表，但有 `t_goods` + `t_trade` |
| Agent 直连数据库 | Agent 通过 Java Internal API 操作 | 安全 + 复用业务逻辑 + 敏感词过滤 |
| 8 个 Tool 平铺 | 12 个 Tool，结构化分组 | 明确每个 Tool 的 Java API 对应关系 |
| 无确认机制 | 人机协作节点（interrupt） | 发布类关键操作必须用户确认 |
| 未定义前端行为 | 明确 SSE、确认卡片、Token 登录、错误状态 | 前后端协议对齐 |
| "＜3秒响应" | 分级响应目标（3s/5s/15s） | 区分有无图片/有无 Tool 调用 |
| V2-V5 简单罗列 | V2-V5 继承 V1 架构演进 | 每版本有明确增量，不推翻重来 |

---

> **核心定位重申**：Campus Trading Agent 不是一个"带 AI 功能的二手交易平台"，而是一个**以 AI Agent 为统一交互入口的智能交易平台**。其核心设计理念是 **Intent-Driven Interaction（意图驱动交互）**。

# ShopHelper 电商平台项目设计文档

> **版本**：v0.3 | **更新时间**：2026-04-21  
> **定位**：全栈仿淘宝电商平台，重点覆盖数据库设计、高并发场景处理、AI 智能导购 Agent 与前端工程化四大核心学习目标。

---

## 一、整体架构

### 1.1 设计原则

- **契约优先**：所有跨模块协作必须先定义接口契约，再开始编码实现。
- **统一身份来源**：登录态接口统一从 JWT / Session 中解析用户身份，禁止客户端在业务请求体中直接传入可伪造的 `userId`。
- **边界清晰**：Java 主服务负责业务能力与数据一致性，Python Agent 通过 MCP Server 调用业务能力，不直接访问主业务数据库。
- **事件解耦**：跨服务异步流程优先通过 Kafka 事件完成，禁止依赖其他模块的内部表结构或私有 DTO。
- **可演进**：接口默认向后兼容，新增字段优先使用可选字段；破坏性变更必须升级版本并同步更新文档。

```
┌────────────────────────────────────────────────────────────┐
│              Client — shop-web (Vue 3 + TS)                │
│         Vite + Element Plus + Pinia + Vue Router           │
│     首页 / 商品 / 购物车 / 订单 / 秒杀 / AI 对话          │
└───────────────────────────┬────────────────────────────────┘
                            │ HTTP / SSE
┌───────────────────────────▼────────────────────────────────┐
│             API Gateway（Spring Cloud Gateway）             │
│          认证过滤 / 限流 / 路由 / 负载均衡                    │
└──────────┬──────────────────────────────┬──────────────────┘
           │                              │
┌──────────▼──────────┐       ┌───────────▼──────────────────┐
│  Java 电商主服务集群  │       │  Python Agent 微服务          │
│  (Spring Boot 3)    │       │  (FastAPI + LangGraph)        │
│                     │       │                               │
│  - 用户服务          │◄─────►│  - 导购 Agent                │
│  - 商品服务          │  REST │  - 订单助手 Agent             │
│  - 订单服务          │       │  - 商品推荐 Agent             │
│  - 购物车服务        │       │                               │
│  - 秒杀服务          │       │  LangGraph 编排多 Agent 流    │
│  - 搜索服务          │       └───────────────┬──────────────┘
└──────────┬──────────┘                       │ MCP Protocol
           │                      ┌───────────▼──────────────┐
           │                      │   MCP Server（工具暴露层） │
           │                      │  - search_products        │
           │                      │  - get_cart               │
           │                      │  - get_orders             │
           │                      │  - get_user_profile       │
           │                      │  - recommend_by_history   │
           │                      └───────────┬──────────────┘
           │◄─────────────────────────────────┘
           │
┌──────────▼──────────────────────────────────────────────────┐
│                          数据层                              │
│   MySQL 8        Redis 7      Elasticsearch 8     Kafka 3   │
│   主业务数据      缓存/Session  商品全文搜索/向量   异步消息   │
│   分库分表        分布式锁      RAG 向量检索         秒杀队列  │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、技术选型

### 2.1 Java 电商主服务

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Spring Boot | 3.x | 主框架 |
| 微服务 | Spring Cloud | 2023.x | 服务注册/发现/网关 |
| 服务注册/配置 | Nacos | 2.x | 注册中心 + 配置中心 |
| 网关 | Spring Cloud Gateway | — | 路由 / 限流 / 认证过滤 |
| ORM | MyBatis-Plus | 3.5.x | 数据库访问 |
| 认证 | Spring Security + JWT | — | 用户认证授权 |
| 关系型 DB | MySQL | 8.0 | 主业务数据 |
| 缓存 | Redis (Redisson) | 7.x | 缓存 / 分布式锁 / Session |
| 搜索 | Elasticsearch | 8.x | 商品全文搜索 + 向量存储 |
| 消息队列 | Kafka | 3.x | 异步解耦 / 秒杀队列 |
| 限流熔断 | Sentinel | 1.8.x | 秒杀限流 / 熔断保护 |
| 分库分表 | ShardingSphere | 5.x | 订单分表 / 读写分离 |
| API 文档 | Knife4j (Swagger 3) | — | 接口文档 |
| 构建工具 | Maven | 3.9.x | 多模块依赖管理 |

### 2.2 前端（Vue 3 + TypeScript）

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Vue | 3.4.x | 组件化 UI |
| 语言 | TypeScript | 5.x | 类型安全 |
| 构建工具 | Vite | 5.x | 快速构建 & HMR |
| UI 组件库 | Element Plus | 2.7.x | 电商 C 端 + 管理页面 |
| 状态管理 | Pinia | 2.x | 全局状态（用户 / 购物车 / Token） |
| 路由 | Vue Router | 4.x | SPA 路由 |
| HTTP 客户端 | Axios | 1.7.x | 统一请求封装（含 JWT 自动刷新） |
| CSS 方案 | UnoCSS | 0.60.x | 原子化 CSS，按需生成 |
| SSE 客户端 | EventSource API | — | Agent 对话流式接收 |
| 代码规范 | ESLint + Prettier | — | 统一风格 |

### 2.3 Python Agent 微服务

| 层次 | 技术 | 用途 |
|------|------|------|
| Web 框架 | FastAPI | 对外暴露 Agent 对话接口（SSE 流式） |
| Agent 编排 | LangGraph | 多 Agent 状态机编排（ReAct 模式） |
| MCP 集成 | langchain-mcp-adapters | 连接 MCP Server，获取工具列表 |
| LLM 接入 | LangChain + DeepSeek / Qwen API | 大模型调用 |
| RAG 检索 | LangChain + ES dense_vector | 商品语义检索 |
| 向量化 | BGE-large-zh / API Embedding | 商品文本向量化 |

### 2.4 基础设施

| 组件 | 技术 | 说明 |
|------|------|------|
| 容器化 | Docker + Docker Compose | 本地开发一键启动所有中间件 |
| 链路追踪 | SkyWalking（可选） | 分布式调用链追踪 |
| 监控 | Prometheus + Grafana（可选） | 指标监控大盘 |

---

## 三、项目结构

```
ShopHelper/                        # 项目根目录
├── backend/                       # Java 后端（多 Maven 模块）
├── frontend/                      # Vue 3 前端 SPA
├── docs/                          # 设计文档
├── docker/                        # Docker 初始化脚本
├── docker-compose.yml             # 基础设施 + 前端容器
└── .gitignore
```

### 3.1 Java 主服务（多 Maven 模块）

```
backend/                           # 父 POM
├── shop-gateway/               # API 网关（限流/路由/认证过滤）
├── shop-auth/                  # 认证服务（登录/JWT/刷新 Token）
├── shop-user/                  # 用户服务（注册/个人信息/地址）
├── shop-product/               # 商品服务（CRUD/分类/SKU/库存）
├── shop-order/                 # 订单服务（下单流程/状态机）
├── shop-cart/                  # 购物车服务（Redis Hash 存储）
├── shop-seckill/               # 秒杀服务（Redis 预热/Lua 扣减）
├── shop-search/                # 搜索服务（ES 全文检索/聚合）
├── shop-mcp-server/            # MCP 工具暴露层（供 Agent 调用）
└── shop-common/                # 公共模块（Result/异常/工具类/DTO）
```

### 3.2 前端（Vue 3 SPA）

```
frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                    # Axios 请求封装
│   │   ├── request.ts          # Axios 实例（拦截器、JWT 自动刷新）
│   │   ├── auth.ts             # 认证相关 API
│   │   ├── product.ts          # 商品相关 API
│   │   ├── cart.ts             # 购物车相关 API
│   │   ├── order.ts            # 订单相关 API
│   │   ├── seckill.ts          # 秒杀相关 API
│   │   └── agent.ts            # Agent 对话 SSE API
│   ├── assets/                 # 静态资源（图片/字体）
│   ├── components/             # 公共组件
│   │   ├── ProductCard.vue     # 商品卡片
│   │   ├── CartItem.vue        # 购物车条目
│   │   ├── OrderStatus.vue     # 订单状态标签
│   │   └── ChatBubble.vue      # AI 对话气泡
│   ├── composables/            # 组合式函数
│   │   ├── useAuth.ts          # 登录态管理
│   │   └── useSSE.ts           # SSE 连接封装
│   ├── layouts/                # 布局组件
│   │   ├── DefaultLayout.vue   # 默认布局（Header + Footer）
│   │   └── BlankLayout.vue     # 空白布局（登录页）
│   ├── pages/                  # 页面组件（按路由组织）
│   │   ├── home/               # 首页（推荐 + 分类入口）
│   │   ├── login/              # 登录 / 注册
│   │   ├── product/            # 商品详情
│   │   ├── search/             # 搜索结果
│   │   ├── cart/               # 购物车
│   │   ├── order/              # 订单列表 / 详情 / 确认
│   │   ├── seckill/            # 秒杀活动
│   │   ├── chat/               # AI 导购对话
│   │   └── user/               # 个人中心
│   ├── router/                 # 路由配置
│   │   └── index.ts
│   ├── stores/                 # Pinia 状态管理
│   │   ├── user.ts             # 用户信息 & Token
│   │   └── cart.ts             # 购物车状态
│   ├── types/                  # TypeScript 类型定义
│   │   ├── api.d.ts            # API 响应类型（与后端 Result<T> 对齐）
│   │   ├── product.d.ts        # 商品相关类型
│   │   └── order.d.ts          # 订单相关类型
│   ├── utils/                  # 工具函数
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
├── uno.config.ts
├── .eslintrc.cjs
└── .prettierrc
```

### 3.3 Python Agent 微服务

```
agent-service/
├── api/
│   └── chat.py                 # FastAPI SSE 流式对话接口
├── agents/
│   ├── shopping_guide/         # 导购 Agent（推荐/问答）
│   ├── order_assistant/        # 订单助手 Agent（查单/退换货）
│   ├── recommend/              # 推荐 Agent（协同过滤结合 LLM）
│   └── graph.py                # LangGraph 多 Agent 编排入口
├── mcp/
│   └── client.py               # MCP Client（连接 shop-mcp-server）
├── rag/
│   ├── indexer.py              # 商品向量离线索引构建
│   └── retriever.py            # 商品语义检索
├── config/
│   └── settings.py             # LLM API Key / ES / MCP 地址配置
└── requirements.txt
```

---

## 四、数据库设计

### 4.1 核心表

| 表名 | 描述 | 重点设计 |
|------|------|---------|
| `user` | 用户基础信息 | 手机号/邮箱唯一索引，手机号 AES 加密 |
| `user_address` | 收货地址 | 默认地址标志位 |
| `category` | 商品分类 | 树形结构（parent_id 自关联） |
| `product` | 商品信息 | 逻辑删除，`status` 状态机 |
| `product_sku` | SKU 规格 | 库存字段 + 乐观锁版本号 |
| `order` | 订单主表 | 按 `user_id % 16` 水平分表 |
| `order_item` | 订单明细 | 随订单表分片 |
| `seckill_activity` | 秒杀活动 | 开始/结束时间，限购数量 |
| `seckill_stock` | 秒杀库存 | Redis 预热，DB 做兜底记录 |

### 4.2 字段规范

- **主键**：`bigint UNSIGNED` + 雪花算法（Snowflake ID）
- **必备字段**：`create_time DATETIME`, `update_time DATETIME`, `is_deleted TINYINT(1) DEFAULT 0`
- **索引命名**：普通索引 `idx_{表名}_{字段}`，唯一索引 `uk_{表名}_{字段}`
- **手机号存储**：拆分为 `phone_ciphertext`（AES-CBC 密文）+ `phone_hash`（HMAC-SHA256，E.164 归一化），唯一索引建在 hash 列上
- **软删除 + 唯一索引**：增加 `delete_version BIGINT DEFAULT 0`，唯一索引改为 `(field, delete_version)`，软删除时置 `delete_version = id`
- **订单状态**：TINYINT 枚举码（0~6），API 层映射字符串；参见 schema.sql 注释
- **订单地址**：订单表必须存储地址快照（snap_* 字段），防止用户改地址后历史订单丢失
- **订单支付期限**：`expire_time DATETIME` 字段存储支付截止时间（下单时写入，如 30 分钟后），超时后由定时任务扫描并自动取消；API 响应中对应字段为 `expireTime`
- **订单明细**：`order_item` 含 `user_id`，与 `order` 表用同一分片键（ShardingSphere binding table）
- **SKU 唯一性**：`product_sku` 增加 `spec_hash`（spec_json MD5），建联合唯一索引 `(product_id, spec_hash)`
- **数值约束**：金额 / 库存 / 数量字段均加 `CHECK` 约束
- **完整 DDL**：见 `docs/schema.sql`

### 4.3 分库分表策略

| 表 | 策略 | 工具 |
|----|------|------|
| 订单表 | 按 `user_id % 16` 水平分表 | ShardingSphere |
| 商品表 | 单库，搜索走 ES | — |
| 读写分离 | 写主库，读从库 | ShardingSphere |

---

## 五、高并发场景设计

### 5.1 商品详情页（高并发读）

```
请求 → 查 Redis 缓存
  ├─ 命中：直接返回（P99 < 5ms）
  └─ 未命中 → Redisson 互斥锁 → 查 DB → 写 Redis（TTL: 30min ± random(5min)）→ 返回

缓存三大问题：
  - 穿透：BloomFilter 过滤非法商品 ID
  - 击穿：Redisson 互斥锁重建缓存
  - 雪崩：TTL 随机抖动
```

### 5.2 秒杀（高并发写）

```
1. 活动开始前：商品库存预热到 Redis（SET seckill:stock:{activityId}:{skuId} {count}）
2. 请求到达 Gateway → 令牌桶限流（Sentinel）
3. → 校验用户是否已购（Redis SET 去重）
4. → Lua 脚本原子扣减库存（DECR + 判断 >= 0）
5. → 扣减成功 → 发 Kafka 消息（异步创建订单）
6. → 前端轮询 / SSE 获取下单结果

防超卖兜底：DB 层 UPDATE ... SET stock = stock - 1 WHERE stock > 0（乐观锁）
```

### 5.3 订单异步化（Kafka）

```
Topic: order-create
  消费者：库存服务（扣减 DB 库存，幂等校验）
  消费者：通知服务（短信/站内消息）
  消费者：积分服务（赠送积分）

幂等性：
  - 订单号唯一键（数据库唯一约束）
  - Redis Set 记录已消费消息 ID（SETNX + TTL）
```

---

## 六、智能导购 Agent 设计

### 6.1 Agent 架构

```
用户消息（HTTP POST）
    │
    ▼
FastAPI SSE 接口 /agent/chat
    │
    ▼
LangGraph 路由节点（意图识别）
    ├── 商品咨询 / 推荐 → 导购 Agent
    ├── 订单相关        → 订单助手 Agent
    └── 其他闲聊        → 通用回复节点
    │
    ▼（以导购 Agent 为例，ReAct 模式）
导购 Agent
    ├── 思考：用户想找什么？
    ├── 调用 MCP Tool: search_products（RAG 语义检索 Top-K 商品）
    ├── 调用 MCP Tool: get_user_profile（用户偏好 / 历史）
    ├── 调用 MCP Tool: get_cart（避免推荐重复商品）
    └── 生成回复（含结构化商品卡片数据）
    │
    ▼
SSE 流式返回给前端
```

### 6.2 MCP Server 工具清单

| 工具名 | 描述 | 对应 Java API |
|--------|------|--------------|
| `search_products` | 语义搜索商品（RAG + ES kNN） | `GET /api/v1/search/products` |
| `get_product_detail` | 获取商品详情 | `GET /api/v1/products/{id}` |
| `get_cart` | 获取用户购物车 | `GET /api/v1/cart` |
| `add_to_cart` | 加入购物车 | `POST /api/v1/cart/items` |
| `remove_cart_item` | 删除购物车单项 | `DELETE /api/v1/cart/items/{itemId}` |
| `get_orders` | 获取用户订单列表 | `GET /api/v1/orders` |
| `cancel_order` | 取消订单（订单助手 Agent 专用） | `POST /api/v1/orders/{orderId}/cancel` |
| `get_user_profile` | 获取用户偏好/历史 | `GET /api/v1/users/me/profile` |
| `get_recommendations` | 协同过滤推荐商品 | `GET /api/v1/recommendations/me` |

### 6.3 RAG 商品检索

```
离线阶段（定时任务）：
  MySQL 商品数据 → 文本化（标题 + 描述 + 类目标签）
    → Embedding 模型（BGE-large-zh）
    → Elasticsearch dense_vector 索引

在线阶段：
  用户查询 → Query Embedding → ES kNN 搜索（Top-10）
    → 商品列表注入 LLM Prompt
    → LLM 基于真实商品数据生成推荐回复
```

---

## 七、API 规范

### 7.1 契约优先原则

- 所有对外 REST API、MCP Tool、Kafka Topic、Redis Key 在编码前必须先补充到设计文档或独立契约文档。
- Java 服务之间、Agent 与 MCP Server 之间只能依赖公开契约，不能依赖实现细节。
- 接口一旦进入联调阶段，新增字段必须保持向后兼容，删除字段或修改语义属于破坏性变更。
- 所有契约必须包含：用途、请求参数、响应结构、错误码、鉴权方式、幂等要求、示例。
- 没有契约的能力默认不能进入编码阶段；如需探索实现，先补接口草案，再实现原型。

### 7.2 通用请求规范

- REST API 统一前缀：`/api/v1`
- 鉴权方式：`Authorization: Bearer <JWT>`
- 链路追踪：请求头透传 `X-Request-Id`，服务端若未收到则自动生成
- 幂等请求：创建订单、秒杀下单、支付类接口必须支持 `Idempotency-Key`
- JSON 字段风格：对外接口统一使用 `camelCase`
- 时间字段：业务字段统一使用 ISO 8601 字符串，响应包装中的 `timestamp` 使用毫秒时间戳
- 分页参数：统一使用 `pageNum`、`pageSize`，默认 `pageNum=1`、`pageSize=20`，最大 `pageSize=100`

分页响应结构：

```json
{
  "list": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 20,
  "hasNext": false
}
```

### 7.3 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "requestId": "9f0d6d3cb1f44a4d8f33f0e4f3e2c001",
  "timestamp": 1745162400000
}
```

### 7.4 错误码

| 错误码 | 含义 |
|--------|------|
| 200 | 成功 |
| 40001 | 参数校验失败 |
| 40101 | 未登录 / Token 失效 |
| 40301 | 权限不足 |
| 40401 | 资源不存在 |
| 50001 | 服务内部错误 |
| 50901 | 库存不足 |
| 50902 | 秒杀活动已结束 |
| 50903 | 已参与过此次秒杀 |
| 50904 | 订单状态不允许此操作 |

错误响应约束：

- `message` 面向前端或调用方，要求可读
- `requestId` 用于日志检索与跨服务排障
- 业务异常必须映射稳定错误码，禁止直接返回框架原始异常

### 7.5 核心 REST API 契约

#### 7.5.1 认证服务

| 接口 | 说明 | 请求参数 | 响应数据 | 约束 |
|------|------|---------|---------|------|
| `POST /api/v1/auth/login` | 用户登录 | `loginType`, `principal`, `password` | `accessToken`, `refreshToken`, `expiresIn` | 登录成功后由服务端签发 JWT |
| `POST /api/v1/auth/refresh` | 刷新令牌 | `refreshToken` | 新的 `accessToken`, `refreshToken` | 刷新 Token 必须轮转 |

#### 7.5.2 商品与搜索服务

| 接口 | 说明 | 请求参数 | 响应数据 | 约束 |
|------|------|---------|---------|------|
| `GET /api/v1/products/{productId}` | 查询商品详情 | 路径参数 `productId` | 商品基本信息、SKU 列表、库存摘要 | 对外仅返回上架商品 |
| `GET /api/v1/search/products` | 商品搜索 | `keyword`, `categoryId`, `pageNum`, `pageSize`, `sortBy`, `sortOrder` | 分页商品列表、聚合过滤信息 | 搜索服务只负责检索，不返回管理后台字段 |

#### 7.5.3 购物车服务

| 接口 | 说明 | 请求参数 | 响应数据 | 约束 |
|------|------|---------|---------|------|
| `GET /api/v1/cart` | 获取当前用户购物车 | 无 | 购物车项、总数量、预估总价 | 当前用户身份从 JWT 中解析 |
| `POST /api/v1/cart/items` | 加入购物车 | `skuId`, `quantity` | 最新购物车快照 | `quantity > 0`，同一 SKU 合并数量 |
| `PUT /api/v1/cart/items/{itemId}` | 修改购物车项 | `quantity`, `selected` | 最新购物车快照 | 不允许修改不属于当前用户的购物车项 |
| `DELETE /api/v1/cart/items/{itemId}` | 删除购物车单项 | 路径参数 `itemId` | 最新购物车快照 | 不允许删除不属于当前用户的购物车项 |
| `DELETE /api/v1/cart` | 清空购物车 | 无 | 空购物车快照 | 只清空当前用户购物车；Agent 不允许调用此接口 |

#### 7.5.4 订单服务

订单状态机：

```
PENDING_PAYMENT ──(支付成功)──► PAID ──(仓库发货)──► SHIPPED ──(用户签收)──► COMPLETED
      │                          │
      │(超时/主动取消)             │(发货前主动取消，触发退款)
      ▼                          ▼
  CANCELLED                  REFUNDING ──(退款到账)──► REFUNDED
```

- `cancel` 仅允许在 `PENDING_PAYMENT` 或 `PAID`（发货前）状态下操作；`PAID` 取消时自动触发退款流程，状态流转为 `REFUNDING`
- `pay` 仅允许在 `PENDING_PAYMENT` 状态下操作；支付结果通过支付平台异步回调确认，接口本身只创建支付会话

| 接口 | 说明 | 请求参数 | 响应数据 | 约束 |
|------|------|---------|---------|------|
| `POST /api/v1/orders` | 创建订单 | `addressId`, `items[]`, `remark`, `source` | `orderId`, `orderNo`, `orderStatus`, `payAmount` | 必须校验价格与库存，不能信任客户端价格；需携带 `Idempotency-Key` 头 |
| `GET /api/v1/orders` | 查询订单列表 | `pageNum`, `pageSize`, `status` | 订单分页列表 | 仅能查询当前用户订单；`status` 枚举：`PENDING_PAYMENT \| PAID \| SHIPPED \| COMPLETED \| CANCELLED \| REFUNDING \| REFUNDED` |
| `GET /api/v1/orders/{orderId}` | 查询订单详情 | 路径参数 `orderId` | 订单主信息、订单项、物流摘要 | 仅能查询当前用户订单 |
| `POST /api/v1/orders/{orderId}/cancel` | 取消订单 | `reason`（取消原因，字符串） | `orderId`, `orderStatus` | 仅允许 `PENDING_PAYMENT` / `PAID`（发货前）状态；`PAID` 取消自动进入退款流程 |
| `POST /api/v1/orders/{orderId}/pay` | 发起支付会话 | `paymentMethod`（枚举：`ALIPAY \| WECHAT \| MOCK`） | `payUrl`（跳转链接）或 `qrCode` | 仅允许 `PENDING_PAYMENT` 状态；订单实际支付状态由支付平台异步回调更新；需携带 `Idempotency-Key` 头 |
| `POST /api/v1/orders/payment-callback` | 支付平台异步回调 | 支付平台标准参数（签名验证） | HTTP 200 "success" | 仅允许来自支付平台白名单 IP；需验签；幂等处理；触发 `order.paid` 事件 |

#### 7.5.5 秒杀服务

| 接口 | 说明 | 请求参数 | 响应数据 | 约束 |
|------|------|---------|---------|------|
| `POST /api/v1/seckill/{activityId}/items/{skuId}/submit` | 提交秒杀请求 | `addressId` | `orderToken`, `status=PROCESSING` | 需携带 `Idempotency-Key`；Redis Lua 扣减成功后发 Kafka 异步创建订单；50901 = 售罄；50903 = 已参与 |
| `GET /api/v1/seckill/{activityId}/result?orderToken=...` | 查询秒杀结果 | `orderToken`（query param） | `status`（PROCESSING/SUCCESS/FAILED）, `orderId`, `orderNo` | 前端轮询，建议 500ms 间隔，最多 10 次 |

#### 7.5.6 用户画像与推荐服务

| 接口 | 说明 | 请求参数 | 响应数据 | 约束 |
|------|------|---------|---------|------|
| `GET /api/v1/users/me/profile` | 获取当前用户画像 | 无 | 历史类目、价格偏好、品牌偏好 | 必须脱敏，禁止返回手机号等敏感信息 |
| `GET /api/v1/recommendations/me` | 获取当前用户推荐商品 | `scene`, `pageSize` | 推荐商品列表、推荐标签 | 用户身份从 JWT 中解析，不接受外部传 `userId`；`scene` 枚举：`homepage \| product_detail \| cart \| checkout \| category` |

订单创建请求示例：

```json
{
  "addressId": "190000000000000001",
  "items": [
    {
      "skuId": "290000000000000001",
      "quantity": 2
    }
  ],
  "remark": "工作日白天配送",
  "source": "app"
}
```

### 7.6 Agent 对话接口

```
POST /agent/chat
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "sessionId": "string",    // 多轮对话 Session ID，由客户端生成并复用
  "message": "string",
  "context": {              // 可选，当前页面上下文
    "pageType": "product_detail",   // 枚举：homepage | product_detail | cart | search | category | order
    "productId": "string",
    "categoryId": "string"
  }
}
```

响应（SSE 流式），统一 envelope 结构：

```
data: {"type": "text",         "traceId": "...", "data": {"content": "为您找到以下商品..."}}
data: {"type": "product_card", "traceId": "...", "data": {"id": "...", "name": "...", "price": 99.9}}
data: {"type": "tool_call",    "traceId": "...", "data": {"toolName": "search_products", "status": "success"}}
data: {"type": "error",        "traceId": "...", "data": {"code": "TOOL_CALL_FAILED", "message": "搜索服务暂时不可用，请稍后重试"}}
data: {"type": "done",         "traceId": "...", "data": {}}
```

约束：

- `userId` 由 Agent 服务从 JWT 中解析，客户端不允许直接传入
- `sessionId` 由客户端生成并复用，用于串联多轮对话
- SSE 事件类型至少包含：`text`、`product_card`、`tool_call`、`error`、`done`，所有事件共享统一 envelope（`type` / `traceId` / `data`）
- `product_card` 只允许引用真实商品 ID，禁止模型虚构商品数据
- Agent 回复中所有商品、订单、购物车相关事实必须来自 MCP Tool 返回结果
- `error` 事件中的 `code` 使用字符串形式（与 REST 数字错误码区分），前端应展示 `message` 字段

建议的 SSE 事件结构：

```json
{
  "type": "tool_call",
  "traceId": "5c9c1984f9db438ca9a4c75e1f5d6ee2",
  "data": {
    "toolName": "search_products",
    "status": "success"
  }
}
```

### 7.7 MCP Tool 契约

#### 鉴权与身份传递

Agent 服务调用 MCP Server 时，统一使用标准 `Authorization: Bearer <JWT>` 头（转发用户的原始 JWT）：

- MCP Server 验证 JWT 有效性并提取 `userId`，用于所有用户身份相关工具（`get_cart`、`get_orders`、`cancel_order` 等）
- 无需用户身份的工具（如 `search_products`）也需要有效 JWT，确保只有已认证用户才能使用 Agent 能力
- MCP Server 不直接访问主业务数据库，通过调用 Java 主服务内部 API 完成业务操作

所有 MCP Tool 必须满足以下规则：

- 输入参数使用 JSON Schema 明确定义必填项、类型、枚举值与默认值
- 输出统一返回结构化 JSON，禁止直接返回自然语言段落
- 所有 Tool 都要返回 `traceId`，便于回溯到下游 Java 服务日志

推荐返回包装：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "traceId": "5c9c1984f9db438ca9a4c75e1f5d6ee2"
}
```

| Tool 名 | 输入参数 | 输出数据 | 约束 |
|--------|---------|---------|------|
| `search_products` | `keyword`, `topK`, `categoryId?`, `priceMin?`, `priceMax?` | 商品列表、召回分数 | 只返回可售商品 |
| `get_product_detail` | `productId` | 商品详情、SKU、库存摘要 | `productId` 必填 |
| `get_cart` | 无 | 当前用户购物车快照 | 用户身份来自 JWT，不从 Tool 参数传入 |
| `add_to_cart` | `skuId`, `quantity` | 更新后的购物车快照 | 失败时返回稳定错误码 |
| `remove_cart_item` | `itemId` | 更新后的购物车快照 | 仅可操作当前用户购物车项 |
| `get_orders` | `pageNum`, `pageSize`, `status?` | 订单分页列表 | 仅查询当前用户 |
| `cancel_order` | `orderId`, `reason` | `orderId`, `orderStatus` | 仅允许 `PENDING_PAYMENT` / `PAID`（发货前）；由订单助手 Agent 调用 |
| `get_user_profile` | 无 | 用户偏好、历史类目、价格带偏好 | 脱敏，不返回敏感原始字段 |
| `get_recommendations` | `scene`, `topK` | 推荐商品列表、推荐理由标签 | 推荐理由需可追溯；`scene` 枚举：`homepage \| product_detail \| cart \| checkout \| category` |

### 7.8 Kafka 事件契约

事件统一封装格式：

```json
{
  "eventId": "evt_20260421_000001",
  "eventType": "order.created",
  "version": "1.0",
  "occurredAt": "2026-04-21T10:00:00+08:00",
  "traceId": "5c9c1984f9db438ca9a4c75e1f5d6ee2",
  "bizKey": "orderNo_202604210001",
  "payload": {}
}
```

| Topic | 事件类型 | 关键 payload | 约束 |
|------|---------|-------------|------|
| `order-create` | `order.created` | `orderId`, `orderNo`, `userId`, `items[]`, `source` | 消费方必须以 `eventId + bizKey` 做幂等；**库存扣减在下单事务内同步完成，Kafka 仅用于通知等异步下游** |
| `seckill-order-create` | `seckill.order.created` | `activityId`, `skuId`, `userId`, `orderToken`, `snap*` 快照字段 | 秒杀链路要求低延迟，失败需可补偿；`orderToken` 为秒杀服务在 Redis 扣减成功时生成的 UUID，同步持久化到秒杀预约记录，重试时复用同一 Token，确保下游订单创建幂等 |
| `order-notify` | `order.paid` \| `order.shipped` \| `order.completed` \| `order.cancelled` \| `order.cancel_and_refund` \| `order.refunded` | `orderId`, `orderStatus`, `changedAt` | 由订单服务在状态变更时发布；各消费者按 `eventType` 过滤；详见 `docs/kafka-contract.md` |

事件约束：

- Producer 只能追加字段，不能修改既有字段语义
- Consumer 不得依赖未声明字段
- 失败重试、死信队列、重放策略需要在实现文档中单独补充

### 7.9 Redis Key 规范

| Key 模式 | 用途 | TTL / 说明 |
|---------|------|------------|
| `product:detail:{productId}` | 商品详情缓存 | 30 分钟 + 随机抖动 |
| `cart:{userId}` | 用户购物车（Redis Hash，field 为 itemId） | 用户活跃期内续期 |
| `seckill:stock:{activityId}:{skuId}` | 秒杀库存（String，Lua 原子扣减） | 活动结束后清理 |
| `seckill:users:{activityId}` | 秒杀用户去重集合（Set） | 活动结束后清理 |
| `seckill:token:{activityId}:{userId}` | 秒杀 orderToken 持久化（String） | 与活动等生命周期，防止 Redis 重启后丢失 |
| `mq:idempotent:{group}:{eventId}` | 消费幂等记录 | 建议 24 小时 |
| `auth:refresh:{userId}:{tokenId}` | Refresh Token 存储（String） | 7 天；登出时主动删除实现 Token 黑名单 |
| `bloom:product:ids` | 商品 ID BloomFilter（防缓存穿透） | 持久化，商品上架时更新 |

---

## 八、编码规范

### 8.1 Java 规范

- 遵循**阿里巴巴 Java 开发手册**
- 包命名：`com.shophelper.{模块名}.{层次}`（如 `com.shophelper.product.service`）
- 分层模型：接口层 `DTO`，服务层 `BO`，持久层 `Entity`，严禁跨层直接使用
- 统一返回：`Result<T>` 包装所有响应
- 异常处理：`@RestControllerAdvice` 全局捕获，禁止吞掉异常
- 事务：Service 层写操作加 `@Transactional(rollbackFor = Exception.class)`
- 敏感字段：手机号、身份证号 AES 加密存储
- 禁止 `SELECT *`，所有查询列举具体字段

### 8.2 数据库规范

- 表名：小写下划线，如 `order_item`
- 禁止使用数据库外键，关联关系在代码层保证
- 所有表必备：`id`, `create_time`, `update_time`, `is_deleted`
- 单表索引不超过 5 个

### 8.3 Python Agent 规范

- 使用 `pydantic` 定义所有输入/输出模型
- LangGraph 节点函数必须是纯函数（无副作用）
- MCP 工具参数 / 返回值需有完整 JSON Schema 注释（供大模型理解）
- 日志统一使用 `structlog`（结构化日志便于排查 Agent 推理链路）

### 8.4 Agent 实施规范

- 每个 Agent 开工前必须先声明本次负责的模块边界、依赖契约、目标产物
- 没有接口契约的功能，先补文档，再写代码
- 不允许在未更新文档的情况下新增 REST API、MCP Tool、Kafka 事件或 Redis Key
- 涉及跨模块调用时，优先补充 DTO / Schema / 示例，而不是先写控制器或调用代码
- 每次交付至少包含：代码、契约同步、最小测试或联调说明
- 若实现与文档冲突，以文档修订后的契约为准，禁止“代码先跑起来再说”

---

## 九、Docker Compose 服务清单

```yaml
# docker-compose.yml（开发环境，仅基础设施）
# 应用服务（shop-gateway、shop-mcp-server、agent-service 等）使用 IDE 或命令行单独启动
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: shophelper123
      MYSQL_DATABASE: shophelper

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  elasticsearch:
    image: elasticsearch:8.13.0
    ports: ["9200:9200"]
    environment:
      - xpack.security.enabled=false
      - discovery.type=single-node

  kibana:
    image: kibana:8.13.0
    ports: ["5601:5601"]

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]

  nacos:
    image: nacos/nacos-server:v2.3.0
    ports: ["8848:8848"]
    environment:
      MODE: standalone
```

---

## 十、开发路线图

### Phase 0 - 契约定义（必须先完成）

- 明确统一认证模型（JWT、刷新机制、当前用户获取方式）
- 输出核心表 DDL、字段枚举、索引设计
- 输出核心 REST API 契约（认证、商品、购物车、订单、搜索）
- 输出 MCP Tool 输入输出 Schema 与错误契约
- 输出 Kafka Topic、事件结构、幂等策略
- 输出 Redis Key 命名、TTL、失效策略
- 约定联调方式：Mock 数据、测试账号、示例请求与响应

Phase 0 验收标准：

- 前后端、Java 服务、Agent 服务都能仅依赖文档完成接口 mock
- 每个关键能力至少有一个请求示例和一个响应示例
- 文档中不再出现“实现时再定”的关键字段和关键行为

### Phase 1 — 基础电商（学习重点：数据库设计）

- 项目脚手架搭建（Spring Boot 多模块 + Docker Compose）
- 前端脚手架搭建（Vue 3 + TypeScript + Vite + Element Plus）
- 用户模块：注册 / 登录 / JWT 认证（前后端联调）
- 商品模块：CRUD / 分类 / SKU / 库存（前后端联调）
- 购物车模块：Redis Hash 实现（前后端联调）
- 订单模块：下单流程 / 订单状态机（前后端联调）
- 数据库设计：ER 图 + 完整建表 SQL

### Phase 2 — 高并发专项（学习重点：高并发）

- Redis 缓存层（商品缓存 + 缓存三大问题解决方案）
- Kafka 异步订单处理（消费幂等性）
- 秒杀模块（Redis Lua 原子扣减 + Sentinel 限流 + 防超卖）
- ES 商品搜索（全文检索 + 聚合过滤）
- 分布式锁（Redisson 实战）
- 读写分离 + 分表模拟（ShardingSphere）

### Phase 3 — 智能导购 Agent（学习重点：AI Agent）

- ES 商品向量化（RAG 离线索引构建）
- MCP Server 工具实现（shop-mcp-server 模块）
- Python Agent 微服务（FastAPI + LangGraph）
- 多 Agent 编排（导购 / 订单助手 / 推荐 Agent）
- SSE 流式对话接口
- 前端 AI 对话 UI（Chat 页面 + SSE 流式渲染 + 商品卡片展示）

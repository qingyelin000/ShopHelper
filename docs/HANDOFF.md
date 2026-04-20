# ShopHelper Phase 0 交付说明

> **状态**：Phase 0「契约优先」文档阶段全部完成，Phase 1 骨架与基础设施已完成，可以开始 Phase 1 业务编码。  
> **时间**：2026-04-21

---

## 一、交付物清单

| 文件 | 用途 | 状态 |
|------|------|------|
| `docs/project-design.md` | 主架构文档：技术选型、项目结构、数据库设计、高并发设计、Agent 架构、API 规范（v0.3） | ✅ |
| `docs/schema.sql` | 9 张 MySQL 8.0 核心表 DDL | ✅ |
| `docs/api-contract.md` | 20 个 REST 接口完整 JSON 请求/响应示例 | ✅ |
| `docs/mcp-tool-schema.md` | 9 个 MCP Tool 的 JSON Schema 定义及 Agent 使用规则 | ✅ |
| `docs/kafka-contract.md` | 3 个 Kafka Topic 完整事件契约（Envelope、幂等、死信） | ✅ |
| `backend/` | Maven 多模块骨架（10 个模块）| ✅ |
| `frontend/` | Vue 3 + TS + Element Plus SPA 骨架 | ✅ |
| `docker-compose.yml` | 基础设施全部验证可运行 | ✅ |

---

## 一B、Phase 1 骨架完成情况（编码前必读）

### 已完成的基础工作

#### 后端（`backend/`）

| 模块 | 端口 | 状态 |
|------|------|------|
| shop-gateway | 8080 | 骨架 ✅，已配置所有 8 个服务路由 |
| shop-auth | 8081 | 骨架 ✅ |
| shop-user | 8082 | 骨架 ✅ |
| shop-product | 8083 | 骨架 ✅ |
| shop-order | 8084 | 骨架 ✅ |
| shop-cart | 8085 | 骨架 ✅ |
| shop-seckill | 8086 | 骨架 ✅ |
| shop-search | 8087 | 骨架 ✅ |
| shop-mcp-server | 8088 | 骨架 ✅ |
| shop-common-core | — | ✅ Result<T>, ErrorCode, BusinessException, PageResult |
| shop-common-web | — | ✅ GlobalExceptionHandler, RequestIdFilter |

**编译验证**：`mvn compile` 通过（使用 JDK 21 + Lombok 1.18.36）

**重要依赖说明**：
- `shardingsphere-jdbc:5.4.1` 已从 shop-order pom.xml 注释掉（该版本不在 Maven Central），**实现 phase1-order 时需要确认正确的 artifact 坐标**
- 开发环境使用 JDK 21（Eclipse Temurin，安装在 `C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot`）
- Maven 使用 IntelliJ 内置：`D:\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd`

#### 前端（`frontend/`）

- Vue 3 + Vite + TypeScript + Element Plus + Pinia + UnoCSS
- API 层：Axios + JWT 注入 + 单飞 token 刷新 + X-Request-Id
- SSE：`@microsoft/fetch-event-source`（支持 POST + JWT，native EventSource 不支持）
- 11 页面、4 组件、2 布局、路由守卫
- Docker 多阶段镜像，已验证构建成功

#### Docker Compose（`docker-compose.yml`）

| 服务 | 镜像 | 端口 | 状态 |
|------|------|------|------|
| MySQL | mysql:8.0 | **3307**:3306 | ✅ healthy |
| Redis | redis:7-alpine | 6379:6379 | ✅ healthy |
| Nacos | nacos/nacos-server:v2.3.2 | 8848/9848 | ✅ healthy |
| Kafka | **apache/kafka:3.7.0** | 9092/9094 | ✅ healthy |
| Elasticsearch | elasticsearch:8.13.0 | 9200:9200 | ✅ healthy |
| 前端 | 本地构建 (nginx) | 3000:80 | ✅ running |

**注意**：
- MySQL 用 **3307** 端口（本机 3306 已被本地 MySQL 80 占用）
- Kafka 从 bitnami 换为 **apache/kafka:3.7.0**（bitnami 已离开 Docker Hub）
  - env var 前缀从 `KAFKA_CFG_*` 改为 `KAFKA_*`
  - healthcheck 路径：`/opt/kafka/bin/kafka-topics.sh`
  - 需要 `CLUSTER_ID` 环境变量用于 KRaft storage format
- nginx 使用 `resolver 127.0.0.11` + `set $upstream` 变量，前端容器可在网关未启动时正常运行

---

## 二、重要架构决策（编码前必读）

### 数据库

| 决策 | 做法 |
|------|------|
| 手机号唯一索引 | `phone_hash`（HMAC-SHA256 + E.164 归一化）作唯一索引，`phone_ciphertext` 仅用于展示/解密 |
| 软删除 + 唯一索引 | `delete_version BIGINT DEFAULT 0`，唯一索引建在 `(field, delete_version)`，软删除时置 `delete_version = id` |
| 订单分片 | `order` 和 `order_item` 均按 `user_id % 16` 分表（ShardingSphere binding table），`order_item` 必须含 `user_id` 列 |
| 订单 order_no | 格式 `yyyyMMdd + shard_idx + 毫秒 + 随机4位`，使支付回调可通过 `order_no` 直接路由到分片，无需全分片扫描 |
| 订单地址 | 订单表存 `snap_*` 快照字段，防止用户修改/删除地址后历史订单数据丢失 |
| 订单支付期限 | `order.expire_time` 字段存储支付截止时间，定时任务超时自动取消 |
| SKU 唯一性 | `spec_hash`（spec_json MD5）+ 联合唯一索引 `(product_id, spec_hash)` 防重 |

### 业务流程

| 决策 | 做法 |
|------|------|
| 普通下单库存扣减 | 在 `POST /orders` 事务内**同步** CAS 扣减 `product_sku.stock`，Kafka `order-create` 仅用于通知/积分等异步下游，不再承担库存扣减 |
| 订单状态枚举 | TINYINT 0~6：0=PENDING_PAYMENT / 1=PAID / 2=SHIPPED / 3=COMPLETED / 4=CANCELLED / 5=REFUNDING / 6=REFUNDED |
| PAID 状态取消 | 状态变为 `REFUNDING`（不是 `CANCELLED`），等待支付平台退款回调后变为 `REFUNDED` |
| 积分触发点 | **唯一触发点**：`order.completed` 事件；`order-create` 消费者不赠积分 |
| 秒杀幂等 | `orderToken` = Redis Lua 扣减成功瞬间生成的 UUID，持久化到 `seckill:token:{activityId}:{userId}`，订单服务以此幂等键创建订单 |

### Agent / MCP

| 决策 | 做法 |
|------|------|
| MCP 鉴权 | Agent 透传用户原始 JWT via 标准 `Authorization: Bearer <JWT>`，MCP Server 验证并提取 `userId` |
| 高风险工具 | `add_to_cart` / `remove_cart_item` / `cancel_order` 调用前必须向用户二次确认 |
| `clear_cart` | **不暴露为 MCP Tool**（过于危险） |
| SSE 事件格式 | 统一 `{"type":..., "traceId":..., "data":{...}}`，错误码用字符串（区别于 REST 数字码），`done` 事件永远作为终止符 |

---

## 三、API 覆盖（20 个接口）

| 模块 | 接口数 | 特殊说明 |
|------|--------|---------|
| 认证 | 2 | login / refresh，refresh token 每次轮转 |
| 商品 + 搜索 | 2 | 搜索支持 priceMin/priceMax 过滤 |
| 购物车 | 5 | GET / POST / PUT / DELETE item / DELETE cart |
| 订单 | 6 | 含创建/列表/详情/取消/支付/回调；取消有 PAID→REFUNDING 示例 |
| 秒杀 | 2 | submit（Idempotency-Key）/ result（轮询） |
| 用户画像 + 推荐 | 2 | 脱敏，scene 枚举 5 种 |
| Agent 对话 | 1 | SSE 流式，6 种事件类型 |

---

## 四、Phase 1 推荐开始顺序

```
1. phase1-scaffold  → Maven 多模块骨架 + Docker Compose 基础设施（所有模块的前置）✅
2. phase1-web       → 前端 Vue 3 脚手架（Vite + TS + Element Plus + Pinia）✅
3. phase1-infra     → 基础设施验证（mvn compile + docker compose up）✅
4. phase1-user      → 用户注册/登录/JWT（依赖 scaffold）⬅ 下一步
5. phase1-product   → 商品 CRUD/SKU/库存（依赖 scaffold）
6. phase1-cart      → 购物车（依赖 product）
7. phase1-order     → 下单流程/状态机（依赖 cart + product）
8. phase1-db-design → ER 图（可与上面并行）
```

---

## 五、Redis Key 速查

| Key | 用途 | TTL |
|-----|------|-----|
| `product:detail:{productId}` | 商品详情缓存 | 30min ± 随机抖动 |
| `cart:{userId}` | 购物车（Hash） | 活跃期续期 |
| `seckill:stock:{activityId}:{skuId}` | 秒杀库存 | 活动结束清理 |
| `seckill:users:{activityId}` | 用户去重 Set | 活动结束清理 |
| `seckill:token:{activityId}:{userId}` | orderToken 持久化 | 与活动同生命周期 |
| `mq:idempotent:{group}:{eventId}` | Kafka 消费幂等 | 24h |
| `auth:refresh:{userId}:{tokenId}` | Refresh Token | 7天，登出主动删除 |
| `bloom:product:ids` | 商品 ID BloomFilter | 持久化 |

---

## 六、关键文件速查

| 文件 | 说明 |
|------|------|
| `backend/pom.xml` | 根 POM，版本管理（Spring Boot 3.2.4 / JDK 17 source / Lombok 1.18.36） |
| `backend/shop-gateway/.../application.yml` | 网关路由，含 `/api/v1/agent/**` → shop-mcp-server |
| `frontend/src/api/request.ts` | Axios 实例：JWT 注入、单飞 token 刷新、X-Request-Id、错误解包 |
| `frontend/src/composables/useSSE.ts` | SSE composable（@microsoft/fetch-event-source，支持 POST+JWT） |
| `frontend/nginx.conf` | SPA fallback + `/api/` proxy（无尾部斜线）+ SSE 配置 |
| `docker-compose.yml` | 基础设施编排，MySQL 3307，Kafka apache/kafka:3.7.0 |

---

## 七、Git 提交记录（GitHub: qingyelin000/ShopHelper）

| commit | 说明 |
|--------|------|
| cf5d803 | feat: Phase 1 scaffold — backend + frontend skeleton（初始提交，105文件）|
| 7604861 | fix: resolve Maven compile issues on JDK 21（Lombok升级、compiler plugin、ShardingSphere注释）|
| adb45a5 | fix: docker-compose infrastructure corrections（Kafka换apache镜像、MySQL改3307）|
| 600be82 | fix: nginx upstream lazy-resolve for Docker startup order |


---

## 一、交付物清单

| 文件 | 用途 | 状态 |
|------|------|------|
| `docs/project-design.md` | 主架构文档：技术选型、项目结构、数据库设计、高并发设计、Agent 架构、API 规范（v0.2） | ✅ |
| `docs/schema.sql` | 9 张 MySQL 8.0 核心表 DDL | ✅ |
| `docs/api-contract.md` | 20 个 REST 接口完整 JSON 请求/响应示例 | ✅ |
| `docs/mcp-tool-schema.md` | 9 个 MCP Tool 的 JSON Schema 定义及 Agent 使用规则 | ✅ |
| `docs/kafka-contract.md` | 3 个 Kafka Topic 完整事件契约（Envelope、幂等、死信） | ✅ |

---

## 二、重要架构决策（编码前必读）

### 数据库

| 决策 | 做法 |
|------|------|
| 手机号唯一索引 | `phone_hash`（HMAC-SHA256 + E.164 归一化）作唯一索引，`phone_ciphertext` 仅用于展示/解密 |
| 软删除 + 唯一索引 | `delete_version BIGINT DEFAULT 0`，唯一索引建在 `(field, delete_version)`，软删除时置 `delete_version = id` |
| 订单分片 | `order` 和 `order_item` 均按 `user_id % 16` 分表（ShardingSphere binding table），`order_item` 必须含 `user_id` 列 |
| 订单 order_no | 格式 `yyyyMMdd + shard_idx + 毫秒 + 随机4位`，使支付回调可通过 `order_no` 直接路由到分片，无需全分片扫描 |
| 订单地址 | 订单表存 `snap_*` 快照字段，防止用户修改/删除地址后历史订单数据丢失 |
| 订单支付期限 | `order.expire_time` 字段存储支付截止时间，定时任务超时自动取消 |
| SKU 唯一性 | `spec_hash`（spec_json MD5）+ 联合唯一索引 `(product_id, spec_hash)` 防重 |

### 业务流程

| 决策 | 做法 |
|------|------|
| 普通下单库存扣减 | 在 `POST /orders` 事务内**同步** CAS 扣减 `product_sku.stock`，Kafka `order-create` 仅用于通知/积分等异步下游，不再承担库存扣减 |
| 订单状态枚举 | TINYINT 0~6：0=PENDING_PAYMENT / 1=PAID / 2=SHIPPED / 3=COMPLETED / 4=CANCELLED / 5=REFUNDING / 6=REFUNDED |
| PAID 状态取消 | 状态变为 `REFUNDING`（不是 `CANCELLED`），等待支付平台退款回调后变为 `REFUNDED` |
| 积分触发点 | **唯一触发点**：`order.completed` 事件；`order-create` 消费者不赠积分 |
| 秒杀幂等 | `orderToken` = Redis Lua 扣减成功瞬间生成的 UUID，持久化到 `seckill:token:{activityId}:{userId}`，订单服务以此幂等键创建订单 |

### Agent / MCP

| 决策 | 做法 |
|------|------|
| MCP 鉴权 | Agent 透传用户原始 JWT via 标准 `Authorization: Bearer <JWT>`，MCP Server 验证并提取 `userId` |
| 高风险工具 | `add_to_cart` / `remove_cart_item` / `cancel_order` 调用前必须向用户二次确认 |
| `clear_cart` | **不暴露为 MCP Tool**（过于危险） |
| SSE 事件格式 | 统一 `{"type":..., "traceId":..., "data":{...}}`，错误码用字符串（区别于 REST 数字码），`done` 事件永远作为终止符 |

---

## 三、API 覆盖（20 个接口）

| 模块 | 接口数 | 特殊说明 |
|------|--------|---------|
| 认证 | 2 | login / refresh，refresh token 每次轮转 |
| 商品 + 搜索 | 2 | 搜索支持 priceMin/priceMax 过滤 |
| 购物车 | 5 | GET / POST / PUT / DELETE item / DELETE cart |
| 订单 | 6 | 含创建/列表/详情/取消/支付/回调；取消有 PAID→REFUNDING 示例 |
| 秒杀 | 2 | submit（Idempotency-Key）/ result（轮询） |
| 用户画像 + 推荐 | 2 | 脱敏，scene 枚举 5 种 |
| Agent 对话 | 1 | SSE 流式，6 种事件类型 |

---

## 四、Phase 1 推荐开始顺序

```
1. phase1-scaffold  → Maven 多模块骨架 + Docker Compose 基础设施（所有模块的前置）✅
2. phase1-web       → 前端 Vue 3 脚手架（Vite + TS + Element Plus + Pinia）✅
3. phase1-user      → 用户注册/登录/JWT（依赖 scaffold）
4. phase1-product   → 商品 CRUD/SKU/库存（依赖 scaffold）
5. phase1-cart      → 购物车（依赖 product）
6. phase1-order     → 下单流程/状态机（依赖 cart + product）
7. phase1-db-design → ER 图（可与上面并行）
```

---

## 五、Redis Key 速查

| Key | 用途 | TTL |
|-----|------|-----|
| `product:detail:{productId}` | 商品详情缓存 | 30min ± 随机抖动 |
| `cart:{userId}` | 购物车（Hash） | 活跃期续期 |
| `seckill:stock:{activityId}:{skuId}` | 秒杀库存 | 活动结束清理 |
| `seckill:users:{activityId}` | 用户去重 Set | 活动结束清理 |
| `seckill:token:{activityId}:{userId}` | orderToken 持久化 | 与活动同生命周期 |
| `mq:idempotent:{group}:{eventId}` | Kafka 消费幂等 | 24h |
| `auth:refresh:{userId}:{tokenId}` | Refresh Token | 7天，登出主动删除 |
| `bloom:product:ids` | 商品 ID BloomFilter | 持久化 |

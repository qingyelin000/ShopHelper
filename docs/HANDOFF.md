# ShopHelper Phase 0 交付说明

> **状态**：Phase 0「契约优先」文档阶段全部完成，可以开始 Phase 1 编码。  
> **时间**：2026-04-21

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

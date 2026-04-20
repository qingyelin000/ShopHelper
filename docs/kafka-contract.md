# ShopHelper Kafka 事件契约文档

> **版本**：v0.1 | **创建时间**：2026-04-21  
>
> 本文档定义所有 Kafka Topic 的事件类型、完整 payload 结构、生产者 / 消费者归属、幂等策略与错误处理。

---

## 一、全局约定

### 1.1 事件 Envelope（统一封装）

所有 Kafka 消息 payload 的外层结构统一如下：

```json
{
  "eventId":    "evt_20260421_000001",
  "eventType":  "order.created",
  "version":    "1.0",
  "occurredAt": "2026-04-21T10:00:00+08:00",
  "traceId":    "5c9c1984f9db438ca9a4c75e1f5d6ee2",
  "bizKey":     "orderNo_20260421000001",
  "payload":    {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `eventId` | string | 全局唯一事件 ID，格式 `evt_{yyyyMMdd}_{sequence}`，用于幂等去重 |
| `eventType` | string | 事件类型，格式 `{domain}.{action}`（全小写点分） |
| `version` | string | 事件结构版本，破坏性变更时升级（如 `2.0`） |
| `occurredAt` | string | 事件发生时间（ISO 8601，含时区） |
| `traceId` | string | 分布式链路追踪 ID，透传至所有消费端日志 |
| `bizKey` | string | 业务主键，格式 `{bizType}_{bizId}`，用于幂等和死信重放路由 |
| `payload` | object | 事件具体数据（各 Topic 独立定义） |

### 1.2 幂等策略

消费端**必须**以 `eventId + bizKey` 的组合做幂等，实现方式：

```
Redis SET: mq:idempotent:{consumerGroup}:{eventId}
  - 值：处理结果（"success" / "failed"）
  - TTL：24 小时
  - 操作：SETNX（原子操作，成功才处理，失败说明已处理过）
```

### 1.3 错误处理原则

| 情况 | 策略 |
|------|------|
| 网络抖动 / 临时错误 | 自动重试（指数退避，最多 3 次） |
| 业务异常（如订单已存在） | 幂等直接返回成功，不重试 |
| 不可恢复错误 | 投递至死信 Topic `{topic}.dlq`，人工介入 |
| 消费超时 | 记录日志，发 alert，等待下次 rebalance 后重新消费 |

### 1.4 Topic 配置建议

| Topic | 分区数 | 副本数 | 保留时间 |
|-------|--------|--------|---------|
| `order-create` | 8 | 2 | 7 天 |
| `seckill-order-create` | 16 | 2 | 3 天 |
| `order-notify` | 4 | 2 | 7 天 |
| `*.dlq`（死信） | 1 | 2 | 30 天 |

---

## 二、Topic 定义

---

### 📨 Topic: `order-create`

**用途**：普通下单流程中，订单服务创建完订单记录后异步通知库存服务扣减 DB 库存，并触发通知和积分等下游。

**生产者**：`shop-order`（订单服务，在事务提交后发送）

**消费者**：

| 消费者组 | 服务 | 消费动作 |
|---------|------|---------|
| `notify-group` | `shop-notify`（预留） | 发送下单成功短信/Push |
| `points-group` | `shop-points`（预留） | 赠送积分（仅用于特殊活动下单即送场景，正常场景积分在 order.completed 时才赠送） |

> **注意**：普通下单的库存扣减在 `POST /orders` 接口的数据库事务内**同步完成**，不依赖 Kafka 消费实现。Kafka 仅用于下游通知与积分等异步处理。

**事件类型**：`order.created`

**完整 Payload：**

```json
{
  "eventId":    "evt_20260421_000001",
  "eventType":  "order.created",
  "version":    "1.0",
  "occurredAt": "2026-04-21T10:00:00+08:00",
  "traceId":    "5c9c1984f9db438ca9a4c75e1f5d6ee2",
  "bizKey":     "orderNo_20260421000001",
  "payload": {
    "orderId":    "580000000000000001",
    "orderNo":    "20260421000001",
    "userId":     "200000000000000001",
    "payAmount":  398.00,
    "source":     "app",
    "items": [
      {
        "skuId":      "490000000000000001",
        "productId":  "390000000000000001",
        "quantity":   2,
        "unitPrice":  199.00
      }
    ],
    "createTime": "2026-04-21T10:00:00+08:00"
  }
}
```

**消费约束：**
- 库存服务消费此事件时，执行 `UPDATE product_sku SET stock = stock - quantity WHERE id = skuId AND stock >= quantity`
- 若扣减失败（超卖），记录补偿日志，不抛异常（Redis 已完成预扣，理论上不应发生，但需有日志）
- 消费幂等：以 `mq:idempotent:stock-deduct-group:{eventId}` 去重

---

### ⚡ Topic: `seckill-order-create`

**用途**：秒杀场景下，Redis 扣库存成功后异步创建订单，要求超低延迟，失败后可补偿重放。

**生产者**：`shop-seckill`（秒杀服务，Lua 脚本扣减成功后立即发送）

**消费者**：

| 消费者组 | 服务 | 消费动作 |
|---------|------|---------|
| `seckill-order-group` | `shop-order` | 创建正式订单记录（以 orderToken 作幂等键） |

**事件类型**：`seckill.order.created`

**完整 Payload：**

```json
{
  "eventId":    "evt_20260421_000002",
  "eventType":  "seckill.order.created",
  "version":    "1.0",
  "occurredAt": "2026-04-21T12:00:00.050+08:00",
  "traceId":    "a1b2c3d4e5f64a5b9c1d2e3f4a5b6c7d",
  "bizKey":     "orderToken_550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "activityId":   "700000000000000001",
    "skuId":        "490000000000000001",
    "productId":    "390000000000000001",
    "userId":       "200000000000000001",
    "addressId":    "190000000000000001",
    "seckillPrice": 99.00,
    "quantity":     1,
    "orderToken":   "550e8400-e29b-41d4-a716-446655440000",
    "occurredAt":   "2026-04-21T12:00:00.050+08:00",
    "snapProductName":  "2026春季新款修身连衣裙",
    "snapProductImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
    "snapSkuCode":      "DRESS-RED-M",
    "snapSkuSpecJson":  {"颜色": "红色", "尺寸": "M"}
  }
}
```

**`orderToken` 说明：**
- 秒杀服务在 Redis Lua 脚本扣减成功的瞬间生成，格式为 UUID v4
- 同步持久化到 Redis（Key: `seckill:token:{activityId}:{userId}`，TTL 与活动等长）
- 订单服务以 `orderToken` 为幂等键创建订单，重试时复用相同 Token，确保只创建一次订单

**消费约束：**
- 订单服务收到此事件后，以 `orderToken` 作幂等键创建订单
- 若 `orderToken` 对应的订单已存在（重复消费），直接返回成功，不重复创建
- 若地址 / 商品不存在等不可恢复错误，投递至 `seckill-order-create.dlq`，并异步回滚 Redis 库存

---

### 📢 Topic: `order-notify`

**用途**：订单状态发生变更时（支付成功、发货、完成、取消、退款），通知下游服务（通知服务、积分服务等）。

**生产者**：`shop-order`（订单服务，在每次状态变更后发送）

**消费者**：

| 消费者组 | 服务 | 监听事件类型 | 消费动作 |
|---------|------|------------|---------|
| `notify-status-group` | `shop-notify`（预留） | 所有 | 发送状态变更通知（短信/Push） |
| `points-complete-group` | `shop-points`（预留） | `order.completed` | 订单完成后赠送积分（唯一积分触发点，避免与 order-create 消费者重复） |
| `refund-group` | `shop-refund`（预留） | `order.cancel_and_refund` | 发起退款到支付平台 |

**事件类型枚举**：

| `eventType` | 触发时机 |
|------------|---------|
| `order.paid` | 支付平台回调支付成功，订单状态变为 PAID |
| `order.shipped` | 运营后台标记发货 |
| `order.completed` | 用户确认收货 / 自动确认 |
| `order.cancelled` | 用户主动取消（PENDING_PAYMENT 状态） |
| `order.cancel_and_refund` | 用户取消（PAID 状态，需退款） |
| `order.refunded` | 退款到账，订单状态变为 REFUNDED |

**完整 Payload 示例（`order.paid`）：**

```json
{
  "eventId":    "evt_20260421_000003",
  "eventType":  "order.paid",
  "version":    "1.0",
  "occurredAt": "2026-04-21T10:05:30+08:00",
  "traceId":    "b2c3d4e5f60a1b2c3d4e5f6a1b2c3d4e",
  "bizKey":     "orderNo_20260421000001",
  "payload": {
    "orderId":      "580000000000000001",
    "orderNo":      "20260421000001",
    "userId":       "200000000000000001",
    "orderStatus":  "PAID",
    "payAmount":    398.00,
    "payType":      "ALIPAY",
    "changedAt":    "2026-04-21T10:05:30+08:00"
  }
}
```

**完整 Payload 示例（`order.cancel_and_refund`）：**

```json
{
  "eventId":    "evt_20260421_000004",
  "eventType":  "order.cancel_and_refund",
  "version":    "1.0",
  "occurredAt": "2026-04-21T10:10:00+08:00",
  "traceId":    "c3d4e5f60a1b2c3d4e5f6a1b2c3d4e5f",
  "bizKey":     "orderNo_20260421000001",
  "payload": {
    "orderId":        "580000000000000001",
    "orderNo":        "20260421000001",
    "userId":         "200000000000000001",
    "orderStatus":    "REFUNDING",
    "refundAmount":   398.00,
    "cancelReason":   "不想要了",
    "originalPayType": "ALIPAY",
    "changedAt":      "2026-04-21T10:10:00+08:00"
  }
}
```

**消费约束：**
- 各消费者组只消费自己关心的 `eventType`，通过消费端过滤（不在 Kafka 层过滤）
- 积分消费组只在 `order.completed` 时赠送积分，`order.cancelled` 后不补
- 退款消费组收到 `order.cancel_and_refund` 后调用支付平台退款接口，完成后发布 `order.refunded` 事件

---

## 三、死信处理

所有 Topic 对应的死信 Topic 命名：`{topic}.dlq`

| 死信 Topic | 来源 | 处理要求 |
|-----------|------|---------|
| `order-create.dlq` | 消费失败 3 次 | 人工审核，确认库存状态，手动补偿或丢弃 |
| `seckill-order-create.dlq` | 消费失败 3 次 | 优先回滚 Redis 库存，再审核订单状态 |
| `order-notify.dlq` | 消费失败 3 次 | 人工补发通知，积分/退款需确认是否已执行 |

死信消息保留 30 天，需有运维告警（消息积压 > 0 即报警）。

---

## 四、扩展事件（Phase 2 预留）

以下事件暂不实现，占位记录，避免后期 Topic 命名冲突：

| Topic | 事件类型 | 用途 |
|-------|---------|------|
| `inventory-alert` | `inventory.low_stock` | 库存低于阈值时告警 |
| `user-behavior` | `user.product_viewed` | 用户浏览行为，供推荐服务消费 |
| `user-behavior` | `user.search_queried` | 用户搜索行为记录 |

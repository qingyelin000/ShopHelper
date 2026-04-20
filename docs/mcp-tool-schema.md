# ShopHelper MCP Tool 契约文档

> **版本**：v0.1 | **创建时间**：2026-04-21  
>
> 本文档定义 `shop-mcp-server` 模块暴露的全部 MCP Tool 的 JSON Schema、输出结构与使用约束。  
> LangGraph Agent 通过 MCP 协议调用这些工具。

---

## 一、全局约定

### 1.1 鉴权

所有工具调用必须在 MCP 连接建立时携带用户 JWT：

```
Authorization: Bearer <user_JWT>
```

MCP Server 在每次工具调用前验证 JWT 有效性，并从中提取 `userId` 供用户身份相关工具使用。

### 1.2 统一返回结构

所有工具返回 JSON，格式如下：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "traceId": "5c9c1984f9db438ca9a4c75e1f5d6ee2"
}
```

**失败时：**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "商品不存在或已下架"
  },
  "traceId": "5c9c1984f9db438ca9a4c75e1f5d6ee2"
}
```

### 1.3 错误码（字符串，与 REST 数字错误码区分）

| 错误码 | 含义 |
|--------|------|
| `PRODUCT_NOT_FOUND` | 商品不存在或已下架 |
| `SKU_NOT_FOUND` | SKU 不存在 |
| `STOCK_INSUFFICIENT` | 库存不足 |
| `CART_ITEM_NOT_FOUND` | 购物车项不存在 |
| `ORDER_NOT_FOUND` | 订单不存在 |
| `ORDER_STATUS_NOT_ALLOWED` | 订单状态不允许此操作 |
| `INVALID_PARAM` | 参数非法 |
| `DOWNSTREAM_ERROR` | 下游 Java 服务调用失败 |
| `UNAUTHORIZED` | JWT 无效或已过期 |

### 1.4 工具清单总览

| 工具名 | 用途 | 需要用户身份 |
|--------|------|------------|
| `search_products` | 语义搜索商品 | 否（但需有效 JWT） |
| `get_product_detail` | 获取商品详情与 SKU | 否 |
| `get_cart` | 获取当前用户购物车 | ✅ |
| `add_to_cart` | 加入购物车 | ✅ |
| `remove_cart_item` | 删除购物车单项 | ✅ |
| `get_orders` | 获取用户订单列表 | ✅ |
| `cancel_order` | 取消订单 | ✅ |
| `get_user_profile` | 获取用户偏好/历史 | ✅ |
| `get_recommendations` | 获取推荐商品列表 | ✅ |

---

## 二、工具定义

---

### 🔍 `search_products`

**用途**：对用户自然语言查询进行语义理解，通过 RAG（Elasticsearch kNN）检索最相关商品，是导购 Agent 的核心工具。

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "keyword": {
      "type": "string",
      "description": "用户查询关键词（支持自然语言，如"适合春天穿的红色连衣裙"）",
      "minLength": 1,
      "maxLength": 200
    },
    "topK": {
      "type": "integer",
      "description": "返回商品数量，默认 10",
      "default": 10,
      "minimum": 1,
      "maximum": 50
    },
    "categoryId": {
      "type": "string",
      "description": "（可选）限定分类ID，不传则全品类搜索"
    },
    "priceMin": {
      "type": "number",
      "description": "（可选）最低价格（元），不传则不限",
      "minimum": 0
    },
    "priceMax": {
      "type": "number",
      "description": "（可选）最高价格（元），不传则不限",
      "minimum": 0
    }
  },
  "required": ["keyword"]
}
```

**输出示例：**

```json
{
  "success": true,
  "data": {
    "products": [
      {
        "productId": "390000000000000001",
        "name": "2026春季新款修身连衣裙",
        "categoryName": "女装",
        "mainImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
        "price": 199.00,
        "salesCount": 2380,
        "recallScore": 0.92,
        "availableSkus": [
          {
            "skuId": "490000000000000001",
            "spec": {"颜色": "红色", "尺寸": "M"},
            "price": 199.00,
            "stock": 85
          }
        ]
      }
    ],
    "total": 1,
    "keyword": "适合春天穿的红色连衣裙"
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

**约束：**
- 只返回 `status=1`（上架）的商品
- `availableSkus` 只包含 `stock > 0` 的 SKU
- `recallScore` 为 ES kNN 召回分数（0~1），Agent 可据此排序
- 若无匹配结果，`products` 返回空数组，`total` 为 0，不返回错误

---

### 📦 `get_product_detail`

**用途**：获取指定商品的完整信息（含所有 SKU、库存摘要），用于向用户展示商品详情或确认下单前的商品信息。

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "productId": {
      "type": "string",
      "description": "商品ID（Snowflake ID 字符串）"
    }
  },
  "required": ["productId"]
}
```

**输出示例：**

```json
{
  "success": true,
  "data": {
    "productId": "390000000000000001",
    "name": "2026春季新款修身连衣裙",
    "subTitle": "显瘦百搭，多色可选",
    "categoryId": "100000000000000002",
    "categoryName": "女装",
    "mainImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
    "price": 199.00,
    "salesCount": 2380,
    "skuList": [
      {
        "skuId": "490000000000000001",
        "skuCode": "DRESS-RED-M",
        "spec": {"颜色": "红色", "尺寸": "M"},
        "price": 199.00,
        "stock": 85,
        "available": true
      },
      {
        "skuId": "490000000000000003",
        "skuCode": "DRESS-BLUE-M",
        "spec": {"颜色": "蓝色", "尺寸": "M"},
        "price": 219.00,
        "stock": 0,
        "available": false
      }
    ]
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

**约束：**
- 只返回 `status=1`（上架）的商品，否则返回 `PRODUCT_NOT_FOUND` 错误
- `description`（富文本）不在此工具返回，避免 context 过长

---

### 🛒 `get_cart`

**用途**：获取当前登录用户的购物车，用于了解用户已有商品，避免重复推荐，或在订单助手场景下核对商品。

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {},
  "required": []
}
```

> 无参数，用户身份从 JWT 中解析

**输出示例：**

```json
{
  "success": true,
  "data": {
    "userId": "200000000000000001",
    "items": [
      {
        "itemId": "cart_item_001",
        "productId": "390000000000000001",
        "productName": "2026春季新款修身连衣裙",
        "productImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
        "skuId": "490000000000000001",
        "skuSpec": {"颜色": "红色", "尺寸": "M"},
        "unitPrice": 199.00,
        "quantity": 2,
        "totalPrice": 398.00,
        "stock": 85,
        "selected": true,
        "isAvailable": true
      }
    ],
    "totalQuantity": 2,
    "estimatedTotal": 398.00
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

---

### ➕ `add_to_cart`

**用途**：将指定 SKU 加入当前用户购物车，适用于导购 Agent 在获得用户明确同意后操作。  
**⚠️ Agent 使用前必须向用户二次确认。**

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "skuId": {
      "type": "string",
      "description": "要加入购物车的 SKU ID"
    },
    "quantity": {
      "type": "integer",
      "description": "加购数量，默认 1",
      "default": 1,
      "minimum": 1,
      "maximum": 99
    }
  },
  "required": ["skuId"]
}
```

**输出示例（成功）：**

```json
{
  "success": true,
  "data": {
    "message": "已成功加入购物车",
    "cartSnapshot": {
      "totalQuantity": 3,
      "estimatedTotal": 597.00
    }
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

**输出示例（库存不足）：**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "STOCK_INSUFFICIENT",
    "message": "库存不足，当前可购数量为 3"
  },
  "traceId": "5c9c1984f9db438c"
}
```

---

### 🗑️ `remove_cart_item`

**用途**：删除当前用户购物车中的指定商品项，适用于订单助手帮助用户整理购物车。  
**⚠️ Agent 使用前必须向用户二次确认。**

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "itemId": {
      "type": "string",
      "description": "购物车项 ID（从 get_cart 工具返回的 itemId 字段获取）"
    }
  },
  "required": ["itemId"]
}
```

**输出示例：**

```json
{
  "success": true,
  "data": {
    "message": "商品已从购物车移除",
    "cartSnapshot": {
      "totalQuantity": 1,
      "estimatedTotal": 199.00
    }
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

---

### 📋 `get_orders`

**用途**：获取当前用户的订单列表，用于订单助手查询订单状态、物流等。

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "pageNum": {
      "type": "integer",
      "description": "页码，默认 1",
      "default": 1,
      "minimum": 1
    },
    "pageSize": {
      "type": "integer",
      "description": "每页条数，默认 5，最大 20",
      "default": 5,
      "minimum": 1,
      "maximum": 20
    },
    "status": {
      "type": "string",
      "description": "（可选）筛选订单状态",
      "enum": ["PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED", "REFUNDING", "REFUNDED"]
    }
  },
  "required": []
}
```

**输出示例：**

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "orderId": "580000000000000001",
        "orderNo": "20260421000001",
        "orderStatus": "PAID",
        "payAmount": 398.00,
        "itemCount": 1,
        "coverImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
        "productNames": ["2026春季新款修身连衣裙"],
        "createTime": "2026-04-21T10:00:00+08:00",
        "payTime": "2026-04-21T10:05:30+08:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 5,
    "hasNext": false
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

---

### ❌ `cancel_order`

**用途**：取消当前用户的指定订单，仅由订单助手 Agent 在获得用户明确授权后调用。  
**⚠️ 高风险操作，Agent 必须向用户二次确认才能调用。**

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "orderId": {
      "type": "string",
      "description": "要取消的订单 ID"
    },
    "reason": {
      "type": "string",
      "description": "取消原因（用户说明）",
      "maxLength": 256
    }
  },
  "required": ["orderId", "reason"]
}
```

**输出示例（成功）：**

```json
{
  "success": true,
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "orderStatus": "CANCELLED",
    "cancelTime": "2026-04-21T10:10:00+08:00",
    "refundNote": null
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

**输出示例（PAID 状态取消，触发退款）：**

```json
{
  "success": true,
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "orderStatus": "REFUNDING",
    "cancelTime": "2026-04-21T10:10:00+08:00",
    "refundNote": "订单已取消，退款预计 3~5 个工作日到账"
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

**输出示例（状态不允许）：**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ORDER_STATUS_NOT_ALLOWED",
    "message": "订单已发货，无法取消，请申请退换货"
  },
  "traceId": "5c9c1984f9db438c"
}
```

---

### 👤 `get_user_profile`

**用途**：获取当前用户的消费偏好和历史行为摘要，帮助导购 Agent 提供个性化推荐。

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {},
  "required": []
}
```

> 无参数，用户身份从 JWT 中解析

**输出示例：**

```json
{
  "success": true,
  "data": {
    "userId": "200000000000000001",
    "preferredCategories": [
      {"categoryId": "100000000000000002", "categoryName": "女装", "weight": 0.72},
      {"categoryId": "100000000000000005", "categoryName": "运动户外", "weight": 0.18}
    ],
    "priceBandPreference": {
      "min": 50,
      "max": 300,
      "typical": 150
    },
    "recentBrowseCount": 38,
    "purchaseCount": 12
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

**约束：**
- 绝对不返回手机号、邮箱、地址等敏感原始信息
- `weight` 表示该类目的兴趣权重，合计为 1.0

---

### 🎯 `get_recommendations`

**用途**：根据当前场景和用户历史，返回个性化推荐商品列表，带可追溯的推荐标签。

**输入 JSON Schema：**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "scene": {
      "type": "string",
      "description": "推荐场景，决定推荐算法策略",
      "enum": ["homepage", "product_detail", "cart", "checkout", "category"]
    },
    "topK": {
      "type": "integer",
      "description": "推荐商品数量，默认 6",
      "default": 6,
      "minimum": 1,
      "maximum": 20
    }
  },
  "required": ["scene"]
}
```

**输出示例：**

```json
{
  "success": true,
  "data": {
    "scene": "homepage",
    "algorithm": "collaborative_filtering_v2",
    "list": [
      {
        "productId": "390000000000000002",
        "name": "韩版宽松卫衣外套",
        "mainImage": "https://cdn.shophelper.com/products/yyy/main.jpg",
        "price": 159.00,
        "salesCount": 5620,
        "recommendTag": "你常购的品类",
        "score": 0.91
      }
    ]
  },
  "error": null,
  "traceId": "5c9c1984f9db438c"
}
```

**约束：**
- `recommendTag` 必须有实际依据（不能是"猜你喜欢"等无意义标签），需与推荐理由对应
- `score` 为推荐置信分（0~1），Agent 可据此决定是否展示

---

## 三、Agent 使用规则

### 3.1 高风险工具（操作型）

以下工具会修改用户数据，**Agent 在调用前必须向用户明确确认**：

| 工具 | 确认话术示例 |
|------|------------|
| `add_to_cart` | "要帮您把「XX 连衣裙（红色/M）」加入购物车吗？" |
| `remove_cart_item` | "确认要从购物车删除「XX 商品」吗？" |
| `cancel_order` | "确认要取消订单「20260421000001」吗？取消后无法恢复。" |

### 3.2 工具调用顺序建议

**导购 Agent 典型调用链：**
```
get_user_profile → search_products → get_cart（避免推荐重复）→ 生成回复
```

**订单助手 Agent 典型调用链：**
```
get_orders → （用户指定订单后）cancel_order 或 展示详情
```

### 3.3 禁止行为

- 禁止 Agent 伪造工具调用结果（所有商品/订单事实必须来自真实工具返回）
- 禁止在用户未确认的情况下调用 `add_to_cart`、`remove_cart_item`、`cancel_order`
- 禁止 Agent 将工具返回的用户个人信息（价格偏好、历史类目等）直接完整地朗读给用户

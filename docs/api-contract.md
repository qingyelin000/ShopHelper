# ShopHelper API 契约文档

> **版本**：v0.4 | **更新时间**：2026-04-21  
> 本文档覆盖当前已落地核心 REST API 的请求 / 响应示例，是各服务模块开发与联调的编码基准。
>
> **阅读说明**：
> - 所有请求示例默认已带 `Authorization: Bearer <JWT>` 头（无特殊标注时）
> - 响应 `data` 字段为业务数据，`code` 为业务状态码（非 HTTP 状态码）
> - 时间字段统一使用 ISO 8601 字符串（如 `2026-04-21T10:00:00+08:00`）
> - 金额单位：元（保留两位小数）

---

## 一、通用格式

### 1.1 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "requestId": "9f0d6d3cb1f44a4d8f33f0e4f3e2c001",
  "timestamp": 1745162400000
}
```

### 1.2 错误响应

```json
{
  "code": 40001,
  "message": "参数校验失败：quantity 必须大于 0",
  "data": null,
  "requestId": "9f0d6d3cb1f44a4d8f33f0e4f3e2c002",
  "timestamp": 1745162400000
}
```

### 1.3 分页响应（data 字段结构）

```json
{
  "list": [],
  "total": 150,
  "pageNum": 1,
  "pageSize": 20,
  "hasNext": true
}
```

### 1.4 错误码速查

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

---

## 二、认证服务 `/api/v1/auth`

### 2.1 用户登录

`POST /api/v1/auth/login`  
**无需 JWT**

**请求：**

```json
{
  "loginType": "phone",
  "principal": "13800138000",
  "password": "MyPass@2026"
}
```

> `loginType` 枚举：`phone | email | username`  
> `principal` 对应登录方式的账号标识

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.xxx.yyy",
    "refreshToken": "rt_9f0d6d3c_20260421",
    "expiresIn": 7200,
    "tokenType": "Bearer"
  },
  "requestId": "aaa111",
  "timestamp": 1745162400000
}
```

**错误示例（密码错误）：**

```json
{
  "code": 40001,
  "message": "账号或密码错误",
  "data": null,
  "requestId": "aaa112",
  "timestamp": 1745162400001
}
```

---

### 2.2 刷新 Token

`POST /api/v1/auth/refresh`  
**无需 JWT**

**请求：**

```json
{
  "refreshToken": "rt_9f0d6d3c_20260421"
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.new.token",
    "refreshToken": "rt_b8c9e4d2_20260421",
    "expiresIn": 7200,
    "tokenType": "Bearer"
  },
  "requestId": "bbb111",
  "timestamp": 1745162401000
}
```

> Refresh Token 每次刷新后轮转（旧 Token 立即失效）

---

### 2.3 退出登录

`POST /api/v1/auth/logout`  
**需要 JWT**

**请求（无请求体）：**

```http
POST /api/v1/auth/logout
Authorization: Bearer <JWT>
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "requestId": "logout111",
  "timestamp": 1745162401003
}
```

> 服务端会主动撤销该用户当前所有仍有效的 Refresh Token；已签发的 Access Token 不做黑名单拦截，待自然过期

---

## 二B、用户服务 `/api/v1/users`

### 2B.1 用户注册

`POST /api/v1/users/register`  
**无需 JWT**

**请求：**

```json
{
  "phone": "13800138000",
  "password": "MyPass@2026",
  "nickname": "小林"
}
```

> `nickname` 当前落到用户表 `username` 字段，作为登录展示名与用户名使用
> 新注册用户角色固定为 `USER`；管理员账号需通过受控初始化（Bootstrap Token）或数据库变更授予 `ADMIN`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "requestId": "reg111",
  "timestamp": 1745162401001
}
```

**错误示例（手机号已注册）：**

```json
{
  "code": 40001,
  "message": "手机号已注册",
  "data": null,
  "requestId": "reg112",
  "timestamp": 1745162401002
}
```

---

### 2B.1A 初始化首个管理员

`POST /api/v1/users/bootstrap/admin`  
**无需 JWT**

> 请求头必须携带：`X-Bootstrap-Token: <SHOP_ADMIN_BOOTSTRAP_TOKEN>`  
> 该接口仅在系统内还没有可用管理员时可调用一次。

**请求：**

```json
{
  "username": "小林"
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "190000000000000001",
    "username": "小林",
    "role": "ADMIN",
    "status": 1,
    "createTime": "2026-04-21T10:00:00+08:00",
    "updateTime": "2026-04-22T09:30:00+08:00"
  },
  "requestId": "bootstrap111",
  "timestamp": 1745162401003
}
```

**错误示例（系统已有管理员）：**

```json
{
  "code": 40301,
  "message": "系统中已存在管理员，不能再次初始化",
  "data": null,
  "requestId": "bootstrap112",
  "timestamp": 1745162401004
}
```

---

### 2B.1B 管理员用户列表

`GET /api/v1/users/admin?keyword=lin&role=ADMIN&status=1&pageNum=1&pageSize=20`  
**必须携带管理员 JWT**

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "userId": "190000000000000001",
        "username": "小林",
        "role": "ADMIN",
        "status": 1,
        "createTime": "2026-04-21T10:00:00+08:00",
        "updateTime": "2026-04-22T09:30:00+08:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 20,
    "hasNext": false
  },
  "requestId": "userAdmin111",
  "timestamp": 1745162401005
}
```

---

### 2B.1C 修改用户角色

`PUT /api/v1/users/admin/{userId}/role`  
**必须携带管理员 JWT**

**请求：**

```json
{
  "role": "ADMIN"
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "190000000000000002",
    "username": "运营同学",
    "role": "ADMIN",
    "status": 1,
    "createTime": "2026-04-21T12:00:00+08:00",
    "updateTime": "2026-04-22T09:35:00+08:00"
  },
  "requestId": "userAdmin112",
  "timestamp": 1745162401006
}
```

**错误示例（尝试降级最后一个可用管理员）：**

```json
{
  "code": 40001,
  "message": "系统至少需要保留一个可用管理员",
  "data": null,
  "requestId": "userAdmin113",
  "timestamp": 1745162401007
}
```

---

### 2B.2 获取当前用户地址列表

`GET /api/v1/users/me/addresses`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "190000000000000001",
      "receiverName": "林小青",
      "receiverPhone": "138****8000",
      "province": "广东省",
      "city": "深圳市",
      "district": "南山区",
      "detailAddress": "科技园科苑路 15 号",
      "postalCode": "518057",
      "isDefault": true
    }
  ],
  "requestId": "addr111",
  "timestamp": 1745162401004
}
```

---

### 2B.3 新增收货地址

`POST /api/v1/users/me/addresses`

**请求：**

```json
{
  "receiverName": "林小青",
  "receiverPhone": "13800138000",
  "province": "广东省",
  "city": "深圳市",
  "district": "南山区",
  "detailAddress": "科技园科苑路 15 号",
  "postalCode": "518057",
  "isDefault": true
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "190000000000000001",
    "receiverName": "林小青",
    "receiverPhone": "138****8000",
    "province": "广东省",
    "city": "深圳市",
    "district": "南山区",
    "detailAddress": "科技园科苑路 15 号",
    "postalCode": "518057",
    "isDefault": true
  },
  "requestId": "addr112",
  "timestamp": 1745162401005
}
```

> 当用户还没有任何地址时，首条地址会自动设为默认地址

---

### 2B.4 修改收货地址

`PUT /api/v1/users/me/addresses/{addressId}`

**请求：**

```json
{
  "receiverName": "林小青",
  "receiverPhone": "13800138001",
  "province": "广东省",
  "city": "深圳市",
  "district": "南山区",
  "detailAddress": "粤海街道高新南一道 8 号",
  "postalCode": "518057"
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "190000000000000001",
    "receiverName": "林小青",
    "receiverPhone": "138****8001",
    "province": "广东省",
    "city": "深圳市",
    "district": "南山区",
    "detailAddress": "粤海街道高新南一道 8 号",
    "postalCode": "518057",
    "isDefault": true
  },
  "requestId": "addr113",
  "timestamp": 1745162401006
}
```

---

### 2B.5 删除收货地址

`DELETE /api/v1/users/me/addresses/{addressId}`

> 若删除的是默认地址，系统会自动把最近一条其他地址设为默认地址

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "requestId": "addr114",
  "timestamp": 1745162401007
}
```

---

### 2B.6 设为默认地址

`PUT /api/v1/users/me/addresses/{addressId}/default`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "requestId": "addr115",
  "timestamp": 1745162401008
}
```

---

## 三、商品与搜索服务

### 3.1 查询商品详情

`GET /api/v1/products/{productId}`

**请求（无请求体）：**

```
GET /api/v1/products/390000000000000001
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "390000000000000001",
    "categoryId": "100000000000000002",
    "categoryName": "女装",
    "name": "2026春季新款修身连衣裙",
    "subTitle": "显瘦百搭，多色可选",
    "mainImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
    "description": "<p>商品详情富文本...</p>",
    "price": 199.00,
    "salesCount": 2380,
    "status": "ON_SALE",
    "skuList": [
      {
        "id": "490000000000000001",
        "skuCode": "DRESS-RED-M",
        "spec": {"颜色": "红色", "尺寸": "M"},
        "price": 199.00,
        "stock": 85,
        "status": "ENABLED"
      },
      {
        "id": "490000000000000002",
        "skuCode": "DRESS-RED-L",
        "spec": {"颜色": "红色", "尺寸": "L"},
        "price": 199.00,
        "stock": 42,
        "status": "ENABLED"
      },
      {
        "id": "490000000000000003",
        "skuCode": "DRESS-BLUE-M",
        "spec": {"颜色": "蓝色", "尺寸": "M"},
        "price": 219.00,
        "stock": 0,
        "status": "ENABLED"
      }
    ],
    "createTime": "2026-03-01T09:00:00+08:00",
    "updateTime": "2026-04-20T15:30:00+08:00"
  },
  "requestId": "ccc111",
  "timestamp": 1745162400000
}
```

> `stock: 0` 时前端展示"已售罄"，不允许加购

---

### 3.2 商品搜索

`GET /api/v1/search/products`

**请求：**

```
GET /api/v1/search/products?keyword=红色连衣裙&categoryId=&priceMin=0&priceMax=300&pageNum=1&pageSize=20&sortBy=sales&sortOrder=desc
```

> `sortBy` 枚举：`default | price | sales | new`  
> `sortOrder` 枚举：`asc | desc`  
> `priceMin` / `priceMax`：（可选）价格过滤，单位元，不传则不限

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "390000000000000001",
        "name": "2026春季新款修身连衣裙",
        "mainImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
        "price": 199.00,
        "salesCount": 2380,
        "highlight": {
          "name": "2026春季新款修身<em>连衣裙</em>"
        }
      }
    ],
    "total": 156,
    "pageNum": 1,
    "pageSize": 20,
    "hasNext": true,
    "aggregations": {
      "priceRanges": [
        {"label": "0-100", "count": 23},
        {"label": "100-300", "count": 89},
        {"label": "300+", "count": 44}
      ],
      "categories": [
        {"id": "100000000000000002", "name": "女装", "count": 112}
      ]
    }
  },
  "requestId": "ddd111",
  "timestamp": 1745162400000
}
```

---

### 3.3 商品后台管理（新增）

> 当前为 Phase 1 内部管理接口，统一挂在 `/api/v1/products/admin/**`，用于分类、商品、SKU 的增改删查。  
> **必须携带管理员 JWT**；普通用户或未登录请求均不可调用。  
> 状态枚举：商品 `PENDING_REVIEW | ON_SALE | OFF_SALE`，分类/SKU `ENABLED | DISABLED`

| 接口 | 说明 | 关键参数 |
|------|------|---------|
| `GET /api/v1/products/admin/categories` | 查询分类列表 | 无 |
| `GET /api/v1/products/admin/categories/{categoryId}` | 查询分类详情 | `categoryId` |
| `POST /api/v1/products/admin/categories` | 新增分类 | `parentId?`, `name`, `iconUrl?`, `sortOrder?`, `status` |
| `PUT /api/v1/products/admin/categories/{categoryId}` | 更新分类 | 同新增 |
| `DELETE /api/v1/products/admin/categories/{categoryId}` | 删除分类 | 仅当无子分类且无商品时允许 |
| `GET /api/v1/products/admin` | 查询商品列表 | `categoryId?`, `status?`, `keyword?`, `pageNum?`, `pageSize?` |
| `GET /api/v1/products/admin/{productId}` | 查询商品详情（含全部未删除 SKU） | `productId` |
| `POST /api/v1/products/admin` | 新增商品 | `categoryId`, `name`, `subTitle?`, `mainImage?`, `description?`, `status` |
| `PUT /api/v1/products/admin/{productId}` | 更新商品 | 同新增 |
| `DELETE /api/v1/products/admin/{productId}` | 删除商品 | 逻辑删除商品及其 SKU |
| `POST /api/v1/products/admin/{productId}/skus` | 新增 SKU | `skuCode`, `spec`, `price`, `stock`, `status` |
| `PUT /api/v1/products/admin/{productId}/skus/{skuId}` | 更新 SKU | 同新增 |
| `DELETE /api/v1/products/admin/{productId}/skus/{skuId}` | 删除 SKU | 商品上架时不允许删除最后一个启用 SKU |

**约束说明：**

- 商品展示价 `product.price` 会在 SKU 新增/更新/删除后同步刷新为“当前启用 SKU 的最低价”
- 新商品如果尚未配置可售 SKU，不允许直接创建为 `ON_SALE`
- 已上架商品至少需要保留一个启用 SKU；若要清空或禁用最后一个启用 SKU，需先把商品改为 `OFF_SALE`
- SKU 仍遵循全局 `skuCode` 唯一、同一商品下 `spec_hash` 唯一

---

## 四、购物车服务 `/api/v1/cart`

### 4.1 获取购物车

`GET /api/v1/cart`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
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
    "selectedCount": 1,
    "totalQuantity": 2,
    "estimatedTotal": 398.00
  },
  "requestId": "eee111",
  "timestamp": 1745162400000
}
```

> `isAvailable: false` 表示商品已下架或库存不足，前端置灰并提示

---

### 4.2 加入购物车

`POST /api/v1/cart/items`

**请求：**

```json
{
  "skuId": "490000000000000001",
  "quantity": 1
}
```

**响应：**（返回最新购物车快照，结构同 4.1）

```json
{
  "code": 200,
  "message": "success",
  "data": { "...最新购物车快照..." },
  "requestId": "fff111",
  "timestamp": 1745162400000
}
```

**错误示例（库存不足）：**

```json
{
  "code": 50901,
  "message": "库存不足，当前可购数量为 3",
  "data": null,
  "requestId": "fff112",
  "timestamp": 1745162400001
}
```

---

### 4.3 修改购物车项

`PUT /api/v1/cart/items/{itemId}`

**请求：**

```json
{
  "quantity": 3,
  "selected": true
}
```

**响应：**（结构同 4.1）

---

### 4.4 删除购物车单项

`DELETE /api/v1/cart/items/{itemId}`

**请求（无请求体）：**

```
DELETE /api/v1/cart/items/cart_item_001
```

**响应：**（返回删除后的最新购物车快照，结构同 4.1）

---

### 4.5 清空购物车

`DELETE /api/v1/cart`

**请求（无请求体）**

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [],
    "selectedCount": 0,
    "totalQuantity": 0,
    "estimatedTotal": 0.00
  },
  "requestId": "ggg111",
  "timestamp": 1745162400000
}
```

---

## 五、订单服务 `/api/v1/orders`

### 5.1 创建订单

`POST /api/v1/orders`  
**必须携带 `Idempotency-Key` 头**

**请求头：**

```
Idempotency-Key: idem_20260421_user123_uuid001
```

**请求体：**

```json
{
  "addressId": "190000000000000001",
  "items": [
    {
      "skuId": "490000000000000001",
      "quantity": 2
    }
  ],
  "remark": "工作日白天配送",
  "source": "app"
}
```

> `source` 枚举：`app | web | mini`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "orderStatus": "PENDING_PAYMENT",
    "totalAmount": 398.00,
    "payAmount": 398.00,
    "freightAmount": 0.00,
    "expireTime": "2026-04-21T10:30:00+08:00",
    "createTime": "2026-04-21T10:00:00+08:00"
  },
  "requestId": "hhh111",
  "timestamp": 1745162400000
}
```

**错误示例（库存不足）：**

```json
{
  "code": 50901,
  "message": "商品「2026春季新款修身连衣裙（红色/M）」库存不足",
  "data": null,
  "requestId": "hhh112",
  "timestamp": 1745162400001
}
```

---

### 5.2 查询订单列表

`GET /api/v1/orders`

**请求：**

```
GET /api/v1/orders?pageNum=1&pageSize=10&status=PENDING_PAYMENT
```

> `status` 枚举：`PENDING_PAYMENT | PAID | SHIPPED | COMPLETED | CANCELLED | REFUNDING | REFUNDED`（可不传，返回全部）

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "orderId": "580000000000000001",
        "orderNo": "20260421000001",
        "orderStatus": "PENDING_PAYMENT",
        "payAmount": 398.00,
        "itemCount": 1,
        "coverImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
        "createTime": "2026-04-21T10:00:00+08:00",
        "expireTime": "2026-04-21T10:30:00+08:00"
      }
    ],
    "total": 42,
    "pageNum": 1,
    "pageSize": 10,
    "hasNext": true
  },
  "requestId": "iii111",
  "timestamp": 1745162400000
}
```

---

### 5.3 查询订单详情

`GET /api/v1/orders/{orderId}`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "orderStatus": "PAID",
    "totalAmount": 398.00,
    "freightAmount": 0.00,
    "payAmount": 398.00,
    "payType": "ALIPAY",
    "source": "app",
    "remark": "工作日白天配送",
    "address": {
      "receiverName": "张三",
      "receiverPhone": "138****8000",
      "fullAddress": "广东省深圳市南山区科技园路1号"
    },
    "items": [
      {
        "itemId": "order_item_001",
        "productId": "390000000000000001",
        "productName": "2026春季新款修身连衣裙",
        "productImage": "https://cdn.shophelper.com/products/xxx/main.jpg",
        "skuSpec": {"颜色": "红色", "尺寸": "M"},
        "unitPrice": 199.00,
        "quantity": 2,
        "totalPrice": 398.00
      }
    ],
    "logistics": null,
    "createTime": "2026-04-21T10:00:00+08:00",
    "payTime": "2026-04-21T10:05:30+08:00"
  },
  "requestId": "jjj111",
  "timestamp": 1745162400000
}
```

> `address.receiverPhone` 响应时脱敏显示  
> `logistics: null` 表示尚未发货

---

### 5.4 取消订单

`POST /api/v1/orders/{orderId}/cancel`

**请求：**

```json
{
  "reason": "不想买了"
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "orderStatus": "CANCELLED",
    "cancelTime": "2026-04-21T10:10:00+08:00"
  },
  "requestId": "kkk111",
  "timestamp": 1745162400000
}
```

**错误示例（状态不允许取消）：**

```json
{
  "code": 50904,
  "message": "订单已发货，无法取消，请申请退换货",
  "data": null,
  "requestId": "kkk112",
  "timestamp": 1745162400001
}
```

**PAID 状态取消成功（自动触发退款，状态流转为 REFUNDING）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "orderStatus": "REFUNDING",
    "cancelTime": "2026-04-21T10:10:00+08:00",
    "refundNote": "订单已取消，退款预计 3~5 个工作日到账"
  },
  "requestId": "kkk113",
  "timestamp": 1745162400002
}
```

> `PENDING_PAYMENT` 状态取消 → 直接 `CANCELLED`；`PAID` 状态取消（发货前） → 进入 `REFUNDING`，等待支付平台退款回调后变为 `REFUNDED`

---

### 5.5 发起支付

`POST /api/v1/orders/{orderId}/pay`  
**必须携带 `Idempotency-Key` 头**（防止重复创建支付会话）

**请求头：**

```
Idempotency-Key: idem_pay_20260421_user123_order001
```

**请求体：**

```json
{
  "paymentMethod": "MOCK"
}
```

> `paymentMethod` 枚举：`ALIPAY | WECHAT | MOCK`  
> `MOCK` 为开发/测试环境专用，直接返回成功不走真实支付

**响应（MOCK 支付）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "paymentMethod": "MOCK",
    "paymentSessionId": "mock_session_001",
    "payUrl": null,
    "qrCode": null,
    "note": "MOCK支付，订单将自动标记为已支付"
  },
  "requestId": "lll111",
  "timestamp": 1745162400000
}
```

**响应（支付宝）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "580000000000000001",
    "orderNo": "20260421000001",
    "paymentMethod": "ALIPAY",
    "paymentSessionId": "alipay_session_abc123",
    "payUrl": "https://openapi.alipay.com/gateway.do?...",
    "qrCode": null
  },
  "requestId": "lll112",
  "timestamp": 1745162400000
}
```

---

### 5.5A 确认收货

`POST /api/v1/orders/{orderId}/confirm`

> 当前仅允许 `SHIPPED` 状态调用；若订单未发货会返回状态不允许错误。

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "requestId": "lll113",
  "timestamp": 1745162400001
}
```

---

### 5.6 支付平台异步回调

`POST /api/v1/orders/payment-callback`  
**无需 JWT；来源 IP 白名单校验 + 签名验证**

> 此接口由支付平台服务器调用，非前端调用  
> 需在业务层验证签名，通过后返回 HTTP 200 + 纯文本 `success`

---

## 五-B、秒杀服务 `/api/v1/seckill`

### 5B.1 秒杀下单（提交秒杀请求）

`POST /api/v1/seckill/{activityId}/items/{skuId}/submit`  
**必须携带 `Idempotency-Key` 头**

> 前端需在活动开始时一次性提交，重试时复用相同 `Idempotency-Key`

**请求头：**

```
Idempotency-Key: idem_seckill_actv001_user123_sku001
```

**请求体：**

```json
{
  "addressId": "190000000000000001"
}
```

**响应（秒杀成功，等待订单异步创建）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderToken": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PROCESSING",
    "message": "秒杀资格确认，订单处理中，请稍候查询结果"
  },
  "requestId": "seckill111",
  "timestamp": 1745162400000
}
```

**错误示例（库存已抢完）：**

```json
{
  "code": 50901,
  "message": "手慢了！本次秒杀商品已售罄",
  "data": null,
  "requestId": "seckill112",
  "timestamp": 1745162400001
}
```

**错误示例（已参与过此次秒杀）：**

```json
{
  "code": 50903,
  "message": "您已参与过此次秒杀，请勿重复提交",
  "data": null,
  "requestId": "seckill113",
  "timestamp": 1745162400002
}
```

---

### 5B.2 查询秒杀下单结果

`GET /api/v1/seckill/{activityId}/result?orderToken={orderToken}`

> 秒杀订单由 Kafka 异步创建，前端以轮询方式查询结果，建议间隔 500ms，最多轮询 10 次

**响应（订单创建成功）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderToken": "550e8400-e29b-41d4-a716-446655440000",
    "status": "SUCCESS",
    "orderId": "580000000000000002",
    "orderNo": "20260421SK000001",
    "payAmount": 99.00,
    "expireTime": "2026-04-21T12:30:00+08:00"
  },
  "requestId": "seckill121",
  "timestamp": 1745162400000
}
```

**响应（订单仍在处理中）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderToken": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PROCESSING",
    "orderId": null,
    "orderNo": null
  },
  "requestId": "seckill122",
  "timestamp": 1745162400000
}
```

> `status` 枚举：`PROCESSING`（处理中）| `SUCCESS`（订单创建成功）| `FAILED`（订单创建失败，如库存回滚）

---

## 六、用户画像与推荐 `/api/v1/users` & `/api/v1/recommendations`

### 6.1 获取用户画像

`GET /api/v1/users/me/profile`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "200000000000000001",
    "preferredCategories": [],
    "priceBandPreference": {
      "min": 0,
      "max": 0,
      "typical": 0
    },
    "recentBrowseCount": 0,
    "purchaseCount": 0
  },
  "requestId": "mmm111",
  "timestamp": 1745162400000
}
```

> 当前 Phase 1 尚未接入浏览 / 购买行为数据管道，空画像是当前实现的正常返回

---

### 6.2 获取推荐商品

`GET /api/v1/recommendations/me`

**请求：**

```
GET /api/v1/recommendations/me?scene=homepage&pageSize=10
```

> `scene` 枚举：`homepage | product_detail | cart | checkout | category`

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scene": "homepage",
    "list": [],
    "total": 0,
    "algorithm": "cold_start_v1"
  },
  "requestId": "nnn111",
  "timestamp": 1745162400000
}
```

> 当前为冷启动实现；当 `list` 为空时，前端首页会回退到普通商品列表

---

## 七、Agent 对话接口 `/agent/chat`

**注意**：此接口由 Python Agent 微服务提供，端口与 Java 主服务不同（Docker 部署时统一通过 Gateway 暴露）

### 7.1 发起对话

`POST /agent/chat`  
响应为 **Server-Sent Events（SSE）** 流式输出

**请求：**

```json
{
  "sessionId": "session_user123_20260421001",
  "message": "帮我找一件适合春天穿的红色连衣裙，预算 200 以内",
  "context": {
    "pageType": "homepage"
  }
}
```

> `pageType` 枚举：`homepage | product_detail | cart | search | category | order`

**SSE 响应流：**

```
data: {"type":"tool_call","traceId":"5c9c1984f9db438c","data":{"toolName":"get_user_profile","status":"calling"}}

data: {"type":"tool_call","traceId":"5c9c1984f9db438c","data":{"toolName":"get_user_profile","status":"success"}}

data: {"type":"tool_call","traceId":"5c9c1984f9db438c","data":{"toolName":"search_products","status":"calling"}}

data: {"type":"tool_call","traceId":"5c9c1984f9db438c","data":{"toolName":"search_products","status":"success"}}

data: {"type":"text","traceId":"5c9c1984f9db438c","data":{"content":"根据您的预算，我找到了以下几款适合春天穿的红色连衣裙～"}}

data: {"type":"product_card","traceId":"5c9c1984f9db438c","data":{"id":"390000000000000001","name":"2026春季新款修身连衣裙","price":199.00,"mainImage":"https://cdn.shophelper.com/products/xxx/main.jpg","salesCount":2380,"skuId":"490000000000000001","spec":{"颜色":"红色","尺寸":"M"}}}

data: {"type":"text","traceId":"5c9c1984f9db438c","data":{"content":"这款连衣裙采用柔软面料，修身剪裁显瘦，正好在您的预算内，需要帮您加入购物车吗？"}}

data: {"type":"done","traceId":"5c9c1984f9db438c","data":{}}
```

**错误事件示例（搜索服务超时）：**

```
data: {"type":"error","traceId":"5c9c1984f9db438c","data":{"code":"TOOL_CALL_FAILED","message":"搜索服务暂时不可用，请稍后重试"}}

data: {"type":"done","traceId":"5c9c1984f9db438c","data":{}}
```

> `error` 事件后必须跟随 `done` 事件，前端以 `done` 作为流结束信号

---

## 附录：接口覆盖清单

| 服务 | 接口 | 是否有示例 |
|------|------|-----------|
| 认证 | POST /auth/login | ✅ |
| 认证 | POST /auth/refresh | ✅ |
| 认证 | POST /auth/logout | ✅ |
| 用户 | POST /users/register | ✅ |
| 用户 | GET /users/me/addresses | ✅ |
| 用户 | POST /users/me/addresses | ✅ |
| 用户 | PUT /users/me/addresses/{id} | ✅ |
| 用户 | DELETE /users/me/addresses/{id} | ✅ |
| 用户 | PUT /users/me/addresses/{id}/default | ✅ |
| 商品 | GET /products/{id} | ✅ |
| 搜索 | GET /search/products | ✅ |
| 购物车 | GET /cart | ✅ |
| 购物车 | POST /cart/items | ✅ |
| 购物车 | PUT /cart/items/{id} | ✅ |
| 购物车 | DELETE /cart/items/{id} | ✅ |
| 购物车 | DELETE /cart | ✅ |
| 订单 | POST /orders | ✅ |
| 订单 | GET /orders | ✅ |
| 订单 | GET /orders/{id} | ✅ |
| 订单 | POST /orders/{id}/cancel | ✅（含PAID→REFUNDING示例） |
| 订单 | POST /orders/{id}/pay | ✅（含Idempotency-Key） |
| 订单 | POST /orders/payment-callback | ✅（说明） |
| 秒杀 | POST /seckill/{actId}/items/{skuId}/submit | ✅ |
| 秒杀 | GET /seckill/{actId}/result | ✅ |
| 用户 | GET /users/me/profile | ✅ |
| 推荐 | GET /recommendations/me | ✅ |
| Agent | POST /agent/chat (SSE) | ✅ |

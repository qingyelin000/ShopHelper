-- =============================================
-- ShopHelper 核心业务表 DDL
-- MySQL 8.0 | 版本 v0.1 | 创建时间 2026-04-21
-- =============================================
-- 说明：
--   1. 主键全部使用 Snowflake 算法生成的 BIGINT UNSIGNED
--   2. 所有业务表包含 create_time / update_time / is_deleted 基础字段
--   3. 禁止使用数据库外键，关联关系由业务层代码保证
--   4. order / order_item 由 ShardingSphere 按 user_id % 16 水平分表
--      建表时建物理表 order_0~order_15 / order_item_0~order_item_15，此处给出逻辑表模板
-- =============================================

-- -----------------------------------------------
-- 1. 用户表
-- -----------------------------------------------
CREATE TABLE `user`
(
    `id`                BIGINT UNSIGNED  NOT NULL COMMENT '用户ID（雪花算法）',
    `username`          VARCHAR(64)      NOT NULL COMMENT '用户名',
    `password_hash`     VARCHAR(128)     NOT NULL COMMENT 'BCrypt 加密后的密码',
    `phone_ciphertext`  VARCHAR(128)     NOT NULL COMMENT '手机号密文（AES-CBC 加密，用于展示/解密）',
    `phone_hash`        VARCHAR(64)      NOT NULL COMMENT '手机号 HMAC-SHA256（E.164格式归一化后，用于唯一性校验与检索）',
    `email`             VARCHAR(128)              DEFAULT NULL COMMENT '邮箱（可选）',
    `avatar_url`        VARCHAR(512)              DEFAULT NULL,
    `status`            TINYINT          NOT NULL DEFAULT 1 COMMENT '账号状态：1=正常 0=禁用',
    `delete_version`    BIGINT           NOT NULL DEFAULT 0 COMMENT '软删除版本：未删除为0，删除后置为 id，支持同名账号重新注册',
    `create_time`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`        TINYINT(1)       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`, `delete_version`),
    UNIQUE KEY `uk_user_phone_hash` (`phone_hash`, `delete_version`),
    -- email 可为 NULL；MySQL UNIQUE 索引允许多个 NULL，不冲突
    UNIQUE KEY `uk_user_email` (`email`, `delete_version`),
    CHECK (`status` IN (0, 1)),
    CHECK (`is_deleted` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- -----------------------------------------------
-- 2. 用户收货地址表
-- -----------------------------------------------
CREATE TABLE `user_address`
(
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '地址ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `receiver_name`   VARCHAR(64)     NOT NULL COMMENT '收货人姓名',
    `receiver_phone`  VARCHAR(128)    NOT NULL COMMENT '收货人手机号（AES加密）',
    `province`        VARCHAR(32)     NOT NULL COMMENT '省',
    `city`            VARCHAR(32)     NOT NULL COMMENT '市',
    `district`        VARCHAR(32)     NOT NULL COMMENT '区/县',
    `detail_address`  VARCHAR(256)    NOT NULL COMMENT '详细地址',
    `postal_code`     VARCHAR(16)              DEFAULT NULL COMMENT '邮政编码',
    `is_default`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认地址，业务层保证同一用户只有一个默认',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_address_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户收货地址表';

-- -----------------------------------------------
-- 3. 商品分类表
-- -----------------------------------------------
CREATE TABLE `category`
(
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示一级分类',
    `name`        VARCHAR(64)     NOT NULL COMMENT '分类名称',
    `icon_url`    VARCHAR(512)             DEFAULT NULL COMMENT '分类图标',
    `sort_order`  INT             NOT NULL DEFAULT 0 COMMENT '排序权重，越大越靠前',
    `level`       TINYINT         NOT NULL COMMENT '层级：1=一级 2=二级 3=三级',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category_parent_id` (`parent_id`),
    CHECK (`level` IN (1, 2, 3)),
    CHECK (`status` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品分类表';

-- -----------------------------------------------
-- 4. 商品表
-- -----------------------------------------------
-- 说明：price 字段为冗余展示价（取最低SKU价），由业务层在SKU变更时异步更新，不作为结算依据
CREATE TABLE `product`
(
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `category_id` BIGINT UNSIGNED NOT NULL COMMENT '所属分类ID',
    `name`        VARCHAR(256)    NOT NULL COMMENT '商品名称',
    `sub_title`   VARCHAR(512)             DEFAULT NULL COMMENT '副标题/卖点',
    `main_image`  VARCHAR(512)             DEFAULT NULL COMMENT '主图URL',
    `description` TEXT                     DEFAULT NULL COMMENT '商品详情（富文本）',
    `price`       DECIMAL(12, 2)  NOT NULL COMMENT '展示价（冗余字段，取最低SKU价，非结算依据）',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0=待审核 1=上架 2=下架',
    `sales_count` INT             NOT NULL DEFAULT 0 COMMENT '累计销量（冗余缓存，异步维护，不保证实时准确）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_category_id` (`category_id`),
    KEY `idx_product_status` (`status`),
    CHECK (`status` IN (0, 1, 2)),
    CHECK (`price` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';

-- -----------------------------------------------
-- 5. 商品 SKU 表
-- -----------------------------------------------
CREATE TABLE `product_sku`
(
    `id`          BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    `product_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属商品ID',
    `sku_code`    VARCHAR(64)     NOT NULL COMMENT 'SKU 编码（全局唯一）',
    `spec_json`   JSON            NOT NULL COMMENT '规格属性快照，如 {"颜色":"红色","尺寸":"XL"}',
    `spec_hash`   VARCHAR(64)     NOT NULL COMMENT 'spec_json 的 MD5，用于防止同商品下规格组合重复',
    `price`       DECIMAL(12, 2)  NOT NULL COMMENT 'SKU 售价',
    `stock`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '可用库存',
    `version`     INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，每次库存变更+1',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    UNIQUE KEY `uk_product_sku_spec` (`product_id`, `spec_hash`),
    KEY `idx_product_sku_product_id` (`product_id`),
    CHECK (`price` >= 0),
    CHECK (`status` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品SKU表';

-- -----------------------------------------------
-- 6. 订单主表（逻辑表模板，分表后为 order_0 ~ order_15）
-- -----------------------------------------------
-- 分片键：user_id % 16
-- 状态码映射（status TINYINT）：
--   0=PENDING_PAYMENT  1=PAID  2=SHIPPED  3=COMPLETED
--   4=CANCELLED  5=REFUNDING  6=REFUNDED
-- order_no 生成策略：yyyyMMdd + shard_idx + 毫秒时间戳 + 随机4位
--   使 order_no 内含路由信息，避免支付回调时全分片扫描
CREATE TABLE `order`
(
    `id`                     BIGINT UNSIGNED NOT NULL COMMENT '订单ID（雪花算法，全局唯一）',
    `order_no`               VARCHAR(32)     NOT NULL COMMENT '业务订单号（含分片路由信息）',
    `user_id`                BIGINT UNSIGNED NOT NULL COMMENT '用户ID（分片键）',

    -- 冗余的地址快照，防止用户修改/删除地址后历史订单地址丢失
    `address_id`             BIGINT UNSIGNED NOT NULL COMMENT '原收货地址ID（仅供参考）',
    `snap_receiver_name`     VARCHAR(64)     NOT NULL COMMENT '快照：收货人姓名',
    `snap_receiver_phone`    VARCHAR(128)    NOT NULL COMMENT '快照：收货人手机号（AES加密）',
    `snap_province`          VARCHAR(32)     NOT NULL COMMENT '快照：省',
    `snap_city`              VARCHAR(32)     NOT NULL COMMENT '快照：市',
    `snap_district`          VARCHAR(32)     NOT NULL COMMENT '快照：区/县',
    `snap_detail_address`    VARCHAR(256)    NOT NULL COMMENT '快照：详细地址',

    `total_amount`           DECIMAL(12, 2)  NOT NULL COMMENT '商品原价合计',
    `pay_amount`             DECIMAL(12, 2)  NOT NULL COMMENT '实付金额（扣减优惠后）',
    `freight_amount`         DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '运费',
    `pay_type`               TINYINT                  DEFAULT NULL COMMENT '支付方式：1=支付宝 2=微信 3=模拟支付',
    `source`                 VARCHAR(16)     NOT NULL DEFAULT 'app' COMMENT '下单来源：app/web/mini',
    `status`                 TINYINT         NOT NULL DEFAULT 0 COMMENT '订单状态码（见上方枚举说明）',
    `cancel_reason`          VARCHAR(256)             DEFAULT NULL COMMENT '取消原因',
    `remark`                 VARCHAR(512)             DEFAULT NULL COMMENT '用户备注',
    `expire_time`            DATETIME                 DEFAULT NULL COMMENT '支付截止时间（PENDING_PAYMENT 状态下，超时后自动取消）',
    `pay_time`               DATETIME                 DEFAULT NULL COMMENT '支付成功时间',
    `create_time`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`             TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    -- order_no 在单分片内唯一；全局唯一性由 order_no 生成策略保证
    UNIQUE KEY `uk_order_order_no` (`order_no`),
    KEY `idx_order_user_id` (`user_id`),
    KEY `idx_order_status` (`status`),
    CHECK (`status` IN (0, 1, 2, 3, 4, 5, 6)),
    CHECK (`total_amount` >= 0),
    CHECK (`pay_amount` >= 0),
    CHECK (`freight_amount` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单主表（逻辑表，分表后为 order_0~order_15）';

-- -----------------------------------------------
-- 7. 订单明细表（逻辑表模板，分表后为 order_item_0 ~ order_item_15）
-- -----------------------------------------------
-- 分片键：user_id % 16，与 order 表保持一致（ShardingSphere binding table）
CREATE TABLE `order_item`
(
    `id`                BIGINT UNSIGNED NOT NULL COMMENT '明细ID',
    `order_id`          BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    `order_no`          VARCHAR(32)     NOT NULL COMMENT '订单号（冗余，避免跨表查询）',
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '用户ID（分片键，与 order 对齐）',
    `product_id`        BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `product_name`      VARCHAR(256)    NOT NULL COMMENT '快照：下单时商品名称',
    `product_image`     VARCHAR(512)             DEFAULT NULL COMMENT '快照：下单时商品主图',
    `sku_id`            BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    `sku_code`          VARCHAR(64)     NOT NULL COMMENT '快照：下单时SKU编码',
    `sku_spec_json`     JSON            NOT NULL COMMENT '快照：下单时SKU规格',
    `unit_price`        DECIMAL(12, 2)  NOT NULL COMMENT '快照：下单时单价（结算依据）',
    `quantity`          INT             NOT NULL COMMENT '购买数量',
    `total_price`       DECIMAL(12, 2)  NOT NULL COMMENT '小计金额（unit_price × quantity）',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order_id` (`order_id`),
    KEY `idx_order_item_user_id` (`user_id`),
    CHECK (`quantity` > 0),
    CHECK (`unit_price` >= 0),
    CHECK (`total_price` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细表（逻辑表，分表后为 order_item_0~order_item_15）';

-- -----------------------------------------------
-- 8. 秒杀活动表
-- -----------------------------------------------
CREATE TABLE `seckill_activity`
(
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    `name`        VARCHAR(128)    NOT NULL COMMENT '活动名称',
    `start_time`  DATETIME        NOT NULL COMMENT '活动开始时间',
    `end_time`    DATETIME        NOT NULL COMMENT '活动结束时间',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0=待开始 1=进行中 2=已结束',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_seckill_activity_status` (`status`),
    KEY `idx_seckill_activity_time` (`start_time`, `end_time`),
    CHECK (`end_time` > `start_time`),
    CHECK (`status` IN (0, 1, 2))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀活动表';

-- -----------------------------------------------
-- 9. 秒杀库存表
-- -----------------------------------------------
-- Redis 为扣减主路径，DB 为兜底记录；available_stock 随 Redis 扣减同步（异步或补偿）
CREATE TABLE `seckill_stock`
(
    `id`               BIGINT UNSIGNED NOT NULL COMMENT '记录ID',
    `activity_id`      BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
    `sku_id`           BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    `seckill_price`    DECIMAL(12, 2)  NOT NULL COMMENT '秒杀价',
    `total_stock`      INT UNSIGNED    NOT NULL COMMENT '活动总库存',
    `available_stock`  INT UNSIGNED    NOT NULL COMMENT 'DB兜底可用库存（Redis主扣，DB跟随）',
    `max_per_user`     TINYINT         NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`       TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seckill_stock_activity_sku` (`activity_id`, `sku_id`),
    KEY `idx_seckill_stock_activity_id` (`activity_id`),
    CHECK (`seckill_price` >= 0),
    CHECK (`total_stock` >= `available_stock`),
    CHECK (`max_per_user` >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀库存表';

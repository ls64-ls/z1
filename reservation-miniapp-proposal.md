# 微信小程序会议室预约系统 — 架构设计文档

> **目标场景**: 连锁商户会议室 / 工位 / 场地预约
> **运行平台**: 微信小程序
> **规模**: 中型连锁（1,000 ~ 50,000 DAU）
> **文档语言**: 中英混合（解释用中文，技术术语和代码用英文）

---

## 目录

1. [系统架构](#1-系统架构-system-architecture)
2. [数据模型](#2-数据模型-data-model)
3. [API 设计](#3-api-设计-api-design)
4. [核心业务流程](#4-核心业务流程-core-business-flows)
5. [并发策略](#5-并发策略-concurrency-strategy)
6. [部署架构](#6-部署架构-deployment-architecture)
7. [项目结构](#7-项目结构-project-structure)
8. [附录：关键代码片段](#8-附录关键代码片段)

---

## 1. 系统架构 / System Architecture

### 1.1 技术栈选型 / Technology Stack

| Layer | Technology | 选型理由 / Rationale |
|---|---|---|
| **Backend Framework** | **Java 17 + Spring Boot 3.2** | 预订系统事务逻辑复杂，Spring `@Transactional` + PostgreSQL exclusion constraint 是天配。编译期类型安全对业务关键系统至关重要。备选：Go（团队有 Go 经验且不需要复杂 ORM 时）、NestJS（团队 Node.js 经验丰富时） |
| **Database** | **PostgreSQL 16** | `tsrange` 范围类型 + GiST 索引 + exclusion constraint 是并发冲突检测的"银弹"。MySQL 无此能力，需在应用层实现所有冲突检测，复杂且不可靠 |
| **Cache** | **Redis 7 Stack** | 缓存热点数据（房间列表、时段可用性）+ 分布式锁（Redisson）+ 轻量消息队列（Redis Streams） |
| **ORM** | **MyBatis-Plus + jOOQ** | MyBatis-Plus 处理简单 CRUD；jOOQ 处理复杂查询（报表聚合、冲突检测的 CTE 查询），类型安全 SQL 生成 |
| **Object Storage** | **Tencent Cloud COS** | 与微信生态同一云厂商，内网传输免流量费 |
| **Mini Program** | **Taro 3 (React)** | 支持 React/Vue 语法，一套代码可编译到 H5，管理后台组件可复用 |
| **Admin Dashboard** | **Vue 3 + Vite + Element Plus** | 成熟的企业级后台 UI 方案 |
| **Message Queue** | **Redis Streams** (轻量) | 初期够用，避免引入额外中间件。后期可迁移至 Apache Pulsar |
| **Monitoring** | **Prometheus + Grafana + SkyWalking** | SkyWalking 做全链路追踪，Prometheus 做指标采集 |
| **Logging** | **Logback + ELK** | 集中式日志检索 |
| **DB Migration** | **Flyway** | 版本化 SQL 迁移，与 Spring Boot 深度集成 |
| **CI/CD** | **GitHub Actions / 腾讯云 CODING** | 自动构建、测试、部署 |

### 1.2 整体架构图 / Architecture Diagram

```
                         ┌─────────────────────────────────────┐
                         │          WeChat 小程序客户端           │
                         │   (Taro 3 / React + WXML + WXSS)      │
                         └──────────────┬──────────────────────┘
                                        │ HTTPS
                         ┌──────────────▼──────────────────────┐
                         │        API Gateway (CLB + Nginx)     │
                         │   - Rate Limiting & WAF              │
                         │   - SSL Termination                  │
                         │   - WebSocket Proxy (长连接推送)      │
                         └──────────────┬──────────────────────┘
                                        │
                         ┌──────────────▼──────────────────────┐
                         │     Spring Boot 3.2 服务集群 (TKE)    │
                         │                                      │
                         │  ┌──────────┐  ┌──────────────┐     │
                         │  │ Auth     │  │ Booking       │     │
                         │  │ Service  │  │ Service       │     │
                         │  ├──────────┤  ├──────────────┤     │
                         │  │ Room     │  │ Notification  │     │
                         │  │ Service  │  │ Service       │     │
                         │  ├──────────┤  ├──────────────┤     │
                         │  │ Admin    │  │ Report        │     │
                         │  │ Service  │  │ Service       │     │
                         │  └──────────┘  └──────────────┘     │
                         └──────┬──────────────┬───────────────┘
                                │              │
                   ┌────────────▼──┐    ┌──────▼──────────┐
                   │  PostgreSQL   │    │  Redis Cluster   │
                   │  (Primary +   │    │  - Cache         │
                   │   Read Replica)│    │  - Distributed   │
                   │               │    │    Lock          │
                   │               │    │  - Sessions      │
                   │               │    │  - Streams       │
                   └───────────────┘    └──────────────────┘
                                │
                   ┌────────────▼──────────────┐
                   │  Tencent Cloud COS         │
                   │  (Room Images, Exports)    │
                   └───────────────────────────┘
```

### 1.3 读写分离策略 / Read/Write Separation

对于中型 DAU 规模，采用经典读写分离方案：

- **Write operations**（预订创建、修改、取消、签到）：全部走 **PG Primary**
- **Read operations**（房间列表、可用时段查询、历史记录）：走 **PG Replica**，Redis 缓存兜底
- **Cache strategy**:
  - Room 基础信息：Cache-Aside，TTL 30 min
  - 时段可用性（availability）：Cache-Aside，TTL 5 min（变化频繁，短 TTL）
  - 用户 Booking 历史：Cache-Aside，TTL 10 min
- **Cache invalidation**：写操作提交后通过 Redis Pub/Sub 广播失效消息，各节点监听并清除本地/Redis 缓存

---

## 2. 数据模型 / Data Model

### 2.1 Entity Relationship Overview

```
Organization (连锁商户)
    │ 1:N
    ▼
Venue (门店/分店)
    │ 1:N
    ├──> Room (会议室/空间)
    │       │ N:M ──> Amenity (设施)
    │       │ 1:N ──> RoomImage (图片)
    │       │ 1:N ──> AvailabilityRule (可用性规则)
    │       │ 1:N ──> BlockedTimeSlot (封锁时段)
    │       │
    │       1:N
    ▼
Booking (预订) ── N:1 ──> User (微信用户)
    │
    ├── 1:1 ──> BookingCheckIn (签到/签退)
    └── N:1 ──> RecurringRule (重复规则)
```

### 2.2 Core Table DDL

#### 2.2.1 Organization（连锁商户）

```sql
CREATE TABLE organization (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    contact_phone   VARCHAR(20),
    status          SMALLINT      NOT NULL DEFAULT 1,     -- 1=active, 0=suspended
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_org_status ON organization(status);
```

#### 2.2.2 Venue（门店）

```sql
CREATE TABLE venue (
    id              BIGSERIAL PRIMARY KEY,
    org_id          BIGINT        NOT NULL REFERENCES organization(id),
    name            VARCHAR(100)  NOT NULL,
    address         TEXT          NOT NULL,
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    open_time       TIME          NOT NULL DEFAULT '08:00',
    close_time      TIME          NOT NULL DEFAULT '22:00',
    timezone        VARCHAR(50)   NOT NULL DEFAULT 'Asia/Shanghai',
    status          SMALLINT      NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_venue_org   ON venue(org_id);
CREATE INDEX idx_venue_status ON venue(status);
```

#### 2.2.3 Room（会议室/空间）

```sql
CREATE TABLE room (
    id                BIGSERIAL PRIMARY KEY,
    venue_id          BIGINT        NOT NULL REFERENCES venue(id),
    name              VARCHAR(100)  NOT NULL,               -- e.g. "A101 光谷厅"
    capacity          INT           NOT NULL DEFAULT 1,
    floor             INT,
    area_sqm          DECIMAL(8,2),
    description       TEXT,
    price_per_hour    DECIMAL(10,2),
    price_per_half_day DECIMAL(10,2),
    price_per_day     DECIMAL(10,2),
    min_duration      INT           NOT NULL DEFAULT 30,    -- 最短预订时长 (min)
    max_duration      INT,                                  -- NULL = 不限
    status            SMALLINT      NOT NULL DEFAULT 1,     -- 1=active, 0=inactive, 2=maintenance
    version           INT           NOT NULL DEFAULT 0,     -- optimistic lock
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_room_venue     ON room(venue_id);
CREATE INDEX idx_room_status    ON room(status);
CREATE INDEX idx_room_capacity  ON room(capacity);
```

#### 2.2.4 Amenity & RoomImage（设施与图片）

```sql
CREATE TABLE amenity (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(50)   NOT NULL,       -- e.g. 'Projector', 'Whiteboard', 'Video Conference'
    icon    VARCHAR(255)
);

CREATE TABLE room_amenity (
    room_id     BIGINT NOT NULL REFERENCES room(id),
    amenity_id  BIGINT NOT NULL REFERENCES amenity(id),
    PRIMARY KEY (room_id, amenity_id)
);

CREATE TABLE room_image (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT       NOT NULL REFERENCES room(id),
    url         VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_cover    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_room_image_room ON room_image(room_id);
```

#### 2.2.5 User（微信用户）

```sql
CREATE TABLE "user" (
    id              BIGSERIAL PRIMARY KEY,
    openid          VARCHAR(64)  NOT NULL UNIQUE,
    unionid         VARCHAR(64),
    nickname        VARCHAR(100),
    avatar_url      VARCHAR(500),
    phone           VARCHAR(20),
    organization    VARCHAR(200),               -- 用户填写的公司/组织名
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_user_openid  ON "user"(openid);
CREATE INDEX idx_user_unionid ON "user"(unionid);
```

#### 2.2.6 Booking（预订）— 核心表

```sql
CREATE TABLE booking (
    id              BIGSERIAL PRIMARY KEY,
    booking_no      VARCHAR(32)   NOT NULL UNIQUE,      -- 业务主键, e.g. "BK202605240001"
    user_id         BIGINT        NOT NULL REFERENCES "user"(id),
    room_id         BIGINT        NOT NULL REFERENCES room(id),
    venue_id        BIGINT        NOT NULL REFERENCES venue(id),

    -- 时间范围：核心字段
    -- tsrange 使用半开区间 [start, end)，天然避免边界重叠问题
    time_slot       TSRANGE       NOT NULL,

    -- 冗余字段，方便查询和索引
    booking_date    DATE          NOT NULL,
    start_time      TIME          NOT NULL,
    end_time        TIME          NOT NULL,

    -- 状态机
    -- PENDING → CONFIRMED → CHECKED_IN → COMPLETED
    --                 ↘ CANCELLED
    -- PENDING 超时 → EXPIRED
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',

    title           VARCHAR(200),
    attendee_count  INT,
    remark          TEXT,

    -- 重复预订关联
    recurring_rule_id BIGINT,

    -- 支付
    total_amount    DECIMAL(10,2),
    paid_amount     DECIMAL(10,2) DEFAULT 0,
    payment_status  VARCHAR(20)   DEFAULT 'UNPAID',    -- UNPAID, PAID, REFUNDED

    -- 乐观锁
    version         INT           NOT NULL DEFAULT 0,

    cancelled_at    TIMESTAMPTZ,
    cancelled_by    BIGINT        REFERENCES "user"(id),
    cancel_reason   VARCHAR(500),

    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ===== 索引 =====
CREATE INDEX idx_booking_status_date ON booking(status, booking_date);
CREATE INDEX idx_booking_user         ON booking(user_id, status);
CREATE INDEX idx_booking_room_date    ON booking(room_id, booking_date);
CREATE INDEX idx_booking_no           ON booking(booking_no);

-- ===== 排他约束：防超卖的数据库最后防线 =====
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking ADD CONSTRAINT excl_booking_no_overlap
    EXCLUDE USING GIST (
        room_id WITH =,
        time_slot WITH &&
    ) WHERE (status NOT IN ('CANCELLED', 'EXPIRED'));

-- 说明：该约束确保对于同一 room_id，任意两条有效预订的 time_slot 不重叠
-- && 操作符利用 GiST 索引，O(log N) 复杂度
-- WHERE 子句排除已取消/过期预订，缩小约束范围
```

#### 2.2.7 RecurringRule（重复规则）

```sql
CREATE TABLE recurring_rule (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES "user"(id),
    room_id             BIGINT       NOT NULL REFERENCES room(id),
    recurrence_type     VARCHAR(20)  NOT NULL,   -- DAILY, WEEKLY, BIWEEKLY, MONTHLY
    days_of_week        JSONB,                   -- [1,3,5] = 周一三五 (ISO week)
    start_date          DATE         NOT NULL,
    end_date            DATE,                    -- NULL = 无限重复
    start_time          TIME         NOT NULL,
    end_time            TIME         NOT NULL,
    generated_count     INT          NOT NULL DEFAULT 0,
    last_generated_date DATE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, PAUSED, ENDED
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_rrule_user   ON recurring_rule(user_id);
CREATE INDEX idx_rrule_status ON recurring_rule(status);
```

#### 2.2.8 BookingCheckIn（签到/签退）

```sql
CREATE TABLE booking_check_in (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT       NOT NULL UNIQUE REFERENCES booking(id),
    check_in_time   TIMESTAMPTZ,
    check_out_time  TIMESTAMPTZ,
    check_in_method VARCHAR(20),              -- QR_CODE, LOCATION, MANUAL
    check_in_lat    DECIMAL(10,7),
    check_in_lng    DECIMAL(10,7),
    check_out_lat   DECIMAL(10,7),
    check_out_lng   DECIMAL(10,7),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

#### 2.2.9 AvailabilityRule（可用性规则）

```sql
CREATE TABLE availability_rule (
    id                BIGSERIAL PRIMARY KEY,
    room_id           BIGINT       NOT NULL REFERENCES room(id),
    effective_from    DATE         NOT NULL,
    effective_to      DATE,
    available_days    JSONB        NOT NULL,     -- [1,2,3,4,5] = 周一到周五
    time_slots        JSONB        NOT NULL,     -- [{"start":"08:00","end":"12:00"}, ...]
    max_advance_days  INT,                       -- 最多提前多少天预订
    min_advance_hours INT          DEFAULT 1,    -- 最少提前多少小时
    priority          INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_avail_rule_room ON availability_rule(room_id, effective_from);
```

#### 2.2.10 BlockedTimeSlot（封锁时段 — 维护/节假日）

```sql
CREATE TABLE blocked_time_slot (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT,                    -- NULL = 整店封锁
    venue_id        BIGINT REFERENCES venue(id),
    time_slot       TSRANGE      NOT NULL,
    reason          VARCHAR(200) NOT NULL,
    created_by      BIGINT       NOT NULL REFERENCES "user"(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_blocked_room       ON blocked_time_slot(room_id);
CREATE INDEX idx_blocked_venue      ON blocked_time_slot(venue_id);
CREATE INDEX idx_blocked_time_slot  ON blocked_time_slot USING GIST (time_slot);
```

### 2.3 关键设计决策 / Key Design Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| Booking 时间类型 | PostgreSQL `tsrange` 半开区间 `[start, end)` | 天然避免边界重叠：上一段 `[08:00, 09:00)` 和下一段 `[09:00, 10:00)` 互不冲突 |
| 防超卖机制 | Exclusion Constraint + GiST 索引 | 数据库级别的正确性保证，非应用层的"尽力而为" |
| 业务主键 | `booking_no` (e.g. `BK202605240001`) | 暴露给前端和微信通知，避免泄露自增 ID 及业务量 |
| RecurringRule 存储 | 存规则而非预先展开所有实例 | 避免存储爆炸；定时任务按需批量生成，保持 60 天存量 |
| 状态机 | `PENDING → CONFIRMED → CHECKED_IN → COMPLETED` | 清晰的流转路径，支持支付和签到场景 |

---

## 3. API 设计 / API Design

### 3.1 API Convention

- Base path: `/api/v1`
- Authentication: `Authorization: Bearer <JWT>`
- Response format: 统一 `Result<T>` wrapper

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "timestamp": 1716500000000
}
```

- Error codes: `0` = success, `1xxx` = param error, `2xxx` = auth error, `3xxx` = business error, `5xxx` = server error

### 3.2 Auth Module — `/api/v1/auth`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/login` | 微信登录：接收 `code`（wx.login），返回 JWT |
| `POST` | `/auth/refresh` | 刷新 Token |
| `GET` | `/auth/profile` | 获取当前用户信息 |
| `PUT` | `/auth/profile` | 更新用户信息（昵称、手机号、组织） |
| `POST` | `/auth/phone` | 微信手机号授权解密 |

```java
// POST /api/v1/auth/login
// Request
{ "code": "081xAb0w3..." }

// Response
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOi...",
    "expiresIn": 7200,
    "user": { "id": 10001, "nickname": "张三", "avatarUrl": "...", "phone": "138****1234" }
  }
}
```

### 3.3 Room Module — `/api/v1/venues`, `/api/v1/rooms`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/venues` | 分页获取门店列表（支持经纬度排序） |
| `GET` | `/venues/{id}` | 门店详情 |
| `GET` | `/rooms/search` | **核心接口**：按条件搜索可用会议室 |
| `GET` | `/rooms/{id}` | 房间详情（含设施、图片） |
| `GET` | `/rooms/{id}/availability` | 指定日期的时段可用性 |
| `GET` | `/rooms/{id}/calendar` | 指定月份的预订日历（月视图） |

```java
// GET /api/v1/rooms/search?venueId=1&date=2026-05-28
//   &startTime=09:00&endTime=11:00&capacity=10&amenityIds=1,3&page=1&size=20

// Response
{
  "code": 0,
  "data": {
    "page": 1, "size": 20, "total": 5,
    "records": [{
      "id": 101,
      "name": "A101 光谷厅",
      "venueName": "光谷分店",
      "capacity": 20,
      "areaSqm": 45.0,
      "pricePerHour": 100.00,
      "amenities": [{"id":1, "name":"投影仪"}, {"id":3, "name":"白板"}],
      "coverImage": "https://cos.example.com/room/101/cover.jpg",
      "available": true,
      "remainingSlots": 3
    }]
  }
}
```

### 3.4 Booking Module — `/api/v1/bookings`（核心）

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/bookings` | **创建预订**（核心写接口） |
| `POST` | `/bookings/pre-check` | 预订前冲突检测（轻量，不创建记录） |
| `GET` | `/bookings/{id}` | 预订详情 |
| `PUT` | `/bookings/{id}` | 修改预订（时间/房间变更） |
| `DELETE` | `/bookings/{id}` | 取消预订 |
| `GET` | `/bookings/my` | 我的预订列表（分页，支持按状态筛选） |
| `GET` | `/bookings/my/history` | 我的历史预订 |
| `POST` | `/bookings/recurring` | 创建重复预订 |
| `PUT` | `/bookings/recurring/{ruleId}` | 修改重复规则 |
| `DELETE` | `/bookings/recurring/{ruleId}` | 取消整个重复序列 |

```java
// POST /api/v1/bookings
// Request
{
  "roomId": 101,
  "bookingDate": "2026-05-28",
  "startTime": "09:00",
  "endTime": "11:00",
  "title": "Q2 产品回顾会",
  "attendeeCount": 15,
  "remark": "需要白板和投影仪",
  "recurring": {                              // 可选
    "type": "WEEKLY",
    "daysOfWeek": [1, 3, 5],
    "endDate": "2026-08-28"
  }
}

// Response (Success)
{
  "code": 0,
  "data": {
    "bookingNo": "BK202605280001",
    "id": 5001,
    "status": "CONFIRMED",
    "roomName": "A101 光谷厅",
    "bookingDate": "2026-05-28",
    "startTime": "09:00",
    "endTime": "11:00",
    "totalAmount": 200.00,
    "recurringRuleId": null
  }
}

// Response (Conflict)
{
  "code": 3001,
  "message": "该时段已被预订",
  "data": {
    "conflictBooking": { "startTime": "08:30", "endTime": "10:00" },
    "suggestion": { "availableStart": "11:00", "availableEnd": "13:00" }
  }
}
```

### 3.5 Check-in Module — `/api/v1/checkin`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/checkin/check-in` | 签到（扫码/定位/手动） |
| `POST` | `/checkin/check-out` | 签退 |
| `GET` | `/checkin/status/{bookingId}` | 签到状态查询 |

### 3.6 Admin Module — `/api/v1/admin`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/rooms` | 房间管理列表 |
| `POST` | `/admin/rooms` | 新增房间 |
| `PUT` | `/admin/rooms/{id}` | 修改房间 |
| `DELETE` | `/admin/rooms/{id}` | 下架房间（软删除） |
| `POST` | `/admin/rooms/{id}/availability-rules` | 设置可用性规则 |
| `POST` | `/admin/rooms/{id}/block` | 封锁时段 |
| `DELETE` | `/admin/block/{id}` | 解除封锁 |
| `GET` | `/admin/bookings` | 全部预订列表（按门店/房间/日期/状态筛选） |
| `PUT` | `/admin/bookings/{id}/status` | 强制修改预订状态 |

### 3.7 Report Module — `/api/v1/reports`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/reports/usage` | 空间使用率报表 |
| `GET` | `/reports/peak-hours` | 高峰时段分析 |
| `GET` | `/reports/revenue` | 收入报表 |
| `GET` | `/reports/user-activity` | 用户活跃度 |
| `GET` | `/reports/export` | 导出 CSV/Excel |

---

## 4. 核心业务流程 / Core Business Flows

### 4.1 预订流程 / Booking Flow

```
User                                   Server                              DB/Redis
 │                                       │                                    │
 │  1. Browse/Search rooms               │                                    │
 │  GET /rooms/search ──────────────────> │                                    │
 │                                       │  查询房间 + 缓存 ──────────────> Redis
 │                                       │  时段可用性 ──────────────────> PG Replica
 │  <── Room list with availability      │                                    │
 │                                       │                                    │
 │  2. View calendar                     │                                    │
 │  GET /rooms/{id}/calendar ──────────> │                                    │
 │                                       │  Query all valid bookings ─────> PG Replica
 │  <── Calendar data (available/booked) │                                    │
 │                                       │                                    │
 │  3. Pre-check (optional)              │                                    │
 │  POST /bookings/pre-check ──────────> │                                    │
 │                                       │  Lightweight conflict detection ─> PG Replica
 │  <── Available / Conflict + suggestion│                                    │
 │                                       │                                    │
 │  4. Confirm booking                   │                                    │
 │  POST /bookings ────────────────────> │                                    │
 │                                       │  ┌──────────────────────────────┐ │
 │                                       │  │ a. Acquire Redis Lock        │ │
 │                                       │  │    (room:date granularity)   │ │
 │                                       │  │ b. BEGIN TRANSACTION         │ │
 │                                       │  │ c. SELECT ... FOR UPDATE room│ │
 │                                       │  │ d. App-level conflict check  │ │
 │                                       │  │ e. INSERT INTO booking       │ │
 │                                       │  │ f. COMMIT ── Exclusion Const.│ │
 │                                       │  │    auto-validates, 冲突则回滚 │ │
 │                                       │  │ g. Release Redis Lock        │ │
 │                                       │  └──────────────────────────────┘ │
 │  <── Booking result                   │                                    │
 │                                       │                                    │
 │  5. Post-processing (async)           │                                    │
 │                                       │  Send WeChat notification ───> Redis Streams
 │                                       │  Invalidate cache ────────────> Redis Pub/Sub
 │                                       │  Write audit log ─────────────> PG
```

### 4.2 冲突检测逻辑 / Conflict Detection Logic

```java
/**
 * Multi-layer conflict detection for booking.
 *
 * Layer 1: Pre-check (Replica, no lock)  — fast feedback to user
 * Layer 2: Redis distributed lock         — serialize same-room same-date writes
 * Layer 3: Application-level check        — SELECT COUNT with tsrange && operator
 * Layer 4: PostgreSQL Exclusion Constraint — database-level final safeguard
 */
@Service
public class BookingService {

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public BookingResult createBooking(CreateBookingRequest req) {
        String lockKey = "booking:lock:room:" + req.getRoomId()
                       + ":date:" + req.getBookingDate();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Layer 2: Redis distributed lock — timeout 5s
            if (!lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.BOOKING_BUSY,
                    "系统繁忙，请稍后重试");
            }

            // Layer 3: Pessimistic lock on room row
            Room room = roomMapper.selectByIdForUpdate(req.getRoomId());
            if (room.getStatus() != RoomStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.ROOM_UNAVAILABLE, "该会议室暂不可用");
            }

            // App-level overlap check
            int conflicts = bookingMapper.countConflicts(
                req.getRoomId(), req.getBookingDate(),
                req.getStartTime(), req.getEndTime());
            if (conflicts > 0) {
                throw new BusinessException(ErrorCode.SLOT_CONFLICT, "该时段已被预订");
            }

            // Insert — Layer 4: Exclusion Constraint auto-validates
            Booking booking = buildBooking(req, room);
            bookingMapper.insert(booking);

            // Post-commit async hooks
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.sendBookingConfirmation(booking);
                        cacheService.evictRoomAvailability(
                            booking.getRoomId(), booking.getBookingDate());
                    }
                });

            return BookingResult.success(booking);

        } catch (DataIntegrityViolationException e) {
            // PG error code 23P01 = exclusion constraint violation
            if (e.getMostSpecificCause() != null
                && e.getMostSpecificCause().getMessage().contains("excl_booking_no_overlap")) {
                throw new BusinessException(ErrorCode.SLOT_CONFLICT,
                    "该时段已被他人抢先预订");
            }
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

**冲突检测 SQL（利用 GiST 索引）**：

```sql
-- App-layer conflict check, O(log N) via GiST index
SELECT COUNT(*)
FROM booking
WHERE room_id = #{roomId}
  AND booking_date = #{bookingDate}
  AND status NOT IN ('CANCELLED', 'EXPIRED')
  AND time_slot && tsrange(
      (#{bookingDate} + #{startTime})::timestamp,
      (#{bookingDate} + #{endTime})::timestamp,
      '[)'
  );

-- Find nearest available slot after conflict (CTE with window function)
WITH conflicts AS (
    SELECT time_slot
    FROM booking
    WHERE room_id = #{roomId}
      AND booking_date = #{bookingDate}
      AND status NOT IN ('CANCELLED', 'EXPIRED')
    ORDER BY time_slot
),
gaps AS (
    SELECT
        upper(time_slot) AS gap_start,
        lead(lower(time_slot)) OVER (ORDER BY time_slot) AS gap_end,
        lead(lower(time_slot)) OVER (ORDER BY time_slot) - upper(time_slot) AS gap_duration
    FROM conflicts
)
SELECT gap_start, gap_end, gap_duration
FROM gaps
WHERE gap_duration >= interval '#{minDuration} minutes'
ORDER BY gap_start;
```

### 4.3 取消流程 / Cancellation Flow

```
User clicks Cancel
    │
    ▼
DELETE /bookings/{id}
    │
    ▼
┌───────────────────────────────────┐
│ Validation:                        │
│ - Booking belongs to current user? │
│ - Status is CONFIRMED?             │
│ - Before cancellation deadline?    │
│   (e.g. 1 hour before start)       │
└──────────────┬────────────────────┘
               │ Pass
               ▼
┌───────────────────────────────────┐
│ Transaction:                       │
│ - UPDATE booking SET              │
│     status = 'CANCELLED',         │
│     cancelled_at = now(),         │
│     cancelled_by = userId         │
│   WHERE id=? AND version=?        │
│   (optimistic lock)               │
│ - If paid, create refund record   │
│ - If recurring, prompt user to    │
│   cancel single or all instances  │
└──────────────┬────────────────────┘
               │ Commit
               ▼
┌───────────────────────────────────┐
│ Async:                             │
│ - Send WeChat notification        │
│ - Invalidate Redis cache          │
│ - Notify waitlist users if any    │
└───────────────────────────────────┘
```

### 4.4 重复预订流程 / Recurring Booking

```
POST /bookings/recurring
    │
    ▼
┌───────────────────────────────────────┐
│ 1. Validate the master time slot      │
│ 2. Create RecurringRule               │
│ 3. Generate instances (next 30 days): │
│    - Iterate daysOfWeek × date range  │
│    - Conflict check per date          │
│    - No conflict → CREATE booking     │
│    - Conflict   → log & skip          │
│ 4. Scheduled job: generates upcoming  │
│    instances weekly (maintain ~60 day │
│    buffer)                            │
└───────────────────────────────────────┘
```

---

## 5. 并发策略 / Concurrency Strategy

### 5.1 四层防护架构 / Defense in Depth

```
                    Incoming Request
                          │
       ┌──────────────────▼──────────────────┐
       │  Layer 1: Redis Distributed Lock     │  Performance Layer
       │  Key: booking:lock:room:{id}:date    │  Serializes writes to same room+date
       │  TTL: 5s                             │  Minimizes contention scope
       └──────────────────┬──────────────────┘
                          │ Lock acquired
       ┌──────────────────▼──────────────────┐
       │  Layer 2: App-level Conflict Check   │  Application Layer
       │  SELECT COUNT(*) WHERE time_slot &&  │  Fast fail with user-friendly feedback
       │  Uses GiST index, O(log N)           │
       └──────────────────┬──────────────────┘
                          │ No conflict
       ┌──────────────────▼──────────────────┐
       │  Layer 3: PG Pessimistic Lock        │  Transaction Layer
       │  SELECT ... FOR UPDATE on room       │  Locks target row within transaction
       │  + Optimistic lock (version)         │  Prevents concurrent modification
       └──────────────────┬──────────────────┘
                          │
       ┌──────────────────▼──────────────────┐
       │  Layer 4: PG Exclusion Constraint    │  Database Safeguard
       │  EXCLUDE USING GIST (                │  Even if all above layers fail,
       │    room_id =, time_slot &&           │  this one WON'T. PG rejects the
       │  )                                    │  overlapping INSERT with 23P01.
       └──────────────────────────────────────┘
```

### 5.2 各场景防护分析

| Scenario | Guard Layer | Outcome |
|----------|-------------|---------|
| Two users book same room + same slot simultaneously | Redis Lock serializes | First succeeds, second fails in conflict check → "时段已被预订" |
| Redis Lock times out, second request enters | Pessimistic lock + conflict check + Exclusion Constraint | First already committed, overlap detected → conflict |
| Two different rooms, same time slot | Redis Lock keys differ (`room:1:date` vs `room:2:date`) | Full parallelism, no contention |
| Network partition causes Redis Lock false release | Pessimistic lock + Exclusion Constraint | DB-level guarantee holds |
| Application bug causes detection logic to fail | Exclusion Constraint | PG directly rejects overlapping INSERT → SQL error 23P01 |

### 5.3 性能估算 / Performance Estimate

- **Redis Lock granularity**: `booking:lock:room:{id}:date:{date}`
  - ~200 rooms, peak ~10% concurrently booked → ~20 locks active simultaneously
  - Each lock held < 100ms (transaction duration) → negligible wait time
- **GiST index INSERT**: O(log N), < 5ms even with millions of bookings
- **Conflict check query**: GiST index lookup, < 5ms
- **Overall booking response time**: P99 < 500ms (with Redis lock wait)

### 5.4 替代方案对比 / Alternatives Comparison

| Approach | Pros | Cons | Recommendation |
|----------|------|------|----------------|
| **PG Exclusion Constraint + Redis Lock (chosen)** | High performance, strongest correctness, simple implementation | Depends on PG-specific features | **Highly Recommended** |
| MySQL + SELECT FOR UPDATE + app-level check | No PG dependency | All conflict detection in app layer, complex and error-prone | Fallback option |
| Pure Redis distributed lock | Extremely fast | **Unsafe**: no persistence guarantee, network partition → double booking | **Not Recommended** |
| Pure optimistic lock (version) | Simple | Only detects UPDATE conflicts, NOT INSERT conflicts (ABA problem) | Insufficient alone |

---

## 6. 部署架构 / Deployment Architecture

### 6.1 基础设施拓扑 / Infrastructure Topology

```
                         ┌──────────────────────┐
                         │   DNS (DNSPod)        │
                         │   + CDN (腾讯云 CDN)   │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │   CLB (Load Balancer) │
                         │   + WAF (Web Firewall) │
                         └──────────┬───────────┘
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
    ┌────────▼────────┐   ┌────────▼────────┐   ┌────────▼────────┐
    │  App Server 1   │   │  App Server 2   │   │  App Server 3   │
    │  Spring Boot    │   │  Spring Boot    │   │  Spring Boot    │
    │  4C8G           │   │  4C8G           │   │  4C8G           │
    └────────┬────────┘   └────────┬────────┘   └────────┬────────┘
             │                      │                      │
             └──────────────────────┼──────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
┌───────▼────────┐       ┌─────────▼─────────┐       ┌────────▼────────┐
│ PostgreSQL 16  │       │  Redis 7 Cluster  │       │  Tencent COS    │
│ Primary 8C16G  │       │  3 Nodes          │       │  (Images/Files) │
│ Replica 4C16G  │       │  2C4G each        │       │                 │
│ SSD 500GB      │       │  Sentinel mode    │       │                 │
└────────────────┘       └───────────────────┘       └─────────────────┘
```

### 6.2 容量规划 / Capacity Planning

| Component | Spec | Quantity | Notes |
|-----------|------|----------|-------|
| App Server | 4C8G | 3 (min) ~ 10 (peak) | HPA auto-scaling, CPU > 70% trigger |
| PostgreSQL Primary | 8C16G, SSD 500GB | 1 | Writes only, no sharding needed at this scale |
| PostgreSQL Replica | 4C16G, SSD 500GB | 1~2 | Reads, load-balanced via Pgpool-II |
| Redis | 2C4G | 3 (Cluster) | Sentinel mode with auto-failover |
| CLB | - | 1 | Tencent Cloud CLB, bandwidth on-demand |

### 6.3 监控体系 / Monitoring Stack

```
App Metrics (Micrometer + Prometheus Registry)
    ├── JVM Metrics (heap, GC, threads)
    ├── HTTP Metrics (QPS, latency, error rate)
    ├── Business Metrics (booking count, conflict rate)
    └── Custom Metrics (Redis lock wait time, DB pool usage)

    ──> Prometheus (scrape / 15s)
           ├──> Grafana Dashboards
           │    ├── Business: 实时预订量、空间使用率、收入
           │    ├── Technical: QPS, P99 Latency, Error Rate, DB Pool
           │    └── Alert: 异常状态集中展示
           │
           └──> AlertManager
                ├── P99 > 2s → 企业微信通知
                ├── Error Rate > 1% → 企业微信通知
                ├── DB Pool > 80% → 企业微信通知
                └── Redis Down → 电话告警

Tracing (Apache SkyWalking)
    └── End-to-end trace, locate slow queries and distributed bottlenecks
```

### 6.4 CI/CD Pipeline

```
Git Push (main branch)
    │
    ▼
GitHub Actions / 腾讯云 CODING
    ├──> Build: mvn clean package -Pprod
    │    ├── Checkstyle + SpotBugs
    │    ├── Unit Tests (JUnit 5 + Testcontainers)
    │    └── Docker Build + Push → TCR (腾讯云容器镜像)
    │
    ├──> Deploy Staging
    │    ├── Integration Tests
    │    └── Auto-approve
    │
    └──> Deploy Production (Blue-Green)
         ├── Start new version Pods (Green)
         ├── Health check → switch CLB weight
         ├── Observe 10 min (canary)
         ├── Decommission old Pods (Blue)
         └── Auto-rollback on anomaly
```

---

## 7. 项目结构 / Project Structure

### 7.1 Monorepo 目录树

```
wechat-room-booking/
├── README.md
├── .gitignore
├── docker-compose.yml              # 本地开发环境 (PG + Redis)
├── pom.xml                          # Maven Parent POM
│
├── docs/
│   ├── architecture.md
│   ├── api-spec/
│   │   └── openapi.yaml
│   ├── db/
│   │   ├── schema.sql              # 完整建表 DDL
│   │   └── seed.sql                # 种子数据
│   └── diagrams/
│       └── er-diagram.puml
│
├── backend/                         # ====== Java Spring Boot ======
│   ├── pom.xml
│   ├── booking-server/
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/booking/
│   │       ├── BookingApplication.java
│   │       ├── config/
│   │       │   ├── RedisConfig.java
│   │       │   ├── SecurityConfig.java
│   │       │   ├── MyBatisPlusConfig.java
│   │       │   ├── RedissonConfig.java
│   │       │   └── SchedulerConfig.java
│   │       ├── controller/          # REST controllers (thin layer)
│   │       │   ├── AuthController.java
│   │       │   ├── RoomController.java
│   │       │   ├── BookingController.java
│   │       │   ├── CheckInController.java
│   │       │   ├── AdminRoomController.java
│   │       │   ├── AdminBookingController.java
│   │       │   └── ReportController.java
│   │       ├── service/             # Business logic
│   │       │   ├── AuthService.java
│   │       │   ├── RoomService.java
│   │       │   ├── BookingService.java           # 核心预订逻辑
│   │       │   ├── BookingConflictDetector.java  # 冲突检测
│   │       │   ├── RecurringBookingService.java
│   │       │   ├── CheckInService.java
│   │       │   ├── NotificationService.java
│   │       │   ├── AdminService.java
│   │       │   └── ReportService.java
│   │       ├── repository/mapper/   # MyBatis-Plus Mappers
│   │       │   ├── UserMapper.java
│   │       │   ├── RoomMapper.java
│   │       │   ├── BookingMapper.java
│   │       │   └── ...
│   │       ├── entity/              # PO (Persistent Objects)
│   │       │   ├── User.java
│   │       │   ├── Room.java
│   │       │   ├── Booking.java
│   │       │   └── ...
│   │       ├── dto/
│   │       │   ├── request/
│   │       │   │   ├── CreateBookingRequest.java
│   │       │   │   └── RoomSearchRequest.java
│   │       │   └── response/
│   │       │       ├── BookingResult.java
│   │       │       └── RoomDetailVO.java
│   │       ├── enums/
│   │       │   ├── BookingStatus.java
│   │       │   ├── PaymentStatus.java
│   │       │   └── RecurrenceType.java
│   │       ├── exception/
│   │       │   ├── BusinessException.java
│   │       │   ├── ErrorCode.java
│   │       │   └── GlobalExceptionHandler.java
│   │       ├── security/
│   │       │   ├── JwtTokenProvider.java
│   │       │   ├── WeChatAuthService.java        # code2session
│   │       │   ├── WeChatPhoneDecryptor.java
│   │       │   ├── JwtAuthFilter.java
│   │       │   └── UserContext.java              # ThreadLocal
│   │       ├── scheduler/          # 定时任务
│   │       │   ├── RecurringBookingGenerator.java
│   │       │   ├── ExpiredBookingCleaner.java
│   │       │   └── BookingReminderSender.java
│   │       ├── cache/
│   │       │   ├── RoomCacheManager.java
│   │       │   └── AvailabilityCacheManager.java
│   │       └── util/
│   │           ├── TimeSlotUtil.java
│   │           └── BookingNoGenerator.java
│   └── booking-common/              # 公共模块
│       └── src/main/java/com/example/booking/common/
│           ├── Result.java
│           ├── PageResult.java
│           └── constants/
│
├── miniapp/                         # ====== Taro 3 小程序 ======
│   ├── package.json
│   ├── tsconfig.json
│   ├── project.config.json
│   ├── src/
│   │   ├── app.tsx
│   │   ├── app.config.ts
│   │   ├── app.scss
│   │   ├── pages/
│   │   │   ├── index/               # 首页（门店列表）
│   │   │   ├── search/              # 搜索会议室
│   │   │   ├── room-detail/         # 房间详情
│   │   │   ├── booking/             # 预订确认页
│   │   │   ├── booking-success/     # 预订成功
│   │   │   ├── my-bookings/         # 我的预订
│   │   │   ├── booking-detail/      # 预订详情
│   │   │   ├── check-in/            # 签到（扫码）
│   │   │   └── profile/             # 个人中心
│   │   ├── components/
│   │   │   ├── RoomCard/
│   │   │   ├── Calendar/            # 日期选择器（含可用性标记）
│   │   │   ├── TimeSlotPicker/      # 时间段选择器
│   │   │   ├── VenueSwitcher/
│   │   │   └── FilterBar/
│   │   ├── services/                # API 调用层
│   │   │   ├── api.ts               # wx.request 封装 + 拦截器
│   │   │   ├── auth.ts
│   │   │   ├── rooms.ts
│   │   │   └── bookings.ts
│   │   ├── store/                   # Zustand / Redux
│   │   ├── hooks/
│   │   ├── utils/
│   │   └── constants/
│   └── config/
│
├── admin/                           # ====== Vue 3 管理后台 ======
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/
│       ├── pages/
│       │   ├── dashboard/           # 数据大盘
│       │   ├── room-manage/         # 会议室管理
│       │   ├── booking-manage/      # 预订管理
│       │   ├── schedule/            # 排期/可用性管理
│       │   └── reports/             # 报表
│       ├── components/
│       ├── services/
│       └── store/
│
└── deploy/                          # ====== 部署配置 ======
    ├── docker/
    │   ├── Dockerfile.backend
    │   └── Dockerfile.frontend
    ├── k8s/
    │   ├── namespace.yaml
    │   ├── deployment-backend.yaml
    │   ├── service-backend.yaml
    │   ├── configmap.yaml
    │   ├── secret.yaml
    │   └── hpa.yaml
    ├── nginx/
    │   └── nginx.conf
    └── flyway/
        ├── V1__init_schema.sql
        ├── V2__add_indexes.sql
        └── V3__seed_amenities.sql
```

### 7.2 关键设计决策总结

| 决策 / Decision | 选择 / Choice | 理由 / Rationale |
|---|---|---|
| Backend language | Java + Spring Boot 3.2 | 事务管理成熟，ORM 生态好，适合复杂业务 |
| Database | PostgreSQL 16 | `tsrange` + Exclusion Constraint 是防超卖的终极方案 |
| Mini program framework | Taro 3 (React) | 组件可复用至 H5 管理后台 |
| ORM | MyBatis-Plus + jOOQ | CRUD 用 MP；复杂查询用 jOOQ 类型安全 SQL |
| Cache | Redis 7 Stack | 缓存 + 分布式锁 + Streams 三位一体 |
| Object storage | Tencent Cloud COS | 与微信生态同云，内网免流量 |
| Deployment | Kubernetes (TKE) | 自动扩缩容、滚动更新、蓝绿部署 |
| DB Migration | Flyway | 版本化 SQL 迁移，与 Spring Boot 深度集成 |
| Monitoring | Prometheus + Grafana + SkyWalking | 指标 + 链路追踪全覆盖 |

---

## 8. 附录：关键代码片段

### A.1 BookingNo Generator

```java
@Component
public class BookingNoGenerator {

    private final StringRedisTemplate redisTemplate;

    // 预订编号：日期 + Redis 自增序号 → "BK202605280001"
    public String generate(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "booking:seq:" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        redisTemplate.expire(redisKey, Duration.ofDays(2));
        return "BK" + dateStr + String.format("%04d", seq);
    }
}
```

### A.2 Exclusion Constraint 详解

```sql
-- PostgreSQL Exclusion Constraint — 防超卖的数据库最后防线
-- 使用 btree_gist 扩展，允许在 GiST 索引上使用标量 = 操作符
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking ADD CONSTRAINT excl_booking_no_overlap
    EXCLUDE USING GIST (
        room_id WITH =,        -- 同一房间才检查
        time_slot WITH &&      -- 时间段有交集 (overlap) 即冲突
    ) WHERE (status NOT IN ('CANCELLED', 'EXPIRED'));

-- && 操作符利用 GiST 索引，百万级数据查询 < 5ms
-- WHERE 子句排除已取消/过期预订，避免约束范围过大
```

### A.3 微信登录 / WeChat Login

```java
@Service
public class WeChatAuthService {

    @Value("${wechat.miniapp.appid}")
    private String appId;

    @Value("${wechat.miniapp.secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public WeChatSession code2Session(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid=" + appId
            + "&secret=" + appSecret
            + "&js_code=" + code
            + "&grant_type=authorization_code";

        WeChatSessionResponse resp = restTemplate.getForObject(url, WeChatSessionResponse.class);
        if (resp.getErrcode() != 0) {
            throw new BusinessException(ErrorCode.WECHAT_AUTH_FAILED, resp.getErrmsg());
        }
        return new WeChatSession(resp.getOpenid(), resp.getUnionid(), resp.getSessionKey());
    }
}
```

### A.4 Booking 状态机

```
                    ┌─────────┐
                    │ PENDING │  待支付（超时自动过期）
                    └────┬────┘
                         │ 支付成功
                    ┌────▼──────┐
                    │ CONFIRMED │  已确认
                    └────┬──────┘
               ┌─────────┼─────────┐
               │ 取消     │ 签到     │ 管理员取消
          ┌────▼───┐ ┌───▼─────┐ ┌▼──────────┐
          │CANCELLED│ │CHECKED_IN│ │CANCELLED  │
          └─────────┘ └───┬─────┘ └───────────┘
                          │ 签退
                     ┌────▼──────┐
                     │ COMPLETED │
                     └───────────┘

    PENDING 超时 (15 min) → EXPIRED（定时任务清理）
```

### A.5 本地开发环境 docker-compose.yml

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: room_booking
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    ports:
      - "5432:5432"
    volumes:
      - pg_data:/var/lib/postgresql/data
      - ./docs/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes

volumes:
  pg_data:
```

---

> **文档版本**: v1.0
> **最后更新**: 2026-05-24
> **作者**: AI 架构设计 / AI Architecture Design

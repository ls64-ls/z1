# 系统架构

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│  ┌──────────────────────┐  ┌─────────────────────────────┐  │
│  │  Taro 3 微信小程序    │  │  Vue 3 管理后台 (Element+)  │  │
│  │  (WeChat MiniProgram) │  │  (Admin Dashboard)          │  │
│  └──────────┬───────────┘  └──────────────┬──────────────┘  │
└─────────────┼──────────────────────────────┼────────────────┘
              │ HTTPS (WX) / HTTP             │ HTTP
              ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       API 网关层                             │
│              Nginx (生产) / 直连 (开发)                      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.2.5                         │
│                   (booking-server :8088)                     │
│                                                              │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ JWT 认证  │  │ Controller   │  │ GlobalException      │  │
│  │ Filter   │  │ Layer (8)    │  │ Handler              │  │
│  └──────────┘  └──────┬───────┘  └──────────────────────┘  │
│                        │                                     │
│  ┌─────────────────────┼──────────────────────────────────┐ │
│  │           Service Layer (9 services)                    │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │ │
│  │  │ Booking  │ │ Room     │ │ Auth     │ │ CheckIn  │  │ │
│  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │  │ │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘  │ │
│  │       │             │            │            │         │ │
│  │  ┌────┴─────────────┴────────────┴────────────┴────┐   │ │
│  │  │              Cache Layer                         │   │ │
│  │  │  RoomCacheManager / AvailabilityCacheManager     │   │ │
│  │  └────────────────────┬────────────────────────────┘   │ │
│  └───────────────────────┼────────────────────────────────┘ │
│                          │                                   │
│  ┌───────────────────────┼────────────────────────────────┐ │
│  │          Repository / Mapper Layer (MyBatis-Plus)      │ │
│  │          12 Mapper Interfaces                           │ │
│  └───────────────────────┼────────────────────────────────┘ │
└──────────────────────────┼──────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
┌──────────────────────┐  ┌──────────────────────────┐
│   PostgreSQL 16      │  │      Redis 7              │
│   (port 5434)        │  │      (port 6380)          │
│                      │  │                           │
│  • 12 tables         │  │  • Cache-aside (rooms,    │
│  • GiST index        │  │    availability)          │
│  • Exclusion constr. │  │  • Redisson 分布式锁       │
│  • tsrange type      │  │  • Redis Streams (通知)    │
│  • Flyway migration  │  │  • AOF 持久化              │
└──────────────────────┘  └──────────────────────────┘
```

## 分层设计

### Controller 层（8 个控制器）

负责 HTTP 请求解析、参数校验、JWT 用户上下文获取，不包含业务逻辑。

| 控制器 | 路径前缀 | 职责 |
|--------|----------|------|
| `AuthController` | `/api/v1/auth` | 微信登录、Token 刷新、开发登录 |
| `VenueController` | `/api/v1/venues` | 场馆列表（公开） |
| `RoomController` | `/api/v1/rooms` | 房间搜索、详情、可用时段 |
| `BookingController` | `/api/v1/bookings` | 预订 CRUD、预检、取消 |
| `CheckInController` | `/api/v1/checkin` | 签到 / 签退 |
| `AdminRoomController` | `/api/v1/admin/rooms` | 管理端房间管理 |
| `AdminBookingController` | `/api/v1/admin/bookings` | 管理端预订管理 |
| `ReportController` | `/api/v1/reports` | 使用率 / 收入报表 |

### Service 层（9 个服务）

核心业务逻辑，事务管理，缓存策略，异步通知。

| 服务 | 关键职责 |
|------|----------|
| `BookingService` | 四层冲突防护、预订 CRUD、预检、建议最近可用时段 |
| `RecurringBookingService` | 重复规则管理、60 天缓冲实例生成 |
| `RoomService` | 缓存优先的房间查询、可用时段计算 |
| `VenueService` | 场馆列表 + 房间数统计 |
| `AuthService` | 微信 code2session、JWT 签发、用户创建 |
| `CheckInService` | 签到状态流转 |
| `NotificationService` | 异步 Redis Streams 消息推送 |
| `AdminRoomService` | 房间 CRUD + 设施关联管理 |
| `AdminBookingService` | 管理端预订查询 + 状态变更 |
| `ReportService` | 使用率 / 收入聚合统计 |

### Repository 层

MyBatis-Plus BaseMapper，12 个 Mapper 接口对应 12 张表。`BookingMapper` 额外包含自定义 `countConflicts()` 方法做 GiST 冲突检测。

## 认证流程

```
小程序用户                    后端
    │                          │
    │  1. wx.login() → code    │
    │  2. POST /auth/login     │
    │     { code }             │
    │                          │  3. WeChat code2Session API
    │                          │     → openid, session_key
    │                          │  4. 查找或创建 User
    │                          │  5. 签发 JWT (2h)
    │  6. 存储 token           │
    │                          │
    │  7. 后续请求携带          │
    │     Authorization:       │
    │     Bearer <token>       │
    │                          │  8. JwtAuthFilter 校验
    │                          │  9. UserContext.set()
```

### 公开端点（无需认证）

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/dev-login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/venues`
- `GET /api/v1/rooms/**`
- `/error`

## 缓存策略

采用 Cache-Aside 模式：

```
读取:
  1. 查 Redis
  2. 命中 → 返回
  3. 未命中 → 查 DB → 写入 Redis → 返回

写入:
  1. 更新 DB
  2. 删除 Redis 对应 key（惰性加载）
```

| 缓存域 | TTL | Key 格式 |
|--------|-----|----------|
| 房间列表（按场馆） | 30 min | `room:list:venue:{venueId}` |
| 房间详情 | 30 min | `room:detail:{roomId}` |
| 可用时段 | 5 min | `availability:{roomId}:{date}` |

## 预订冲突防护（四层）

```
用户请求
   │
   ▼
┌────────────────────────────────────────────┐
│ Layer 1: Redis 分布式锁                     │
│ Key: booking:lock:room:{id}:date:{date}    │
│ tryLock(3s wait, 5s lease)                 │
│ 获取失败 → 返回 "系统繁忙"                   │
└────────────────────┬───────────────────────┘
                     │ 获取成功
                     ▼
┌────────────────────────────────────────────┐
│ Layer 2: 应用层冲突检查                      │
│ bookingMapper.countConflicts(room, date,   │
│   start, end)                              │
│ 利用 GiST 索引高效查询区间重叠                │
│ 冲突数 > 0 → 返回 "时段冲突"                 │
└────────────────────┬───────────────────────┘
                     │ 无冲突
                     ▼
┌────────────────────────────────────────────┐
│ Layer 3: 插入 booking 记录                  │
│ time_slot 字段写入 tsrange                   │
│ PostgreSQL 自动执行排他约束:                  │
│ EXCLUDE USING GIST (room_id WITH =,        │
│   time_slot WITH &&)                       │
│ WHERE status NOT IN ('CANCELLED','EXPIRED')│
│ 约束违反 → DataIntegrityViolationException  │
└────────────────────┬───────────────────────┘
                     │ 插入成功
                     ▼
┌────────────────────────────────────────────┐
│ Layer 4: 乐观锁                             │
│ version 字段防止并发 UPDATE 覆盖              │
└────────────────────────────────────────────┘
   │
   ▼
Post-commit 钩子:
  • 发送 Redis Streams 通知
  • 清除可用时段缓存
```

## 异步通知

使用 Spring `@Async` + Redis Streams 实现非阻塞通知：

```
BookingService.create()
  └─ TransactionSynchronization.afterCommit()
       └─ notificationService.sendBookingConfirmation()
            └─ StreamRecords.newRecord()
                 .in("stream:wechat:notifications")
                 .ofMap({ type: "booking_confirmed", ... })
```

消息类型：
- `booking_confirmed` — 预订确认
- `booking_cancelled` — 预订取消
- `booking_reminder` — 即将开始的提醒

## 定时任务

| 调度器 | 频率 | 职责 |
|--------|------|------|
| `RecurringBookingGenerator` | 每天 02:30 | 为重复规则生成 60 天缓冲的预订实例 |
| `BookingReminderSender` | 每 15 分钟 | 检查未来 1 小时内开始的预订，发送提醒 |
| `BookingExpireScheduler` | 每小时 | 将过期未签到的预订标记为 EXPIRED |

## 微信小程序前端架构

```
Pages (8 pages)
  ├── index/             首页 — 场馆列表 + 房间卡片
  ├── search/            搜索 — 筛选条件 + 结果列表
  ├── room-detail/       详情 — 日历 + 时段选择 + 预订入口
  ├── booking/           预订 — 表单填写 + 费用计算
  ├── booking-success/   成功 — 摘要展示
  ├── booking-detail/    详情 — 完整预订信息 + 取消/签到
  ├── my-bookings/       我的预订 — 分状态列表
  ├── check-in/          签到 — 签到操作
  └── profile/           个人中心

Components (3 components)
  ├── Calendar           日历选择器（月份切换、可用标记、今天标识）
  ├── RoomCard           房间卡片（图片、设施、价格、预订按钮）
  └── TimeSlotPicker     时段选择器（空闲/已约状态）

State (Zustand)
  ├── bookingStore       预订流程状态（房间、日期、起止时间）
  └── userStore          用户登录状态

Services (6 API modules)
  ├── api.ts             HTTP 客户端（Taro.request 封装）
  ├── auth.ts            登录 API
  ├── bookings.ts        预订 CRUD API
  ├── rooms.ts           房间 API
  ├── venues.ts          场馆 API
  └── checkin.ts         签到 API
```

## 管理后台前端架构

```
Pages (5 pages)
  ├── dashboard/         仪表盘
  ├── room-manage/       房间管理（CRUD + 上下架）
  ├── booking-manage/    预订管理（列表 + 状态变更）
  ├── schedule/          排期视图
  └── reports/           报表

State (Pinia)
  └── store/index.ts     全局状态

Services (4 API modules)
  ├── api.ts             Axios 实例
  ├── bookings.ts        预订管理 API
  ├── rooms.ts           房间管理 API
  └── venues.ts          场馆 API
```

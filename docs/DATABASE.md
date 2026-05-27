# 数据库设计

## 概览

- **数据库**: PostgreSQL 16
- **数据库名**: `room_booking`
- **表数量**: 12 张
- **字符集**: UTF-8
- **时区**: `TIMESTAMPTZ`（带时区时间戳）
- **迁移工具**: Flyway 10.11

## ER 关系图

```
organization (1) ──< (N) venue (1) ──< (N) room (1) ──< (N) booking
                                                          │
                          room (1) ──< (N) room_image     │
                          room (1) ──< (N) room_amenity >─ amenity
                          room (1) ──< (N) availability_rule
                          room (1) ──< (N) blocked_time_slot
                          venue(1) ──< (N) blocked_time_slot
                                                          │
                          user (1) ──< (N) booking ──< (1) booking_check_in
                          user (1) ──< (N) recurring_rule
```

## 表结构

### 1. organization — 组织 / 公司

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `name` | VARCHAR(200) | NOT NULL | 组织名称 |
| `logo_url` | VARCHAR(500) | | Logo URL |
| `contact` | VARCHAR(100) | | 联系人 |
| `phone` | VARCHAR(30) | | 联系电话 |
| `address` | TEXT | | 地址 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | ACTIVE / INACTIVE / SUSPENDED |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

### 2. venue — 门店 / 场馆

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `organization_id` | BIGINT | NOT NULL, FK → organization | 所属组织 |
| `name` | VARCHAR(200) | NOT NULL | 场馆名称 |
| `address` | TEXT | NOT NULL | 详细地址 |
| `contact_person` | VARCHAR(100) | | 联系人 |
| `phone` | VARCHAR(30) | | 联系电话 |
| `description` | TEXT | | 描述 |
| `images_json` | JSONB | | 图片列表 (JSON 数组) |
| `latitude` | DOUBLE PRECISION | | 纬度 |
| `longitude` | DOUBLE PRECISION | | 经度 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | ACTIVE / INACTIVE / MAINTENANCE |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

### 3. room — 会议室

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `venue_id` | BIGINT | NOT NULL, FK → venue | 所属场馆 |
| `name` | VARCHAR(200) | NOT NULL | 房间名称 |
| `description` | TEXT | | 描述 |
| `capacity` | INT | NOT NULL, DEFAULT 0 | 容纳人数 |
| `area_sqm` | NUMERIC(8,2) | | 面积 (m²) |
| `floor` | INT | | 楼层 |
| `price_per_hour` | NUMERIC(10,2) | NOT NULL, DEFAULT 0 | 每小时价格 |
| `price_per_halfday` | NUMERIC(10,2) | | 半天价格 |
| `price_per_day` | NUMERIC(10,2) | | 全天价格 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | AVAILABLE / MAINTENANCE / OFFLINE |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 排序 |
| `version` | INT | NOT NULL, DEFAULT 1 | 乐观锁 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

### 4. amenity — 设施 / 设备

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `name` | VARCHAR(100) | NOT NULL | 设施名称 |
| `icon` | VARCHAR(100) | | 图标标识 |
| `category` | VARCHAR(50) | | 分类 |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 排序 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

默认种子数据：投影仪、白板、视频会议、WiFi、咖啡机、音响系统。

### 5. room_amenity — 房间-设施关联（多对多）

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `room_id` | BIGINT | NOT NULL, FK → room ON DELETE CASCADE | |
| `amenity_id` | BIGINT | NOT NULL, FK → amenity ON DELETE CASCADE | |
| UNIQUE (room_id, amenity_id) | | | 防止重复关联 |

### 6. room_image — 房间图片

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `room_id` | BIGINT | NOT NULL, FK → room ON DELETE CASCADE | |
| `url` | VARCHAR(500) | NOT NULL | 图片 URL |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 排序 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

### 7. user — 微信用户

> 表名带引号 `"user"` 因为 `user` 是 PostgreSQL 保留字。

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `openid` | VARCHAR(100) | NOT NULL, UNIQUE INDEX | 微信 OpenID |
| `unionid` | VARCHAR(100) | INDEX | 微信 UnionID |
| `nickname` | VARCHAR(100) | | 昵称 |
| `avatar_url` | VARCHAR(500) | | 头像 URL |
| `phone` | VARCHAR(30) | | 手机号 |
| `email` | VARCHAR(200) | | 邮箱 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | ACTIVE / BLACKLISTED / INACTIVE |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

### 8. booking — 预订记录（核心表）

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `booking_no` | VARCHAR(50) | NOT NULL, UNIQUE INDEX | 预订编号 |
| `user_id` | BIGINT | NOT NULL, FK → user | 预订人 |
| `room_id` | BIGINT | NOT NULL, FK → room | 会议室 |
| `booking_date` | DATE | NOT NULL | 预订日期 |
| `start_time` | TIME | NOT NULL | 开始时间 |
| `end_time` | TIME | NOT NULL | 结束时间 |
| `time_slot` | TSRANGE | NOT NULL | PostgreSQL 时间范围类型 |
| `title` | VARCHAR(300) | | 会议主题 |
| `purpose` | TEXT | | 用途说明 |
| `attendee_count` | INT | | 参会人数 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | PENDING/CONFIRMED/CHECKED_IN/COMPLETED/CANCELLED/EXPIRED/REJECTED |
| `remark` | TEXT | | 备注 |
| `version` | INT | NOT NULL, DEFAULT 1 | 乐观锁 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

**关键约束:**

```sql
-- 时间顺序约束
CONSTRAINT check_time_order CHECK (start_time < end_time)

-- 排他约束：同一房间、同一时段、有效状态不可重叠
ALTER TABLE booking
    ADD CONSTRAINT excl_booking_no_overlap
    EXCLUDE USING GIST (room_id WITH =, time_slot WITH &&)
    WHERE (status NOT IN ('CANCELLED', 'EXPIRED'));
```

这是整个系统的核心防护：PostgreSQL 的 GiST 索引 + 排他约束确保同一房间的有效预订不会在时间上重叠。`btree_gist` 扩展必须在建表前启用。

**索引:**

| 索引 | 类型 | 用途 |
|------|------|------|
| `idx_booking_status_date` | B-tree | 按状态+日期查询 |
| `idx_booking_user` | B-tree | 按用户查询我的预订 |
| `idx_booking_room_date` | B-tree | 按房间+日期查询冲突 |
| `idx_booking_no` | UNIQUE B-tree | 预订编号唯一 |
| GiST (隐式) | GiST | 排他约束自动创建的 GiST 索引 |

### 9. recurring_rule — 重复预订规则

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `user_id` | BIGINT | NOT NULL, FK → user | |
| `room_id` | BIGINT | NOT NULL, FK → room | |
| `title` | VARCHAR(300) | | 会议主题 |
| `purpose` | TEXT | | 用途 |
| `attendee_count` | INT | | 参会人数 |
| `start_date` | DATE | NOT NULL | 开始日期 |
| `end_date` | DATE | | 结束日期 |
| `start_time` | TIME | NOT NULL | 开始时间 |
| `end_time` | TIME | NOT NULL | 结束时间 |
| `repeat_type` | VARCHAR(20) | NOT NULL, CHECK | DAILY / WEEKLY / BIWEEKLY / MONTHLY |
| `repeat_days` | INT[] | | 重复的星期几 (ISO: 1=Mon, 7=Sun) |
| `status` | VARCHAR(20) | NOT NULL, CHECK | ACTIVE / PAUSED / STOPPED |
| `remark` | TEXT | | 备注 |
| `last_generated_date` | DATE | | 上次生成实例的截止日期 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

### 10. booking_check_in — 签到记录

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `booking_id` | BIGINT | NOT NULL, FK → booking | |
| `check_in_time` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | 签到时间 |
| `check_out_time` | TIMESTAMPTZ | | 签退时间 |
| `operator_id` | BIGINT | FK → user | 操作人 |
| `remark` | TEXT | | 备注 |

### 11. availability_rule — 可用时段规则

定义每个房间每周每天的开放时间。

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `room_id` | BIGINT | NOT NULL, FK → room ON DELETE CASCADE | |
| `day_of_week` | INT | NOT NULL, CHECK 1-7 | ISO 星期 (1=周一) |
| `open_time` | TIME | NOT NULL | 开放时间 |
| `close_time` | TIME | NOT NULL | 关闭时间 |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT true | 是否启用 |
| UNIQUE (room_id, day_of_week) | | | 每天一条规则 |

默认种子数据：所有房间周一至周五 08:00-22:00。

### 12. blocked_time_slot — 封禁时段

用于临时封锁某个房间或整个场馆的特定时段（如节假日、维护）。

| 列 | 类型 | 约束 | 说明 |
|----|------|------|------|
| `id` | BIGSERIAL | PK | 主键 |
| `room_id` | BIGINT | FK → room ON DELETE CASCADE | 目标房间（可与 venue_id 二选一） |
| `venue_id` | BIGINT | FK → venue ON DELETE CASCADE | 目标场馆（封锁全部房间） |
| `title` | VARCHAR(300) | | 标题 |
| `reason` | TEXT | | 原因 |
| `time_slot` | TSRANGE | NOT NULL | 封禁时间范围 |
| `blocked_date` | DATE | | 封禁日期 |
| `start_time` | TIME | | 开始时间 |
| `end_time` | TIME | | 结束时间 |
| `created_by` | BIGINT | FK → user | 操作人 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| CHECK(room_id IS NOT NULL OR venue_id IS NOT NULL) | | | 至少指定一个目标 |

---

## 状态流转

### 预订状态

```
PENDING ──→ CONFIRMED ──→ CHECKED_IN ──→ COMPLETED
   │            │
   └──→ EXPIRED │
                │
                └──→ CANCELLED
```

- `PENDING`: 刚创建，待支付确认
- `CONFIRMED`: 已确认（当前版本创建后直接 CONFIRMED）
- `CHECKED_IN`: 已签到
- `COMPLETED`: 已签退完成
- `CANCELLED`: 已取消（仅 PENDING/CONFIRMED 可取消）
- `EXPIRED`: 过期（超时未签到自动标记）

### 房间状态

```
AVAILABLE ←→ MAINTENANCE
   ↓
OFFLINE
```

### 场馆状态

```
ACTIVE ←→ INACTIVE
   ↓
MAINTENANCE
```

---

## tsrange 与排他约束说明

PostgreSQL 的 `tsrange` 类型存储时间范围 `[start, end)`，半开区间（包含起点，不含终点）。

GiST 索引上的排他约束 `EXCLUDE USING GIST` 是预订去重的保证：

```sql
EXCLUDE USING GIST (room_id WITH =, time_slot WITH &&)
WHERE (status NOT IN ('CANCELLED', 'EXPIRED'))
```

**工作原理:**
- `room_id WITH =` — 仅比较同一房间
- `time_slot WITH &&` — 使用 GiST 索引检查时间范围重叠
- `WHERE` 子句 — 已取消和已过期的预订不参与约束检查

这意味着：同一房间、同一时段（部分重叠也算）、两条有效状态的预订无法同时插入。

**Java 侧:** 使用 `TsRangeTypeHandler` 自定义 MyBatis 类型处理器，将 String 格式的 `"[\"2026-05-27 14:00\",\"2026-05-27 16:00\")"` 写入为 PostgreSQL `PGobject` 类型 `tsrange`。

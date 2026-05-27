# API 参考

基础路径: `http://localhost:8088/api/v1`

所有响应均为统一格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误码 `code != 0` 时，`message` 包含错误描述，`data` 为 `null`。

---

## 1. 认证接口 `/api/v1/auth`

### POST /api/v1/auth/login

微信小程序登录。

**请求体:**
```json
{
  "code": "wx_login_code"
}
```

**响应 data:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 7200,
  "user": {
    "id": 1,
    "nickname": "微信用户",
    "avatarUrl": "https://...",
    "phone": null
  }
}
```

### POST /api/v1/auth/dev-login

开发环境登录（无需微信 code）。

**请求体:**
```json
{
  "openid": "dev_test_user"
}
```

### POST /api/v1/auth/refresh

刷新 JWT Token。需携带当前有效 Token。

### GET /api/v1/auth/profile

获取当前登录用户信息。需认证。

---

## 2. 场馆接口 `/api/v1/venues`

### GET /api/v1/venues

获取全部活跃场馆列表。**无需认证。**

**响应 data:**
```json
[
  {
    "id": 1,
    "name": "光谷主店",
    "address": "武汉市洪山区...",
    "latitude": 30.5068,
    "longitude": 114.4172,
    "openTime": "08:00",
    "closeTime": "22:00",
    "timezone": "Asia/Shanghai",
    "status": "ACTIVE",
    "roomCount": 3
  }
]
```

---

## 3. 房间接口 `/api/v1/rooms`

### GET /api/v1/rooms

搜索 / 列表房间。**无需认证。**

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `venueId` | Long | 否 | 场馆 ID |
| `capacity` | Integer | 否 | 最小容纳人数 |
| `amenityIds` | List\<Long\> | 否 | 设施 ID 列表 |
| `page` | Integer | 否 | 页码，默认 1 |
| `size` | Integer | 否 | 每页条数，默认 10 |

**响应 data:**
```json
{
  "page": 1,
  "size": 10,
  "total": 3,
  "records": [
    {
      "id": 1,
      "name": "会议室A",
      "venueId": 1,
      "venueName": "光谷主店",
      "capacity": 6,
      "areaSqm": 25.0,
      "floor": 3,
      "description": "小型会议室...",
      "pricePerHour": 50.00,
      "pricePerHalfDay": 180.00,
      "pricePerDay": 300.00,
      "status": "AVAILABLE",
      "available": true,
      "remainingSlots": 8,
      "amenities": [
        { "id": 1, "name": "投影仪", "icon": "projector" }
      ],
      "images": ["https://..."],
      "coverImage": "https://..."
    }
  ]
}
```

### GET /api/v1/rooms/search

同上，额外支持时间和设施筛选。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `date` | String | 否 | 日期 (yyyy-MM-dd) |
| `startTime` | String | 否 | 开始时间 (HH:mm) |
| `endTime` | String | 否 | 结束时间 (HH:mm) |

### GET /api/v1/rooms/{id}

获取房间详情。**无需认证。**

**响应 data:** 单个 RoomVO 对象（结构同上）。

### GET /api/v1/rooms/{id}/availability

获取房间某天的可用时段。**无需认证。**

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `date` | String | 是 | 日期 (yyyy-MM-dd) |

**响应 data:**
```json
{
  "date": "2026-05-27",
  "dayOfWeek": 3,
  "slots": [
    { "start": "08:00", "end": "09:00", "available": true },
    { "start": "09:00", "end": "10:00", "available": false },
    { "start": "10:00", "end": "11:00", "available": true }
  ]
}
```

---

## 4. 预订接口 `/api/v1/bookings`

### POST /api/v1/bookings/pre-check

预订前冲突检测。**需认证。**

**请求体:**
```json
{
  "roomId": 1,
  "bookingDate": "2026-05-27",
  "startTime": "09:00",
  "endTime": "11:00"
}
```

**响应 data — 可用:**
```json
{
  "available": true,
  "conflictBooking": null,
  "suggestion": null
}
```

**响应 data — 冲突:**
```json
{
  "available": false,
  "conflictBooking": {
    "startTime": "09:00",
    "endTime": "11:00"
  },
  "suggestion": {
    "availableStart": "11:00",
    "availableEnd": "13:00"
  }
}
```

### POST /api/v1/bookings

创建预订。**需认证。**

**请求体:**
```json
{
  "roomId": 1,
  "bookingDate": "2026-05-27",
  "startTime": "14:00",
  "endTime": "16:00",
  "title": "项目评审会",
  "attendeeCount": 5,
  "remark": "需要投影仪",
  "recurring": null
}
```

**重复预订:**
```json
{
  "roomId": 1,
  "bookingDate": "2026-06-01",
  "startTime": "09:00",
  "endTime": "10:00",
  "title": "每周站会",
  "recurring": {
    "type": "WEEKLY",
    "daysOfWeek": [1, 3, 5],
    "endDate": "2026-07-31"
  }
}
```

**响应 data:**
```json
{
  "id": 42,
  "bookingNo": "BK20260527001",
  "status": "CONFIRMED"
}
```

### GET /api/v1/bookings

获取当前用户的预订列表。**需认证。**

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | String | 否 | 筛选状态 |
| `page` | Integer | 否 | 页码，默认 1 |
| `size` | Integer | 否 | 每页条数，默认 10 |

**状态枚举:**

| 值 | 中文 |
|----|------|
| `PENDING` | 待支付 |
| `CONFIRMED` | 已确认 |
| `CHECKED_IN` | 已签到 |
| `COMPLETED` | 已完成 |
| `CANCELLED` | 已取消 |
| `EXPIRED` | 已过期 |

**响应 data:**
```json
{
  "page": 1,
  "size": 10,
  "total": 5,
  "records": [
    {
      "id": 42,
      "bookingNo": "BK20260527001",
      "roomId": 1,
      "roomName": "会议室A",
      "venueName": "光谷主店",
      "userId": 1,
      "bookingDate": "2026-05-27",
      "startTime": "14:00",
      "endTime": "16:00",
      "title": "项目评审会",
      "attendeeCount": 5,
      "status": "CONFIRMED",
      "remark": "需要投影仪",
      "totalAmount": 100.00,
      "createdAt": "2026-05-27 10:30:00"
    }
  ]
}
```

### GET /api/v1/bookings/{id}

获取预订详情。**需认证。**

### POST /api/v1/bookings/{id}/cancel

取消预订。**需认证。**

**请求体（可选）:**
```json
{
  "reason": "会议取消"
}
```

### DELETE /api/v1/bookings/recurring/{ruleId}

取消整个重复预订序列。**需认证。**

---

## 5. 签到接口 `/api/v1/checkin`

### POST /api/v1/checkin

签到。**需认证。**

**请求体:**
```json
{
  "bookingId": 42
}
```

### POST /api/v1/checkin/checkout

签退。**需认证。**

**请求体:**
```json
{
  "bookingId": 42
}
```

---

## 6. 管理端接口

> 以下接口需要管理员权限（当前版本未做角色校验）。

### 6.1 房间管理 `/api/v1/admin/rooms`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/admin/rooms` | 创建房间 |
| `PUT` | `/api/v1/admin/rooms/{id}` | 更新房间 |
| `DELETE` | `/api/v1/admin/rooms/{id}` | 删除房间 |
| `PUT` | `/api/v1/admin/rooms/{id}/status` | 更新房间状态 |

**创建 / 更新请求体:**
```json
{
  "venueId": 1,
  "name": "新会议室",
  "capacity": 10,
  "pricePerHour": 80.00,
  "areaSqm": 40.0,
  "floor": 5,
  "description": "中型会议室",
  "amenityIds": [1, 2, 4]
}
```

**状态变更请求体:**
```json
{
  "status": "OFFLINE"
}
```

### 6.2 预订管理 `/api/v1/admin/bookings`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/admin/bookings` | 预订列表 |

**查询参数:**

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | String | 状态筛选 |
| `venueId` | Long | 场馆筛选 |
| `keyword` | String | 关键字搜索（标题/房间名） |
| `page` | Integer | 页码 |
| `size` | Integer | 每页条数 |

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/api/v1/admin/bookings/{id}/status` | 状态变更 |

**状态变更请求体:**
```json
{
  "status": "CANCELLED"
}
```

### 6.3 报表 `/api/v1/reports`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/reports/usage` | 使用率报表 |

**查询参数:** `startDate`, `endDate`, `venueId`

**响应 data:**
```json
{
  "totalBookings": 128,
  "dailyBreakdown": {
    "2026-05-21": 12,
    "2026-05-22": 15
  }
}
```

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/reports/revenue` | 收入报表 |

**查询参数:** `startDate`, `endDate`, `venueId`

**响应 data:**
```json
{
  "totalBookings": 128,
  "totalHours": 256
}
```

---

## 通用错误码

| code | 说明 |
|------|------|
| 0 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 / Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 3001 | 房间不可用 |
| 3002 | 时段冲突 |
| 3003 | 系统繁忙（锁获取失败） |
| 3004 | 预订不存在 |
| 3005 | 状态不允许该操作 |
| 5000 | 服务器内部错误 |

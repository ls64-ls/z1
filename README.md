# WeChat Room Booking System

微信会议室预订系统 — 基于 Spring Boot + Taro + Vue 3 的三层全栈应用，支持会议室浏览、时段选择、冲突检测、重复预订、签到管理等功能。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **语言** | Java | 17 |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **数据库** | PostgreSQL | 16 |
| **缓存 / 锁** | Redis 7 + Redisson | 3.23.5 |
| **数据库迁移** | Flyway | 10.11 |
| **认证** | JWT (jjwt) + 微信 code2session | 0.12.5 |
| **小程序前端** | Taro 3 (React) | — |
| **管理后台** | Vue 3 + Element Plus + Vite | — |
| **容器化** | Docker Compose | — |
| **编排** | Kubernetes (部署配置就绪) | — |

## 项目结构

```
wechat-room-booking/
├── backend/                          # Java 后端
│   ├── booking-common/               # 公共模块（通用返回体、分页）
│   └── booking-server/               # 主服务（控制器、服务、实体、缓存、安全）
├── miniapp/                          # Taro 3 微信小程序
│   └── src/
│       ├── components/               # Calendar, RoomCard, TimeSlotPicker
│       ├── pages/                    # 首页、搜索、详情、预订、我的预订、签到
│       ├── services/                 # API 调用封装
│       ├── store/                    # Zustand 状态管理
│       └── utils/                    # 日期、微信工具函数
├── admin/                            # Vue 3 管理后台
│   └── src/
│       ├── pages/                    # 仪表盘、房间管理、预订管理、排期、报表
│       ├── services/                 # Axios API 封装
│       ├── router/                   # Vue Router 路由
│       └── store/                    # Pinia 状态管理
├── deploy/                           # 部署配置
│   ├── docker/                       # Dockerfile
│   ├── k8s/                          # Kubernetes 清单
│   └── nginx/                        # Nginx 配置
├── docs/                             # 文档
│   ├── api-spec/                     # API 规范（待补充）
│   ├── db/                           # 数据库 schema / seed
│   └── diagrams/                     # 架构图（待补充）
├── scripts/                          # 部署 / 初始化脚本
├── docker-compose.yml                # 本地开发基础设施
└── pom.xml                           # Maven 父 POM
```

## 快速开始

### 前置条件

- JDK 17+
- Maven 3.8+
- Node.js 18+
- Docker Desktop（用于 PostgreSQL + Redis）

### 1. 启动基础设施

```bash
docker-compose up -d
```

启动 PostgreSQL 16（端口 5434）和 Redis 7（端口 6380）。

### 2. 启动后端

```bash
cd backend/booking-server
mvn spring-boot:run
```

服务启动在 `http://localhost:8088`，Flyway 会在启动时自动执行数据库迁移和种子数据。

### 3. 启动小程序（可选）

```bash
cd miniapp
npm install
npm run dev:weapp
```

用微信开发者工具打开 `miniapp/dist` 目录。

### 4. 启动管理后台（可选）

```bash
cd admin
npm install
npm run dev
```

访问 `http://localhost:3000`。

## 核心功能

### 用户端（小程序）

- **会议室浏览与搜索** — 按场馆、容量、设施筛选
- **日历选择 + 时段查看** — 查看每天各时段可用状态
- **冲突预检** — 提交前实时检测时段是否可用，给出备选建议
- **预订创建** — 选择连续时段，填写会议主题
- **重复预订** — 支持每日/每周/双周/每月重复规则
- **我的预订** — 按状态筛选，查看详情
- **签到 / 签退** — 到场签到确认

### 管理端（Web 后台）

- **仪表盘** — 关键指标概览
- **房间管理** — 房间 CRUD、上下架、设施配置
- **预订管理** — 全部预订列表、状态变更、取消
- **报表** — 使用率统计、收入报表

## 预订冲突防护（四层）

1. **Redis 分布式锁** — `booking:lock:room:{id}:date:{date}` 粒度
2. **应用层 GiST 检查** — `bookingMapper.countConflicts()`
3. **PostgreSQL 排他约束** — `EXCLUDE USING GIST (room_id WITH =, time_slot WITH &&)`
4. **乐观锁** — `version` 字段防止并发更新覆盖

## API 概览

| 前缀 | 用途 |
|------|------|
| `POST /api/v1/auth/*` | 微信登录、Token 刷新、开发登录 |
| `GET /api/v1/venues` | 场馆列表 |
| `GET /api/v1/rooms` | 房间搜索、详情、可用时段 |
| `POST /api/v1/bookings` | 预订创建、预检、列表、取消 |
| `POST /api/v1/checkin` | 签到 / 签退 |
| `GET /api/v1/admin/*` | 管理端房间和预订接口 |
| `GET /api/v1/reports/*` | 使用率 / 收入报表 |

完整 API 文档见 [docs/API.md](docs/API.md)。

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `JWT_SECRET` | JWT 签名密钥 | `your-256-bit-secret-key-change-in-production` |
| `WX_APPID` | 微信小程序 AppID | `wx801379289235bba3` |
| `WX_SECRET` | 微信小程序 Secret | `aa06d76ae017a47600ebe4fa82780c5b` |

## 数据库

- **数据库名**: `room_booking`
- **用户**: `dev` / `dev123`
- **端口**: `5434`（宿主机映射）
- **表数量**: 12 张（含 GiST 索引、排他约束）
- **迁移工具**: Flyway，SQL 文件位于 `src/main/resources/db/migration/`

详细数据库文档见 [docs/DATABASE.md](docs/DATABASE.md)。

## 相关文档

- [系统架构](docs/ARCHITECTURE.md)
- [API 参考](docs/API.md)
- [数据库设计](docs/DATABASE.md)
- [开发指南](docs/DEVELOPMENT.md)

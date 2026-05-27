# 开发指南

## 环境准备

### 必需软件

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 17 | `JAVA_HOME` 需正确配置 |
| Maven | 3.8+ | 项目构建 |
| Node.js | 18+ | 前端构建 |
| Docker Desktop | — | PostgreSQL + Redis |
| 微信开发者工具 | — | 小程序调试 |

### 推荐 IDE

- **后端**: IntelliJ IDEA (Community/Ultimate)
- **前端**: VS Code + Volar / Vue Language Features

---

## 本地开发环境搭建

### 1. 克隆并进入项目

```bash
cd wechat-room-booking
```

### 2. 启动 Docker 服务

```bash
docker-compose up -d
```

验证：

```bash
docker ps
# 应看到 postgres:16-alpine 和 redis:7-alpine 两个容器在运行
```

### 3. 初始化数据库

数据库 Schema 由 Flyway 在应用启动时自动执行，无需手动操作。

如果需要手动执行 SQL：

```bash
# 连接 PostgreSQL
docker exec -it wechat-room-booking-postgres-1 psql -U dev -d room_booking

# 查看所有表
\dt

# 查看表结构
\d booking

# 退出
\q
```

### 4. 构建后端

```bash
# 在项目根目录
mvn clean compile -DskipTests

# 仅编译 booking-server
cd backend/booking-server
mvn clean compile -DskipTests
```

### 5. 启动后端

```bash
cd backend/booking-server
mvn spring-boot:run
```

或通过 IDE 运行 `BookingApplication.main()`。

启动日志中出现以下内容表示成功：

```
Started BookingApplication in X.XXX seconds
```

服务端口: `http://localhost:8088`

### 6. 测试后端 API

```bash
# 开发登录获取 Token
curl -X POST http://localhost:8088/api/v1/auth/dev-login \
  -H "Content-Type: application/json" \
  -d '{"openid":"test_user_001"}'

# 使用返回的 Token 访问需要认证的接口
curl http://localhost:8088/api/v1/bookings \
  -H "Authorization: Bearer eyJhbGciOiJI..."

# 查看场馆列表（无需认证）
curl http://localhost:8088/api/v1/venues

# 查看可用房间
curl "http://localhost:8088/api/v1/rooms?venueId=1"
```

### 7. 启动小程序前端

```bash
cd miniapp
npm install
npm run dev:weapp
```

用微信开发者工具打开 `miniapp/dist` 目录。

**注意:** 需要修改 `miniapp/src/services/api.ts` 中的 `API_BASE_URL` 或通过构建配置指向正确的后端地址。默认指向 `http://127.0.0.1:8088/api/v1`。

### 8. 启动管理后台

```bash
cd admin
npm install
npm run dev
```

访问 `http://localhost:3000`。

---

## 项目配置

### 后端配置 (`application.yml`)

```yaml
server:
  port: 8088                          # 服务端口

spring:
  datasource:
    url: jdbc:postgresql://localhost:5434/room_booking
    username: dev
    password: dev123

  data:
    redis:
      host: localhost
      port: 6380

jwt:
  secret: ${JWT_SECRET:your-secret}   # 生产环境通过环境变量注入
  expiration: 7200                     # Token 有效期（秒）

wechat:
  miniapp:
    appid: ${WX_APPID:wx...}          # 开发环境使用默认值
    secret: ${WX_SECRET:aa06...}
```

### 小程序配置 (`miniapp/config/dev.ts`)

```typescript
export default {
  env: {
    API_BASE_URL: '"http://127.0.0.1:8088/api/v1"'
  }
}
```

### 管理后台配置 (`admin/vite.config.ts`)

Vite 开发服务器默认运行在 `localhost:3000`，API 代理配置在 `vite.config.ts` 中。

---

## 数据库迁移

Flyway 迁移脚本位于：

```
backend/booking-server/src/main/resources/db/migration/
```

命名规范: `V{version}__{description}.sql`

| 文件 | 说明 |
|------|------|
| `V1__init_schema.sql` | 初始化全部 12 张表、索引、约束 |
| `V2__seed_data.sql` | 开发种子数据（场馆、房间、设施、可用规则） |

**添加新迁移:**

```bash
# 创建新的迁移文件
# 文件名格式: V3__add_feature.sql
```

Flyway 配置：

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true          # 非空数据库也能启动迁移
    validate-on-migrate: false         # 开发环境关闭校验
```

---

## 目录约定

### 后端包结构

```
com.example.booking
├── BookingApplication.java           # 入口
├── cache/                            # Redis 缓存管理
├── config/                           # Spring / MyBatis 配置
├── controller/                       # REST 控制器
├── dto/
│   ├── request/                      # 请求 DTO (含 Jakarta Validation)
│   └── response/                     # 响应 VO (Lombok @Builder)
├── entity/                           # 数据库实体 (MyBatis-Plus)
├── enums/                            # 枚举
├── exception/                        # 异常 + 全局异常处理
├── repository/mapper/                # MyBatis-Plus Mapper 接口
├── scheduler/                        # 定时任务
├── security/                         # JWT 认证 (Filter, Provider, Context)
├── service/                          # 业务服务
│   └── admin/                        # 管理端服务
└── util/                             # 工具类
```

### 小程序文件约定

- **页面**: `src/pages/{page-name}/index.tsx` + `index.scss` + `index.config.ts`
- **组件**: `src/components/{ComponentName}/index.tsx` + `index.scss`
- **服务**: `src/services/{module}.ts`（对应后端 Controller）
- **状态**: `src/store/{domain}Store.ts`（Zustand）
- **常量**: `src/constants/enums.ts`

---

## 代码规范

### Java / Spring Boot

- **Lombok**: 实体和 DTO 统一使用 `@Data`，响应 VO 额外加 `@Builder @NoArgsConstructor @AllArgsConstructor`
- **事务**: 写操作加 `@Transactional`，超时时间按需设置（预订 10s）
- **异常**: 业务异常抛 `BusinessException(ErrorCode, message)`，由 `GlobalExceptionHandler` 统一捕获
- **时间类型**: 日期用 `LocalDate`，时间用 `LocalTime`，时间戳用 `OffsetDateTime`
- **数据库时间范围**: 使用 PostgreSQL `tsrange`，Java 侧通过 `TsRangeTypeHandler` 做 String ↔ PGobject 转换

### TypeScript / React (小程序)

- **页面组件**: 函数组件 + Hooks
- **状态管理**: Zustand（轻量、类型安全）
- **网络请求**: 通过 `api.ts` 封装的 `Taro.request`
- **样式**: SCSS，BEM 风格类名（如 `.booking-card__header`）

### Vue 3 / TypeScript (管理后台)

- **组合式 API**: `<script setup lang="ts">`
- **状态管理**: Pinia
- **UI 框架**: Element Plus
- **HTTP 客户端**: Axios（`admin/src/services/api.ts`）

---

## 调试

### 后端调试

- **SQL 日志**: MyBatis-Plus 已开启 `StdOutImpl`，所有 SQL 打印到控制台
- **日志级别**: `com.example.booking: DEBUG`
- **IDEA 远程调试**: 在 Run Configuration 中添加 Remote JVM Debug，端口 5005

### 小程序调试

- 微信开发者工具自带调试器（Console / Network / Storage / AppData）
- `console.log` / `console.error` 输出在开发者工具控制台
- Network 面板可查看 API 请求和响应

### 管理后台调试

- 浏览器 DevTools (F12)
- Network 面板查看 API 调用
- Vue DevTools 查看组件状态

### Redis 调试

```bash
# 连接 Redis
docker exec -it wechat-room-booking-redis-1 redis-cli

# 查看缓存 key
KEYS room:*
KEYS availability:*

# 查看 key 内容
GET room:detail:1
TTL room:detail:1

# 监控实时命令
MONITOR
```

---

## 常见问题

### Flyway 报 "Found non-empty schema(s)"

数据库已有表但无 Flyway 历史。已配置 `baseline-on-migrate: true` 解决。

### 端口被占用

`docker-compose.yml` 使用宿主机端口 5434 (PG) 和 6380 (Redis)。如果被占用，修改端口映射或停掉占用进程。

### 微信登录接口返回错误

开发环境使用 `/api/v1/auth/dev-login` 接口跳过微信 code2session 调用。生产环境需要配置正确的 `WX_APPID` 和 `WX_SECRET`。

### BookingVO 等响应为 null 或空对象

Lombok `@Builder` 会移除无参构造器，导致 Jackson 反序列化失败。所有响应 VO 已添加 `@NoArgsConstructor @AllArgsConstructor`。

---

## 构建部署

### 后端打包

```bash
mvn clean package -DskipTests
# JAR 包位于: backend/booking-server/target/booking-server-*.jar
```

### Docker 构建

```bash
docker build -f deploy/docker/Dockerfile.backend -t room-booking-backend .
```

### Kubernetes 部署

```bash
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.yaml
kubectl apply -f deploy/k8s/deployment-backend.yaml
kubectl apply -f deploy/k8s/service-backend.yaml
kubectl apply -f deploy/k8s/hpa.yaml
```

# A 组开发计划

> 课程截止：2026-06-21
> 当前里程碑：MVP 后端落地 + 与 B / C 联调

## 1. 当前已交付内容

- 后端工程骨架：Spring Boot 3.2 + MyBatis-Plus + MySQL，端口 8080
- 数据库 10 张表的版本化迁移与演示数据：[`V1__create_core_schema.sql`](../backend/src/main/resources/db/migration/V1__create_core_schema.sql) / [`V2__seed_demo_data.sql`](../backend/src/main/resources/db/migration/V2__seed_demo_data.sql)
- 接口实现：登录、乘客需求 CRUD、推荐匹配、匹配请求、订单履约、付款、评价、管理端审核 / 信用分调整
- 全局异常 + 统一返回结构 + CORS 配置
- 状态机服务（事务性「同意请求 -> 创建订单 -> 扣座位」）
- 单元测试（订单状态枚举 + ID 生成器）

## 2. 模块边界

| 模块 | 路径 | 责任 |
| --- | --- | --- |
| 鉴权 | `controller/AuthController` + `UserService` | 演示登录、用户资料 |
| 乘客 | `PassengerController` + `PassengerOrderService` | 需求 CRUD、推荐匹配 |
| 司机 | `DriverController` + `DriverService` | 车辆、行程、收件箱 |
| 匹配 | `MatchRequestController` + `MatchRequestService` | 双向同意、状态流转 |
| 订单 | `OrderController` + `OrderService` | 履约、付款、评价 |
| 管理 | `AdminController` + `AdminService` | 审核、订单看板、信用分 |
| 公共 | `common/*` `config/*` `enums/*` | 通用基础设施 |

## 3. 编码规范

### 3.1 包与命名

- 包名小写，结构 `com.pintu.<层>`，禁止跨层引用（controller 不直接调用 mapper）
- 类名大驼峰，方法名小驼峰
- 常量使用全大写下划线分隔，写在最相关的类中
- DTO 命名：`*Request` 表示请求体，`*VO` 表示对外视图对象

### 3.2 实体与表

- 实体使用 `@Data` 简化 getter / setter
- 主键统一 `@TableId(type = IdType.INPUT)`，由业务侧生成
- 时间字段优先使用字符串 `departureTime` 等业务可读格式
- 创建时间 / 更新时间使用 `LocalDateTime`，由 MySQL 默认值自动维护

### 3.3 异常处理

- 业务异常一律抛 `BusinessException`，禁止返回 `null` 表示失败
- 参数校验使用 Jakarta Validation（`@NotBlank` / `@NotNull` / `@Positive`），由 `GlobalExceptionHandler` 统一拦截
- Service 层使用 `@Transactional(rollbackFor = Exception.class)` 标注涉及多表写入的方法

### 3.4 日志

- 类上使用 `@Slf4j`
- 业务异常 `warn`、未预期异常 `error` + 堆栈
- 不在日志中输出密码、token 等敏感字段

## 4. 与 B / C 联调

### 4.1 B 组（乘客端）

替换 [`src/api/passengerApi.js`](../src/api/passengerApi.js) 中转发 mock 的 import 为：

```js
const BASE = 'http://localhost:8080'
const headers = { 'Content-Type': 'application/json', 'X-User-Id': 'passenger-001' }

export const createPassengerApi = () => ({
  login: (req) => fetch(`${BASE}/api/auth/login`, { method: 'POST', headers, body: JSON.stringify(req) }).then(r => r.json()).then(r => r.data),
  createOrder: (req) => fetch(`${BASE}/api/passenger/orders`, { method: 'POST', headers, body: JSON.stringify(req) }).then(r => r.json()).then(r => r.data),
  // ...
})
```

页面层不变，状态字段映射沿用现有 `mapBackendOrderStatus`。

### 4.2 C 组（司机端 + 管理端）

类似地替换 [`src/api/driverApi.js`](../src/api/driverApi.js) 与 [`src/api/adminApi.js`](../src/api/adminApi.js)。注意：

- 司机端请求带 `X-User-Id: driver-001`
- 管理端请求带 `X-User-Id: admin-001`

## 5. 测试计划

### 5.1 单元测试（已落地）

- `OrderStatusTest` 覆盖订单状态枚举的全部分支
- `IdGeneratorTest` 覆盖 ID 生成器的前缀与唯一性

### 5.2 路径测试（待补充）

针对状态机的 7 条主要跳转路径（覆盖语句 + 路径）：

| 路径 | 步骤 |
| --- | --- |
| 1 | 待匹配 -> 协商中 -> 已确认（accept）|
| 2 | 已确认 -> 履约中（start）|
| 3 | 履约中 -> 待付款（arrive）|
| 4 | 待付款 -> 已完成（pay）|
| 5 | 协商中 -> 待匹配（reject）|
| 6 | 任意状态 -> 已取消（cancel）|
| 7 | 已完成 -> 已评价（review）|

后续在 `OrderServiceTest` 中以 H2 内存数据库或 Mockito 覆盖。

### 5.3 集成测试

将前端 [`scripts/integration-smoke.mjs`](../scripts/integration-smoke.mjs) 改造为打真后端版本，覆盖端到端主线。

## 6. 后续待办

| 优先级 | 项 | 说明 |
| --- | --- | --- |
| P0 | 与 B 组联调登录 + 发单 | 暴露 CORS、字段格式、错误码 |
| P0 | 与 C 组联调司机收件箱 + 同意请求 | 验证事务一致性 |
| P1 | 补充 `OrderServiceTest` 路径测试 | 课程报告测试章节素材 |
| P1 | 集成烟测改造为打真后端 | 演示前一键回归 |
| P2 | 评价后联动信用分 | C 组规则确认后落地 |
| P2 | 申诉工单接口 | 报告扩展设计，演示不必要 |

## 7. 风险

1. MyBatis-Plus 默认 `IdType.ASSIGN_ID` 会生成雪花 ID。当前已统一为 `IdType.INPUT` + 业务前缀生成器，避免与前端展示错位。
2. `LocalDateTime` 默认序列化为 ISO 格式（含 `T`）。当前 `application.yml` 已配置 `yyyy-MM-dd HH:mm`，前端无需做额外处理。
3. 数据库已改用 Flyway 版本化迁移，应用启动不会重复清表；修改已发布迁移会触发校验失败，后续结构变更必须新增迁移版本。
4. 业务层校验（同一请求重复发起、订单状态机非法跳转）依赖 Service 层抛 `BusinessException`，前端不能仅靠按钮禁用。

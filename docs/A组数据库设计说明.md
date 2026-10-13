# A 组数据库设计说明

数据库结构由 Flyway 管理，迁移脚本位于
`backend/src/main/resources/db/migration/`。服务重启不会重复建表或清空数据；
任何结构调整都应新增版本迁移，不得修改已经发布的迁移文件。

> 字符集：`utf8mb4`
> 引擎：`InnoDB`
> 主键：`VARCHAR(32)`，沿用前端约定的业务前缀

## 1. 实体关系总览

```text
user ──┬── (passenger) ── passenger_order ─── match_request ─── order_record ── payment
       │                                                         │
       └── (driver) ──── driver_trip ────────┘                   └── review
                  │
                  └── vehicle ── audit_record
                  └── credit_record
```

## 2. 表清单

| 表 | 说明 | 状态字段 |
| --- | --- | --- |
| `user` | 乘客 / 司机 / 管理员统一表 | status（账号状态）|
| `vehicle` | 车辆信息，绑定司机 | status（待审核 / 已认证 / 已驳回）|
| `passenger_order` | 乘客拼车需求 | status（七阶段订单状态）|
| `driver_trip` | 司机已发布的行程 | status（已发布 / 履约中 / 已完成 / 已取消）|
| `match_request` | 乘客向司机发起的拼车请求 | status（待处理 / 已同意 / 已拒绝 / 已失效）|
| `order_record` | 双方确认后形成的正式订单 | status / payment_status / review_status |
| `payment` | 模拟支付记录 | status |
| `review` | 订单评价 | — |
| `audit_record` | 实名 / 车辆审核记录 | status |
| `credit_record` | 信用分变动流水 | — |

基础 DDL 见 [`V1__create_core_schema.sql`](../backend/src/main/resources/db/migration/V1__create_core_schema.sql)，字段规范化见 [`V3__normalize_time_and_location_types.sql`](../backend/src/main/resources/db/migration/V3__normalize_time_and_location_types.sql)，演示数据见 [`V2__seed_demo_data.sql`](../backend/src/main/resources/db/migration/V2__seed_demo_data.sql)。

## 3. 关键字段

### 3.1 `user`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | VARCHAR(32) | 主键，前缀 passenger / driver / admin |
| username | VARCHAR(64) | 登录账号，唯一 |
| password | VARCHAR(128) | MVP 阶段为明文，仅供演示 |
| role | VARCHAR(16) | passenger / driver / admin |
| credit | INT | 信用分 0~100 |
| status | VARCHAR(16) | 正常 / 观察 / 限制发单 / 限制接单 / 已封禁 |
| trip_count、saved_money、carbon_saved | — | 个人页统计指标 |

### 3.2 `passenger_order`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | VARCHAR(32) | 主键，前缀 po- |
| start_location / end_location | VARCHAR(128) | 起终点 |
| start_lng / start_lat / end_lng / end_lat | DECIMAL(10,7) | WGS-84 起终点坐标，可为空 |
| departure_time | DATETIME | API 格式 yyyy-MM-dd HH:mm，时区 Asia/Shanghai |
| status | VARCHAR(16) | 七阶段订单状态 |

需求被 `match_request.accept` 后，`status` 与 `order_record.status` 同步推进。

### 3.3 `order_record`

唯一聚合根，用于驱动状态机：

```
待确认 -> 已确认 -> 履约中 -> 待付款 -> 已完成
                                   \-> 已取消
```

`payment_status`（未支付 / 已支付）与 `review_status`（未评价 / 已评价）是独立维度，不影响订单主状态。

订单同时保存起终点坐标、计划出发时间、`actual_departure` 和 `actual_arrival`，形成不受后续需求或行程修改影响的履约快照。

### 3.4 `match_request`

| 字段 | 说明 |
| --- | --- |
| status | 待处理 -> 已同意 / 已拒绝 / 已失效 |
| order_id | 同意后回写正式订单 ID |
| reject_reason | 拒绝时填写，乘客端展示 |

同一对 `passenger_order_id + driver_trip_id` 同时只能存在一条 `待处理 / 已同意` 记录（业务层校验）。

## 4. 索引与约束

- `user.username` 唯一索引，登录直查
- `vehicle.plate` 唯一索引，避免重复车牌
- `passenger_order.passenger_id` / `status` 二级索引，支持订单列表筛选
- `driver_trip.driver_id` / `status` 二级索引，支持司机端查询
- `match_request.passenger_order_id` / `driver_trip_id` 用于查询匹配
- `order_record.passenger_id` / `driver_id` / `status` 用于看板筛选
- `review.order_id` 唯一索引，保证一单一评

## 5. 逻辑删除

所有业务表统一使用 `deleted` 字段（0 未删除 / 1 已删除）配合 MyBatis-Plus 的 `@TableLogic`。删除操作不会真正清除行，便于课程报告截图与数据回放。

## 6. 数据迁移 / 重置策略

应用使用 Flyway 管理数据库版本：

- `V1__create_core_schema.sql` 创建核心表，不包含 `DROP TABLE`
- `V2__seed_demo_data.sql` 插入演示账号、演示车辆和演示行程
- `V3__normalize_time_and_location_types.sql` 将出发时间升级为 `DATETIME`，补充 `DECIMAL(10,7)` 坐标和实际履约时间
- `V4__complete_domain_foundation.sql` 增加核心外键、状态/范围检查、复合索引，以及认证、协商、通知、信用、申诉、规则、轨迹和安全领域表
- `spring.sql.init.mode = never`，避免 Spring SQL 初始化与 Flyway 重复执行
- `validate-on-migrate = true`，启动时校验已执行迁移的完整性

已执行的迁移不得原地修改；后续结构或数据变更必须新增 `V3__...sql` 等迁移文件。需要重置本地演示数据库时，应明确删除并重建开发数据库，再由 Flyway 从 `V1` 重新迁移，不能依赖应用重启清库。

## 7. 与前端 ID 对齐

前端 [`src/mock/localPassengerGroupStore.js`](../src/mock/localPassengerGroupStore.js) 中已硬编码：

- `passenger-001` 赵同学
- `driver-001` 陈师傅
- 车牌 `沪A·P2T08`

`V2__seed_demo_data.sql` 中的演示数据与之完全一致，避免演示截图与代码示例错位。新增演示账号时，需要 A / B / C 三方同步更新。

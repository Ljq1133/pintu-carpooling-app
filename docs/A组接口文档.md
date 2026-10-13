# A 组接口文档

> 路径：`backend/`
> 端口：`8080`
> 接口前缀：`/api`
> 统一返回结构：`{ "code": 0, "msg": "ok", "data": ... }`，业务异常 code 非 0

统一状态定义见 [`状态与接口契约.md`](./状态与接口契约.md)。服务启动后可通过
`/api-docs` 获取 OpenAPI JSON，通过 `/swagger-ui.html` 浏览接口。

本文档与 [`B组接口需求清单.md`](./B组接口需求清单.md) / [`C组接口需求清单.md`](./C组接口需求清单.md) 保持字段一致；后端落地时如有调整，以本文档为准并同步给 B / C。

## 1. 通用约定

### 1.1 鉴权

MVP 阶段未接入 JWT。所有需要"当前登录用户"的接口通过请求头 `X-User-Id` 标识：

```http
X-User-Id: passenger-001
```

未传时按演示账号兜底（passenger-001 / driver-001）。

请求体中的 `passengerId` / `driverId` 不作为身份依据；涉及订单、行程和匹配请求的写操作会校验 `X-User-Id` 与资源归属。

### 1.2 ID 命名

| 实体 | 前缀 | 示例 |
| --- | --- | --- |
| 用户 | passenger- / driver- / admin- | passenger-001 |
| 车辆 | veh- | veh-001 |
| 乘客需求 | po- | po-1718260000000-1 |
| 司机行程 | trip- | trip-001 |
| 匹配请求 | req- | req-1718260000000-1 |
| 正式订单 | order- | order-1718260000000-1 |
| 支付 | pay- | pay-1718260000000-1 |
| 评价 | review- | review-1718260000000-1 |

### 1.3 时间与金额

- 时间：字符串 `yyyy-MM-dd HH:mm`
- 金额：数字（元），保留两位小数
- 路线：返回 `start` / `end` 与 `route`（前端可直接展示）

### 1.4 状态枚举

| 类别 | 取值 |
| --- | --- |
| 订单状态 | 待确认 / 已确认 / 履约中 / 待付款 / 已完成 / 已取消 / 已归档 |
| 匹配请求 | 待处理 / 已同意 / 已拒绝 / 已失效 |
| 支付状态 | 未支付 / 已支付 |
| 评价状态 | 未评价 / 已评价 |
| 用户状态 | 正常 / 观察 / 限制发单 / 限制接单 / 已封禁 |
| 审核状态 | 待审核 / 已通过 / 已驳回 |

后端对外返回正式订单状态使用上述七阶段；乘客需求另含待匹配、协商中两个前置阶段。

## 2. 鉴权与用户

### 2.1 登录

```http
POST /api/auth/login
```

请求：

```json
{ "username": "passenger_demo", "password": "123456", "role": "passenger" }
```

响应：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "token": "demo-token-passenger-001",
    "user": {
      "id": "passenger-001",
      "name": "赵同学",
      "phone": "138 **** 5678",
      "role": "passenger",
      "credit": 98,
      "status": "正常",
      "tripCount": 32,
      "savedMoney": 268.50,
      "carbonSaved": 16.20
    }
  }
}
```

### 2.2 乘客资料

```http
GET /api/passenger/profile
Header: X-User-Id: passenger-001
```

### 2.3 司机资料

```http
GET /api/driver/profile
Header: X-User-Id: driver-001
```

返回字段同登录响应中的 `user` 子结构。

## 3. 乘客需求与匹配

### 3.1 发布需求

```http
POST /api/passenger/orders
```

请求：

```json
{
  "start": "同济科技园",
  "end": "SOHO",
  "departureTime": "2026-06-13 08:30",
  "passengerCount": 1,
  "note": "希望准时出发"
}
```

返回 `PassengerOrderVO`，关键字段：

```json
{
  "id": "po-...",
  "passengerOrderId": "po-...",
  "orderId": null,
  "status": "待匹配",
  "createdAt": "2026-06-13 08:00"
}
```

### 3.2 列表与详情

```http
GET /api/passenger/orders?status=待匹配
GET /api/passenger/orders/{id}
```

`{id}` 同时支持 `passengerOrderId` 和 `orderId`。

### 3.3 取消

```http
POST /api/passenger/orders/{id}/cancel
{ "reason": "临时有事" }
```

### 3.4 推荐司机

```http
GET /api/passenger/orders/{passengerOrderId}/matches
```

返回数组，每项 `RecommendedTripVO`：

```json
{
  "driverTripId": "trip-001",
  "driverId": "driver-001",
  "driverName": "陈师傅",
  "vehicle": { "plate": "沪A·P2T08", "model": "比亚迪 秦 PLUS", "color": "白色" },
  "start": "同济科技园",
  "end": "SOHO",
  "route": "同济科技园 -> SOHO",
  "departureTime": "2026-06-13 08:30",
  "availableSeats": 3,
  "overlap": 92,
  "driverCredit": 96,
  "price": 18.60,
  "recommendLevel": "推荐"
}
```

匹配规则：起终点相互包含、剩余座位 > 0、司机信用 ≥ 80、顺路度 ≥ 60、司机状态可接单。

### 3.5 发起请求 / 查询 / 同意 / 拒绝

```http
POST /api/match-requests
{ "passengerOrderId": "po-...", "driverTripId": "trip-...", "message": "我想拼这趟车" }

GET  /api/match-requests/{id}
POST /api/match-requests/{id}/accept
POST /api/match-requests/{id}/reject   { "reason": "顺路度不够" }
```

同意接口为事务操作，会同时：

1. 创建 `order_record`（状态 `已确认`）
2. 扣减 `driver_trip.available_seats`
3. 更新 `passenger_order.status` 为 `已确认`
4. 更新 `match_request.status` 为 `已同意` 并回写 `orderId`

## 4. 司机端

### 4.1 车辆与行程

```http
GET  /api/driver/vehicles
POST /api/driver/trips
GET  /api/driver/trips
GET  /api/driver/orders
GET  /api/driver/trips/{tripId}/matches
```

发布行程请求：

```json
{
  "start": "中央科技园",
  "end": "SOHO",
  "departureTime": "2026-06-13 08:30",
  "vehicleId": "veh-001",
  "availableSeats": 3,
  "price": 18.60
}
```

发布前会校验：车辆已认证、司机信用 ≥ 60、状态非"限制接单 / 已封禁"。

## 5. 订单履约

### 5.1 状态机

```http
POST /api/orders/{id}/start    # 已确认 -> 履约中
POST /api/orders/{id}/arrive   # 履约中 -> 待付款
POST /api/orders/{id}/pay      # 待付款 -> 已完成
POST /api/orders/{id}/review   # 已完成 -> 评价补充
```

### 5.2 付款

```http
POST /api/orders/{id}/pay
{ "amount": 18.60, "method": "mock_balance" }
```

仅允许订单所属乘客付款，且金额必须与订单价格完全一致。每个订单只能生成一条支付记录。

返回：

```json
{
  "id": "pay-...",
  "orderId": "order-...",
  "amount": 18.60,
  "status": "已支付",
  "orderStatus": "已完成",
  "paymentStatus": "已支付",
  "reviewStatus": "未评价",
  "paidAt": "2026-06-13 09:20"
}
```

### 5.3 评价

```http
POST /api/orders/{id}/review
{ "rating": 5, "comment": "司机很准时" }
```

仅在 `paymentStatus == 已支付` 时允许提交。

## 6. 管理端

### 6.1 审核

```http
GET  /api/admin/audits/users
POST /api/admin/audits/users/{id}/approve
POST /api/admin/audits/users/{id}/reject       { "reason": "证件模糊" }

GET  /api/admin/audits/vehicles
POST /api/admin/audits/vehicles/{id}/approve
POST /api/admin/audits/vehicles/{id}/reject
```

车辆审核结果会自动同步 `vehicle.status`：通过 -> `已认证`，驳回 -> `已驳回`。

### 6.2 订单看板

```http
GET /api/admin/orders?status=已确认
```

返回 `AdminOrderVO[]`，包含 `risk` 字段（待付款订单为 `待支付`，其余 `正常`）。

### 6.3 信用分

```http
GET  /api/admin/users/credit
POST /api/admin/users/{id}/credit
{ "delta": -10, "reason": "已确认订单后无故取消" }
```

调整后会按阈值自动更新 `status`，并写入 `credit_record`。

## 7. 错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 1001 | 业务异常（默认） |
| 1002 | 参数校验失败 |
| 1003 | 账号或密码不正确 |
| 1004 | 角色不匹配 |
| 1005 | 账号状态不允许该操作 |
| 1006 | 订单当前状态不允许取消 |
| 1007 | 司机行程已满员 |
| 1008 | 同一对乘客需求 + 司机行程重复请求 |
| 1009 | 匹配请求已处理 |
| 1010 | 司机当前不可接单 |
| 1011~1016 | 订单状态机非法跳转 / 重复操作 |
| 1017 | 车辆未通过审核 |
| 1018 | 审核记录已处理 |
| 1404 | 资源不存在 |
| 5000 | 服务器内部错误 |

## 8. 联调检查清单

1. CORS：浏览器开发者工具 Network 面板看 `OPTIONS` 预检 200，无报错
2. 登录：返回 `token` 与完整 `user` 字段
3. 发单：`status=待匹配`，`createdAt` 格式 `yyyy-MM-dd HH:mm`
4. 推荐：返回 `vehicle` 嵌套对象、`overlap`、`driverCredit`、`recommendLevel`
5. 同意：`order_record` 写入数据库，按乘客人数扣减 `driver_trip.available_seats`；同一乘客需求只能生成一个正式订单
6. 付款：写入 `payment` 表，订单进入 `已完成`
7. 评价：写入 `review` 表，订单 `reviewStatus` 变 `已评价`
8. 管理端：调整信用分后用户 `status` 联动，前端推荐列表立即过滤

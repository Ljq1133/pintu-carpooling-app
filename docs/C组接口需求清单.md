# C 组接口需求清单

本文档用于 C 组与 A 组后端对接。接口名称为建议版本，最终路径可以调整，但字段和状态含义需要统一。

## 1. 司机端接口

### 1.1 获取司机资料

```http
GET /api/driver/profile
```

建议返回司机姓名、信用分、账号状态和认证状态。

### 1.2 获取司机车辆

```http
GET /api/driver/vehicles
```

```json
[
  {
    "id": "veh-001",
    "plate": "沪A·P2T08",
    "model": "比亚迪 秦 PLUS",
    "color": "白色",
    "seats": 3,
    "status": "已认证"
  }
]
```

### 1.3 发布行程

```http
POST /api/driver/trips
```

```json
{
  "start": "中央科技园",
  "end": "SOHO",
  "departureTime": "2026-06-13 08:30",
  "vehicleId": "veh-001",
  "availableSeats": 3
}
```

### 1.4 获取司机行程

```http
GET /api/driver/trips
```

### 1.5 获取行程匹配请求

```http
GET /api/driver/trips/{tripId}/matches
```

建议返回请求 ID、乘客、路线、出发时间、顺路度、信用分、人数、价格和状态。

### 1.6 获取司机订单

```http
GET /api/driver/orders
```

返回当前司机的正式订单，用于司机工作台展示已确认、履约中、待付款、已完成和已取消订单。

### 1.7 同意或拒绝请求

```http
POST /api/match-requests/{id}/accept
POST /api/match-requests/{id}/reject
```

同意后应创建或确认唯一订单，并扣减可用座位；拒绝后请求状态变为已拒绝。

### 1.8 开始行程

```http
POST /api/orders/{id}/start
```

订单由已确认进入履约中，并校验当前司机身份；重复调用返回明确的业务错误。

### 1.9 确认到达

```http
POST /api/orders/{id}/arrive
```

订单由履约中进入待付款，不能跳过开始行程直接到达；重复调用返回明确的业务错误。

### 1.10 紧急联系人与一键报警

司机工作台安全区域使用账户级紧急联系人接口；履约中的订单可触发课程 Mock 报警。

```http
GET /api/safety/emergency-contacts
POST /api/safety/emergency-contacts
PUT /api/safety/emergency-contacts/{contactId}
DELETE /api/safety/emergency-contacts/{contactId}
POST /api/safety/orders/{orderId}/alerts
```

司机只能维护自己的联系人，也只能对自己参与且状态为 `履约中` 的订单触发报警。报警不会改变订单状态。

## 2. 管理端接口

### 2.0 当前管理员信息与权限

```http
GET /api/admin/me
```

返回当前管理员资料、子角色和权限列表。管理端继续共用统一登录入口，JWT 仍使用 `role=admin`，后台内部权限由子角色控制。

子角色内部值：

```text
SUPER_ADMIN
AUDITOR
OPS
CUSTOMER_SERVICE
```

当前 P0 权限码：

```text
AUDIT_REVIEW
ORDER_VIEW
CREDIT_MANAGE
```

### 2.1 用户审核

```http
GET /api/admin/audits/users?status=&page=&size=
GET /api/admin/audits/{id}
POST /api/admin/audits/users/{id}/approve
POST /api/admin/audits/users/{id}/reject
```

驳回时必须提交非空 `reason`；审核操作需要 `AUDIT_REVIEW` 权限。

### 2.2 车辆审核

```http
GET /api/admin/audits/vehicles?status=&page=&size=
POST /api/admin/audits/vehicles/{id}/approve
POST /api/admin/audits/vehicles/{id}/reject
```

审核结果需要同步影响司机是否可以发布行程；驳回时必须提交非空 `reason`。

### 2.3 订单看板

```http
GET /api/admin/orders?status=&paymentStatus=&keyword=&dateFrom=&dateTo=
GET /api/admin/orders/summary?status=&paymentStatus=&keyword=&dateFrom=&dateTo=
```

订单列表和统计都需要 `ORDER_VIEW` 权限。列表默认脱敏返回乘客/司机姓名；筛选支持订单状态、支付状态、关键词和创建时间范围。

### 2.4 调整用户信用分

```http
GET /api/admin/users/credit
POST /api/admin/users/{id}/credit
```

```json
{
  "delta": -10,
  "reason": "已确认订单后无故取消"
}
```

读取和调整信用分都需要 `CREDIT_MANAGE` 权限。返回调整后的信用分和账号状态。低信用限制需要立即同步到司机端或乘客端。

## 3. 状态契约

### 3.1 匹配请求状态

```text
待处理 -> 已同意
待处理 -> 已拒绝
待处理 -> 已失效
```

### 3.2 订单状态

```text
待确认 -> 已确认 -> 履约中 -> 待付款 -> 已完成
                    \-> 已取消
```

状态变化必须由后端校验，前端不能直接覆盖状态字段。

### 3.3 审核状态

```text
待审核
已通过
已驳回
```

### 3.4 用户状态

```text
正常
观察
限制发单
限制接单
已封禁
```

## 4. 后端需要保证的规则

- 同一匹配请求只能处理一次。
- 同一乘客请求与司机行程只能生成一个有效订单。
- 接单和取消时座位数更新需要使用事务。
- 订单状态接口需要幂等。
- 管理员操作需要记录操作人、时间、原因和变更前后值。
- 所有受限操作均由后端再次校验，不能只依赖前端按钮禁用。

## 5. 当前前端适配层

```text
src/api/driverApi.js
src/api/driverApi.mock.js
src/api/adminApi.js
src/api/adminApi.mock.js
src/api/passengerApi.js
src/api/passengerApi.mock.js
```

A 组接口可用后，优先替换各 API 文件中的 Mock 实现，组件层保持不变。

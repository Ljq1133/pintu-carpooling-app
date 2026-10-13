# B 组接口需求清单

这份文档用于 B 组乘客端和 A 组后端对接。当前接口名称、路径和字段是建议版本，最终可以由 A 组根据后端规范统一调整。

B 组当前会先使用 mock 数据和本地状态函数开发乘客端 MVP。当前已落地 `src/api/passengerApi.mock.js` 和 `src/api/passengerApi.js`，页面组件通过 `passengerApi.js` 统一调用数据动作。后续 A 组接口完成后，建议优先替换 `passengerApi.js` / adapter 的数据来源，页面组件尽量不直接依赖具体后端路径。

## 1. 基础约定

### 1.1 身份与数据范围

- B 组页面默认操作当前登录乘客的数据。
- 如果 A 组暂时没有完成真实鉴权，可以先通过 `passengerId` 或演示账号模拟当前用户。
- 演示账号建议为：

```text
passenger_demo / 123456
```

### 1.2 关键对象关系

建议后端至少区分以下对象：

- `PassengerOrder`：乘客发布的拼车需求
- `DriverTrip`：司机发布的行程
- `MatchRequest`：乘客和司机之间的匹配请求
- `OrderRecord`：乘客主动请求被司机同意后形成的正式订单
- `Payment`：模拟支付记录
- `Review`：订单评价

如果 A 组为了简化实现合并了部分表，接口返回字段仍建议保留这些概念，方便前端和报告说明保持一致。

### 1.3 ID 使用约定

为了避免乘客需求和正式订单混淆，建议统一以下 ID 语义：

- `passengerOrderId`：乘客发布的拼车需求 ID，例如 `po-001`
- `driverTripId`：司机发布的行程 ID，例如 `trip-001`
- `matchRequestId`：乘客和司机之间的匹配请求 ID，例如 `req-001`
- `orderId`：双方确认流程中形成的正式订单 ID，例如 `order-1001`

乘车页中的地图定位 / 地点搜索在 B 组当前阶段仍为 mock 交互。后续接真实地图 API 时，建议保持页面侧字段含义不变：出发地写入 `start`，目的地写入 `end`，地图组件只负责辅助定位和选择。

接口返回中建议同时保留 `passengerOrderId` 和 `orderId`：

- 还没有司机同意时，`orderId` 可以为空
- 查询推荐司机、修改拼车需求时，主要使用 `passengerOrderId`
- 付款、评价等正式订单操作，主要使用 `orderId`

如果 A 组决定只使用一个统一订单表，也建议在返回字段里保留这些语义，便于 B 组前端判断当前页面处于“需求阶段”还是“正式订单阶段”。

### 1.4 日期、金额和状态字段

- 时间字段建议使用字符串格式：`YYYY-MM-DD HH:mm`
- 金额字段使用数字，单位为元
- 路线字段建议同时返回 `start`、`end`，前端可自行展示为 `start -> end`

## 2. 乘客基础接口

### 2.1 乘客登录

```http
POST /api/auth/login
```

用途：乘客使用账号密码登录系统。

建议请求字段：

```json
{
  "username": "passenger_demo",
  "password": "123456",
  "role": "passenger"
}
```

建议返回字段：

```json
{
  "token": "demo-token",
  "user": {
    "id": "passenger-001",
    "name": "赵同学",
    "phone": "138 **** 5678",
    "role": "passenger",
    "credit": 98,
    "status": "正常"
  }
}
```

说明：如果 MVP 阶段不做真实鉴权，可以先返回固定演示用户。

### 2.2 获取乘客个人信息

```http
GET /api/passenger/profile
```

用途：展示乘客“我的”页面统计信息。

建议返回字段：

```json
{
  "id": "passenger-001",
  "name": "赵同学",
  "phone": "138 **** 5678",
  "credit": 98,
  "tripCount": 32,
  "savedMoney": 268.5,
  "carbonSaved": 16.2,
  "status": "正常"
}
```

## 3. 乘客拼车需求接口

### 3.1 发布拼车需求

```http
POST /api/passenger/orders
```

用途：乘客填写出发地、目的地、出发时间等信息后发布拼车需求。

建议请求字段：

```json
{
  "start": "同济科技园",
  "end": "SOHO",
  "departureTime": "2026-06-09 08:30",
  "passengerCount": 1,
  "note": "希望准时出发"
}
```

建议返回字段：

```json
{
  "id": "po-001",
  "passengerId": "passenger-001",
  "start": "同济科技园",
  "end": "SOHO",
  "departureTime": "2026-06-09 08:30",
  "passengerCount": 1,
  "note": "希望准时出发",
  "status": "待匹配",
  "createdAt": "2026-06-09 08:00"
}
```

说明：乘客发布需求后，订单建议先进入 `待匹配`。

### 3.2 获取乘客订单列表

```http
GET /api/passenger/orders
```

用途：展示当前乘客的订单列表。

建议支持查询参数：

```text
status=待匹配
group=ongoing
```

建议返回字段：

```json
[
  {
    "id": "order-1001",
    "passengerOrderId": "po-001",
    "route": "同济科技园 -> SOHO",
    "start": "同济科技园",
    "end": "SOHO",
    "departureTime": "2026-06-09 08:30",
    "driverName": "陈师傅",
    "vehicle": "沪A·P2T08 · 比亚迪 秦 PLUS",
    "price": 18.6,
    "status": "进行中",
    "paymentStatus": "未支付",
    "reviewStatus": "未评价"
  }
]
```

说明：如果还没有形成正式订单，`driverName`、`vehicle`、`price` 可以为空，前端会按 `待匹配` 状态展示。

### 3.3 获取订单详情

```http
GET /api/passenger/orders/{id}
```

用途：展示乘客历史订单的轻量行程摘要。进行中订单的实时信息默认回到乘车页展示，不在订单详情页承载主流程。

建议返回字段：

```json
{
  "id": "order-1001",
  "passengerOrderId": "po-001",
  "driverTripId": "trip-001",
  "passengerName": "赵同学",
  "driverName": "陈师傅",
  "driverPhone": "138 **** 2678",
  "vehicle": "沪A·P2T08 · 比亚迪 秦 PLUS",
  "start": "同济科技园",
  "end": "SOHO",
  "departureTime": "2026-06-09 08:30",
  "passengerCount": 1,
  "price": 18.6,
  "status": "进行中",
  "paymentStatus": "未支付",
  "reviewStatus": "未评价",
  "summary": "同济科技园到 SOHO，陈师傅，比亚迪 秦 PLUS，1 人同行"
}
```

### 3.4 修改乘客拼车需求

```http
PATCH /api/passenger/orders/{id}
```

用途：乘客修改出发地、目的地、时间、人数或备注。

建议请求字段：

```json
{
  "start": "同济科技园",
  "end": "五角场",
  "departureTime": "2026-06-09 08:45",
  "passengerCount": 1,
  "note": "时间改为 8:45"
}
```

建议返回字段：

```json
{
  "id": "po-001",
  "status": "待匹配"
}
```

说明：

- `待匹配` 状态下修改后可仍为 `待匹配`
- 已进入 `进行中` 的正式订单不建议直接修改路线、时间、人数等核心信息
- 如后续需要改签流程，建议由 A / C 组一起定义重新匹配或司机确认规则
- 已完成、已取消订单不允许修改

### 3.5 取消订单

```http
POST /api/passenger/orders/{id}/cancel
```

用途：乘客取消拼车需求或订单。

建议请求字段：

```json
{
  "reason": "临时有事，取消出行"
}
```

建议返回字段：

```json
{
  "id": "order-1001",
  "status": "已取消",
  "creditChanged": false
}
```

说明：

- `待匹配` 状态建议允许乘客取消
- 未支付的 `进行中` 订单可先在 B 组 mock 中允许取消，是否扣信用分需要 A 组确认
- 司机已送达但乘客未付款时是否允许取消，需要 A / C 组联调时确认
- 已完成、已取消订单不允许再次取消

## 4. 匹配推荐接口

### 4.1 获取推荐司机行程

```http
GET /api/passenger/orders/{passengerOrderId}/matches
```

用途：根据乘客拼车需求展示推荐司机行程。

建议返回字段：

```json
[
  {
    "driverTripId": "trip-001",
    "driverId": "driver-001",
    "driverName": "陈师傅",
    "vehicle": {
      "plate": "沪A·P2T08",
      "model": "比亚迪 秦 PLUS",
      "color": "白色"
    },
    "start": "同济科技园",
    "end": "SOHO",
    "departureTime": "2026-06-09 08:30",
    "availableSeats": 3,
    "overlap": 92,
    "driverCredit": 96,
    "price": 18.6,
    "recommendLevel": "推荐"
  }
]
```

推荐规则建议：

- 起点相同或相近
- 终点相同或相近
- 出发时间在允许范围内
- 司机剩余座位数大于 0
- 司机信用分达到最低要求
- 顺路度不低于 60%

说明：MVP 阶段可以先用模拟顺路度 `overlap`，不接真实地图 API。

### 4.2 乘客发起拼车请求

```http
POST /api/match-requests
```

用途：乘客选择某个司机行程后，向司机发起拼车请求。

建议请求字段：

```json
{
  "passengerOrderId": "po-001",
  "driverTripId": "trip-001",
  "message": "我想拼这趟车"
}
```

建议返回字段：

```json
{
  "id": "req-001",
  "passengerOrderId": "po-001",
  "driverTripId": "trip-001",
  "status": "待处理",
  "createdAt": "2026-06-09 08:05"
}
```

说明：乘客发起请求后，请求状态为 `待处理`，等待司机端处理。

### 4.3 查询匹配请求状态

```http
GET /api/match-requests/{id}
```

用途：乘客端轮询或刷新查看司机是否同意或拒绝。

建议返回字段：

```json
{
  "id": "req-001",
  "passengerOrderId": "po-001",
  "driverTripId": "trip-001",
  "status": "已同意",
  "orderId": "order-1001",
  "handledAt": "2026-06-09 08:08",
  "rejectReason": ""
}
```

说明：

- 司机同意后，建议返回正式订单 `orderId`
- 司机拒绝后，`orderId` 可以为空，建议返回简短 `rejectReason`，乘客端展示拒绝信息并允许重新选择司机
- 乘客主动发起请求时，司机同意后订单建议直接进入 `进行中`
- 乘客端发送请求后默认回到乘车页展示等待确认状态，不跳到订单页承载主流程
- 若后续实现司机主动向乘客发起接车请求，才需要乘客二次确认

## 5. 正式订单操作接口

### 5.1 乘客确认司机主动接车请求

```http
POST /api/orders/{id}/passenger-confirm
```

用途：仅用于司机主动向乘客发起接车请求的场景，乘客确认后订单进入 `进行中`。

建议请求字段：

```json
{
  "confirm": true
}
```

建议返回字段：

```json
{
  "id": "order-1001",
  "status": "进行中"
}
```

说明：

- 当前 B 组 MVP 只实现乘客主动向司机发起请求，司机同意后直接进入 `进行中`，不需要乘客二次确认。
- 该接口为后续“司机主动接车请求”预留。

### 5.2 乘客模拟付款

```http
POST /api/orders/{id}/pay
```

用途：mock 司机送达后，乘客在 `进行中` 订单上进行模拟付款。

建议请求字段：

```json
{
  "amount": 18.6,
  "method": "mock_balance"
}
```

建议返回字段：

```json
{
  "id": "pay-001",
  "orderId": "order-1001",
  "amount": 18.6,
  "status": "已支付",
  "orderStatus": "已完成",
  "paymentStatus": "已支付",
  "reviewStatus": "未评价",
  "paidAt": "2026-06-09 09:20"
}
```

说明：

- 支付只由乘客侧触发
- MVP 只做模拟支付，不接真实支付平台
- 支付前订单主状态仍显示为 `进行中`
- 支付完成后订单主状态进入 `已完成`
- 支付完成后允许评价，评价不再推动订单主状态

### 5.3 乘客提交评价

```http
POST /api/orders/{id}/review
```

用途：乘客付款后提交订单评价。

建议请求字段：

```json
{
  "rating": 5,
  "comment": "司机很准时，路线也顺路"
}
```

建议返回字段：

```json
{
  "id": "review-001",
  "orderId": "order-1001",
  "rating": 5,
  "comment": "司机很准时，路线也顺路",
  "status": "已评价",
  "orderStatus": "已完成"
}
```

说明：评价只能在支付完成后提交；订单已在付款后进入 `已完成`，评价只补充评价信息。

## 6. B 组本地模拟说明

以下动作是 B 组开发阶段为了独立演示闭环使用的本地 mock，不建议作为乘客端真实接口：

- mock 司机同意请求
- mock 司机拒绝请求
- mock 行程开始
- mock 行程完成

真实联调阶段，这些状态变化应由 C 组司机端操作或后端订单状态机触发。B 组只负责在乘客端接收并展示状态变化。

当前 mock 动作已经封装在 `src/api/passengerApi.mock.js`，页面侧统一通过 `src/api/passengerApi.js` 调用。联调时如果 A/C 组接口返回七阶段订单状态，adapter 需要保留 `rawStatus`，并映射到 B 组展示状态：`待匹配 / 进行中 / 已完成 / 已取消`。

## 7. 需要统一的枚举字段

### 7.1 订单状态

建议统一为：

- 待匹配
- 进行中
- 已完成
- 已取消

### 7.2 匹配请求状态

建议统一为：

- 待处理
- 已同意
- 已拒绝

### 7.3 支付状态

建议统一为：

- 未支付
- 已支付

### 7.4 评价状态

建议统一为：

- 未评价
- 已评价

### 7.5 用户限制状态

建议与 C 组保持一致：

- 正常
- 观察
- 限制发单
- 限制接单
- 已封禁

## 8. 与 A / C 组联调检查项

1. 乘客发布需求后，A 组是否能保存 `PassengerOrder`
2. 推荐司机行程是否能正确过滤无座位、低信用分、低顺路度行程
3. B 组发起的 `MatchRequest` 是否能在 C 组司机端展示
4. C 组司机同意后，B 组是否能拿到正式订单 `orderId`
5. 乘客主动发起请求时，司机同意后订单是否直接进入 `进行中`
6. 司机主动发起接车请求时，是否需要乘客确认后再进入 `进行中`
7. 司机端确认到达或开始行程后，乘客端是否继续显示 `进行中` 并更新实时行程信息
8. 司机端确认送达后，乘客端是否继续显示 `进行中` 并开放付款入口
9. 乘客付款后，订单是否记录支付状态并进入 `已完成`
10. 乘客评价后，订单是否只更新评价状态，不再改变订单主状态
11. 管理端订单看板是否能看到 B 组创建并完成的订单

## 9. MVP 后续扩展接口需求

以下接口不是当前 B 组 MVP 必须实现内容，只用于记录真实乘客日常使用时可能需要的后续能力。是否进入开发，需要先和 A / C 组确认。

### 9.1 地点选择和常用地址

```http
GET /api/locations/search?keyword=同济
GET /api/passenger/favorite-locations
POST /api/passenger/favorite-locations
DELETE /api/passenger/favorite-locations/{id}
```

用途：支持出发地 / 目的地搜索、常用地址和最近地点。B 组可先用 mock 地点选择页实现，真实地图和 POI 数据后续再接。

### 9.2 等待超时和重新推荐

```http
GET /api/match-requests/{id}
POST /api/match-requests/{id}/cancel
GET /api/passenger/orders/{passengerOrderId}/matches
```

用途：司机长时间未处理时，乘客可以取消当前请求并重新选择其他推荐司机。需要 A / C 组确认请求超时时长和取消后司机端展示规则。

### 9.3 司机详情和匹配理由

```http
GET /api/driver/trips/{driverTripId}
GET /api/drivers/{driverId}/profile
```

用途：展示司机信用分、车辆、历史评价、接单率、距离、预计到达时间和推荐理由。B 组只负责乘客端展示，司机资料来源需要 C 组或 A 组提供。

### 9.4 修改需求和取消原因

```http
PATCH /api/passenger/orders/{id}
POST /api/passenger/orders/{id}/cancel
```

用途：支持乘客修改时间、地点、人数、备注，以及取消订单时提交原因。已进入 `进行中` 的订单是否允许修改、是否扣信用分，需要 A / C 组统一规则。

### 9.5 投诉、申诉和行程安全入口

```http
POST /api/orders/{id}/complaints
GET /api/passenger/emergency-contacts
POST /api/passenger/emergency-contacts
POST /api/orders/{id}/share
```

用途：支持投诉、紧急联系人和行程分享。该方向真实实现会涉及管理端、消息和安全模块，建议先作为报告扩展设计，不作为 B 组当前开发重点。

## 10. B 组开发自测边界

B 组开发过程中需要做单元模块测试和功能自测，确保乘客端页面、状态流转函数和 mock 接口能够支撑 MVP 演示。

这部分自测不等同于课程项目的正式测试阶段。代码组完成开发后，仍需要邀请测试组进行独立测试、集成确认和测试报告整理。

## 11. 已接入的账户级黑名单接口

乘客端“我的 / 安全与隐私”、推荐司机和订单详情已经接入以下真实接口：

```http
GET /api/privacy/blacklist
POST /api/privacy/blacklist
DELETE /api/privacy/blacklist/{blockedUserId}
```

`PassengerOrderVO` 在已有正式订单时返回 `driverId`，用于乘客从订单详情发起拉黑。拉黑不会取消当前订单，只会让双方不再进入后续推荐；列表中的姓名由后端统一脱敏。

## 12. 已接入的行程安全接口

乘客端“我的 / 安全与隐私”已接入紧急联系人 CRUD；进行中行程在司机确认开始后显示课程 Mock 报警入口。

```http
GET /api/safety/emergency-contacts
POST /api/safety/emergency-contacts
PUT /api/safety/emergency-contacts/{contactId}
DELETE /api/safety/emergency-contacts/{contactId}
POST /api/safety/orders/{orderId}/alerts
```

联系人响应只返回脱敏手机号。报警会尝试提交当前位置，定位失败时仍允许上报；报警仅记录安全事件，不改变当前订单状态。

# B/C 乘客端与司机端跨端集成说明

## 1. 本次目标

将原本互相独立的 B 组乘客端 mock 和 C 组司机端 / 管理端 mock 连接成同一条演示链路：

```text
乘客发布需求
-> 选择推荐司机
-> 发起拼车请求
-> 司机端收到请求
-> 司机同意或拒绝
-> 司机开始行程
-> 司机确认送达
-> 乘客付款
-> 管理端同步查看订单
```

## 2. 共享平台状态

共享状态入口：

```text
src/platform/sharedPlatform.js
```

统一维护：

- `passengerState`
- `cState`
- `passengerApi`
- `driverApi`
- `adminApi`

乘客端和 C 组工作台都通过 `useSharedPlatform()` 获取同一份状态。

## 3. API adapter

乘客端：

```text
src/api/passengerApi.js
src/api/passengerApi.mock.js
```

司机端：

```text
src/api/driverApi.js
src/api/driverApi.mock.js
```

管理端：

```text
src/api/adminApi.js
src/api/adminApi.mock.js
```

司机端支持发布行程、读取乘客请求、同意或拒绝、开始行程和确认送达。

管理端支持审核、读取共享订单、同步车辆认证状态和调整信用分。

## 4. 页面变化

乘客端不再自己模拟司机操作。请求发出后，需要在 C 组司机端处理。

司机操作后，乘客端会自动更新为：

- 待确认
- 接驾中
- 行程中
- 待付款
- 已完成

管理端订单看板读取共享正式订单，因此能够看到订单状态变化。

## 5. 自动烟测

运行：

```bash
npm run test:integration
```

覆盖乘客发单、司机处理、行程履约、付款、管理端同步、车辆审核和信用分同步。

## 6. 人工演示步骤

1. 在“06 乘客端 MVP 工作台”发布需求。
2. 在推荐页向陈师傅发送拼车请求。
3. 滚动到“07 C 组跨端工作台”。
4. 在司机端同意请求。
5. 回到乘客端，确认状态变为接驾中。
6. 在司机端点击“开始行程”。
7. 回到乘客端，确认状态变为行程中。
8. 在司机端点击“确认送达”。
9. 在乘客端完成付款。
10. 在管理端确认订单状态为已完成。

import assert from 'node:assert/strict'
import { createSharedPlatform } from '../src/platform/sharedPlatform.js'

const { passengerState, cState, passengerApi, driverApi, adminApi } = createSharedPlatform()
const seedTrip = passengerState.driverTrips.find((trip) => trip.driverId === 'driver-001')

assert.ok(seedTrip, '应存在可供乘客选择的司机行程')

await passengerApi.saveEmergencyContact({
  name: '乘客家属',
  phone: '13812345678',
  relationship: '家属',
  isPrimary: true
})
assert.equal(passengerApi.getEmergencyContacts().length, 1)
assert.equal(driverApi.getEmergencyContacts().length, 0, '司机不能看到乘客的紧急联系人')

await driverApi.saveEmergencyContact({
  name: '司机家属',
  phone: '13912345678',
  relationship: '家属',
  isPrimary: true
})
assert.equal(driverApi.getEmergencyContacts().length, 1)
assert.equal(passengerApi.getEmergencyContacts().length, 1, '乘客联系人列表应保持账户隔离')

const preferences = await passengerApi.updatePreferences({
  darkMode: true,
  fontScale: 'large',
  locale: 'zh-CN'
})
assert.equal(preferences.darkMode, true, '偏好设置应持久化到乘客状态')
const binding = await passengerApi.createBinding({
  bindingType: 'PAYMENT',
  provider: 'mock-pay',
  accountLabel: '余额账户',
  accountValue: 'mock-payment-001'
})
assert.equal(passengerApi.getBindings().some((item) => item.id === binding.id), true, '账号绑定应写入列表')
await passengerApi.deleteBinding(binding.id)
assert.equal(passengerApi.getBindings().find((item) => item.id === binding.id)?.status, 'UNBOUND')
assert.equal(passengerApi.getRetentionPolicies().length >= 4, true, '乘客应能查看数据保留策略')

const supportSession = await passengerApi.createSupportSession({
  subject: '账单咨询',
  message: '需要客服协助',
  attachmentKey: ''
})
await passengerApi.sendSupportMessage(supportSession.id, {
  messageType: 'image',
  content: '',
  attachmentKey: 'mock/support.png'
})
await adminApi.refreshSupportSessions()
assert.equal(adminApi.getSupportSessions().some((item) => item.id === supportSession.id), true, '管理端应能看到客服会话')
await adminApi.claimSupportSession(supportSession.id)
await adminApi.replySupportSession(supportSession.id, { messageType: 'text', content: '已收到' })
const closedSupport = await adminApi.closeSupportSession(supportSession.id)
assert.equal(closedSupport.status, 'CLOSED', '客服应能关闭会话')

const passengerOrder = await passengerApi.createPassengerOrder({
  start: seedTrip.start,
  end: seedTrip.end,
  departureTime: '2026-06-12 18:30',
  passengerCount: 1,
  note: '跨端集成烟测'
})

const recommendations = await passengerApi.getRecommendedTrips(passengerOrder.id)
assert.ok(recommendations.some((trip) => trip.id === seedTrip.id), '乘客端应推荐司机行程')

await passengerApi.blockUser(seedTrip.driverId)
assert.equal(passengerApi.getBlacklist().some((item) => item.blockedUserId === seedTrip.driverId), true)
assert.equal(
  (await passengerApi.getRecommendedTrips(passengerOrder.id)).some((trip) => trip.id === seedTrip.id),
  false,
  '乘客拉黑司机后，乘客推荐应排除该司机'
)
assert.equal(
  driverApi.getRecommendations().some((item) => item.passengerId === passengerOrder.passengerId),
  false,
  '乘客拉黑司机后，司机推荐也应排除该乘客'
)
await passengerApi.unblockUser(seedTrip.driverId)
assert.equal(
  (await passengerApi.getRecommendedTrips(passengerOrder.id)).some((trip) => trip.id === seedTrip.id),
  true,
  '乘客解除拉黑后应恢复推荐'
)

await driverApi.blockUser(passengerOrder.passengerId)
assert.equal(driverApi.getBlacklist().some((item) => item.blockedUserId === passengerOrder.passengerId), true)
assert.equal(
  (await passengerApi.getRecommendedTrips(passengerOrder.id)).some((trip) => trip.id === seedTrip.id),
  false,
  '司机拉黑乘客后，乘客推荐也应排除该司机'
)
assert.equal(
  driverApi.getRecommendations().some((item) => item.passengerId === passengerOrder.passengerId),
  false,
  '司机拉黑乘客后，司机推荐应排除该乘客'
)
await driverApi.unblockUser(passengerOrder.passengerId)

const request = await passengerApi.sendMatchRequest({
  passengerOrderId: passengerOrder.id,
  driverTripId: seedTrip.id
})
assert.equal(driverApi.getMatchRequests().some((item) => item.id === request.id), true, '司机端应收到乘客请求')

const acceptedRequest = await driverApi.acceptMatchRequest(request.id)
assert.equal(acceptedRequest.status, '已同意')
assert.ok(acceptedRequest.conversationId, '司机同意后应创建协商会话')
assert.equal(
  passengerState.passengerOrders.find((item) => item.id === passengerOrder.id)?.status,
  '协商中',
  '乘客订单应进入协商中'
)
assert.equal(adminApi.getOrders().some((item) => item.id === acceptedRequest.orderId), false, '双向确认前不应生成正式订单')

const proposal = await driverApi.createProposal(acceptedRequest.conversationId, {
  price: 20,
  departureTime: '2026-06-12 18:40',
  passengerCount: 1,
  pickupLocation: passengerOrder.start,
  note: '北门集合'
})
assert.equal(proposal.version, 1, '首次协商方案版本应为 1')

const passengerConfirmed = await passengerApi.confirmProposal(acceptedRequest.conversationId, proposal.version)
assert.deepEqual(passengerConfirmed.confirmedRoles.sort(), ['PASSENGER'])

const finalizedProposal = await driverApi.confirmProposal(acceptedRequest.conversationId, proposal.version)
assert.deepEqual(finalizedProposal.confirmedRoles.sort(), ['DRIVER', 'PASSENGER'])
assert.ok(finalizedProposal.orderId, '双方确认同一版本方案后应生成正式订单')

const order = driverApi.getOrders().find((item) => item.id === finalizedProposal.orderId)
assert.ok(order, '司机端应看到新生成的正式订单')
assert.equal(order.status, '已确认')
assert.equal(adminApi.getOrders().find((item) => item.id === order.id)?.status, '已确认')

const startedOrder = await driverApi.startTrip(order.id)
assert.equal(startedOrder.live.phase, 'trip')
assert.equal(adminApi.getOrders().find((item) => item.id === order.id)?.status, '履约中')
passengerState.trajectories[order.id] = []
const preLocationSafetyOrder = adminApi.getSafetyOrders().find((item) => item.orderId === order.id)
assert.equal(preLocationSafetyOrder?.noLocationUpdate, true, '未上报位置前安全看板应提示无位置更新')

const batchPoints = await driverApi.reportLocationBatch(order.id, [
  { longitude: 121.51, latitude: 31.21, clientReportId: 'integration-loc-1' }
])
assert.equal(batchPoints.length, 1, '司机批量位置补传应写入轨迹')
assert.equal(adminApi.getSafetyOrders().some((item) => item.orderId === order.id), true, '安全监控应展示履约中订单')
assert.equal(adminApi.getSafetyOrders().find((item) => item.orderId === order.id)?.noLocationUpdate, false, '补传位置后无更新提示应消失')

const passengerAlert = await passengerApi.triggerAlert(order.id, {
  alertType: '一键报警',
  longitude: 121.5,
  latitude: 31.2,
  details: '乘客端集成测试'
})
const driverAlert = await driverApi.triggerAlert(order.id, {
  alertType: '一键报警',
  details: '司机端集成测试'
})
assert.equal(passengerAlert.status, '已上报')
assert.equal(driverAlert.status, '已上报')
assert.equal(passengerState.safetyAlerts.length, 2)
assert.equal(startedOrder.live.phase, 'trip', '报警不能改变订单履约阶段')
assert.equal(adminApi.getOrders().find((item) => item.id === order.id)?.status, '履约中')

const virtualPhone = await passengerApi.getVirtualPhone(order.id)
assert.ok(virtualPhone?.virtualNumber, '行程中应能查询虚拟号')
const shareLink = await passengerApi.createShareLink(order.id, { expiresMinutes: 120 })
assert.ok(shareLink?.token, '乘客应能生成行程分享链接')
const trajectory = await passengerApi.getTrajectory(order.id)
assert.ok(trajectory.length > 0, '乘客应能查看本人订单轨迹')
const appeal = await passengerApi.createAppeal(order.id, {
  reasonType: 'SERVICE_DISPUTE',
  description: '集成 smoke 申诉',
  evidenceKeys: ['mock/evidence.png']
})
assert.equal(appeal.status, 'OPEN')
await adminApi.refreshAppeals()
assert.equal(adminApi.getAppeals().some((item) => item.id === appeal.id), true, '管理端应能看到乘客申诉工单')
const transitionedAppeal = await adminApi.transitionAppeal(appeal.id, {
  status: 'IN_PROGRESS',
  comment: '管理员处理中',
  assigneeId: 'admin-001'
})
assert.equal(transitionedAppeal.status, 'IN_PROGRESS')
const arbitration = await adminApi.arbitrateAppeal(appeal.id, {
  actionType: 'ORDER_NOTE',
  remark: '集成 smoke 仲裁备注',
  confirmed: true
})
assert.equal(arbitration.actionType, 'ORDER_NOTE')
const orderDetail = await adminApi.getOrderDetail(order.id)
assert.equal(orderDetail.appeals.some((item) => item.id === appeal.id), true, '订单详情应聚合申诉数据')
assert.equal(orderDetail.alerts.length >= 2, true, '订单详情应聚合安全告警')

const exportedCsv = await adminApi.exportOrdersCsv({})
assert.equal(exportedCsv.includes(order.id), true, '订单导出 CSV 应包含当前订单')
const smokeRule = await adminApi.createRule({
  ruleType: 'PRICING',
  code: 'INTEGRATION_SMOKE',
  name: '集成测试计价',
  grayPercent: 100,
  content: '{"basePrice":9}'
})
await adminApi.publishRule(smokeRule.id)
assert.equal(adminApi.getRules().find((item) => item.id === smokeRule.id)?.status, 'ACTIVE', '规则发布后应变为 ACTIVE')
const retention = await adminApi.saveRetentionPolicy({
  dataType: 'LOCATION',
  retentionDays: 32,
  deleteMode: 'ARCHIVE_THEN_DELETE',
  description: 'integration'
})
assert.equal(retention.retentionDays, 32)
const cleanupPreview = await adminApi.cleanupRetention({ dryRun: true })
assert.equal(cleanupPreview.dryRun, true, '保留策略清理应支持 dryRun 预览')
assert.equal(cleanupPreview.policyCount >= 4, true, '保留策略清理结果应返回策略说明')
const restrictionTarget = adminApi.getCreditUsers()[0]
const manualRestriction = await adminApi.restrictUser(restrictionTarget.id, {
  type: 'LIMIT_COMMUNICATION',
  reason: '集成 smoke 限制通信'
})
assert.equal(manualRestriction.status, 'ACTIVE')
assert.equal(await adminApi.releaseRestriction(restrictionTarget.id, 'LIMIT_COMMUNICATION'), true)

const arrivedOrder = await driverApi.finishTrip(order.id)
assert.equal(arrivedOrder.canPay, true)
assert.equal(adminApi.getOrders().find((item) => item.id === order.id)?.status, '待付款')

const paidOrder = await passengerApi.payOrder(order.id)
assert.equal(paidOrder.status, '已完成')
assert.equal(adminApi.getOrders().find((item) => item.id === order.id)?.status, '已完成')
await adminApi.refreshOpsStatistics()
assert.equal(adminApi.getOpsStatistics().totalOrders >= 1, true, '运营统计应返回订单指标')

const pendingVehicle = cState.vehicles.find((vehicle) => vehicle.status === '待审核')
const vehicleAudit = cState.auditVehicles.find((audit) => audit.plate === pendingVehicle?.plate)
assert.ok(vehicleAudit, '应存在与待审核车辆对应的审核记录')
await adminApi.reviewVehicleAudit(vehicleAudit.id, '已通过')
assert.equal(pendingVehicle.status, '已认证', '车辆审核结果应同步到司机端')

const passengerCredit = adminApi.getCreditUsers().find((user) => user.name === passengerState.passenger.name)
await adminApi.adjustCredit(passengerCredit.id, -50, '集成烟测扣分')
assert.equal(passengerState.passenger.credit, passengerCredit.credit, '信用分应同步到乘客资料')
const blockedPassengerOrder = await passengerApi.createPassengerOrder({
  start: seedTrip.start,
  end: seedTrip.end,
  departureTime: '2026-06-12 19:30',
  passengerCount: 1,
  note: '低信用分发单测试'
})
assert.equal(blockedPassengerOrder, null, '信用分低于 60 的乘客应被限制发单')

const driverCredit = adminApi.getCreditUsers().find((user) => user.id === 'driver-001')
await adminApi.adjustCredit(driverCredit.id, -50, '集成烟测扣分')
const blockedDriverTrip = await driverApi.createTrip({
  start: seedTrip.start,
  end: seedTrip.end,
  time: '2026-06-12 19:30',
  vehicle: cState.vehicles.find((vehicle) => vehicle.status === '已认证')?.plate,
  seats: 2,
  price: 18
})
assert.equal(blockedDriverTrip, null, '信用分低于 60 的司机应被限制发车')

console.log(
  JSON.stringify(
    {
      recommendationCount: recommendations.length,
      blacklistCount: passengerApi.getBlacklist().length + driverApi.getBlacklist().length,
      emergencyContactCount: passengerApi.getEmergencyContacts().length + driverApi.getEmergencyContacts().length,
      safetyAlertCount: passengerState.safetyAlerts.length,
      requestStatus: acceptedRequest.status,
      proposalVersion: finalizedProposal.version,
      orderStatus: paidOrder.status,
      adminOrderStatus: adminApi.getOrders().find((item) => item.id === order.id)?.status,
      vehicleStatus: pendingVehicle.status,
      passengerCredit: passengerState.passenger.credit,
      opsTotalOrders: adminApi.getOpsStatistics().totalOrders
    },
    null,
    2
  )
)

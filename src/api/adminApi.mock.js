import { ORDER_STATUS } from './passengerApi.mock.js'

const asyncReturn = (value) => Promise.resolve(value)

const hasText = (value) => String(value || '').trim().length > 0
const nowText = () => new Date().toISOString().slice(0, 16).replace('T', ' ')
const maskName = (value) => value ? `${String(value).slice(0, 1)}**` : ''
const maskPlate = (value) => {
  const text = String(value || '')
  return text.length < 3 ? text : `${text.slice(0, 2)}***${text.slice(-1)}`
}
const maskVehicle = (value) => {
  const text = String(value || '')
  const [plate, ...rest] = text.split(/\s*路\s*/)
  return [maskPlate(plate), ...rest].filter(Boolean).join(' 路 ')
}
const csvCell = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`
const allowedRuleTypes = new Set(['MATCHING', 'PRICING', 'CREDIT'])

function validateRulePayload(payload = {}) {
  if (!allowedRuleTypes.has(payload.ruleType)) {
    throw new Error('规则类型必须是 MATCHING、PRICING 或 CREDIT')
  }
  try {
    const parsed = JSON.parse(payload.content || '')
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object' || Object.keys(parsed).length === 0) {
      throw new Error('规则内容必须是非空 JSON 对象')
    }
  } catch (error) {
    throw new Error(error.message || '规则内容必须是非空 JSON 对象')
  }
}

function displayOrderStatus(order) {
  const trackedStatuses = [
    ORDER_STATUS.MATCHING,
    ORDER_STATUS.NEGOTIATING,
    ORDER_STATUS.CONFIRMED,
    ORDER_STATUS.IN_PROGRESS,
    ORDER_STATUS.PENDING_PAYMENT,
    ORDER_STATUS.COMPLETED,
    ORDER_STATUS.CANCELLED,
    ORDER_STATUS.ARCHIVED
  ]
  if (trackedStatuses.includes(order.status)) return order.status
  if (order.canPay) return ORDER_STATUS.PENDING_PAYMENT
  if (order.live?.phase === 'trip') return ORDER_STATUS.IN_PROGRESS
  if (order.live?.phase === 'arrived') return ORDER_STATUS.PENDING_PAYMENT
  return ORDER_STATUS.CONFIRMED
}

function normalizeFilters(filters = {}) {
  return {
    status: filters.status || '',
    paymentStatus: filters.paymentStatus || '',
    keyword: String(filters.keyword || '').trim(),
    dateFrom: filters.dateFrom || '',
    dateTo: filters.dateTo || ''
  }
}

function filterAudits(items, filters = {}) {
  const status = filters.status || ''
  return status ? items.filter((item) => item.status === status) : items
}

function filterOrders(items, filters = {}) {
  const query = normalizeFilters(filters)
  return items.filter((order) => {
    if (query.status && order.status !== query.status) return false
    if (query.paymentStatus && order.paymentStatus !== query.paymentStatus) return false
    if (query.keyword) {
      const haystack = [order.id, order.route, order.passenger, order.driver].join(' ')
      if (!haystack.includes(query.keyword)) return false
    }
    if (query.dateFrom && order.createdAt && order.createdAt < query.dateFrom) return false
    if (query.dateTo && order.createdAt && order.createdAt > query.dateTo) return false
    return true
  })
}

function filterAppeals(items, filters = {}) {
  const status = filters.status || ''
  return status ? items.filter((item) => item.status === status) : items
}

function summarizeOrders(orders) {
  const summary = {
    total: orders.length,
    pendingPayment: 0,
    completed: 0,
    cancelled: 0,
    statusCounts: {},
    paymentStatusCounts: {}
  }
  for (const order of orders) {
    summary.statusCounts[order.status] = (summary.statusCounts[order.status] || 0) + 1
    summary.paymentStatusCounts[order.paymentStatus || '未知'] =
      (summary.paymentStatusCounts[order.paymentStatus || '未知'] || 0) + 1
    if (order.status === '待付款') summary.pendingPayment += 1
    if (order.status === '已完成') summary.completed += 1
    if (order.status === '已取消') summary.cancelled += 1
  }
  return summary
}

function requireRejectReason(status, reason) {
  if (status === '已驳回' && !hasText(reason)) {
    throw new Error('驳回原因不能为空')
  }
}

export function createAdminApi({ passengerState, cState }) {
  const allUserAudits = cState.auditUsers
  const allVehicleAudits = cState.auditVehicles
  const staticOrders = cState.adminOrders
  let currentOrderFilters = {}
  let currentAppealFilters = {}

  const getAllOrders = () => {
    const liveOrders = passengerState.orderRecords.map((order) => ({
      id: order.id,
      route: order.route,
      passenger: maskName(order.passengerName),
      driver: maskName(order.driverName),
      vehicle: maskVehicle(order.vehicle),
      status: displayOrderStatus(order),
      paymentStatus: order.paymentStatus,
      price: order.price,
      risk: order.canPay && order.paymentStatus === '未支付' ? '待支付' : '正常',
      createdAt: order.createdAt
    }))
    const liveIds = new Set(liveOrders.map((order) => order.id))
    return [
      ...liveOrders,
      ...staticOrders
        .filter((order) => !liveIds.has(order.id))
        .map((order) => ({
          ...order,
          passenger: maskName(order.passenger),
          driver: maskName(order.driver),
          vehicle: maskVehicle(order.vehicle)
        }))
    ]
  }

  const refreshOrderState = (filters = currentOrderFilters) => {
    currentOrderFilters = { ...filters }
    cState.adminOrders = filterOrders(getAllOrders(), currentOrderFilters)
    cState.adminOrderSummary = summarizeOrders(cState.adminOrders)
    return cState.adminOrders
  }

  const refreshAppealState = (filters = currentAppealFilters) => {
    currentAppealFilters = { ...filters }
    cState.adminAppeals = filterAppeals(passengerState.appeals || [], currentAppealFilters)
    return cState.adminAppeals
  }

  const refreshSafetyState = () => {
    cState.adminSafetyOrders = passengerState.orderRecords
      .filter((order) => order.live?.phase === 'trip')
      .map((order) => ({
        orderId: order.id,
        route: order.route,
        passenger: maskName(order.passengerName),
        driver: maskName(order.driverName),
        status: displayOrderStatus(order),
        latestLocation: (passengerState.trajectories[order.id] || []).at(-1) || null,
        latestLocationAt: (passengerState.trajectories[order.id] || []).at(-1)?.reportedAt || '',
        noLocationUpdate: !(passengerState.trajectories[order.id] || []).length,
        safetyStatus: (passengerState.trajectories[order.id] || []).length ? 'OK' : 'NO_LOCATION_UPDATE',
        virtualPhoneStatus: passengerState.virtualPhones[order.id]?.status || 'UNBOUND',
        shareStatus: passengerState.shareLinks[order.id] ? 'ACTIVE' : 'INACTIVE',
        trajectoryCount: (passengerState.trajectories[order.id] || []).length,
        alertCount: passengerState.safetyAlerts.filter((alert) => alert.orderId === order.id).length
      }))
    cState.adminSafetyAlerts = [...passengerState.safetyAlerts]
    return cState.adminSafetyOrders
  }

  const refreshSupportState = () => {
    cState.adminSupportSessions = passengerState.supportSessions
    cState.supportMessages = passengerState.supportMessages
    return cState.adminSupportSessions
  }

  const retentionPolicies = () => {
    if (!cState.dataRetentionPolicies?.length) {
      cState.dataRetentionPolicies = passengerState.dataRetentionPolicies
    }
    return cState.dataRetentionPolicies
  }

  const refreshStatsState = () => {
    const orders = getAllOrders()
    const total = orders.length
    const completed = orders.filter((order) => order.status === '已完成').length
    const cancelled = orders.filter((order) => order.status === '已取消').length
    const active = orders.filter((order) => ['已确认', '履约中', '待付款'].includes(order.status)).length
    const alerts = passengerState.safetyAlerts.length
    cState.adminOpsStatistics = {
      totalOrders: total,
      completedOrders: completed,
      cancelledOrders: cancelled,
      activeOrders: active,
      safetyAlertCount: alerts,
      openAppealCount: (passengerState.appeals || []).filter((item) => item.status !== 'CLOSED').length,
      conversionRate: total ? Number((((active + completed) / total) * 100).toFixed(2)) : 0,
      completionRate: total ? Number(((completed / total) * 100).toFixed(2)) : 0,
      cancellationRate: total ? Number(((cancelled / total) * 100).toFixed(2)) : 0,
      averageMatchMinutes: 18,
      abnormalRate: total ? Number(((alerts / total) * 100).toFixed(2)) : 0
    }
    return cState.adminOpsStatistics
  }

  const buildOrderDetail = (id) => {
    const order =
      passengerState.orderRecords.find((item) => item.id === id) ||
      getAllOrders().find((item) => item.id === id)
    if (!order) return null
    const trajectory = passengerState.trajectories[id] || []
    const appeals = (passengerState.appeals || []).filter((item) => item.orderId === id)
    return {
      ...order,
      route: order.route,
      passenger: maskName(order.passengerName || order.passenger),
      driver: maskName(order.driverName || order.driver),
      status: displayOrderStatus(order),
      paymentStatus: order.paymentStatus,
      latestLocation: trajectory.at(-1) || null,
      trajectory,
      alerts: passengerState.safetyAlerts.filter((alert) => alert.orderId === id),
      timeline: [
        { id: `${id}-created`, fromStatus: '', toStatus: '已确认', reason: '订单创建', createdAt: order.createdAt },
        { id: `${id}-current`, fromStatus: '已确认', toStatus: displayOrderStatus(order), reason: '当前状态', createdAt: nowText() }
      ],
      payments: order.paymentStatus === '已支付' ? [{ id: `pay-${id}`, amount: order.price, status: '已支付', method: 'mock_balance', paidAt: nowText() }] : [],
      reviews: order.reviewStatus === '已评价' ? [{ id: `review-${id}`, rating: 5, comment: '行程顺利', createdAt: nowText() }] : [],
      creditEvents: [],
      appeals,
      negotiations: order.proposals || []
    }
  }

  const statusForRestriction = (type, role) => {
    if (type === 'FULL_BAN') return '已封禁'
    if (type === 'LIMIT_COMMUNICATION') return '限制通信'
    if (type === 'LIMIT_ACCEPT') return '限制接单'
    if (type === 'LIMIT_POST') return '限制发单'
    return role === 'driver' ? '限制接单' : '限制发单'
  }

  const restoreUserStatus = (user) => {
    if (Number(user.credit || 0) < 60) user.status = user.role === '司机' || user.role === 'driver' ? '限制接单' : '限制发单'
    else if (Number(user.credit || 0) < 80) user.status = '观察'
    else user.status = '正常'
    if (user.id === passengerState.passenger.id || user.name === passengerState.passenger.name) {
      passengerState.passenger.status = user.status
    }
  }

  return {
    getMe() {
      return cState.adminMe
    },

    initialize() {
      refreshOrderState()
      refreshAppealState()
      refreshSafetyState()
      refreshSupportState()
      retentionPolicies()
      refreshStatsState()
      return asyncReturn(null)
    },

    refreshUserAudits(filters = {}) {
      cState.auditUsers = filterAudits(allUserAudits, filters)
      return asyncReturn(cState.auditUsers)
    },

    getUserAudits() {
      return cState.auditUsers
    },

    refreshVehicleAudits(filters = {}) {
      cState.auditVehicles = filterAudits(allVehicleAudits, filters)
      return asyncReturn(cState.auditVehicles)
    },

    getVehicleAudits() {
      return cState.auditVehicles
    },

    getAuditDetail(id) {
      const audit =
        cState.auditUsers.find((item) => item.id === id) ||
        cState.auditVehicles.find((item) => item.id === id) ||
        null
      if (!audit) return asyncReturn(null)
      audit.notifications = audit.notifications || [
        {
          id: `notice-${audit.id}`,
          title: '审核结果通知',
          sendStatus: '已发送',
          retryCount: 0,
          createdAt: audit.handledAt || audit.submittedAt || nowText()
        }
      ]
      return asyncReturn({ ...audit, histories: audit.histories || [], notifications: audit.notifications })
    },

    retryNotification(id) {
      const audits = [...cState.auditUsers, ...cState.auditVehicles]
      const audit = audits.find((item) => (item.notifications || []).some((notice) => notice.id === id))
      const notice = audit?.notifications?.find((item) => item.id === id)
      if (!notice) return asyncReturn(null)
      notice.sendStatus = '已发送'
      notice.retryCount = Number(notice.retryCount || 0) + 1
      notice.createdAt = nowText()
      return asyncReturn(notice)
    },

    reviewUserAudit(id, status, reason = '') {
      requireRejectReason(status, reason)
      const audit = cState.auditUsers.find((item) => item.id === id)
      if (!audit) return asyncReturn(null)
      audit.status = status
      audit.reason = reason
      audit.handledAt = new Date().toISOString().slice(0, 16).replace('T', ' ')
      audit.histories = [
        ...(audit.histories || []),
        { id: `ah-${Date.now()}`, adminId: 'admin-001', fromStatus: '待审核', toStatus: status, reason, createdAt: audit.handledAt }
      ]
      return asyncReturn(audit)
    },

    reviewVehicleAudit(id, status, reason = '') {
      requireRejectReason(status, reason)
      const audit = cState.auditVehicles.find((item) => item.id === id)
      if (!audit) return asyncReturn(null)

      audit.status = status
      audit.reason = reason
      audit.handledAt = new Date().toISOString().slice(0, 16).replace('T', ' ')
      audit.histories = [
        ...(audit.histories || []),
        { id: `ah-${Date.now()}`, adminId: 'admin-001', fromStatus: '待审核', toStatus: status, reason, createdAt: audit.handledAt }
      ]
      const vehicle = cState.vehicles.find((item) => item.plate === audit.plate)
      if (vehicle) vehicle.status = status === '\u5df2\u901a\u8fc7' ? '\u5df2\u8ba4\u8bc1' : '\u5df2\u9a73\u56de'
      return asyncReturn(audit)
    },

    refreshOrders(filters = {}) {
      return asyncReturn(refreshOrderState(filters))
    },

    refreshOrderSummary(filters = {}) {
      currentOrderFilters = { ...filters }
      cState.adminOrderSummary = summarizeOrders(filterOrders(getAllOrders(), currentOrderFilters))
      return asyncReturn(cState.adminOrderSummary)
    },

    getOrders() {
      return refreshOrderState()
    },

    getOrderSummary() {
      return cState.adminOrderSummary
    },

    getOrderDetail(id) {
      const detail = buildOrderDetail(id)
      if (detail) cState.adminOrderDetails[id] = detail
      return asyncReturn(detail)
    },

    refreshOpsStatistics() {
      return asyncReturn(refreshStatsState())
    },

    getOpsStatistics() {
      return refreshStatsState()
    },

    getCreditUsers() {
      return cState.creditUsers
    },

    refreshAppeals(filters = {}) {
      return asyncReturn(refreshAppealState(filters))
    },

    getAppeals() {
      return refreshAppealState()
    },

    refreshSafetyOrders() {
      return asyncReturn(refreshSafetyState())
    },

    getSafetyOrders() {
      return refreshSafetyState()
    },

    refreshSafetyAlerts() {
      refreshSafetyState()
      return asyncReturn(cState.adminSafetyAlerts)
    },

    getSafetyAlerts() {
      refreshSafetyState()
      return cState.adminSafetyAlerts
    },

    getSafetyTrajectory(orderId) {
      return asyncReturn(passengerState.trajectories[orderId] || [])
    },

    exportOrdersCsv(filters = {}) {
      const rows = filterOrders(getAllOrders(), filters)
      const csv = [
        'id,route,passenger,driver,status,paymentStatus,price,risk,createdAt',
        ...rows.map((order) => [
          order.id,
          order.route,
          order.passenger,
          order.driver,
          order.status,
          order.paymentStatus,
          order.price,
          order.risk,
          order.createdAt
        ].map(csvCell).join(','))
      ].join('\n')
      cState.exportedOrderCsv = csv
      return asyncReturn(csv)
    },

    refreshRules() {
      return asyncReturn(cState.ruleConfigs)
    },

    getRules() {
      return cState.ruleConfigs
    },

    createRule(payload = {}) {
      validateRulePayload(payload)
      const now = nowText()
      const rule = {
        id: `rule-${Date.now()}`,
        ruleType: payload.ruleType || 'MATCHING',
        code: payload.code || `RULE_${cState.ruleConfigs.length + 1}`,
        name: payload.name || '新规则',
        activeVersion: null,
        status: 'DRAFT',
        grayPercent: Number(payload.grayPercent || 100),
        versions: [
          {
            id: `rv-${Date.now()}`,
            version: 1,
            content: payload.content,
            status: 'DRAFT',
            createdBy: cState.adminMe?.id || 'admin-001',
            createdAt: now,
            publishedAt: ''
          }
        ],
        createdAt: now,
        updatedAt: now
      }
      cState.ruleConfigs.unshift(rule)
      return asyncReturn(rule)
    },

    updateRule(ruleId, payload = {}) {
      const rule = cState.ruleConfigs.find((item) => item.id === ruleId)
      if (!rule) return asyncReturn(null)
      validateRulePayload(payload)
      const version = Math.max(...rule.versions.map((item) => item.version), 0) + 1
      Object.assign(rule, {
        ruleType: payload.ruleType || rule.ruleType,
        code: payload.code || rule.code,
        name: payload.name || rule.name,
        grayPercent: Number(payload.grayPercent || rule.grayPercent || 100),
        status: 'DRAFT',
        updatedAt: nowText()
      })
      rule.versions.unshift({
        id: `rv-${Date.now()}`,
        version,
        content: payload.content,
        status: 'DRAFT',
        createdBy: cState.adminMe?.id || 'admin-001',
        createdAt: nowText(),
        publishedAt: ''
      })
      return asyncReturn(rule)
    },

    publishRule(ruleId) {
      const rule = cState.ruleConfigs.find((item) => item.id === ruleId)
      if (!rule) return asyncReturn(null)
      const latest = [...rule.versions].sort((left, right) => right.version - left.version)[0]
      latest.status = 'PUBLISHED'
      latest.publishedAt = nowText()
      rule.activeVersion = latest.version
      rule.status = 'ACTIVE'
      rule.updatedAt = latest.publishedAt
      return asyncReturn(rule)
    },

    rollbackRule(ruleId, payload = {}) {
      const rule = cState.ruleConfigs.find((item) => item.id === ruleId)
      if (!rule) return asyncReturn(null)
      const target = rule.versions.find((item) => item.version === Number(payload.toVersion))
      if (!target) return asyncReturn(null)
      target.status = 'PUBLISHED'
      target.publishedAt = nowText()
      rule.activeVersion = target.version
      rule.status = 'ACTIVE'
      rule.updatedAt = target.publishedAt
      return asyncReturn(rule)
    },

    refreshSupportSessions() {
      return asyncReturn(refreshSupportState())
    },

    getSupportSessions() {
      return refreshSupportState()
    },

    getSupportMessages(sessionId) {
      return passengerState.supportMessages[sessionId] || []
    },

    uploadAttachment(file = {}) {
      const fileName = file.name || `admin-support-${Date.now()}.jpg`
      return asyncReturn({
        attachmentKey: `/api/files/support/${cState.adminMe?.id || 'admin-001'}/${fileName}`,
        fileName,
        contentType: file.type || 'image/jpeg',
        size: file.size || 0
      })
    },

    claimSupportSession(sessionId) {
      const session = passengerState.supportSessions.find((item) => item.id === sessionId)
      if (!session || session.status === 'CLOSED') return asyncReturn(null)
      session.status = 'CLAIMED'
      session.assigneeId = cState.adminMe?.id || 'admin-001'
      session.updatedAt = nowText()
      refreshSupportState()
      return asyncReturn(session)
    },

    replySupportSession(sessionId, payload = {}) {
      const session = passengerState.supportSessions.find((item) => item.id === sessionId)
      if (!session || session.status === 'CLOSED') return asyncReturn(null)
      const message = {
        id: `support-admin-msg-${Date.now()}`,
        sessionId,
        senderId: cState.adminMe?.id || 'admin-001',
        senderRole: 'ADMIN',
        messageType: payload.messageType || (payload.attachmentKey ? 'image' : 'text'),
        content: payload.content || '',
        attachmentKey: payload.attachmentKey || '',
        createdAt: nowText()
      }
      passengerState.supportMessages[sessionId] = [...(passengerState.supportMessages[sessionId] || []), message]
      session.status = session.status === 'OPEN' ? 'CLAIMED' : session.status
      session.assigneeId = session.assigneeId || cState.adminMe?.id || 'admin-001'
      session.updatedAt = message.createdAt
      session.lastMessageAt = message.createdAt
      refreshSupportState()
      return asyncReturn(message)
    },

    closeSupportSession(sessionId) {
      const session = passengerState.supportSessions.find((item) => item.id === sessionId)
      if (!session) return asyncReturn(null)
      session.status = 'CLOSED'
      session.closedAt = nowText()
      session.updatedAt = session.closedAt
      refreshSupportState()
      return asyncReturn(session)
    },

    refreshRetentionPolicies() {
      return asyncReturn(retentionPolicies())
    },

    getRetentionPolicies() {
      return retentionPolicies()
    },

    saveRetentionPolicy(payload = {}) {
      const policies = retentionPolicies()
      const existing = policies.find((item) => item.dataType === payload.dataType)
      const policy = {
        ...(existing || {}),
        id: existing?.id || `ret-${Date.now()}`,
        dataType: payload.dataType,
        retentionDays: Number(payload.retentionDays || 30),
        deleteMode: payload.deleteMode || 'ARCHIVE_THEN_DELETE',
        description: payload.description || '',
        updatedBy: cState.adminMe?.id || 'admin-001',
        updatedAt: nowText()
      }
      if (existing) Object.assign(existing, policy)
      else policies.unshift(policy)
      return asyncReturn(policy)
    },

    cleanupRetention(payload = {}) {
      cState.retentionCleanupResult = {
        dryRun: payload.dryRun !== false,
        locationCutoffAt: nowText(),
        locationReports: 0,
        trajectoryPoints: 0,
        policyCount: retentionPolicies().length,
        policies: retentionPolicies(),
        notes: [
          'LOCATION 清理覆盖位置上报和轨迹点。',
          'CHAT、ATTACHMENT、AUDIT 保留策略用于课程版说明和审计追踪。'
        ]
      }
      return asyncReturn(cState.retentionCleanupResult)
    },

    getRetentionCleanupResult() {
      return cState.retentionCleanupResult
    },

    transitionAppeal(id, payload = {}) {
      const appeal = (passengerState.appeals || []).find((item) => item.id === id)
      if (!appeal) return asyncReturn(null)
      const now = new Date().toISOString().slice(0, 16).replace('T', ' ')
      const fromStatus = appeal.status
      appeal.status = payload.status || appeal.status
      appeal.assigneeId = payload.assigneeId ?? appeal.assigneeId
      appeal.updatedAt = now
      if (appeal.status === 'CLOSED') {
        appeal.closedAt = now
        appeal.resolution = payload.comment || appeal.resolution || '已关闭'
      }
      appeal.transitions = [
        ...(appeal.transitions || []),
        {
          id: `transition-admin-${Date.now()}`,
          appealId: appeal.id,
          operatorId: cState.adminMe?.id || 'admin-001',
          fromStatus,
          toStatus: appeal.status,
          comment: payload.comment || '',
          createdAt: now
        }
      ]
      refreshAppealState()
      return asyncReturn(appeal)
    },

    arbitrateAppeal(id, payload = {}) {
      if (!payload.confirmed) throw new Error('仲裁提交前需要二次确认')
      const appeal = (passengerState.appeals || []).find((item) => item.id === id)
      if (!appeal) return asyncReturn(null)
      const record = {
        id: `arb-${Date.now()}`,
        appealId: id,
        adminId: cState.adminMe?.id || 'admin-001',
        actionType: payload.actionType,
        amount: Number(payload.amount || 0),
        creditDelta: Number(payload.creditDelta || 0),
        remark: payload.remark || '',
        confirmedBy: cState.adminMe?.id || 'admin-001',
        createdAt: new Date().toISOString().slice(0, 16).replace('T', ' ')
      }
      appeal.arbitrations = [...(appeal.arbitrations || []), record]
      if (payload.actionType === 'CREDIT_CORRECTION') {
        const targetId = payload.targetUserId || appeal.applicantId
        const user = cState.creditUsers.find((item) => item.id === targetId || item.name === appeal.applicantName)
        if (user) {
          user.credit = Math.max(0, Math.min(100, Number(user.credit || 0) + Number(payload.creditDelta || 0)))
          if (targetId === passengerState.passenger.id || user.name === passengerState.passenger.name) {
            passengerState.passenger.credit = user.credit
          }
        }
      }
      if (payload.actionType === 'ORDER_NOTE') {
        const order = passengerState.orderRecords.find((item) => item.id === appeal.orderId)
        if (order) order.adminNote = payload.remark || ''
      }
      if (payload.actionType === 'REFUND') {
        const order = passengerState.orderRecords.find((item) => item.id === appeal.orderId)
        if (order) order.paymentStatus = '已退款'
      }
      if (payload.actionType === 'FORCE_SETTLEMENT') {
        const order = passengerState.orderRecords.find((item) => item.id === appeal.orderId)
        if (order) {
          order.status = '已完成'
          order.canPay = false
        }
      }
      refreshAppealState()
      return asyncReturn(record)
    },

    adjustCredit(id, delta, reason = '') {
      const user = cState.creditUsers.find((item) => item.id === id)
      if (!user) return asyncReturn(null)

      user.credit = Math.max(0, Math.min(100, user.credit + delta))
      user.reason = reason
      if (user.credit < 60) user.status = user.role === '\u53f8\u673a' ? '\u9650\u5236\u63a5\u5355' : '\u9650\u5236\u53d1\u5355'
      else if (user.credit < 80) user.status = '\u89c2\u5bdf'
      else user.status = '\u6b63\u5e38'

      if (user.name === passengerState.passenger.name) {
        passengerState.passenger.credit = user.credit
        passengerState.passenger.status = user.status
      }
      return asyncReturn(user)
    },

    restrictUser(id, payload = {}) {
      const user = cState.creditUsers.find((item) => item.id === id)
      if (!user || !payload.type) return asyncReturn(null)
      const record = {
        id: `restriction-${Date.now()}`,
        userId: id,
        type: payload.type,
        reason: payload.reason || '管理员风控',
        operatorId: cState.adminMe?.id || 'admin-001',
        status: 'ACTIVE',
        startsAt: nowText(),
        releasedAt: ''
      }
      cState.userRestrictions.unshift(record)
      user.status = statusForRestriction(payload.type, user.role)
      if (user.name === passengerState.passenger.name || user.id === passengerState.passenger.id) {
        passengerState.passenger.status = user.status
      }
      return asyncReturn(record)
    },

    releaseRestriction(id, type) {
      const record = cState.userRestrictions.find(
        (item) => item.userId === id && item.type === type && item.status === 'ACTIVE'
      )
      if (record) {
        record.status = 'RELEASED'
        record.releasedAt = nowText()
      }
      const user = cState.creditUsers.find((item) => item.id === id)
      if (user) restoreUserStatus(user)
      return asyncReturn(true)
    }
  }
}

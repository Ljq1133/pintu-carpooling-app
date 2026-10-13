import { createHttpClient } from './httpClient.js'

const ADMIN_ID = 'admin-001'

const replaceArray = (target, items) => target.splice(0, target.length, ...items)

const upsert = (target, item) => {
  const index = target.findIndex((entry) => entry.id === item.id)
  if (index === -1) target.unshift(item)
  else target.splice(index, 1, item)
  return item
}

const roleLabel = {
  passenger: '乘客',
  driver: '司机',
  admin: '管理员'
}

const defaultSummary = () => ({
  total: 0,
  pendingPayment: 0,
  completed: 0,
  cancelled: 0,
  statusCounts: {},
  paymentStatusCounts: {}
})

function normalizeUserAudit(audit) {
  return {
    ...audit,
    role: '用户',
    type: '实名认证'
  }
}

function normalizeVehicleAudit(audit) {
  return {
    ...audit,
    driver: audit.name || '待审核司机',
    model: audit.name || '车辆资料'
  }
}

function buildQuery(filters = {}) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      params.set(key, String(value).trim())
    }
  })
  const query = params.toString()
  return query ? `?${query}` : ''
}

export function createAdminHttpApi({ passengerState, cState, passengerApi }) {
  const client = createHttpClient({ userId: ADMIN_ID })

  cState.apiError = ''
  cState.adminMe = cState.adminMe || null
  cState.adminOrderSummary = defaultSummary()
  replaceArray(cState.auditUsers, [])
  replaceArray(cState.auditVehicles, [])
  replaceArray(cState.adminOrders, [])
  cState.adminOrderDetails = cState.adminOrderDetails || {}
  replaceArray(cState.creditUsers, [])
  replaceArray(cState.adminAppeals, [])
  replaceArray(cState.adminSafetyOrders, [])
  replaceArray(cState.adminSafetyAlerts, [])
  replaceArray(cState.ruleConfigs, [])
  replaceArray(cState.adminSupportSessions, [])
  replaceArray(cState.dataRetentionPolicies, [])
  cState.adminOpsStatistics = cState.adminOpsStatistics || null
  cState.retentionCleanupResult = cState.retentionCleanupResult || null
  cState.supportMessages = cState.supportMessages || {}
  cState.exportedOrderCsv = ''

  const run = async (operation, fallback = null) => {
    try {
      cState.apiError = ''
      return await operation()
    } catch (error) {
      cState.apiError = error.message
      console.warn('[PinTu Admin API]', error)
      return fallback
    }
  }

  const refreshMe = () =>
    run(async () => {
      cState.adminMe = await client.get('/api/admin/me')
      return cState.adminMe
    }, cState.adminMe)

  const refreshUserAudits = (filters = {}) =>
    run(async () => {
      const items = await client.get(`/api/admin/audits/users${buildQuery({ page: 1, size: 20, ...filters })}`)
      replaceArray(cState.auditUsers, items.map(normalizeUserAudit))
      return cState.auditUsers
    }, cState.auditUsers)

  const refreshVehicleAudits = (filters = {}) =>
    run(async () => {
      const items = await client.get(`/api/admin/audits/vehicles${buildQuery({ page: 1, size: 20, ...filters })}`)
      replaceArray(cState.auditVehicles, items.map(normalizeVehicleAudit))
      return cState.auditVehicles
    }, cState.auditVehicles)

  const refreshOrders = (filters = {}) =>
    run(async () => {
      const items = await client.get(`/api/admin/orders${buildQuery(filters)}`)
      replaceArray(
        cState.adminOrders,
        items.map((order) => ({ ...order, price: Number(order.price || 0) }))
      )
      return cState.adminOrders
    }, cState.adminOrders)

  const refreshOrderSummary = (filters = {}) =>
    run(async () => {
      cState.adminOrderSummary = await client.get(`/api/admin/orders/summary${buildQuery(filters)}`)
      return cState.adminOrderSummary
    }, cState.adminOrderSummary)

  const refreshOpsStatistics = () =>
    run(async () => {
      cState.adminOpsStatistics = await client.get('/api/admin/ops/statistics')
      return cState.adminOpsStatistics
    }, cState.adminOpsStatistics)

  const refreshCreditUsers = () =>
    run(async () => {
      const items = await client.get('/api/admin/users/credit')
      replaceArray(
        cState.creditUsers,
        items.map((user) => ({ ...user, role: roleLabel[user.role] || user.role }))
      )
      return cState.creditUsers
    }, cState.creditUsers)

  const refreshAppeals = (filters = {}) =>
    run(async () => {
      const items = await client.get(`/api/admin/appeals${buildQuery(filters)}`)
      replaceArray(cState.adminAppeals, items)
      return cState.adminAppeals
    }, cState.adminAppeals)

  const refreshSafetyOrders = () =>
    run(async () => {
      const items = await client.get('/api/admin/safety/orders')
      replaceArray(cState.adminSafetyOrders, items)
      return cState.adminSafetyOrders
    }, cState.adminSafetyOrders)

  const refreshSafetyAlerts = () =>
    run(async () => {
      const items = await client.get('/api/admin/safety/alerts')
      replaceArray(cState.adminSafetyAlerts, items)
      return cState.adminSafetyAlerts
    }, cState.adminSafetyAlerts)

  const refreshRules = () =>
    run(async () => {
      const items = await client.get('/api/admin/rules')
      replaceArray(cState.ruleConfigs, items)
      return cState.ruleConfigs
    }, cState.ruleConfigs)

  const refreshSupportSessions = (filters = {}) =>
    run(async () => {
      const items = await client.get(`/api/admin/support/sessions${buildQuery(filters)}`)
      items.forEach((session) => {
        if (Array.isArray(session.messages)) cState.supportMessages[session.id] = session.messages
      })
      replaceArray(cState.adminSupportSessions, items)
      return cState.adminSupportSessions
    }, cState.adminSupportSessions)

  const refreshSupportMessages = (sessionId) =>
    Promise.resolve(cState.supportMessages[sessionId] || [])

  const refreshRetentionPolicies = () =>
    run(async () => {
      const items = await client.get('/api/admin/safety/retention-policies')
      replaceArray(cState.dataRetentionPolicies, items)
      return cState.dataRetentionPolicies
    }, cState.dataRetentionPolicies)

  const initialize = () =>
    Promise.all([
      refreshMe(),
      refreshUserAudits(),
      refreshVehicleAudits(),
      refreshOrders(),
      refreshOrderSummary(),
      refreshCreditUsers(),
      refreshAppeals(),
      refreshSafetyOrders(),
      refreshSafetyAlerts(),
      refreshRules(),
      refreshSupportSessions(),
      refreshRetentionPolicies(),
      refreshOpsStatistics()
    ])

  return {
    initialize,
    refreshMe,
    refreshUserAudits,
    refreshVehicleAudits,
    refreshOrders,
    refreshOrderSummary,
    refreshOpsStatistics,
    refreshAppeals,
    refreshSafetyOrders,
    refreshSafetyAlerts,
    refreshRules,
    refreshSupportSessions,
    refreshSupportMessages,
    refreshRetentionPolicies,

    getMe() {
      return cState.adminMe
    },

    getUserAudits() {
      return cState.auditUsers
    },

    getVehicleAudits() {
      return cState.auditVehicles
    },

    getAuditDetail(id) {
      return run(() => client.get(`/api/admin/audits/${encodeURIComponent(id)}`))
    },

    retryNotification(id) {
      return run(() => client.post(`/api/admin/notifications/${encodeURIComponent(id)}/retry`))
    },

    getOrders() {
      return cState.adminOrders
    },

    getOrderSummary() {
      return cState.adminOrderSummary
    },

    getOrderDetail(id) {
      return run(async () => {
        const detail = await client.get(`/api/admin/orders/${encodeURIComponent(id)}`)
        cState.adminOrderDetails[id] = detail
        return detail
      }, cState.adminOrderDetails[id] || null)
    },

    getOpsStatistics() {
      return cState.adminOpsStatistics
    },

    getCreditUsers() {
      return cState.creditUsers
    },

    getAppeals() {
      return cState.adminAppeals
    },

    getSafetyOrders() {
      return cState.adminSafetyOrders
    },

    getSafetyAlerts() {
      return cState.adminSafetyAlerts
    },

    getSafetyTrajectory(orderId) {
      return run(() => client.get(`/api/admin/safety/orders/${encodeURIComponent(orderId)}/trajectory`), [])
    },

    exportOrdersCsv(filters = {}) {
      return run(async () => {
        const csv = await client.text(`/api/admin/orders/export.csv${buildQuery(filters)}`)
        cState.exportedOrderCsv = csv
        return csv
      }, '')
    },

    getRules() {
      return cState.ruleConfigs
    },

    createRule(payload = {}) {
      return run(async () => {
        const rule = await client.post('/api/admin/rules', payload)
        await refreshRules()
        return rule
      })
    },

    updateRule(ruleId, payload = {}) {
      return run(async () => {
        const rule = await client.put(`/api/admin/rules/${encodeURIComponent(ruleId)}`, payload)
        await refreshRules()
        return rule
      })
    },

    publishRule(ruleId) {
      return run(async () => {
        const rule = await client.post(`/api/admin/rules/${encodeURIComponent(ruleId)}/publish`)
        await refreshRules()
        return rule
      })
    },

    rollbackRule(ruleId, payload = {}) {
      return run(async () => {
        const rule = await client.post(`/api/admin/rules/${encodeURIComponent(ruleId)}/rollback`, payload)
        await refreshRules()
        return rule
      })
    },

    getSupportSessions() {
      return cState.adminSupportSessions
    },

    getSupportMessages(sessionId) {
      return cState.supportMessages[sessionId] || []
    },

    uploadAttachment(file) {
      return run(async () => {
        const formData = new FormData()
        formData.append('file', file)
        return client.upload('/api/support/attachments', formData)
      })
    },

    claimSupportSession(sessionId) {
      return run(async () => {
        const session = await client.post(`/api/admin/support/sessions/${encodeURIComponent(sessionId)}/claim`)
        if (Array.isArray(session.messages)) cState.supportMessages[session.id] = session.messages
        await refreshSupportSessions()
        return session
      })
    },

    replySupportSession(sessionId, payload = {}) {
      return run(async () => {
        const message = await client.post(`/api/admin/support/sessions/${encodeURIComponent(sessionId)}/messages`, {
          messageType: payload.messageType || 'text',
          content: payload.content || '',
          attachmentKey: payload.attachmentKey || '',
          clientMessageId: payload.clientMessageId || `admin-support-${Date.now()}`
        })
        await Promise.all([refreshSupportMessages(sessionId), refreshSupportSessions()])
        return message
      })
    },

    closeSupportSession(sessionId, payload = {}) {
      return run(async () => {
        const session = await client.post(`/api/admin/support/sessions/${encodeURIComponent(sessionId)}/close`, {
          content: payload.content || payload.reason || '处理完成'
        })
        await refreshSupportSessions()
        return session
      })
    },

    getRetentionPolicies() {
      return cState.dataRetentionPolicies
    },

    saveRetentionPolicy(payload = {}) {
      return run(async () => {
        const policy = await client.put('/api/admin/safety/retention-policies', payload)
        await refreshRetentionPolicies()
        return policy
      })
    },

    cleanupRetention(payload = {}) {
      return run(async () => {
        cState.retentionCleanupResult = await client.post('/api/admin/safety/retention-cleanup', {
          dryRun: payload.dryRun !== false
        })
        await refreshRetentionPolicies()
        return cState.retentionCleanupResult
      })
    },

    getRetentionCleanupResult() {
      return cState.retentionCleanupResult
    },

    transitionAppeal(id, payload = {}) {
      return run(async () => {
        const appeal = await client.post(`/api/admin/appeals/${encodeURIComponent(id)}/transition`, {
          status: payload.status,
          comment: payload.comment || '',
          assigneeId: payload.assigneeId || ''
        })
        return upsert(cState.adminAppeals, appeal)
      })
    },

    arbitrateAppeal(id, payload = {}) {
      return run(async () => {
        const record = await client.post(`/api/admin/appeals/${encodeURIComponent(id)}/arbitrations`, {
          actionType: payload.actionType,
          amount: payload.amount,
          creditDelta: payload.creditDelta,
          targetUserId: payload.targetUserId,
          remark: payload.remark || '',
          confirmed: payload.confirmed === true
        })
        await Promise.all([refreshAppeals(), refreshCreditUsers()])
        return record
      })
    },

    reviewUserAudit(id, status, reason = '') {
      return run(async () => {
        const action = status === '已通过' ? 'approve' : 'reject'
        const audit = await client.post(`/api/admin/audits/users/${encodeURIComponent(id)}/${action}`, {
          reason
        })
        await refreshUserAudits()
        return audit
      })
    },

    reviewVehicleAudit(id, status, reason = '') {
      return run(async () => {
        const action = status === '已通过' ? 'approve' : 'reject'
        const audit = await client.post(`/api/admin/audits/vehicles/${encodeURIComponent(id)}/${action}`, {
          reason
        })
        await refreshVehicleAudits()
        return audit
      })
    },

    adjustCredit(id, delta, reason = '') {
      return run(async () => {
        const user = await client.post(`/api/admin/users/${encodeURIComponent(id)}/credit`, {
          delta,
          reason
        })
        await Promise.all([refreshCreditUsers(), id === passengerState.passenger.id ? passengerApi.refreshProfile?.() : null])
        return user
      })
    },

    restrictUser(id, payload = {}) {
      return run(async () => {
        const restriction = await client.post(`/api/admin/users/${encodeURIComponent(id)}/restrictions`, {
          type: payload.type,
          reason: payload.reason || '管理员风控'
        })
        await refreshCreditUsers()
        return restriction
      })
    },

    releaseRestriction(id, type) {
      return run(async () => {
        await client.delete(`/api/admin/users/${encodeURIComponent(id)}/restrictions/${encodeURIComponent(type)}`)
        await refreshCreditUsers()
        return true
      }, false)
    }
  }
}

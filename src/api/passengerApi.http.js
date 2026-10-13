import {
  MATCH_REQUEST_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  REVIEW_STATUS,
  mapBackendOrderStatus
} from './passengerApi.mock.js'
import {
  normalizeConversation,
  normalizeMessage,
  normalizeProposal,
  sortByCreatedAt,
  sortByNewestVersion
} from './negotiationApi.shared.js'
import { createHttpClient } from './httpClient.js'
import { setSession } from '../auth/session.js'

const PASSENGER_ID = 'passenger-001'

const replaceArray = (target, items) => target.splice(0, target.length, ...items)

const defaultDriverIdentity = () => ({
  status: '未申请',
  rejectReason: '',
  submittedAt: '',
  handledAt: ''
})

const upsert = (target, item) => {
  const index = target.findIndex((entry) => entry.id === item.id)
  if (index === -1) target.unshift(item)
  else target.splice(index, 1, item)
  return item
}

const ensureObject = (target, key) => {
  if (!target[key] || Array.isArray(target[key])) target[key] = {}
  return target[key]
}

const normalizeAppeal = (appeal) => ({
  evidences: [],
  transitions: [],
  arbitrations: [],
  ...(appeal || {})
})

function liveState(rawStatus, end) {
  if (rawStatus === '履约中') {
    return {
      phase: 'trip',
      driverLocation: '行程中',
      etaText: '正在前往目的地',
      distanceText: '行程进行中',
      mapText: '车辆正在沿推荐路线行驶'
    }
  }
  if (rawStatus === '待付款') {
    return {
      phase: 'arrived',
      driverLocation: end,
      etaText: '已到达目的地',
      distanceText: '0 公里',
      mapText: '行程已结束，等待乘客付款'
    }
  }
  return {
    phase: 'pickup',
    driverLocation: '出发地附近',
    etaText: '司机正在前往上车点',
    distanceText: '等待司机开始行程',
    mapText: '司机正在前往乘客上车点'
  }
}

function normalizePassengerOrder(vo) {
  const passengerOrderId = vo.passengerOrderId || vo.id
  return {
    id: passengerOrderId,
    passengerId: vo.passengerId,
    start: vo.start,
    end: vo.end,
    departureTime: vo.departureTime,
    passengerCount: vo.passengerCount,
    note: vo.note || '',
    rawStatus: vo.status,
    status: mapBackendOrderStatus(vo.status),
    createdAt: vo.createdAt,
    cancelReason: vo.cancelReason || ''
  }
}

function normalizeOrderRecord(vo) {
  const orderId = vo.orderId || (vo.id !== vo.passengerOrderId ? vo.id : null)
  if (!orderId) return null

  const rawStatus = vo.status
  return {
    id: orderId,
    passengerOrderId: vo.passengerOrderId,
    passengerName: vo.passengerName,
    driverId: vo.driverId,
    driverName: vo.driverName,
    driverPhone: vo.driverPhone,
    vehicle: vo.vehicle,
    route: vo.route || `${vo.start} -> ${vo.end}`,
    start: vo.start,
    end: vo.end,
    departureTime: vo.departureTime,
    passengerCount: vo.passengerCount,
    price: Number(vo.price || 0),
    rawStatus,
    status: mapBackendOrderStatus(rawStatus),
    paymentStatus: vo.paymentStatus || PAYMENT_STATUS.UNPAID,
    reviewStatus: vo.reviewStatus || REVIEW_STATUS.UNREVIEWED,
    canPay: rawStatus === '待付款',
    createdAt: vo.createdAt,
    paidAt: '',
    cancelReason: vo.cancelReason || '',
    review: null,
    live: liveState(rawStatus, vo.end),
    timeline: [
      {
        status: rawStatus,
        time: vo.createdAt,
        description: vo.summary || `订单状态更新为${rawStatus}`
      }
    ]
  }
}

function normalizeTrip(trip) {
  const vehicle = trip.vehicle
    ? `${trip.vehicle.plate || ''}${trip.vehicle.model ? ` · ${trip.vehicle.model}` : ''}`
    : ''
  return {
    ...trip,
    id: trip.driverTripId || trip.id,
    driverTripId: trip.driverTripId || trip.id,
    vehicle,
    route: trip.route || `${trip.start} -> ${trip.end}`,
    scoreLabel: trip.recommendLevel || (trip.overlap >= 80 ? '推荐' : '可选推荐')
  }
}

function normalizeDriverIdentity(identity) {
  return {
    ...defaultDriverIdentity(),
    ...(identity || {}),
    status: identity?.status || '未申请'
  }
}

export function createPassengerHttpApi(state) {
  const client = createHttpClient({ userId: PASSENGER_ID })
  const driverClient = createHttpClient({ userId: 'driver-001' })

  state.apiMode = 'http'
  state.apiError = ''
  replaceArray(state.driverTrips, [])
  replaceArray(state.passengerOrders, [])
  replaceArray(state.matchRequests, [])
  replaceArray(state.orderRecords, [])
  replaceArray(state.creditRecords, [])
  replaceArray(state.passengerEmergencyContacts, [])
  replaceArray(state.appeals, [])
  replaceArray(state.accountBindings, [])
  replaceArray(state.supportSessions, [])
  replaceArray(state.dataRetentionPolicies, [])
  state.accountPreferences = state.accountPreferences || {}
  state.supportMessages = state.supportMessages || {}
  ensureObject(state, 'shareLinks')
  ensureObject(state, 'trajectories')
  ensureObject(state, 'virtualPhones')
  state.driverIdentity = normalizeDriverIdentity(state.driverIdentity)

  const run = async (operation, fallback = null) => {
    try {
      state.apiError = ''
      return await operation()
    } catch (error) {
      state.apiError = error.message
      console.warn('[PinTu API]', error)
      return fallback
    }
  }

  const syncOrderList = (items) => {
    const passengerOrders = items.map(normalizePassengerOrder)
    const orderRecords = items.map(normalizeOrderRecord).filter(Boolean)
    replaceArray(state.passengerOrders, passengerOrders)
    replaceArray(state.orderRecords, orderRecords)
    return passengerOrders
  }

  const refreshMatchRequests = (orders = state.passengerOrders) =>
    run(async () => {
      const groups = await Promise.all(
        orders.map((order) => client.get(`/api/passenger/orders/${encodeURIComponent(order.id)}/requests`))
      )
      const requests = groups.flat()
      const unique = [...new Map(requests.map((request) => [request.id, request])).values()]
      replaceArray(state.matchRequests, unique)
      return state.matchRequests
    }, state.matchRequests)

  const refreshProfile = () =>
    run(async () => {
      const profile = await client.get('/api/account/profile')
      Object.assign(state.passenger, profile)
      return state.passenger
    }, state.passenger)

  const refreshOrders = () =>
    run(async () => {
      const items = await client.get('/api/passenger/orders')
      const orders = syncOrderList(items)
      await refreshMatchRequests(orders)
      return orders
    }, state.passengerOrders)

  const refreshCreditRecords = () =>
    run(async () => {
      const items = await client.get('/api/account/credit-records')
      replaceArray(state.creditRecords, items)
      return state.creditRecords
    }, state.creditRecords)

  const refreshDriverIdentity = async () => {
    try {
      state.apiError = ''
      const identity = await client.get('/api/account/driver-identity')
      state.driverIdentity = normalizeDriverIdentity(identity)
      return state.driverIdentity
    } catch (error) {
      if (error.code === 1404 || error.status === 404) {
        state.driverIdentity = defaultDriverIdentity()
        return state.driverIdentity
      }
      state.apiError = error.message
      console.warn('[PinTu API]', error)
      return state.driverIdentity
    }
  }

  const refreshBlacklist = () =>
    run(async () => {
      const items = await client.get('/api/privacy/blacklist')
      replaceArray(state.passengerBlacklist, items)
      return state.passengerBlacklist
    }, state.passengerBlacklist)

  const refreshEmergencyContacts = () =>
    run(async () => {
      const items = await client.get('/api/safety/emergency-contacts')
      replaceArray(state.passengerEmergencyContacts, items)
      return state.passengerEmergencyContacts
    }, state.passengerEmergencyContacts)

  const refreshAppeals = () =>
    run(async () => {
      const items = await client.get('/api/passenger/appeals')
      replaceArray(state.appeals, items.map(normalizeAppeal))
      return state.appeals
    }, state.appeals)

  const refreshPreferences = () =>
    run(async () => {
      const preferences = await client.get('/api/account/preferences')
      state.accountPreferences = { ...state.accountPreferences, ...preferences }
      return state.accountPreferences
    }, state.accountPreferences)

  const refreshBindings = () =>
    run(async () => {
      const items = await client.get('/api/account/bindings')
      replaceArray(state.accountBindings, items)
      return state.accountBindings
    }, state.accountBindings)

  const refreshSupportSessions = () =>
    run(async () => {
      const items = await client.get('/api/support/sessions')
      replaceArray(state.supportSessions, items)
      return state.supportSessions
    }, state.supportSessions)

  const refreshSupportMessages = (sessionId) =>
    run(async () => {
      if (!sessionId) return []
      const items = await client.get(`/api/support/sessions/${encodeURIComponent(sessionId)}/messages`)
      state.supportMessages[sessionId] = items
      return items
    }, state.supportMessages[sessionId] || [])

  const refreshRetentionPolicies = () =>
    run(async () => {
      const items = await client.get('/api/safety/retention-policies')
      replaceArray(state.dataRetentionPolicies, items)
      return state.dataRetentionPolicies
    }, state.dataRetentionPolicies)

  return {
    async initialize() {
      await Promise.all([
        refreshProfile(),
        refreshOrders(),
        refreshCreditRecords(),
        refreshDriverIdentity(),
        refreshBlacklist(),
        refreshEmergencyContacts(),
        refreshAppeals(),
        refreshPreferences(),
        refreshBindings(),
        refreshSupportSessions(),
        refreshRetentionPolicies()
      ])
    },

    refreshProfile,
    refreshOrders,
    refreshMatchRequests,
    refreshCreditRecords,
    refreshDriverIdentity,
    refreshBlacklist,
    refreshEmergencyContacts,
    refreshAppeals,
    refreshPreferences,
    refreshBindings,
    refreshSupportSessions,
    refreshSupportMessages,
    refreshRetentionPolicies,
    refreshState() {
      return Promise.all([
        refreshProfile(),
        refreshOrders(),
        refreshMatchRequests(),
        refreshCreditRecords(),
        refreshDriverIdentity(),
        refreshBlacklist(),
        refreshEmergencyContacts(),
        refreshAppeals(),
        refreshPreferences(),
        refreshBindings(),
        refreshSupportSessions(),
        refreshRetentionPolicies()
      ])
    },

    getProfile() {
      return Promise.resolve(state.passenger)
    },

    updateProfile(profile) {
      return run(async () => {
        const updated = await client.put('/api/account/profile', {
          name: profile.name,
          gender: profile.gender,
          avatarUrl: profile.avatarUrl
        })
        Object.assign(state.passenger, updated)
        return state.passenger
      }, state.passenger)
    },

    getDriverIdentityStatus() {
      return refreshDriverIdentity()
    },

    applyDriverIdentity(payload = {}) {
      return run(async () => {
        const identity = await client.post('/api/account/driver-identity', {
          realName: payload.realName || state.passenger.name || '拼图用户',
          idCard: payload.idCard || '310101199001011234',
          idCardFrontKey: payload.idCardFrontKey || 'mock/identity/front.jpg',
          idCardBackKey: payload.idCardBackKey || 'mock/identity/back.jpg',
          livenessResult: payload.livenessResult || 'PASS'
        })
        state.driverIdentity = normalizeDriverIdentity(identity)
        return state.driverIdentity
      }, state.driverIdentity)
    },

    getCreditRecords() {
      return refreshCreditRecords()
    },

    getBlacklist() {
      return state.passengerBlacklist
    },

    getEmergencyContacts() {
      return state.passengerEmergencyContacts
    },

    saveEmergencyContact(contact) {
      return run(async () => {
        const payload = {
          name: contact.name,
          phone: contact.phone || '',
          relationship: contact.relationship || '',
          isPrimary: Boolean(contact.isPrimary)
        }
        const saved = contact.id
          ? await client.put(`/api/safety/emergency-contacts/${encodeURIComponent(contact.id)}`, payload)
          : await client.post('/api/safety/emergency-contacts', payload)
        await refreshEmergencyContacts()
        return saved
      })
    },

    deleteEmergencyContact(contactId) {
      return run(async () => {
        await client.delete(`/api/safety/emergency-contacts/${encodeURIComponent(contactId)}`)
        await refreshEmergencyContacts()
        return true
      }, false)
    },

    blockUser(blockedUserId, reason = '用户主动拉黑') {
      return run(async () => {
        const record = await client.post('/api/privacy/blacklist', { blockedUserId, reason })
        await refreshBlacklist()
        return record
      })
    },

    unblockUser(blockedUserId) {
      return run(async () => {
        await client.delete(`/api/privacy/blacklist/${encodeURIComponent(blockedUserId)}`)
        await refreshBlacklist()
        return true
      }, false)
    },

    triggerAlert(orderId, payload = {}) {
      return run(() => client.post(`/api/safety/orders/${encodeURIComponent(orderId)}/alerts`, payload))
    },

    getAppeals() {
      return state.appeals
    },

    getPreferences() {
      return state.accountPreferences
    },

    updatePreferences(payload = {}) {
      return run(async () => {
        const preferences = await client.put('/api/account/preferences', {
          darkMode: Boolean(payload.darkMode),
          fontScale: payload.fontScale || 'standard',
          locale: payload.locale || 'zh-CN',
          protocolVersion: payload.protocolVersion || state.accountPreferences.protocolVersion || '2026-06'
        })
        state.accountPreferences = { ...state.accountPreferences, ...preferences }
        return state.accountPreferences
      }, state.accountPreferences)
    },

    getBindings() {
      return state.accountBindings
    },

    createBinding(payload = {}) {
      return run(async () => {
        const binding = await client.post('/api/account/bindings', {
          bindingType: payload.bindingType || 'PHONE',
          provider: payload.provider || '',
          accountLabel: payload.accountLabel || '',
          accountValue: payload.accountValue || ''
        })
        await refreshBindings()
        return binding
      })
    },

    deleteBinding(bindingId) {
      return run(async () => {
        await client.delete(`/api/account/bindings/${encodeURIComponent(bindingId)}`)
        await refreshBindings()
        return true
      }, false)
    },

    getSupportSessions() {
      return state.supportSessions
    },

    getSupportMessages(sessionId) {
      return state.supportMessages[sessionId] || []
    },

    uploadAttachment(file) {
      return run(async () => {
        const formData = new FormData()
        formData.append('file', file)
        return client.upload('/api/support/attachments', formData)
      })
    },

    createSupportSession(payload = {}) {
      return run(async () => {
        const session = await client.post('/api/support/sessions', {
          subject: payload.subject || '乘客咨询',
          firstMessage: payload.message || payload.firstMessage || '',
          attachmentKey: payload.attachmentKey || '',
          clientMessageId: payload.clientMessageId || `support-${Date.now()}`
        })
        await refreshSupportSessions()
        await refreshSupportMessages(session.id)
        return session
      })
    },

    sendSupportMessage(sessionId, payload = {}) {
      return run(async () => {
        const message = await client.post(`/api/support/sessions/${encodeURIComponent(sessionId)}/messages`, {
          messageType: payload.messageType || 'text',
          content: payload.content || '',
          attachmentKey: payload.attachmentKey || '',
          clientMessageId: payload.clientMessageId || `support-msg-${Date.now()}`
        })
        await refreshSupportMessages(sessionId)
        await refreshSupportSessions()
        return message
      })
    },

    getRetentionPolicies() {
      return state.dataRetentionPolicies
    },

    createAppeal(orderId, payload = {}) {
      return run(async () => {
        const appeal = await client.post(`/api/passenger/orders/${encodeURIComponent(orderId)}/appeals`, {
          reasonType: payload.reasonType || 'SERVICE_DISPUTE',
          description: payload.description || '',
          evidenceKeys: Array.isArray(payload.evidenceKeys) ? payload.evidenceKeys : []
        })
        return upsert(state.appeals, normalizeAppeal(appeal))
      })
    },

    createShareLink(orderId, payload = {}) {
      return run(async () => {
        const link = await client.post(`/api/safety/orders/${encodeURIComponent(orderId)}/share-links`, {
          expiresMinutes: Number(payload.expiresMinutes || 120)
        })
        state.shareLinks[orderId] = link
        return link
      })
    },

    getVirtualPhone(orderId) {
      return run(async () => {
        const binding = await client.get(`/api/safety/orders/${encodeURIComponent(orderId)}/virtual-phone`)
        state.virtualPhones[orderId] = binding
        return binding
      })
    },

    getTrajectory(orderId) {
      return run(async () => {
        const points = await client.get(`/api/safety/orders/${encodeURIComponent(orderId)}/trajectory`)
        state.trajectories[orderId] = points
        return points
      }, [])
    },

    switchToDriver() {
      return run(async () => {
        const session = await client.post('/api/account/role', { role: 'driver' })
        setSession(session)
        return session
      })
    },

    createPassengerOrder(form) {
      return run(async () => {
        const vo = await client.post('/api/passenger/orders', {
          start: form.start,
          end: form.end,
          startLng: form.startLng,
          startLat: form.startLat,
          endLng: form.endLng,
          endLat: form.endLat,
          departureTime: form.departureTime,
          passengerCount: Number(form.passengerCount || 1),
          note: form.note || ''
        })
        return upsert(state.passengerOrders, normalizePassengerOrder(vo))
      })
    },

    getPassengerOrders() {
      return refreshOrders()
    },

    getOrderRequests(passengerOrderId) {
      return run(async () => {
        const items = await client.get(`/api/passenger/orders/${encodeURIComponent(passengerOrderId)}/requests`)
        const unique = [...new Map(items.map((request) => [request.id, request])).values()]
        unique.forEach((request) => {
          const index = state.matchRequests.findIndex((item) => item.id === request.id)
          if (index === -1) state.matchRequests.unshift(request)
          else state.matchRequests.splice(index, 1, request)
        })
        return unique
      }, [])
    },

    getRecommendedTrips(orderOrId) {
      const passengerOrderId =
        typeof orderOrId === 'string' ? orderOrId : orderOrId?.passengerOrderId || orderOrId?.id
      if (!passengerOrderId) return Promise.resolve([])
      return run(async () => {
        const items = await client.get(`/api/passenger/orders/${encodeURIComponent(passengerOrderId)}/matches`)
        return items.map(normalizeTrip)
      }, [])
    },

    sendMatchRequest({ passengerOrderId, driverTripId }) {
      return run(async () => {
        const request = await client.post('/api/match-requests', {
          passengerOrderId,
          driverTripId
        })
        upsert(state.matchRequests, request)
        await refreshOrders()
        return request
      })
    },

    getMatchRequest(matchRequestId) {
      return run(async () => {
        const request = await client.get(`/api/match-requests/${encodeURIComponent(matchRequestId)}`)
        upsert(state.matchRequests, request)
        if (request.status === MATCH_REQUEST_STATUS.ACCEPTED) await refreshOrders()
        return request
      })
    },

    acceptMatchRequest(matchRequestId) {
      return run(async () => {
        const request = await driverClient.post(
          `/api/match-requests/${encodeURIComponent(matchRequestId)}/accept`,
          undefined
        )
        upsert(state.matchRequests, request)
        await refreshOrders()
        return request
      })
    },

    rejectMatchRequest(matchRequestId, reason) {
      return run(async () => {
        const request = await driverClient.post(
          `/api/match-requests/${encodeURIComponent(matchRequestId)}/reject`,
          { reason }
        )
        upsert(state.matchRequests, request)
        await refreshOrders()
        return request
      })
    },

    getConversation(conversationId) {
      return run(async () => normalizeConversation(await client.get(`/api/conversations/${encodeURIComponent(conversationId)}`)), null)
    },

    getConversationMessages(conversationId) {
      return run(async () => {
        const items = await client.get(`/api/conversations/${encodeURIComponent(conversationId)}/messages`)
        return sortByCreatedAt(items).map(normalizeMessage)
      }, [])
    },

    sendMessage(conversationId, request) {
      return run(async () => normalizeMessage(await client.post(
        `/api/conversations/${encodeURIComponent(conversationId)}/messages`,
        {
          messageType: request?.messageType || 'text',
          content: request?.content || '',
          attachmentKey: request?.attachmentKey || '',
          clientMessageId: request?.clientMessageId || ''
        }
      )), null)
    },

    markConversationRead(conversationId) {
      return run(async () => {
        const result = await client.post(`/api/conversations/${encodeURIComponent(conversationId)}/read`, {})
        await refreshOrders()
        return result
      }, null)
    },

    getConversationProposals(conversationId) {
      return run(async () => {
        const items = await client.get(`/api/conversations/${encodeURIComponent(conversationId)}/proposals`)
        return sortByNewestVersion(items).map(normalizeProposal)
      }, [])
    },

    createProposal(conversationId, request) {
      return run(async () => {
        const proposal = await client.post(`/api/conversations/${encodeURIComponent(conversationId)}/proposals`, {
          price: Number(request?.price || 0),
          departureTime: request?.departureTime,
          passengerCount: Number(request?.passengerCount || 1),
          pickupLocation: request?.pickupLocation || '',
          pickupLng: request?.pickupLng ?? null,
          pickupLat: request?.pickupLat ?? null,
          note: request?.note || ''
        })
        await refreshOrders()
        return normalizeProposal(proposal)
      }, null)
    },

    confirmProposal(conversationId, version) {
      return run(async () => {
        const proposal = await client.post(
          `/api/conversations/${encodeURIComponent(conversationId)}/proposals/${encodeURIComponent(version)}/confirm`
        )
        await refreshOrders()
        return normalizeProposal(proposal)
      }, null)
    },

    startTrip(orderId) {
      return run(async () => {
        await driverClient.post(`/api/orders/${encodeURIComponent(orderId)}/start`)
        await refreshOrders()
        return state.orderRecords.find((order) => order.id === orderId) || null
      })
    },

    finishTrip(orderId) {
      return run(async () => {
        await driverClient.post(`/api/orders/${encodeURIComponent(orderId)}/arrive`)
        await refreshOrders()
        return state.orderRecords.find((order) => order.id === orderId) || null
      })
    },

    cancelOrder(orderId, reason) {
      return run(async () => {
        const vo = await client.post(`/api/passenger/orders/${encodeURIComponent(orderId)}/cancel`, { reason })
        await refreshOrders()
        return state.orderRecords.find((order) => order.id === vo.orderId) ||
          state.passengerOrders.find((order) => order.id === vo.passengerOrderId)
      })
    },

    payOrder(orderId) {
      return run(async () => {
        const order = state.orderRecords.find((item) => item.id === orderId)
        if (!order) return null
        await client.post(`/api/orders/${encodeURIComponent(orderId)}/pay`, {
          amount: order.price,
          method: 'mock_balance'
        })
        await refreshOrders()
        return state.orderRecords.find((item) => item.id === orderId) || null
      })
    },

    submitReview(orderId, { rating, comment }) {
      return run(async () => {
        await client.post(`/api/orders/${encodeURIComponent(orderId)}/review`, { rating, comment })
        await refreshOrders()
        const order = state.orderRecords.find((item) => item.id === orderId) || null
        if (order) {
          order.reviewStatus = REVIEW_STATUS.REVIEWED
          order.review = { rating, comment }
        }
        return order
      })
    }
  }
}

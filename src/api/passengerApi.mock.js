import {
  DRIVER_CREDIT_MIN,
  MATCH_REQUEST_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  REVIEW_STATUS,
  cancelPassengerOrder as storeCancelPassengerOrder,
  confirmNegotiationProposal as storeConfirmNegotiationProposal,
  createPassengerGroupState,
  createPassengerOrder as storeCreatePassengerOrder,
  createNegotiationProposal as storeCreateNegotiationProposal,
  getRecommendedTrips as storeGetRecommendedTrips,
  getConversation as storeGetConversation,
  listConversationMessages as storeListConversationMessages,
  listNegotiationProposals as storeListNegotiationProposals,
  markConversationRead as storeMarkConversationRead,
  mockDriverAccept as storeMockDriverAccept,
  mockDriverFinishTrip as storeMockDriverFinishTrip,
  mockDriverReject as storeMockDriverReject,
  mockDriverStartTrip as storeMockDriverStartTrip,
  payOrder as storePayOrder,
  sendConversationMessage as storeSendConversationMessage,
  sendMatchRequest as storeSendMatchRequest,
  submitReview as storeSubmitReview
} from '../mock/localPassengerGroupStore.js'
import {
  normalizeConversation,
  normalizeMessage,
  normalizeProposal,
  sortByCreatedAt,
  sortByNewestVersion
} from './negotiationApi.shared.js'

export {
  DRIVER_CREDIT_MIN,
  MATCH_REQUEST_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  REVIEW_STATUS,
  createPassengerGroupState
}

export const passengerApiNeeds = [
  { method: 'POST', path: '/api/auth/login', desc: '乘客演示账号登录' },
  { method: 'POST', path: '/api/passenger/orders', desc: '发布乘客拼车需求' },
  { method: 'GET', path: '/api/passenger/orders', desc: '获取乘客订单列表' },
  { method: 'GET', path: '/api/passenger/orders/{id}', desc: '获取订单详情' },
  { method: 'GET', path: '/api/passenger/orders/{passengerOrderId}/requests', desc: '查看订单相关拼车请求' },
  { method: 'POST', path: '/api/passenger/orders/{id}/cancel', desc: '乘客取消订单' },
  { method: 'GET', path: '/api/passenger/orders/{passengerOrderId}/matches', desc: '获取推荐司机行程' },
  { method: 'POST', path: '/api/match-requests', desc: '乘客发起拼车请求' },
  { method: 'GET', path: '/api/match-requests/{id}', desc: '查询司机处理结果' },
  { method: 'GET', path: '/api/conversations/{id}', desc: '查看协商会话' },
  { method: 'GET', path: '/api/conversations/{id}/messages', desc: '查看协商聊天记录' },
  { method: 'POST', path: '/api/conversations/{id}/messages', desc: '发送协商消息' },
  { method: 'GET', path: '/api/conversations/{id}/proposals', desc: '查看协商方案版本' },
  { method: 'POST', path: '/api/conversations/{id}/proposals', desc: '提交协商方案' },
  { method: 'POST', path: '/api/conversations/{id}/proposals/{version}/confirm', desc: '确认协商方案' },
  { method: 'POST', path: '/api/orders/{id}/pay', desc: '乘客模拟付款' },
  { method: 'POST', path: '/api/orders/{id}/review', desc: '乘客提交评价' },
  { method: 'GET/POST/PUT/DELETE', path: '/api/safety/emergency-contacts', desc: '维护紧急联系人' },
  { method: 'POST', path: '/api/safety/orders/{orderId}/alerts', desc: '履约中触发课程 Mock 报警' }
]

export const passengerStatusContract = [
  { label: '订单展示状态', value: '待匹配 / 协商中 / 已确认 / 履约中 / 待付款 / 已完成 / 已取消 / 已归档' },
  { label: '匹配请求状态', value: '待处理 / 已同意 / 已拒绝' },
  { label: '支付状态', value: '未支付 / 已支付' },
  { label: '评价状态', value: '未评价 / 已评价' },
  { label: '协商会话', value: '消息 / 方案 / 双向确认后生成正式订单' }
]

export const passengerIntegrationChecklist = [
  'A 组返回 passengerOrderId、driverTripId、matchRequestId、orderId',
  'C 组司机同意后进入协商中，返回 conversationId 并可查看消息/方案',
  'C 组双方确认同一版本方案后生成正式 orderId',
  'C 组司机拒绝后返回 rejectReason，乘客端保留重新选择司机入口',
  '司机送达后订单进入待付款，只开放乘客付款入口',
  '付款后订单进入已完成，评价只更新评价状态'
]

const backendStatusMap = {
  待匹配: ORDER_STATUS.MATCHING,
  协商中: ORDER_STATUS.NEGOTIATING,
  已确认: ORDER_STATUS.CONFIRMED,
  履约中: ORDER_STATUS.IN_PROGRESS,
  待付款: ORDER_STATUS.PENDING_PAYMENT,
  进行中: ORDER_STATUS.IN_PROGRESS,
  已完成: ORDER_STATUS.COMPLETED,
  已取消: ORDER_STATUS.CANCELLED,
  已归档: ORDER_STATUS.ARCHIVED
}

const asyncReturn = (value) => Promise.resolve(value)

const maskName = (value) => value ? `${String(value).slice(0, 1)}**` : ''
const maskPhone = (value) => `${String(value).slice(0, 3)}****${String(value).slice(-4)}`
const maskPlate = (value) => {
  const text = String(value || '')
  return text.length < 3 ? text : `${text.slice(0, 2)}***${text.slice(-1)}`
}
const maskVehicle = (value) => {
  const text = String(value || '')
  const [plate, ...rest] = text.split(/\s*路\s*/)
  return [maskPlate(plate), ...rest].filter(Boolean).join(' 路 ')
}
const nowText = () => new Date().toISOString().slice(0, 16).replace('T', ' ')

const findUserSummary = (state, blockedUserId) => {
  const trip = state.driverTrips.find((item) => item.driverId === blockedUserId)
  if (trip) return { blockedName: maskName(trip.driverName), role: 'driver' }
  if (state.passenger.id === blockedUserId) return { blockedName: maskName(state.passenger.name), role: 'passenger' }
  return { blockedName: '用**', role: 'passenger' }
}

export function mapBackendOrderStatus(status) {
  return backendStatusMap[status] || status
}

const normalizeStatusRecord = (record) => {
  if (!record || typeof record !== 'object') return record
  return {
    ...record,
    status: mapBackendOrderStatus(record.status)
  }
}

const normalizeTrip = (trip) => {
  if (!trip) return trip
  return {
    ...trip,
    driverName: maskName(trip.driverName),
    vehicle: typeof trip.vehicle === 'string' ? maskVehicle(trip.vehicle) : trip.vehicle,
    driverTripId: trip.driverTripId || trip.id,
    route: trip.route || `${trip.start} -> ${trip.end}`
  }
}

const findPassengerOrder = (state, passengerOrderId) =>
  state.passengerOrders.find((order) => order.id === passengerOrderId)

const normalizeOrderInput = (state, orderOrId) => {
  if (typeof orderOrId === 'string') return findPassengerOrder(state, orderOrId)
  return orderOrId
}

const findOrderRecord = (state, orderId) =>
  state.orderRecords.find((order) => order.id === orderId || order.passengerOrderId === orderId)

const splitEvidenceKeys = (value) => {
  if (Array.isArray(value)) return value.map((item) => String(item || '').trim()).filter(Boolean)
  return String(value || '')
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const makeVirtualNumber = (orderId) =>
  `95013${String(Math.abs(String(orderId).split('').reduce((sum, char) => sum + char.charCodeAt(0), 0))).padStart(8, '0').slice(-8)}`

export function createPassengerApi(state) {
  const refreshProfile = () => asyncReturn(state.passenger)
  const refreshOrders = () => asyncReturn(state.passengerOrders.map(normalizeStatusRecord))
  const refreshMatchRequests = () => asyncReturn(state.matchRequests.map((request) => ({ ...request })))
  const refreshState = () => Promise.all([refreshProfile(), refreshOrders(), refreshMatchRequests()])

  const normalizeRequest = (request) => (request ? { ...request } : request)

  return {
    initialize: refreshState,
    refreshProfile,
    refreshOrders,
    refreshMatchRequests,
    refreshState,

    getProfile() {
      return asyncReturn(state.passenger)
    },

    updateProfile(profile) {
      state.passenger = {
        ...state.passenger,
        name: String(profile.name || state.passenger.name || '').trim(),
        gender: profile.gender || state.passenger.gender || '',
        avatarUrl: profile.avatarUrl || state.passenger.avatarUrl || ''
      }
      return asyncReturn(state.passenger)
    },

    getDriverIdentityStatus() {
      return asyncReturn(state.driverIdentity)
    },

    applyDriverIdentity() {
      state.driverIdentity = {
        id: `identity-${Date.now()}`,
        status: '待审核',
        rejectReason: '',
        submittedAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
        handledAt: ''
      }
      return asyncReturn(state.driverIdentity)
    },

    getCreditRecords() {
      return asyncReturn(state.creditRecords)
    },

    switchToDriver() {
      return asyncReturn(null)
    },

    createPassengerOrder(form) {
      if (Number(state.passenger.credit || 0) < 60 || state.passenger.status === '限制发单' || state.passenger.status === '已封禁') {
        return asyncReturn(null)
      }
      const order = storeCreatePassengerOrder(state, form)
      return asyncReturn(normalizeStatusRecord(order))
    },

    getPassengerOrders() {
      return refreshOrders()
    },

    getOrderRequests(passengerOrderId) {
      return asyncReturn(
        state.matchRequests
          .filter((request) => request.passengerOrderId === passengerOrderId)
          .map(normalizeRequest)
      )
    },

    getRecommendedTrips(orderOrId) {
      const order = normalizeOrderInput(state, orderOrId)
      return asyncReturn(storeGetRecommendedTrips(order, state.driverTrips, state.blacklistRecords).map(normalizeTrip))
    },

    getBlacklist() {
      return state.blacklistRecords.filter((record) => record.ownerId === state.passenger.id)
    },

    getEmergencyContacts() {
      return state.passengerEmergencyContacts
    },

    saveEmergencyContact(contact) {
      const existing = state.passengerEmergencyContacts.find((item) => item.id === contact.id)
      if (contact.isPrimary) {
        state.passengerEmergencyContacts.forEach((item) => {
          item.isPrimary = false
        })
      }
      const saved = {
        ...existing,
        id: existing?.id || `contact-passenger-${Date.now()}`,
        name: String(contact.name || '').trim(),
        phoneMasked: contact.phone ? maskPhone(contact.phone) : existing?.phoneMasked,
        relationship: String(contact.relationship || '').trim(),
        isPrimary: Boolean(contact.isPrimary),
        createdAt: existing?.createdAt || nowText(),
        updatedAt: nowText()
      }
      if (existing) Object.assign(existing, saved)
      else state.passengerEmergencyContacts.unshift(saved)
      return asyncReturn(saved)
    },

    deleteEmergencyContact(contactId) {
      const index = state.passengerEmergencyContacts.findIndex((item) => item.id === contactId)
      if (index !== -1) state.passengerEmergencyContacts.splice(index, 1)
      return asyncReturn(true)
    },

    blockUser(blockedUserId, reason = '用户主动拉黑') {
      if (!blockedUserId || blockedUserId === state.passenger.id) return asyncReturn(null)
      const existing = state.blacklistRecords.find(
        (record) => record.ownerId === state.passenger.id && record.blockedUserId === blockedUserId
      )
      if (existing) return asyncReturn(existing)
      const record = {
        ownerId: state.passenger.id,
        blockedUserId,
        ...findUserSummary(state, blockedUserId),
        reason,
        createdAt: new Date().toISOString().slice(0, 16).replace('T', ' ')
      }
      state.blacklistRecords.unshift(record)
      return asyncReturn(record)
    },

    unblockUser(blockedUserId) {
      const index = state.blacklistRecords.findIndex(
        (record) => record.ownerId === state.passenger.id && record.blockedUserId === blockedUserId
      )
      if (index !== -1) state.blacklistRecords.splice(index, 1)
      return asyncReturn(true)
    },

    triggerAlert(orderId, payload = {}) {
      const order = state.orderRecords.find((item) => item.id === orderId)
      if (!order || order.live?.phase !== 'trip') return asyncReturn(null)
      const alert = {
        id: `alert-passenger-${Date.now()}`,
        orderId,
        alertType: payload.alertType || '一键报警',
        longitude: payload.longitude ?? null,
        latitude: payload.latitude ?? null,
        details: payload.details || '',
        status: '已上报',
        createdAt: nowText(),
        policeReportId: `police-${Date.now()}`,
        policeMessage: '课程 Mock 报警已上报，不会真实联系警方'
      }
      state.safetyAlerts.unshift(alert)
      return asyncReturn(alert)
    },

    getAppeals() {
      return state.appeals
    },

    getPreferences() {
      return state.accountPreferences
    },

    refreshPreferences() {
      return asyncReturn(state.accountPreferences)
    },

    updatePreferences(payload = {}) {
      state.accountPreferences = {
        ...state.accountPreferences,
        darkMode: Boolean(payload.darkMode),
        fontScale: payload.fontScale || state.accountPreferences.fontScale || 'standard',
        locale: payload.locale || state.accountPreferences.locale || 'zh-CN',
        protocolVersion: state.accountPreferences.protocolVersion || '2026-06'
      }
      return asyncReturn(state.accountPreferences)
    },

    getBindings() {
      return state.accountBindings
    },

    refreshBindings() {
      return asyncReturn(state.accountBindings)
    },

    createBinding(payload = {}) {
      const value = String(payload.accountValue || '').trim()
      if (!value) return asyncReturn(null)
      const type = payload.bindingType || 'PHONE'
      const binding = {
        id: `binding-${Date.now()}`,
        bindingType: type,
        provider: payload.provider || '',
        accountLabel: payload.accountLabel || type,
        maskedValue: type === 'PHONE' ? maskPhone(value) : `${value.slice(0, 2)}***${value.slice(-2)}`,
        status: 'ACTIVE',
        createdAt: nowText(),
        updatedAt: nowText()
      }
      state.accountBindings.unshift(binding)
      return asyncReturn(binding)
    },

    deleteBinding(bindingId) {
      const binding = state.accountBindings.find((item) => item.id === bindingId)
      if (binding) {
        binding.status = 'UNBOUND'
        binding.updatedAt = nowText()
      }
      return asyncReturn(true)
    },

    getSupportSessions() {
      return state.supportSessions
    },

    refreshSupportSessions() {
      return asyncReturn(state.supportSessions)
    },

    getSupportMessages(sessionId) {
      return state.supportMessages[sessionId] || []
    },

    refreshSupportMessages(sessionId) {
      return asyncReturn(state.supportMessages[sessionId] || [])
    },

    uploadAttachment(file = {}) {
      const fileName = file.name || `support-${Date.now()}.jpg`
      return asyncReturn({
        attachmentKey: `/api/files/support/${state.passenger.id}/${fileName}`,
        fileName,
        contentType: file.type || 'image/jpeg',
        size: file.size || 0
      })
    },

    createSupportSession(payload = {}) {
      const now = nowText()
      const session = {
        id: `support-${Date.now()}`,
        userId: state.passenger.id,
        userName: maskName(state.passenger.name),
        subject: String(payload.subject || '乘客咨询').trim(),
        status: 'OPEN',
        assigneeId: '',
        lastMessageAt: now,
        createdAt: now,
        updatedAt: now,
        closedAt: ''
      }
      const firstMessage = {
        id: `support-msg-${Date.now()}`,
        sessionId: session.id,
        senderId: state.passenger.id,
        senderRole: 'PASSENGER',
        messageType: payload.attachmentKey ? 'image' : 'text',
        content: payload.message || '',
        attachmentKey: payload.attachmentKey || '',
        createdAt: now
      }
      state.supportSessions.unshift(session)
      state.supportMessages[session.id] = [firstMessage]
      return asyncReturn(session)
    },

    sendSupportMessage(sessionId, payload = {}) {
      const session = state.supportSessions.find((item) => item.id === sessionId)
      if (!session || session.status === 'CLOSED') return asyncReturn(null)
      const message = {
        id: `support-msg-${Date.now()}`,
        sessionId,
        senderId: state.passenger.id,
        senderRole: 'PASSENGER',
        messageType: payload.messageType || (payload.attachmentKey ? 'image' : 'text'),
        content: payload.content || '',
        attachmentKey: payload.attachmentKey || '',
        createdAt: nowText()
      }
      state.supportMessages[sessionId] = [...(state.supportMessages[sessionId] || []), message]
      session.updatedAt = message.createdAt
      session.lastMessageAt = message.createdAt
      return asyncReturn(message)
    },

    getRetentionPolicies() {
      return state.dataRetentionPolicies
    },

    refreshRetentionPolicies() {
      return asyncReturn(state.dataRetentionPolicies)
    },

    refreshAppeals() {
      return asyncReturn(state.appeals)
    },

    createAppeal(orderId, payload = {}) {
      const order = findOrderRecord(state, orderId)
      if (!order || order.passengerId !== state.passenger.id) return asyncReturn(null)
      const evidenceKeys = splitEvidenceKeys(payload.evidenceKeys)
      const now = nowText()
      const appeal = {
        id: `appeal-${Date.now()}`,
        orderId: order.id,
        applicantId: state.passenger.id,
        assigneeId: '',
        reasonType: payload.reasonType || 'SERVICE_DISPUTE',
        description: payload.description || '',
        status: 'OPEN',
        resolution: '',
        route: order.route,
        applicantName: state.passenger.name,
        createdAt: now,
        updatedAt: now,
        closedAt: '',
        evidences: evidenceKeys.map((key, index) => ({
          id: `evidence-${Date.now()}-${index}`,
          appealId: '',
          evidenceType: 'URL',
          storageKey: key,
          displayUrl: key,
          uploadedAt: now
        })),
        transitions: [
          {
            id: `transition-${Date.now()}`,
            appealId: '',
            operatorId: state.passenger.id,
            fromStatus: '',
            toStatus: 'OPEN',
            comment: '乘客提交申诉',
            createdAt: now
          }
        ],
        arbitrations: []
      }
      state.appeals.unshift(appeal)
      return asyncReturn(appeal)
    },

    createShareLink(orderId, payload = {}) {
      const order = findOrderRecord(state, orderId)
      if (!order || order.passengerId !== state.passenger.id) return asyncReturn(null)
      const expiresMinutes = Number(payload.expiresMinutes || 120)
      const expiresAt = new Date(Date.now() + expiresMinutes * 60 * 1000).toISOString().slice(0, 16).replace('T', ' ')
      const token = `share-${order.id}-${Date.now()}`
      const link = {
        token,
        publicPath: `/share/${token}`,
        orderId: order.id,
        expiresAt
      }
      state.shareLinks[order.id] = link
      return asyncReturn(link)
    },

    getVirtualPhone(orderId) {
      const order = findOrderRecord(state, orderId)
      if (!order || !order.driverId || order.status !== ORDER_STATUS.IN_PROGRESS || order.live?.phase === 'arrived') {
        return asyncReturn(null)
      }
      if (!state.virtualPhones[order.id]) {
        const virtualNumber = order.virtualPhone || makeVirtualNumber(order.id)
        order.virtualPhone = virtualNumber
        state.virtualPhones[order.id] = {
          id: `vphone-${order.id}`,
          orderId: order.id,
          virtualNumber,
          status: 'ACTIVE',
          boundAt: nowText(),
          unboundAt: ''
        }
      }
      return asyncReturn(state.virtualPhones[order.id])
    },

    getTrajectory(orderId) {
      const order = findOrderRecord(state, orderId)
      if (!order || order.passengerId !== state.passenger.id) return asyncReturn([])
      state.trajectories[order.id] = state.trajectories[order.id] || [
        {
          id: `tp-${order.id}-1`,
          orderId: order.id,
          longitude: 121.479,
          latitude: 31.226,
          sequenceNo: 1,
          reportedAt: nowText()
        }
      ]
      return asyncReturn(state.trajectories[order.id])
    },

    sendMatchRequest({ passengerOrderId, driverTripId }) {
      const request = storeSendMatchRequest(state, passengerOrderId, driverTripId)
      return asyncReturn(normalizeRequest(request))
    },

    getMatchRequest(matchRequestId) {
      const request = state.matchRequests.find((item) => item.id === matchRequestId) || null
      return asyncReturn(normalizeRequest(request))
    },

    acceptMatchRequest(matchRequestId) {
      const request = storeMockDriverAccept(state, matchRequestId)
      return asyncReturn(normalizeRequest(request))
    },

    rejectMatchRequest(matchRequestId, reason) {
      const request = storeMockDriverReject(state, matchRequestId, reason)
      return asyncReturn(normalizeRequest(request))
    },

    getConversation(conversationId) {
      return asyncReturn(normalizeConversation(storeGetConversation(state, conversationId, 'passenger-001')))
    },

    getConversationMessages(conversationId) {
      return asyncReturn(
        sortByCreatedAt(storeListConversationMessages(state, conversationId, 'passenger-001')).map(normalizeMessage)
      )
    },

    sendMessage(conversationId, request) {
      return asyncReturn(normalizeMessage(storeSendConversationMessage(state, conversationId, 'passenger-001', request)))
    },

    markConversationRead(conversationId) {
      return asyncReturn(normalizeConversation(storeMarkConversationRead(state, conversationId, 'passenger-001')))
    },

    getConversationProposals(conversationId) {
      return asyncReturn(
        sortByNewestVersion(storeListNegotiationProposals(state, conversationId, 'passenger-001')).map(normalizeProposal)
      )
    },

    createProposal(conversationId, request) {
      return asyncReturn(normalizeProposal(storeCreateNegotiationProposal(state, conversationId, 'passenger-001', request)))
    },

    confirmProposal(conversationId, version) {
      return asyncReturn(normalizeProposal(storeConfirmNegotiationProposal(state, conversationId, 'passenger-001', version)))
    },

    startTrip(orderId) {
      const orderRecord = storeMockDriverStartTrip(state, orderId)
      return asyncReturn(normalizeStatusRecord(orderRecord))
    },

    finishTrip(orderId) {
      const orderRecord = storeMockDriverFinishTrip(state, orderId)
      return asyncReturn(normalizeStatusRecord(orderRecord))
    },

    cancelOrder(orderId, reason) {
      const order = storeCancelPassengerOrder(state, orderId, reason)
      return asyncReturn(normalizeStatusRecord(order))
    },

    payOrder(orderId) {
      const orderRecord = storePayOrder(state, orderId)
      return asyncReturn(normalizeStatusRecord(orderRecord))
    },

    submitReview(orderId, { rating, comment }) {
      const orderRecord = storeSubmitReview(state, orderId, rating, comment)
      return asyncReturn(normalizeStatusRecord(orderRecord))
    }
  }
}

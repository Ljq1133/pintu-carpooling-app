import { demoDriverTrips, demoLocation, demoPassenger, passengerTabs } from './PassengerGroupData.js'

export const DRIVER_CREDIT_MIN = 80

export const ORDER_STATUS = {
  MATCHING: '待匹配',
  NEGOTIATING: '协商中',
  CONFIRMED: '已确认',
  IN_PROGRESS: '履约中',
  PENDING_PAYMENT: '待付款',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  ARCHIVED: '已归档'
}

export const MATCH_REQUEST_STATUS = {
  PENDING: '待处理',
  ACCEPTED: '已同意',
  REJECTED: '已拒绝',
  EXPIRED: '已失效',
  WITHDRAWN: '已撤回'
}

export const PAYMENT_STATUS = {
  UNPAID: '未支付',
  PAID: '已支付'
}

export const REVIEW_STATUS = {
  UNREVIEWED: '未评价',
  REVIEWED: '已评价'
}

const clone = (value) => JSON.parse(JSON.stringify(value))

const pad = (value) => String(value).padStart(2, '0')

const formatNow = () => {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(
    now.getMinutes()
  )}`
}

const normalizeText = (value) => String(value || '').trim()

const makeId = (prefix, list) => `${prefix}-${Date.now()}-${list.length + 1}`
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

const sameOrMockNearby = (input, target) => {
  const source = normalizeText(input)
  const targetText = normalizeText(target)
  if (!source || !targetText) return false
  return source.includes(targetText) || targetText.includes(source)
}

const createTimelineItem = (status, description) => ({
  status,
  time: formatNow(),
  description
})

const addTimeline = (orderRecord, status, description) => {
  orderRecord.timeline.push(createTimelineItem(status, description))
}

const findPassengerOrder = (state, passengerOrderId) =>
  state.passengerOrders.find((order) => order.id === passengerOrderId)

const findTrip = (state, driverTripId) => state.driverTrips.find((trip) => trip.id === driverTripId)

const findOrderRecord = (state, id) =>
  state.orderRecords.find((order) => order.id === id || order.passengerOrderId === id)

const findConversation = (state, conversationId) =>
  state.conversations.find((conversation) => conversation.id === conversationId)

const findConversationByMatchRequest = (state, matchRequestId) =>
  state.conversations.find((conversation) => conversation.matchRequestId === matchRequestId)

const findLatestProposal = (state, conversationId) =>
  state.negotiationProposals
    .filter((proposal) => proposal.conversationId === conversationId)
    .sort((left, right) => Number(right.version || 0) - Number(left.version || 0))[0]

const actorRole = (actorId) => (actorId === 'driver-001' ? 'DRIVER' : 'PASSENGER')

const participantRole = (conversation, actorId) => {
  if (!conversation) return ''
  if (actorId === conversation.passengerId) return 'PASSENGER'
  if (actorId === conversation.driverId) return 'DRIVER'
  return ''
}

const createMessage = (state, conversationId, senderId, messageType, content, attachmentKey = '', clientMessageId = '') => ({
  id: makeId('msg', state.chatMessages),
  conversationId,
  senderId,
  messageType,
  content,
  attachmentKey,
  clientMessageId,
  readAt: '',
  createdAt: formatNow()
})

const createConversation = (state, request, passengerOrder, trip) => {
  const conversation = findConversationByMatchRequest(state, request.id)
  if (conversation) return conversation

  const createdConversation = {
    id: makeId('conversation', state.conversations),
    orderId: '',
    matchRequestId: request.id,
    passengerId: passengerOrder.passengerId,
    driverId: trip.driverId,
    status: ORDER_STATUS.NEGOTIATING,
    createdAt: formatNow(),
    closedAt: ''
  }
  state.conversations.unshift(createdConversation)
  state.chatMessages.push(
    createMessage(
      state,
      createdConversation.id,
      'system',
      'text',
      '司机已同意请求，双方进入协商中'
    )
  )
  return createdConversation
}

const updatePassengerOrderStatus = (state, passengerOrderId, status) => {
  const passengerOrder = findPassengerOrder(state, passengerOrderId)
  if (passengerOrder) passengerOrder.status = status
}

export function createPassengerGroupState() {
  return {
    tabs: clone(passengerTabs),
    passenger: clone(demoPassenger),
    currentLocation: clone(demoLocation),
    driverTrips: clone(demoDriverTrips),
    passengerOrders: [],
    matchRequests: [],
    orderRecords: [],
    conversations: [],
    chatMessages: [],
    negotiationProposals: [],
    blacklistRecords: [],
    passengerBlacklist: [],
    passengerEmergencyContacts: [],
    safetyAlerts: [],
    appeals: [],
    accountPreferences: {
      darkMode: false,
      fontScale: 'standard',
      locale: 'zh-CN',
      protocolVersion: '2026-06'
    },
    accountBindings: [
      {
        id: 'binding-phone-001',
        bindingType: 'PHONE',
        provider: 'mobile',
        accountLabel: '手机号',
        maskedValue: '138****5678',
        status: 'ACTIVE',
        createdAt: formatNow(),
        updatedAt: formatNow()
      }
    ],
    supportSessions: [],
    supportMessages: {},
    dataRetentionPolicies: [
      {
        id: 'ret-location',
        dataType: 'LOCATION',
        retentionDays: 30,
        deleteMode: 'ARCHIVE_THEN_DELETE',
        description: 'Precise trip locations are retained briefly after trip completion.',
        updatedAt: formatNow()
      },
      {
        id: 'ret-chat',
        dataType: 'CHAT',
        retentionDays: 180,
        deleteMode: 'ARCHIVE_THEN_DELETE',
        description: 'Negotiation and support messages are retained for dispute handling.',
        updatedAt: formatNow()
      },
      {
        id: 'ret-attachment',
        dataType: 'ATTACHMENT',
        retentionDays: 180,
        deleteMode: 'DELETE_OBJECT',
        description: 'Evidence and support attachments follow dispute retention.',
        updatedAt: formatNow()
      },
      {
        id: 'ret-audit',
        dataType: 'AUDIT',
        retentionDays: 365,
        deleteMode: 'KEEP_AUDIT',
        description: 'Security and admin audit events are retained for traceability.',
        updatedAt: formatNow()
      }
    ],
    locationReportQueue: [],
    shareLinks: {},
    trajectories: {},
    virtualPhones: {},
    driverIdentity: {
      status: '未申请',
      rejectReason: '',
      submittedAt: '',
      handledAt: ''
    },
    creditRecords: [
      {
        id: 'credit-001',
        delta: 3,
        reason: '完成拼车行程',
        operatorId: 'system',
        createdAt: formatNow()
      }
    ]
  }
}

export function createPassengerOrder(state, form) {
  const order = {
    id: makeId('po', state.passengerOrders),
    passengerId: state.passenger.id,
    start: normalizeText(form.start),
    end: normalizeText(form.end),
    departureTime: normalizeText(form.departureTime),
    passengerCount: Number(form.passengerCount || 1),
    note: normalizeText(form.note),
    status: ORDER_STATUS.MATCHING,
    createdAt: formatNow()
  }

  state.passengerOrders.unshift(order)
  return order
}

export function getRecommendedTrips(order, driverTrips, blacklistRecords = []) {
  if (!order) return []

  return driverTrips
    .filter((trip) => !blacklistRecords.some((record) =>
      (record.ownerId === order.passengerId && record.blockedUserId === trip.driverId) ||
      (record.ownerId === trip.driverId && record.blockedUserId === order.passengerId)
    ))
    .filter((trip) => Number(trip.availableSeats || 0) > 0)
    .filter((trip) => Number(trip.driverCredit || 0) >= DRIVER_CREDIT_MIN)
    .filter((trip) => Number(trip.overlap || 0) >= 60)
    .filter((trip) => sameOrMockNearby(order.start, trip.start) && sameOrMockNearby(order.end, trip.end))
    .map((trip) => ({
      ...trip,
      driverName: maskName(trip.driverName),
      vehicle: typeof trip.vehicle === 'string' ? maskVehicle(trip.vehicle) : trip.vehicle,
      route: `${trip.start} -> ${trip.end}`,
      scoreLabel: trip.overlap >= 80 ? '推荐' : '可选推荐'
    }))
    .sort((left, right) => right.overlap - left.overlap)
}

export function sendMatchRequest(state, passengerOrderId, driverTripId) {
  const existingRequest = state.matchRequests.find(
    (request) =>
      request.passengerOrderId === passengerOrderId &&
      request.driverTripId === driverTripId &&
      request.status !== MATCH_REQUEST_STATUS.REJECTED
  )
  if (existingRequest) return existingRequest

  const order = findPassengerOrder(state, passengerOrderId)
  const trip = findTrip(state, driverTripId)
  if (!order || !trip || order.status === ORDER_STATUS.CANCELLED) return null

  const request = {
    id: makeId('req', state.matchRequests),
    passengerOrderId,
    driverTripId,
    passengerId: state.passenger.id,
    driverId: trip.driverId,
    driverName: maskName(trip.driverName),
    route: `${order.start} -> ${order.end}`,
    status: MATCH_REQUEST_STATUS.PENDING,
    message: order.note || '我想拼这趟车',
    createdAt: formatNow(),
    handledAt: '',
    orderId: '',
    conversationId: ''
  }

  state.matchRequests.unshift(request)
  order.status = ORDER_STATUS.NEGOTIATING
  return request
}

export function mockDriverAccept(state, requestId) {
  const request = state.matchRequests.find((item) => item.id === requestId)
  if (!request || request.status !== MATCH_REQUEST_STATUS.PENDING) return null

  const passengerOrder = findPassengerOrder(state, request.passengerOrderId)
  const trip = findTrip(state, request.driverTripId)
  if (!passengerOrder || !trip || passengerOrder.status === ORDER_STATUS.CANCELLED) return null

  const existingOrder = findOrderRecord(state, passengerOrder.id)
  request.status = MATCH_REQUEST_STATUS.ACCEPTED
  request.handledAt = formatNow()
  passengerOrder.status = ORDER_STATUS.NEGOTIATING

  const conversation = createConversation(state, request, passengerOrder, trip)
  request.conversationId = conversation.id

  if (existingOrder) {
    request.orderId = existingOrder.id
    conversation.orderId = existingOrder.id
    conversation.status = ORDER_STATUS.CONFIRMED
    conversation.closedAt = formatNow()
    return request
  }

  state.matchRequests.forEach((item) => {
    if (item.passengerOrderId === passengerOrder.id && item.id !== request.id && item.status === MATCH_REQUEST_STATUS.PENDING) {
      item.status = MATCH_REQUEST_STATUS.REJECTED
      item.handledAt = formatNow()
    }
  })

  return request
}

export function mockDriverReject(state, requestId, reason = '司机暂时无法接单') {
  const request = state.matchRequests.find((item) => item.id === requestId)
  if (!request || request.status !== MATCH_REQUEST_STATUS.PENDING) return null

  request.status = MATCH_REQUEST_STATUS.REJECTED
  request.handledAt = formatNow()
  request.orderId = ''
  request.rejectReason = reason

  const passengerOrder = findPassengerOrder(state, request.passengerOrderId)
  const hasPendingRequests = state.matchRequests.some(
    (item) => item.passengerOrderId === request.passengerOrderId && item.status === MATCH_REQUEST_STATUS.PENDING
  )
  if (passengerOrder && passengerOrder.status === ORDER_STATUS.NEGOTIATING && !hasPendingRequests) {
    passengerOrder.status = ORDER_STATUS.MATCHING
  }
  return request
}

export function getConversation(state, conversationId, actorId) {
  const conversation = findConversation(state, conversationId)
  if (!conversation) return null
  if (actorId && actorId !== conversation.passengerId && actorId !== conversation.driverId) return null
  return {
    id: conversation.id,
    orderId: conversation.orderId,
    matchRequestId: conversation.matchRequestId,
    passengerId: conversation.passengerId,
    driverId: conversation.driverId,
    status: conversation.status,
    unreadCount: state.chatMessages.filter(
      (message) =>
        message.conversationId === conversationId &&
        message.senderId !== actorId &&
        !message.readAt
    ).length,
    createdAt: conversation.createdAt,
    closedAt: conversation.closedAt
  }
}

export function listConversationMessages(state, conversationId, actorId) {
  const conversation = findConversation(state, conversationId)
  if (!conversation) return []
  if (actorId && actorId !== conversation.passengerId && actorId !== conversation.driverId) return []
  return state.chatMessages
    .filter((message) => message.conversationId === conversationId)
    .sort((left, right) => left.createdAt.localeCompare(right.createdAt))
    .map((message) => ({ ...message }))
}

export function sendConversationMessage(state, conversationId, actorId, request) {
  const conversation = findConversation(state, conversationId)
  if (!conversation || conversation.status !== ORDER_STATUS.NEGOTIATING) return null
  if (actorId !== conversation.passengerId && actorId !== conversation.driverId) return null

  const type = String(request?.messageType || 'text').toLowerCase()
  if (!['text', 'image'].includes(type)) return null
  if (type === 'text' && !normalizeText(request?.content)) return null
  if (type === 'image' && !normalizeText(request?.attachmentKey)) return null

  if (request?.clientMessageId) {
    const existing = state.chatMessages.find(
      (message) =>
        message.conversationId === conversationId &&
        message.clientMessageId === request.clientMessageId &&
        message.senderId === actorId
    )
    if (existing) return { ...existing }
  }

  const message = createMessage(
    state,
    conversationId,
    actorId,
    type,
    type === 'text' ? normalizeText(request?.content) : '',
    normalizeText(request?.attachmentKey),
    normalizeText(request?.clientMessageId)
  )
  state.chatMessages.push(message)
  return { ...message }
}

export function markConversationRead(state, conversationId, actorId) {
  const conversation = findConversation(state, conversationId)
  if (!conversation) return null
  if (actorId !== conversation.passengerId && actorId !== conversation.driverId) return null

  state.chatMessages.forEach((message) => {
    if (message.conversationId === conversationId && message.senderId !== actorId && !message.readAt) {
      message.readAt = formatNow()
    }
  })
  return getConversation(state, conversationId, actorId)
}

export function listNegotiationProposals(state, conversationId, actorId) {
  const conversation = findConversation(state, conversationId)
  if (!conversation) return []
  if (actorId && actorId !== conversation.passengerId && actorId !== conversation.driverId) return []
  return state.negotiationProposals
    .filter((proposal) => proposal.conversationId === conversationId)
    .sort((left, right) => Number(right.version || 0) - Number(left.version || 0))
    .map((proposal) => ({ ...proposal, confirmedRoles: [...(proposal.confirmedRoles || [])] }))
}

export function createNegotiationProposal(state, conversationId, actorId, request) {
  const conversation = findConversation(state, conversationId)
  if (!conversation || conversation.status !== ORDER_STATUS.NEGOTIATING) return null
  if (actorId !== conversation.passengerId && actorId !== conversation.driverId) return null

  const latest = findLatestProposal(state, conversationId)
  const proposal = {
    id: makeId('proposal', state.negotiationProposals),
    conversationId,
    version: latest ? latest.version + 1 : 1,
    proposerId: actorId,
    price: Number(request?.price || 0),
    departureTime: normalizeText(request?.departureTime),
    passengerCount: Number(request?.passengerCount || 1),
    pickupLocation: normalizeText(request?.pickupLocation),
    pickupLng: request?.pickupLng == null ? null : Number(request.pickupLng),
    pickupLat: request?.pickupLat == null ? null : Number(request.pickupLat),
    note: normalizeText(request?.note),
    confirmedRoles: [],
    orderId: '',
    createdAt: formatNow()
  }
  state.negotiationProposals.push(proposal)
  state.chatMessages.push(
    createMessage(state, conversationId, 'system', 'text', `协商方案 v${proposal.version} 已提交`)
  )
  return { ...proposal, confirmedRoles: [] }
}

function finalizeNegotiationOrder(state, conversation, proposal, actorId) {
  const request = state.matchRequests.find((item) => item.id === conversation.matchRequestId)
  const passengerOrder = findPassengerOrder(state, request?.passengerOrderId || conversation.orderId)
  const trip = findTrip(state, request?.driverTripId || '')
  if (!request || !passengerOrder || !trip) return null

  const existingOrder = findOrderRecord(state, passengerOrder.id)
  if (existingOrder) {
    existingOrder.status = ORDER_STATUS.CONFIRMED
    existingOrder.canPay = false
    existingOrder.live = existingOrder.live || {
      phase: 'pickup',
      driverLocation: `${trip.start}附近`,
      etaText: '约 6 分钟到达上车点',
      distanceText: '约 1.8 公里',
      mapText: '司机正在前往乘客上车点',
      vehicleText: trip.vehicle
    }
    request.orderId = existingOrder.id
    conversation.orderId = existingOrder.id
    conversation.status = ORDER_STATUS.CONFIRMED
    conversation.closedAt = formatNow()
    passengerOrder.status = ORDER_STATUS.CONFIRMED
    return existingOrder
  }

  const orderRecord = {
    id: makeId('order', state.orderRecords),
    passengerOrderId: passengerOrder.id,
    driverTripId: trip.id,
    passengerId: passengerOrder.passengerId,
    driverId: trip.driverId,
    passengerName: maskName(state.passenger.name),
    driverName: maskName(trip.driverName),
    driverPhone: trip.driverPhone ? maskPhone(trip.driverPhone.replace(/\D/g, '')) : '138****2678',
    vehicle: maskVehicle(trip.vehicle),
    route: `${passengerOrder.start} -> ${passengerOrder.end}`,
    start: passengerOrder.start,
    end: passengerOrder.end,
    departureTime: proposal.departureTime,
    passengerCount: proposal.passengerCount,
    price: Number(proposal.price || trip.price || 0),
    status: ORDER_STATUS.CONFIRMED,
    paymentStatus: PAYMENT_STATUS.UNPAID,
    reviewStatus: REVIEW_STATUS.UNREVIEWED,
    canPay: false,
    virtualPhone: '',
    createdAt: formatNow(),
    paidAt: '',
    cancelReason: '',
    review: null,
    live: {
      phase: 'pickup',
      driverLocation: `${trip.start}附近`,
      etaText: '约 6 分钟到达上车点',
      distanceText: '约 1.8 公里',
      mapText: '司机正在前往乘客上车点',
      vehicleText: trip.vehicle
    },
    timeline: [
      createTimelineItem(ORDER_STATUS.MATCHING, '乘客发布拼车需求'),
      createTimelineItem(MATCH_REQUEST_STATUS.PENDING, '乘客向司机发起拼车请求'),
      createTimelineItem(MATCH_REQUEST_STATUS.ACCEPTED, '司机同意请求，进入协商中'),
      createTimelineItem(ORDER_STATUS.CONFIRMED, `双方确认方案 v${proposal.version}`)
    ]
  }
  state.orderRecords.unshift(orderRecord)
  request.orderId = orderRecord.id
  conversation.orderId = orderRecord.id
  conversation.status = ORDER_STATUS.CONFIRMED
  conversation.closedAt = formatNow()
  passengerOrder.status = ORDER_STATUS.CONFIRMED
  trip.availableSeats = Math.max(0, Number(trip.availableSeats || 0) - Number(proposal.passengerCount || 1))
  if (trip.availableSeats === 0) trip.status = '已满座'
  state.chatMessages.push(
    createMessage(state, conversation.id, 'system', 'text', `双方确认方案 v${proposal.version}，正式订单已生成`)
  )
  return orderRecord
}

export function confirmNegotiationProposal(state, conversationId, actorId, version) {
  const conversation = findConversation(state, conversationId)
  if (!conversation || conversation.status !== ORDER_STATUS.NEGOTIATING) return null
  if (actorId !== conversation.passengerId && actorId !== conversation.driverId) return null

  const proposal = findLatestProposal(state, conversationId)
  if (!proposal || Number(proposal.version || 0) !== Number(version || 0)) return null

  const role = participantRole(conversation, actorId)
  if (!role) return null
  if (!proposal.confirmedRoles.includes(role)) proposal.confirmedRoles.push(role)

  let orderRecord = null
  if (proposal.confirmedRoles.includes('PASSENGER') && proposal.confirmedRoles.includes('DRIVER')) {
    orderRecord = finalizeNegotiationOrder(state, conversation, proposal, actorId)
    proposal.orderId = orderRecord?.id || proposal.orderId
  }
  return { ...proposal, confirmedRoles: [...proposal.confirmedRoles], orderId: proposal.orderId || orderRecord?.id || '' }
}

export function confirmPassengerOrder(state, orderId) {
  const orderRecord = findOrderRecord(state, orderId)
  if (!orderRecord || orderRecord.status !== ORDER_STATUS.CONFIRMED) return null

  orderRecord.status = ORDER_STATUS.IN_PROGRESS
  addTimeline(orderRecord, ORDER_STATUS.IN_PROGRESS, '乘客确认司机接车请求，订单进入履约中')
  updatePassengerOrderStatus(state, orderRecord.passengerOrderId, ORDER_STATUS.IN_PROGRESS)
  return orderRecord
}

export function cancelPassengerOrder(state, id, reason = '乘客取消订单') {
  const orderRecord = findOrderRecord(state, id)
  if (orderRecord) {
    const canCancel =
      (orderRecord.status === ORDER_STATUS.CONFIRMED || orderRecord.status === ORDER_STATUS.IN_PROGRESS) &&
      orderRecord.paymentStatus !== PAYMENT_STATUS.PAID
    if (!canCancel) return null

    orderRecord.status = ORDER_STATUS.CANCELLED
    orderRecord.cancelReason = reason
    orderRecord.virtualPhone = ''
    delete state.virtualPhones[orderRecord.id]
    delete state.shareLinks[orderRecord.id]
    addTimeline(orderRecord, ORDER_STATUS.CANCELLED, reason)
    updatePassengerOrderStatus(state, orderRecord.passengerOrderId, ORDER_STATUS.CANCELLED)
    const conversation = state.conversations.find((item) =>
      state.matchRequests.some(
        (request) => request.id === item.matchRequestId && request.passengerOrderId === orderRecord.passengerOrderId
      )
    )
    if (conversation) {
      conversation.status = ORDER_STATUS.CANCELLED
      conversation.closedAt = formatNow()
    }
    return orderRecord
  }

  const passengerOrder = findPassengerOrder(state, id)
  if (!passengerOrder || passengerOrder.status === ORDER_STATUS.CANCELLED) return null
  if (![ORDER_STATUS.MATCHING, ORDER_STATUS.NEGOTIATING].includes(passengerOrder.status)) return null

  passengerOrder.status = ORDER_STATUS.CANCELLED
  passengerOrder.cancelReason = reason
  passengerOrder.cancelledAt = formatNow()

  state.matchRequests.forEach((request) => {
    if (request.passengerOrderId === passengerOrder.id && request.status === MATCH_REQUEST_STATUS.PENDING) {
      request.status = MATCH_REQUEST_STATUS.REJECTED
      request.handledAt = formatNow()
    }
  })

  state.conversations.forEach((conversation) => {
    if (conversation.matchRequestId && state.matchRequests.some((request) => request.id === conversation.matchRequestId && request.passengerOrderId === passengerOrder.id)) {
      conversation.status = ORDER_STATUS.CANCELLED
      conversation.closedAt = formatNow()
    }
  })

  return passengerOrder
}

export function mockDriverStartTrip(state, orderId) {
  const orderRecord = findOrderRecord(state, orderId)
  if (!orderRecord || orderRecord.status !== ORDER_STATUS.CONFIRMED) return null

  orderRecord.status = ORDER_STATUS.IN_PROGRESS
  const virtualPhone = `95013${String(Math.abs(orderRecord.id.split('').reduce((sum, char) => sum + char.charCodeAt(0), 0))).padStart(8, '0').slice(-8)}`
  orderRecord.virtualPhone = virtualPhone
  state.virtualPhones[orderRecord.id] = {
    id: `vphone-${orderRecord.id}`,
    orderId: orderRecord.id,
    virtualNumber: virtualPhone,
    status: 'ACTIVE',
    boundAt: formatNow(),
    unboundAt: ''
  }
  state.trajectories[orderRecord.id] = state.trajectories[orderRecord.id] || [
    {
      id: `tp-${orderRecord.id}-1`,
      orderId: orderRecord.id,
      longitude: 121.479,
      latitude: 31.226,
      sequenceNo: 1,
      reportedAt: formatNow()
    }
  ]
  orderRecord.live = {
    ...orderRecord.live,
    phase: 'trip',
    driverLocation: '行程中',
    etaText: '约 18 分钟到达目的地',
    distanceText: '约 7.2 公里',
    mapText: '车辆正在沿推荐路线行驶'
  }
  addTimeline(orderRecord, ORDER_STATUS.IN_PROGRESS, 'mock 司机确认行程开始')
  updatePassengerOrderStatus(state, orderRecord.passengerOrderId, ORDER_STATUS.IN_PROGRESS)
  return orderRecord
}

export function mockDriverFinishTrip(state, orderId) {
  const orderRecord = findOrderRecord(state, orderId)
  if (!orderRecord || orderRecord.status !== ORDER_STATUS.IN_PROGRESS || orderRecord.paymentStatus === PAYMENT_STATUS.PAID) {
    return null
  }

  orderRecord.status = ORDER_STATUS.PENDING_PAYMENT
  orderRecord.canPay = true
  orderRecord.virtualPhone = ''
  delete state.virtualPhones[orderRecord.id]
  delete state.shareLinks[orderRecord.id]
  orderRecord.live = {
    ...orderRecord.live,
    phase: 'arrived',
    driverLocation: orderRecord.end,
    etaText: '已到达目的地',
    distanceText: '0 公里',
    mapText: '行程已结束，等待乘客付款'
  }
  addTimeline(orderRecord, ORDER_STATUS.PENDING_PAYMENT, 'mock 司机确认送达，等待乘客付款')
  updatePassengerOrderStatus(state, orderRecord.passengerOrderId, ORDER_STATUS.PENDING_PAYMENT)
  return orderRecord
}

export function payOrder(state, orderId) {
  const orderRecord = findOrderRecord(state, orderId)
  if (
    !orderRecord ||
    orderRecord.status !== ORDER_STATUS.PENDING_PAYMENT ||
    !orderRecord.canPay ||
    orderRecord.paymentStatus === PAYMENT_STATUS.PAID
  ) {
    return null
  }

  orderRecord.paymentStatus = PAYMENT_STATUS.PAID
  orderRecord.paidAt = formatNow()
  orderRecord.status = ORDER_STATUS.COMPLETED
  orderRecord.canPay = false
  orderRecord.virtualPhone = ''
  delete state.virtualPhones[orderRecord.id]
  delete state.shareLinks[orderRecord.id]
  addTimeline(orderRecord, ORDER_STATUS.COMPLETED, '乘客完成模拟付款，订单完成')
  updatePassengerOrderStatus(state, orderRecord.passengerOrderId, ORDER_STATUS.COMPLETED)
  return orderRecord
}

export function submitReview(state, orderId, rating, comment) {
  const orderRecord = findOrderRecord(state, orderId)
  if (
    !orderRecord ||
    orderRecord.status !== ORDER_STATUS.COMPLETED ||
    orderRecord.paymentStatus !== PAYMENT_STATUS.PAID ||
    orderRecord.reviewStatus === REVIEW_STATUS.REVIEWED
  ) {
    return null
  }

  orderRecord.reviewStatus = REVIEW_STATUS.REVIEWED
  orderRecord.review = {
    rating: Number(rating || 5),
    comment: normalizeText(comment),
    createdAt: formatNow()
  }
  addTimeline(orderRecord, ORDER_STATUS.COMPLETED, '乘客提交订单评价')
  updatePassengerOrderStatus(state, orderRecord.passengerOrderId, ORDER_STATUS.COMPLETED)
  return orderRecord
}

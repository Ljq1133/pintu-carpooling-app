import assert from 'node:assert/strict'
import { createSharedPlatform } from '../src/platform/sharedPlatform.js'

const calls = []

let passengerOrders = [
  {
    id: 'order-001',
    orderId: 'order-001',
    passengerOrderId: 'po-001',
    passengerId: 'passenger-001',
    driverId: 'driver-001',
    passengerName: '拼图用户',
    driverName: '李师傅',
    driverPhone: '13800001234',
    vehicle: '沪A·P2T08',
    start: '中央科技园',
    end: 'SOHO',
    departureTime: '2026-06-13 18:30',
    passengerCount: 1,
    price: 18.6,
    status: '待付款',
    paymentStatus: '未支付',
    reviewStatus: '未评价',
    createdAt: '2026-06-13 17:30'
  }
]

const orderRequests = {
  'po-001': [],
  'po-002': [
    {
      id: 'req-002',
      passengerOrderId: 'po-002',
      driverTripId: 'trip-001',
      passenger: '拼图用户',
      driverName: '李师傅',
      driverId: 'driver-001',
      passengerId: 'passenger-001',
      route: '中央科技园 -> SOHO',
      status: '已同意',
      time: '2026-06-13 19:30',
      overlap: 92,
      credit: 98,
      price: 21,
      conversationId: 'conversation-001',
      orderId: ''
    }
  ]
}

let conversation = {
  id: 'conversation-001',
  matchRequestId: 'req-002',
  orderId: '',
  passengerId: 'passenger-001',
  driverId: 'driver-001',
  status: '协商中',
  unreadCount: 1,
  createdAt: '2026-06-13 19:05',
  closedAt: ''
}

let messages = [
  {
    id: 'msg-001',
    conversationId: 'conversation-001',
    senderId: 'system',
    messageType: 'text',
    content: '司机已同意请求，双方进入协商。',
    readAt: '',
    createdAt: '2026-06-13 19:05'
  }
]

let proposals = [
  {
    id: 'proposal-001',
    conversationId: 'conversation-001',
    version: 1,
    proposerId: 'driver-001',
    price: 21,
    departureTime: '2026-06-13 19:35',
    passengerCount: 1,
    pickupLocation: '中央科技园北门',
    pickupLng: 121.1,
    pickupLat: 31.2,
    note: '北门集合',
    confirmedRoles: ['PASSENGER'],
    orderId: '',
    createdAt: '2026-06-13 19:06'
  }
]

const jsonResponse = (data, status = 200) =>
  new Response(JSON.stringify({ code: 0, msg: 'ok', data }), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })

globalThis.fetch = async (url, options = {}) => {
  const requestUrl = new URL(url)
  const method = options.method || 'GET'
  const path = requestUrl.pathname
  const body = options.body ? JSON.parse(options.body) : undefined
  const headers = new Headers(options.headers)
  calls.push({ method, path, body, authorization: headers.get('Authorization') })

  if (method === 'POST' && path === '/api/auth/login') {
    const role = body.role
    return jsonResponse({
      token: `access-${role}`,
      refreshToken: `refresh-${role}`,
      expiresIn: 900,
      user: { id: `${role}-001`, role }
    })
  }
  if (method === 'POST' && path === '/api/auth/refresh') {
    return jsonResponse({
      token: 'access-refreshed',
      refreshToken: 'refresh-rotated',
      expiresIn: 900
    })
  }

  if (method === 'GET' && path === '/api/passenger/profile') {
    return jsonResponse({ id: 'passenger-001', name: '拼图用户', credit: 98 })
  }
  if (method === 'GET' && path === '/api/passenger/orders') {
    return jsonResponse(passengerOrders)
  }
  if (method === 'GET' && path === '/api/passenger/orders/po-001/requests') {
    return jsonResponse(orderRequests['po-001'])
  }
  if (method === 'GET' && path === '/api/passenger/orders/po-002/requests') {
    return jsonResponse(orderRequests['po-002'])
  }
  if (method === 'GET' && path === '/api/passenger/orders/po-002/matches') {
    return jsonResponse([
      {
        id: 'trip-001',
        driverTripId: 'trip-001',
        start: '中央科技园',
        end: 'SOHO',
        departureTime: '2026-06-13 19:35',
        overlap: 92,
        price: 21,
        driverName: '李师傅',
        driverCredit: 96,
        vehicle: { plate: '沪A·P2T08', model: '比亚迪 秦 PLUS' }
      }
    ])
  }
  if (method === 'POST' && path === '/api/passenger/orders') {
    return jsonResponse({
      id: 'po-002',
      passengerOrderId: 'po-002',
      passengerId: 'passenger-001',
      ...body,
      status: '待匹配',
      createdAt: '2026-06-13 18:00'
    })
  }
  if (method === 'POST' && path === '/api/orders/order-001/pay') {
    passengerOrders[0] = {
      ...passengerOrders[0],
      status: '已完成',
      paymentStatus: '已支付'
    }
    return jsonResponse({
      id: 'pay-001',
      orderId: 'order-001',
      amount: body.amount,
      status: '已支付'
    })
  }

  if (method === 'GET' && path === '/api/driver/profile') {
    return jsonResponse({ id: 'driver-001', name: '李师傅', credit: 96 })
  }
  if (method === 'GET' && path === '/api/driver/vehicles') {
    return jsonResponse([
      {
        id: 'veh-001',
        plate: '沪A·P2T08',
        model: '比亚迪 秦 PLUS',
        seats: 4,
        status: '已认证'
      }
    ])
  }
  if (method === 'GET' && path === '/api/driver/trips') {
    return jsonResponse([
      {
        id: 'trip-001',
        driverTripId: 'trip-001',
        start: '中央科技园',
        end: 'SOHO',
        departureTime: '2026-06-13 19:35',
        status: '已发布',
        availableSeats: 3,
        vehicle: { plate: '沪A·P2T08', model: '比亚迪 秦 PLUS' }
      }
    ])
  }
  if (method === 'GET' && path === '/api/driver/orders') {
    return jsonResponse(passengerOrders)
  }
  if (method === 'GET' && path === '/api/driver/trips/trip-001/matches') {
    return jsonResponse(orderRequests['po-002'])
  }
  if (method === 'GET' && path === '/api/driver/trips/trip-001/recommendations') {
    return jsonResponse([])
  }
  if (method === 'POST' && path === '/api/driver/trips') {
    return jsonResponse({ id: 'trip-002', driverId: 'driver-001', ...body, status: '已发布' })
  }

  if (method === 'GET' && path === '/api/conversations/conversation-001') {
    return jsonResponse(conversation)
  }
  if (method === 'GET' && path === '/api/conversations/conversation-001/messages') {
    return jsonResponse(messages)
  }
  if (method === 'POST' && path === '/api/conversations/conversation-001/messages') {
    const message = {
      id: `msg-${String(messages.length + 1).padStart(3, '0')}`,
      conversationId: 'conversation-001',
      senderId: headers.get('Authorization') === 'Bearer access-driver' ? 'driver-001' : 'passenger-001',
      messageType: body.messageType,
      content: body.content,
      attachmentKey: body.attachmentKey || '',
      readAt: '',
      createdAt: '2026-06-13 19:08'
    }
    messages = [...messages, message]
    return jsonResponse(message)
  }
  if (method === 'POST' && path === '/api/conversations/conversation-001/read') {
    conversation = { ...conversation, unreadCount: 0 }
    return jsonResponse(conversation)
  }
  if (method === 'GET' && path === '/api/conversations/conversation-001/proposals') {
    return jsonResponse(proposals)
  }
  if (method === 'POST' && path === '/api/conversations/conversation-001/proposals') {
    const proposal = {
      id: 'proposal-002',
      conversationId: 'conversation-001',
      version: 2,
      proposerId: 'driver-001',
      price: body.price,
      departureTime: body.departureTime,
      passengerCount: body.passengerCount,
      pickupLocation: body.pickupLocation,
      pickupLng: body.pickupLng,
      pickupLat: body.pickupLat,
      note: body.note,
      confirmedRoles: [],
      orderId: '',
      createdAt: '2026-06-13 19:09'
    }
    proposals = [proposal, ...proposals]
    return jsonResponse(proposal)
  }
  if (method === 'POST' && path === '/api/conversations/conversation-001/proposals/2/confirm') {
    const confirmed = {
      ...proposals[0],
      confirmedRoles: ['DRIVER', 'PASSENGER'],
      orderId: 'order-002'
    }
    proposals = [confirmed, ...proposals.slice(1)]
    conversation = {
      ...conversation,
      orderId: 'order-002',
      status: '已确认',
      closedAt: '2026-06-13 19:10'
    }
    orderRequests['po-002'][0] = {
      ...orderRequests['po-002'][0],
      orderId: 'order-002'
    }
    passengerOrders = [
      ...passengerOrders,
      {
        id: 'order-002',
        orderId: 'order-002',
        passengerOrderId: 'po-002',
        passengerId: 'passenger-001',
        driverId: 'driver-001',
        passengerName: '拼图用户',
        driverName: '李师傅',
        driverPhone: '13800001234',
        vehicle: '沪A·P2T08',
        start: '中央科技园',
        end: 'SOHO',
        departureTime: confirmed.departureTime,
        passengerCount: confirmed.passengerCount,
        price: confirmed.price,
        status: '已确认',
        paymentStatus: '未支付',
        reviewStatus: '未评价',
        createdAt: '2026-06-13 19:10'
      }
    ]
    return jsonResponse(confirmed)
  }

  if (method === 'GET' && path === '/api/admin/audits/users') return jsonResponse([])
  if (method === 'GET' && path === '/api/admin/audits/vehicles') return jsonResponse([])
  if (method === 'GET' && path === '/api/admin/orders') return jsonResponse(passengerOrders)
  if (method === 'GET' && path === '/api/admin/users/credit') {
    return jsonResponse([
      { id: 'passenger-001', name: '拼图用户', role: 'passenger', credit: 98, status: '正常' }
    ])
  }

  return new Response(
    JSON.stringify({ code: 1404, msg: `未配置测试接口：${method} ${path}`, data: null }),
    { status: 404, headers: { 'Content-Type': 'application/json' } }
  )
}

const platform = createSharedPlatform({ apiMode: 'http' })
await platform.ready

assert.equal(platform.mode, 'http')
assert.equal(platform.driverApi.getVehicles()[0].id, 'veh-001')
assert.equal(platform.passengerState.orderRecords[0].canPay, true)

await platform.passengerApi.createPassengerOrder({
  start: '中央科技园',
  end: 'SOHO',
  departureTime: '2026-06-13 19:30',
  passengerCount: 1,
  note: 'HTTP 适配测试'
})

await platform.driverApi.createTrip({
  start: '中央科技园',
  end: 'SOHO',
  time: '2026-06-13 19:30',
  vehicle: '沪A·P2T08',
  seats: 2,
  price: 18.6
})

const requestList = await platform.passengerApi.getOrderRequests('po-002')
assert.equal(requestList[0].conversationId, 'conversation-001')

const messageList = await platform.passengerApi.getConversationMessages('conversation-001')
assert.equal(messageList.length, 1)

const sentMessage = await platform.passengerApi.sendMessage('conversation-001', {
  content: '我可以提前五分钟到北门。'
})
assert.equal(sentMessage.senderId, 'passenger-001')

const createdProposal = await platform.driverApi.createProposal('conversation-001', {
  price: 22,
  departureTime: '2026-06-13 19:40',
  passengerCount: 1,
  pickupLocation: '中央科技园北门',
  note: '请准时到达'
})
assert.equal(createdProposal.version, 2)

const confirmedProposal = await platform.driverApi.confirmProposal('conversation-001', 2)
assert.equal(confirmedProposal.orderId, 'order-002')
assert.equal(platform.driverApi.getOrders().some((order) => order.id === 'order-002'), true)

await platform.passengerApi.payOrder('order-001')

const createOrderCall = calls.find((call) => call.method === 'POST' && call.path === '/api/passenger/orders')
assert.equal(createOrderCall.authorization, 'Bearer access-passenger')
assert.equal(createOrderCall.body.passengerId, undefined, '用户身份只能由访问令牌传递')

const createTripCall = calls.find((call) => call.method === 'POST' && call.path === '/api/driver/trips')
assert.equal(createTripCall.authorization, 'Bearer access-driver')
assert.equal(createTripCall.body.vehicleId, 'veh-001')

const sendMessageCall = calls.find((call) => call.method === 'POST' && call.path === '/api/conversations/conversation-001/messages')
assert.equal(sendMessageCall.authorization, 'Bearer access-passenger')

const createProposalCall = calls.find((call) => call.method === 'POST' && call.path === '/api/conversations/conversation-001/proposals')
assert.equal(createProposalCall.authorization, 'Bearer access-driver')

const confirmProposalCall = calls.find(
  (call) => call.method === 'POST' && call.path === '/api/conversations/conversation-001/proposals/2/confirm'
)
assert.equal(confirmProposalCall.authorization, 'Bearer access-driver')

const payCall = calls.find((call) => call.method === 'POST' && call.path === '/api/orders/order-001/pay')
assert.equal(payCall.authorization, 'Bearer access-passenger')
assert.equal(payCall.body.amount, 18.6)
assert.equal(platform.passengerState.orderRecords.find((item) => item.id === 'order-001')?.status, '已完成')

console.log(
  JSON.stringify(
    {
      mode: platform.mode,
      requestCount: calls.length,
      passengerAuthorization: createOrderCall.authorization,
      driverAuthorization: createTripCall.authorization,
      confirmedOrderId: confirmedProposal.orderId,
      paidOrderStatus: platform.passengerState.orderRecords.find((item) => item.id === 'order-001')?.status
    },
    null,
    2
  )
)

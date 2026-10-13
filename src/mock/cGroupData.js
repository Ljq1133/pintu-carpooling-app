export const driverProfile = {
  name: '陈师傅',
  phone: '138 **** 2678',
  credit: 96,
  todayTrips: 3,
  income: 86.5,
  carbonSaved: 4.8
}

export const vehicles = [
  {
    id: 'veh-001',
    plate: '沪A·P2T08',
    model: '比亚迪 秦 PLUS',
    color: '白色',
    seats: 3,
    status: '已认证'
  },
  {
    id: 'veh-002',
    plate: '沪N·7U21C',
    model: '大众 速腾',
    color: '灰色',
    seats: 2,
    status: '待审核'
  }
]

export const auditUsers = [
  {
    id: 'auth-001',
    name: '周雨',
    role: '司机',
    type: '实名认证',
    submittedAt: '06-08 19:20',
    status: '待审核'
  },
  {
    id: 'auth-002',
    name: '何嘉',
    role: '乘客',
    type: '实名认证',
    submittedAt: '06-08 18:44',
    status: '待审核'
  }
]

export const adminMe = {
  id: 'admin-001',
  username: 'admin_demo',
  name: '平台管理员',
  role: 'admin',
  subRoles: ['SUPER_ADMIN'],
  subRoleLabels: ['超级管理员'],
  permissions: [
    { code: 'AUDIT_REVIEW', label: '审核处理' },
    { code: 'ORDER_VIEW', label: '订单看板' },
    { code: 'CREDIT_MANAGE', label: '信用管理' },
    { code: 'APPEAL_MANAGE', label: '申诉处理' },
    { code: 'ARBITRATION_MANAGE', label: '仲裁处理' },
    { code: 'SAFETY_MONITOR', label: '安全监控' },
    { code: 'RULE_MANAGE', label: '规则管理' },
    { code: 'SUPPORT_MANAGE', label: '客服会话' },
    { code: 'USER_RESTRICT', label: '用户限制' }
  ]
}

export const auditVehicles = [
  {
    id: 'car-auth-001',
    driver: '陈师傅',
    plate: '沪N·7U21C',
    model: '大众 速腾',
    submittedAt: '06-08 17:35',
    status: '待审核'
  },
  {
    id: 'car-auth-002',
    driver: '刘师傅',
    plate: '沪A·6H39K',
    model: '丰田 卡罗拉',
    submittedAt: '06-08 16:50',
    status: '待审核'
  }
]

export const adminOrders = [
  {
    id: 'order-1001',
    route: '同济科技园 -> SOHO',
    passenger: '赵同学',
    driver: '陈师傅',
    status: '协商中',
    paymentStatus: '未支付',
    price: 18.6,
    risk: '正常',
    createdAt: '2026-06-08 18:20'
  },
  {
    id: 'order-1002',
    route: '清华大学东门 -> 三里屯',
    passenger: '林同学',
    driver: '刘师傅',
    status: '履约中',
    paymentStatus: '未支付',
    price: 15.2,
    risk: '关注',
    createdAt: '2026-06-08 18:10'
  },
  {
    id: 'order-1003',
    route: '回龙观 -> 上地软件园',
    passenger: '王同学',
    driver: '周师傅',
    status: '待付款',
    paymentStatus: '未支付',
    price: 12.0,
    risk: '超时',
    createdAt: '2026-06-08 17:30'
  }
]

export const creditUsers = [
  {
    id: 'driver-001',
    name: '陈师傅',
    role: '司机',
    credit: 96,
    status: '正常'
  },
  {
    id: 'user-001',
    name: '赵同学',
    role: '乘客',
    credit: 98,
    status: '正常'
  },
  {
    id: 'user-002',
    name: '刘师傅',
    role: '司机',
    credit: 72,
    status: '观察'
  },
  {
    id: 'user-003',
    name: '匿名用户 03',
    role: '乘客',
    credit: 58,
    status: '限制发单'
  }
]

export const demoAccounts = [
  ['司机账号', 'driver_demo', '123456', '用于司机端发布行程、接收乘客请求'],
  ['管理员账号', 'admin_demo', '123456', '用于审核、订单看板、信用分管理'],
  ['乘客账号', 'passenger_demo', '123456', '预留给 B 组乘客端联调']
]

export const cGroupApiNeeds = [
  ['GET', '/api/driver/vehicles', '获取司机车辆列表'],
  ['POST', '/api/driver/trips', '司机发布行程'],
  ['GET', '/api/driver/trips/{tripId}/matches', '获取顺路乘客推荐'],
  ['POST', '/api/match-requests/{id}/accept', '司机同意乘客请求'],
  ['POST', '/api/match-requests/{id}/reject', '司机拒绝乘客请求'],
  ['POST', '/api/orders/{id}/start', '司机确认开始行程'],
  ['POST', '/api/orders/{id}/arrive', '司机确认到达目的地'],
  ['GET/POST/PUT/DELETE', '/api/safety/emergency-contacts', '维护司机账户紧急联系人'],
  ['POST', '/api/safety/orders/{orderId}/alerts', '履约中触发课程 Mock 报警'],
  ['GET', '/api/admin/audits/users', '管理员查看实名审核列表'],
  ['POST', '/api/admin/audits/users/{id}/approve', '通过实名审核'],
  ['GET', '/api/admin/audits/vehicles', '管理员查看车辆审核列表'],
  ['GET', '/api/admin/orders', '管理员查看订单看板'],
  ['POST', '/api/admin/users/{id}/credit', '调整用户信用分或限制状态']
]

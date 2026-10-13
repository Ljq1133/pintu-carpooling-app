export const passengerTabs = [
  {
    key: 'ride',
    label: '乘车',
    description: '发布需求、等待司机确认、查看实时行程'
  },
  {
    key: 'matches',
    label: '推荐',
    description: '按推荐规则展示可选司机'
  },
  {
    key: 'orders',
    label: '订单',
    description: '查看进行中入口和历史订单'
  },
  {
    key: 'profile',
    label: '我的',
    description: '展示演示乘客基础资料'
  },
  {
    key: 'integration',
    label: '联调准备',
    description: '第四阶段整理接口替换位置'
  }
]

export const demoPassenger = {
  id: 'passenger-001',
  username: 'passenger_demo',
  password: '123456',
  name: '赵同学',
  phone: '138 **** 5678',
  credit: 98,
  tripCount: 32,
  savedMoney: 268.5,
  carbonSaved: 16.2,
  status: '正常'
}

export const demoLocation = {
  label: '当前位置',
  address: '同济科技园',
  city: '上海',
  source: 'mock'
}

export const demoDriverTrips = [
  {
    id: 'trip-001',
    driverId: 'driver-001',
    driverName: '陈师傅',
    vehicle: '沪A·P2T08 · 比亚迪 秦 PLUS',
    start: '同济科技园',
    end: 'SOHO',
    departureTime: '2026-06-09 08:30',
    availableSeats: 3,
    driverCredit: 96,
    overlap: 92,
    price: 18.6,
    status: '已发布'
  },
  {
    id: 'trip-002',
    driverId: 'driver-002',
    driverName: '李师傅',
    vehicle: '沪B·K8M26 · 大众 ID.3',
    start: '同济科技园',
    end: '五角场',
    departureTime: '2026-06-09 08:45',
    availableSeats: 2,
    driverCredit: 94,
    overlap: 78,
    price: 15.2,
    status: '已发布'
  },
  {
    id: 'trip-003',
    driverId: 'driver-003',
    driverName: '周师傅',
    vehicle: '沪C·Q6N18 · 特斯拉 Model 3',
    start: '同济科技园',
    end: 'SOHO',
    departureTime: '2026-06-09 09:10',
    availableSeats: 0,
    driverCredit: 97,
    overlap: 88,
    price: 21.8,
    status: '已满座'
  },
  {
    id: 'trip-004',
    driverId: 'driver-004',
    driverName: '吴师傅',
    vehicle: '沪D·L5V66 · 广汽埃安 AION S',
    start: '同济科技园',
    end: 'SOHO',
    departureTime: '2026-06-09 09:20',
    availableSeats: 2,
    driverCredit: 95,
    overlap: 58,
    price: 17.4,
    status: '已发布'
  },
  {
    id: 'trip-005',
    driverId: 'driver-005',
    driverName: '郑师傅',
    vehicle: '沪E·R3T19 · 荣威 Ei5',
    start: '同济科技园',
    end: 'SOHO',
    departureTime: '2026-06-09 08:50',
    availableSeats: 2,
    driverCredit: 72,
    overlap: 88,
    price: 16.8,
    status: '信用观察'
  }
]

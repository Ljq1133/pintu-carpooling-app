import {
  adminMe,
  adminOrders,
  auditUsers,
  auditVehicles,
  creditUsers,
  vehicles
} from './cGroupData.js'

const clone = (value) => JSON.parse(JSON.stringify(value))

export function createCGroupState() {
  return {
    vehicles: clone(vehicles),
    adminMe: clone(adminMe),
    auditUsers: clone(auditUsers),
    auditVehicles: clone(auditVehicles),
    adminOrders: clone(adminOrders),
    adminOrderSummary: {
      total: adminOrders.length,
      pendingPayment: adminOrders.filter((order) => order.status === '待付款').length,
      completed: adminOrders.filter((order) => order.status === '已完成').length,
      cancelled: adminOrders.filter((order) => order.status === '已取消').length,
      statusCounts: {},
      paymentStatusCounts: {}
    },
    driverBlacklist: [],
    driverEmergencyContacts: [],
    adminAppeals: [],
    adminSafetyOrders: [],
    adminSafetyAlerts: [],
    adminOrderDetails: {},
    adminOpsStatistics: {
      totalOrders: 0,
      completedOrders: 0,
      cancelledOrders: 0,
      activeOrders: 0,
      safetyAlertCount: 0,
      openAppealCount: 0,
      conversionRate: 0,
      completionRate: 0,
      cancellationRate: 0,
      averageMatchMinutes: 0,
      abnormalRate: 0
    },
    adminSupportSessions: [],
    ruleConfigs: [
      {
        id: 'rule-pricing-default',
        ruleType: 'PRICING',
        code: 'DEFAULT',
        name: '默认计价规则',
        activeVersion: 1,
        status: 'ACTIVE',
        grayPercent: 100,
        versions: [
          {
            id: 'rv-pricing-default-1',
            version: 1,
            content: '{"basePrice":8,"distanceRate":1.6,"durationRate":0.35,"extraPassengerRate":0.2}',
            status: 'PUBLISHED',
            createdBy: 'admin-001',
            createdAt: '2026-06-16 20:00',
            publishedAt: '2026-06-16 20:05'
          }
        ],
        createdAt: '2026-06-16 20:00',
        updatedAt: '2026-06-16 20:05'
      },
      {
        id: 'rule-matching-default',
        ruleType: 'MATCHING',
        code: 'DEFAULT',
        name: '默认匹配规则',
        activeVersion: 1,
        status: 'ACTIVE',
        grayPercent: 100,
        versions: [
          {
            id: 'rv-matching-default-1',
            version: 1,
            content: '{"radiusKm":5,"timeWindowMinutes":120,"minimumOverlap":60,"minimumCredit":60}',
            status: 'PUBLISHED',
            createdBy: 'admin-001',
            createdAt: '2026-06-16 20:00',
            publishedAt: '2026-06-16 20:05'
          }
        ],
        createdAt: '2026-06-16 20:00',
        updatedAt: '2026-06-16 20:05'
      }
    ],
    dataRetentionPolicies: [],
    retentionCleanupResult: null,
    exportedOrderCsv: '',
    userRestrictions: [],
    creditUsers: clone(creditUsers)
  }
}

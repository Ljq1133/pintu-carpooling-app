import { driverProfile } from '../mock/cGroupData.js'
import {
  normalizeConversation,
  normalizeMessage,
  normalizeProposal,
  sortByCreatedAt,
  sortByNewestVersion
} from './negotiationApi.shared.js'
import { createHttpClient } from './httpClient.js'

const DRIVER_ID = 'driver-001'

const replaceArray = (target, items) => target.splice(0, target.length, ...items)

function liveForStatus(status) {
  if (status === '履约中') return { phase: 'trip' }
  if (status === '待付款') return { phase: 'arrived' }
  if (status === '已确认') return { phase: 'pickup' }
  return { phase: 'done' }
}

function normalizeOrder(order) {
  return {
    ...order,
    price: Number(order.price || 0),
    route: order.route || `${order.start} -> ${order.end}`,
    canPay: order.status === '待付款',
    live: liveForStatus(order.status)
  }
}

export function createDriverHttpApi({ passengerState, cState, passengerApi }) {
  const client = createHttpClient({ userId: DRIVER_ID })

  cState.driverProfile = { ...driverProfile }
  cState.driverTrips = []
  cState.driverMatchRequests = []
  cState.driverRecommendations = []
  cState.driverOrders = []
  cState.driverBlacklist = []
  cState.driverEmergencyContacts = []
  cState.locationReportQueue = cState.locationReportQueue || []
  cState.apiError = ''
  replaceArray(cState.vehicles, [])

  const run = async (operation, fallback = null) => {
    try {
      cState.apiError = ''
      return await operation()
    } catch (error) {
      cState.apiError = error.message
      console.warn('[PinTu Driver API]', error)
      return fallback
    }
  }

  const refreshProfile = () =>
    run(async () => {
      const profile = await client.get('/api/driver/profile')
      cState.driverProfile = { ...driverProfile, ...profile }
      return cState.driverProfile
    }, cState.driverProfile)

  const refreshVehicles = () =>
    run(async () => {
      const vehicles = await client.get('/api/driver/vehicles')
      replaceArray(cState.vehicles, vehicles)
      return cState.vehicles
    }, cState.vehicles)

  const refreshTrips = () =>
    run(async () => {
      const trips = await client.get('/api/driver/trips')
      replaceArray(cState.driverTrips, trips)
      return cState.driverTrips
    }, cState.driverTrips)

  const refreshOrders = () =>
    run(async () => {
      const orders = await client.get('/api/driver/orders')
      replaceArray(cState.driverOrders, orders.map(normalizeOrder))
      return cState.driverOrders
    }, cState.driverOrders)

  const refreshMatchRequests = () =>
    run(async () => {
      const requestGroups = await Promise.all(
        cState.driverTrips.map((trip) =>
          client.get(`/api/driver/trips/${encodeURIComponent(trip.id)}/matches`)
        )
      )
      const requests = requestGroups.flat()
      const unique = [...new Map(requests.map((request) => [request.id, request])).values()]
      replaceArray(cState.driverMatchRequests, unique)
      return cState.driverMatchRequests
    }, cState.driverMatchRequests)

  const refreshRecommendations = () =>
    run(async () => {
      const groups = await Promise.all(
        cState.driverTrips
          .filter((trip) => trip.status === '已发布')
          .map(async (trip) => {
            const items = await client.get(`/api/driver/trips/${encodeURIComponent(trip.id)}/recommendations`)
            return items.map((item) => ({ ...item, driverTripId: trip.id }))
          })
      )
      replaceArray(cState.driverRecommendations, groups.flat())
      return cState.driverRecommendations
    }, cState.driverRecommendations)

  const refreshBlacklist = () =>
    run(async () => {
      const items = await client.get('/api/privacy/blacklist')
      replaceArray(cState.driverBlacklist, items)
      return cState.driverBlacklist
    }, cState.driverBlacklist)

  const refreshEmergencyContacts = () =>
    run(async () => {
      const items = await client.get('/api/safety/emergency-contacts')
      replaceArray(cState.driverEmergencyContacts, items)
      return cState.driverEmergencyContacts
    }, cState.driverEmergencyContacts)

  const refreshAll = async () => {
    await Promise.all([
      refreshProfile(),
      refreshVehicles(),
      refreshTrips(),
      refreshOrders(),
      refreshBlacklist(),
      refreshEmergencyContacts()
    ])
    await Promise.all([refreshMatchRequests(), refreshRecommendations()])
  }

  return {
    initialize: refreshAll,
    refreshAll,

    getProfile() {
      return cState.driverProfile
    },

    getVehicles() {
      return cState.vehicles
    },

    getTrips() {
      return cState.driverTrips
    },

    getMatchRequests() {
      return cState.driverMatchRequests
    },

    getRecommendations() {
      return cState.driverRecommendations
    },

    getOrders() {
      return cState.driverOrders
    },

    getBlacklist() {
      return cState.driverBlacklist
    },

    getEmergencyContacts() {
      return cState.driverEmergencyContacts
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
        await Promise.all([refreshBlacklist(), refreshRecommendations()])
        return record
      })
    },

    unblockUser(blockedUserId) {
      return run(async () => {
        await client.delete(`/api/privacy/blacklist/${encodeURIComponent(blockedUserId)}`)
        await Promise.all([refreshBlacklist(), refreshRecommendations()])
        return true
      }, false)
    },

    triggerAlert(orderId, payload = {}) {
      return run(() => client.post(`/api/safety/orders/${encodeURIComponent(orderId)}/alerts`, payload))
    },

    async reportLocationBatch(orderId, points = [], options = {}) {
      const batch = {
        batchId: options.batchId || `batch-${Date.now()}`,
        points: points.map((point, index) => ({
          longitude: point.longitude,
          latitude: point.latitude,
          accuracy: point.accuracy,
          speed: point.speed,
          heading: point.heading,
          reportedAt: point.reportedAt,
          clientReportId: point.clientReportId || `${orderId}-${Date.now()}-${index}`,
          reportIntervalSeconds: Number(options.reportIntervalSeconds || point.reportIntervalSeconds || 5),
          retryCount: Number(point.retryCount || 0)
        }))
      }
      try {
        cState.apiError = ''
        return await client.post(`/api/safety/orders/${encodeURIComponent(orderId)}/locations/batch`, batch)
      } catch (error) {
        const retryCount = Number(options.retryCount || 0) + 1
        cState.locationReportQueue.push({
          orderId,
          batch: {
            ...batch,
            points: batch.points.map((point) => ({ ...point, retryCount }))
          },
          retryCount,
          nextRetryAt: Date.now() + Math.min(60000, 1000 * (2 ** retryCount))
        })
        cState.apiError = error.message
        console.warn('[PinTu Driver API]', error)
        return []
      }
    },

    async flushLocationQueue() {
      const due = cState.locationReportQueue.filter((item) => item.nextRetryAt <= Date.now())
      cState.locationReportQueue = cState.locationReportQueue.filter((item) => item.nextRetryAt > Date.now())
      const results = []
      for (const item of due) {
        try {
          const sent = await client.post(
            `/api/safety/orders/${encodeURIComponent(item.orderId)}/locations/batch`,
            item.batch
          )
          results.push(sent)
        } catch (error) {
          item.retryCount += 1
          item.nextRetryAt = Date.now() + Math.min(60000, 1000 * (2 ** item.retryCount))
          item.batch.points = item.batch.points.map((point) => ({ ...point, retryCount: item.retryCount }))
          cState.locationReportQueue.push(item)
          cState.apiError = error.message
        }
      }
      return results.flat()
    },

    createTrip(form) {
      return run(async () => {
        const vehicle = cState.vehicles.find((item) => item.plate === form.vehicle)
        if (!vehicle) return null
        const trip = await client.post('/api/driver/trips', {
          start: String(form.start || '').trim(),
          end: String(form.end || '').trim(),
          startLng: form.startLng,
          startLat: form.startLat,
          endLng: form.endLng,
          endLat: form.endLat,
          departureTime: String(form.time || '').trim(),
          vehicleId: vehicle.id,
          availableSeats: Number(form.seats || 1),
          price: form.price === '' || form.price == null ? null : Number(form.price)
        })
        await refreshTrips()
        return trip
      })
    },

    acceptMatchRequest(requestId) {
      return run(async () => {
        const request = await client.post(`/api/match-requests/${encodeURIComponent(requestId)}/accept`)
        await Promise.all([
          refreshTrips(),
          refreshOrders(),
          refreshMatchRequests(),
          passengerApi.refreshOrders?.(),
          passengerApi.refreshMatchRequests?.()
        ])
        return request
      })
    },

    rejectMatchRequest(requestId, reason = '司机当前无法接单') {
      return run(async () => {
        const request = await client.post(`/api/match-requests/${encodeURIComponent(requestId)}/reject`, {
          reason
        })
        await Promise.all([refreshMatchRequests(), passengerApi.refreshOrders?.()])
        return request
      })
    },

    invitePassenger(candidate) {
      return run(async () => {
        const request = await client.post('/api/match-requests/driver', {
          passengerOrderId: candidate.passengerOrderId,
          driverTripId: candidate.driverTripId,
          message: '司机邀请您同行'
        })
        await Promise.all([
          refreshMatchRequests(),
          refreshRecommendations(),
          passengerApi.refreshOrders?.(),
          passengerApi.refreshMatchRequests?.()
        ])
        return request
      })
    },

    startTrip(orderId) {
      return run(async () => {
        const order = await client.post(`/api/orders/${encodeURIComponent(orderId)}/start`)
        await Promise.all([refreshOrders(), passengerApi.refreshOrders?.()])
        return order
      })
    },

    finishTrip(orderId) {
      return run(async () => {
        const order = await client.post(`/api/orders/${encodeURIComponent(orderId)}/arrive`)
        await Promise.all([refreshOrders(), passengerApi.refreshOrders?.()])
        return order
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
      return run(async () =>
        normalizeMessage(
          await client.post(`/api/conversations/${encodeURIComponent(conversationId)}/messages`, {
            messageType: request?.messageType || 'text',
            content: request?.content || '',
            attachmentKey: request?.attachmentKey || '',
            clientMessageId: request?.clientMessageId || ''
          })
        ),
      null)
    },

    markConversationRead(conversationId) {
      return run(async () => {
        const result = await client.post(`/api/conversations/${encodeURIComponent(conversationId)}/read`)
        await refreshMatchRequests()
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
        await Promise.all([refreshMatchRequests(), refreshOrders(), passengerApi.refreshOrders?.()])
        return normalizeProposal(proposal)
      }, null)
    },

    confirmProposal(conversationId, version) {
      return run(async () => {
        const proposal = await client.post(
          `/api/conversations/${encodeURIComponent(conversationId)}/proposals/${encodeURIComponent(version)}/confirm`
        )
        await Promise.all([refreshMatchRequests(), refreshOrders(), passengerApi.refreshOrders?.()])
        return normalizeProposal(proposal)
      }, null)
    }
  }
}

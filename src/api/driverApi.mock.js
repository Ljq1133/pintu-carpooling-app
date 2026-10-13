import { driverProfile } from '../mock/cGroupData.js'
import { MATCH_REQUEST_STATUS, ORDER_STATUS } from './passengerApi.mock.js'
import {
  confirmNegotiationProposal as storeConfirmNegotiationProposal,
  createNegotiationProposal as storeCreateNegotiationProposal,
  getConversation as storeGetConversation,
  listConversationMessages as storeListConversationMessages,
  listNegotiationProposals as storeListNegotiationProposals,
  markConversationRead as storeMarkConversationRead,
  sendConversationMessage as storeSendConversationMessage
} from '../mock/localPassengerGroupStore.js'
import { normalizeConversation, normalizeMessage, normalizeProposal, sortByNewestVersion, sortByCreatedAt } from './negotiationApi.shared.js'

const asyncReturn = (value) => Promise.resolve(value)

const makeId = (prefix, list) => `${prefix}-${Date.now()}-${list.length + 1}`
const maskPhone = (value) => `${String(value).slice(0, 3)}****${String(value).slice(-4)}`
const nowText = () => new Date().toISOString().slice(0, 16).replace('T', ' ')

const findPassengerOrder = (state, passengerOrderId) =>
  state.passengerOrders.find((order) => order.id === passengerOrderId)

const findDriverTrip = (state, driverTripId) => state.driverTrips.find((trip) => trip.id === driverTripId)

export function createDriverApi({ passengerState, cState, passengerApi }) {
  const getDriverAccount = () => cState.creditUsers.find((user) => user.id === 'driver-001')
  const isBlocked = (passengerId) => passengerState.blacklistRecords.some((record) =>
    (record.ownerId === 'driver-001' && record.blockedUserId === passengerId) ||
    (record.ownerId === passengerId && record.blockedUserId === 'driver-001')
  )

  return {
    getProfile() {
      const account = getDriverAccount()
      return {
        ...driverProfile,
        credit: account?.credit ?? driverProfile.credit,
        status: account?.status || '正常'
      }
    },

    getVehicles() {
      return cState.vehicles
    },

    getTrips() {
      return passengerState.driverTrips.filter((trip) => trip.driverId === 'driver-001')
    },

    createTrip(form) {
      const vehicle = cState.vehicles.find((item) => item.plate === form.vehicle)
      const account = getDriverAccount()
      if (
        !vehicle ||
        vehicle.status !== '已认证' ||
        Number(account?.credit || 0) < 60 ||
        account?.status === '限制接单' ||
        account?.status === '已封禁'
      ) {
        return asyncReturn(null)
      }

      const trip = {
        id: makeId('trip', passengerState.driverTrips),
        driverId: 'driver-001',
        driverName: driverProfile.name,
        driverPhone: driverProfile.phone,
        vehicle: `${vehicle.plate} · ${vehicle.model}`,
        start: String(form.start || '').trim(),
        end: String(form.end || '').trim(),
        departureTime: String(form.time || '').trim(),
        availableSeats: Number(form.seats || 1),
        driverCredit: driverProfile.credit,
        overlap: 86,
        price: Number(form.price || 18.6),
        status: '已发布'
      }

      passengerState.driverTrips.unshift(trip)
      return asyncReturn(trip)
    },

    getMatchRequests() {
      return passengerState.matchRequests
        .filter((request) => request.driverId === 'driver-001')
        .map((request) => {
          const order = findPassengerOrder(passengerState, request.passengerOrderId)
          const trip = findDriverTrip(passengerState, request.driverTripId)
          return {
            ...request,
            passenger: passengerState.passenger.name,
            route: request.route || `${order?.start || ''} -> ${order?.end || ''}`,
            time: order?.departureTime || '',
            overlap: trip?.overlap || 0,
            credit: passengerState.passenger.credit,
            price: trip?.price || 0
          }
        })
    },

    getRecommendations() {
      const openOrders = passengerState.passengerOrders.filter(
        (order) => order.status === ORDER_STATUS.MATCHING && !isBlocked(order.passengerId)
      )
      const trips = passengerState.driverTrips.filter(
        (trip) => trip.driverId === 'driver-001' && trip.status === '已发布'
      )
      return openOrders.flatMap((order) =>
        trips
          .filter((trip) => Number(trip.availableSeats || 0) >= Number(order.passengerCount || 1))
          .map((trip) => ({
            passengerOrderId: order.id,
            passengerId: order.passengerId,
            passengerName: passengerState.passenger.name,
            driverTripId: trip.id,
            start: order.start,
            end: order.end,
            departureTime: order.departureTime,
            passengerCount: order.passengerCount,
            passengerCredit: passengerState.passenger.credit,
            overlap: trip.overlap,
            score: trip.overlap,
            reason: `路线顺路度 ${trip.overlap}%`
          }))
      )
    },

    getOrders() {
      return passengerState.orderRecords.filter((order) => {
        const trip = findDriverTrip(passengerState, order.driverTripId)
        return trip?.driverId === 'driver-001'
      })
    },

    getBlacklist() {
      return passengerState.blacklistRecords.filter((record) => record.ownerId === 'driver-001')
    },

    getEmergencyContacts() {
      return cState.driverEmergencyContacts
    },

    saveEmergencyContact(contact) {
      const existing = cState.driverEmergencyContacts.find((item) => item.id === contact.id)
      if (contact.isPrimary) {
        cState.driverEmergencyContacts.forEach((item) => {
          item.isPrimary = false
        })
      }
      const saved = {
        ...existing,
        id: existing?.id || `contact-driver-${Date.now()}`,
        name: String(contact.name || '').trim(),
        phoneMasked: contact.phone ? maskPhone(contact.phone) : existing?.phoneMasked,
        relationship: String(contact.relationship || '').trim(),
        isPrimary: Boolean(contact.isPrimary),
        createdAt: existing?.createdAt || nowText(),
        updatedAt: nowText()
      }
      if (existing) Object.assign(existing, saved)
      else cState.driverEmergencyContacts.unshift(saved)
      return asyncReturn(saved)
    },

    deleteEmergencyContact(contactId) {
      const index = cState.driverEmergencyContacts.findIndex((item) => item.id === contactId)
      if (index !== -1) cState.driverEmergencyContacts.splice(index, 1)
      return asyncReturn(true)
    },

    blockUser(blockedUserId, reason = '用户主动拉黑') {
      if (!blockedUserId || blockedUserId === 'driver-001') return asyncReturn(null)
      const existing = passengerState.blacklistRecords.find(
        (record) => record.ownerId === 'driver-001' && record.blockedUserId === blockedUserId
      )
      if (existing) return asyncReturn(existing)
      const record = {
        ownerId: 'driver-001',
        blockedUserId,
        blockedName: `${String(passengerState.passenger.name || '用户').slice(0, 1)}**`,
        role: 'passenger',
        reason,
        createdAt: new Date().toISOString().slice(0, 16).replace('T', ' ')
      }
      passengerState.blacklistRecords.unshift(record)
      return asyncReturn(record)
    },

    unblockUser(blockedUserId) {
      const index = passengerState.blacklistRecords.findIndex(
        (record) => record.ownerId === 'driver-001' && record.blockedUserId === blockedUserId
      )
      if (index !== -1) passengerState.blacklistRecords.splice(index, 1)
      return asyncReturn(true)
    },

    triggerAlert(orderId, payload = {}) {
      const order = passengerState.orderRecords.find((item) => item.id === orderId)
      if (!order || order.live?.phase !== 'trip') return asyncReturn(null)
      const alert = {
        id: `alert-driver-${Date.now()}`,
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
      passengerState.safetyAlerts.unshift(alert)
      return asyncReturn(alert)
    },

    reportLocationBatch(orderId, points = [], options = {}) {
      const order = passengerState.orderRecords.find((item) => item.id === orderId)
      if (!order || order.live?.phase !== 'trip') return asyncReturn([])
      const batchId = options.batchId || `batch-${Date.now()}`
      const reported = points.map((point, index) => ({
        id: `tp-${orderId}-${Date.now()}-${index}`,
        orderId,
        longitude: point.longitude,
        latitude: point.latitude,
        accuracy: point.accuracy,
        speed: point.speed,
        heading: point.heading,
        sequenceNo: (passengerState.trajectories[orderId]?.length || 0) + index + 1,
        reportedAt: point.reportedAt || nowText(),
        clientReportId: point.clientReportId || `${orderId}-${batchId}-${index}`,
        batchId,
        reportIntervalSeconds: Number(options.reportIntervalSeconds || point.reportIntervalSeconds || 5),
        retryCount: Number(point.retryCount || 0)
      }))
      passengerState.trajectories[orderId] = [...(passengerState.trajectories[orderId] || []), ...reported]
      return asyncReturn(reported)
    },

    flushLocationQueue() {
      passengerState.locationReportQueue = []
      return asyncReturn([])
    },

    acceptMatchRequest(requestId) {
      const account = getDriverAccount()
      if (Number(account?.credit || 0) < 60 || account?.status === '限制接单' || account?.status === '已封禁') {
        return asyncReturn(null)
      }

      return passengerApi.acceptMatchRequest(requestId)
    },

    rejectMatchRequest(requestId, reason = '司机当前无法接单') {
      return passengerApi.rejectMatchRequest(requestId, reason)
    },

    invitePassenger(candidate) {
      return passengerApi.sendMatchRequest({
        passengerOrderId: candidate.passengerOrderId,
        driverTripId: candidate.driverTripId
      })
    },

    startTrip(orderId) {
      return passengerApi.startTrip(orderId)
    },

    finishTrip(orderId) {
      return passengerApi.finishTrip(orderId)
    },

    refreshAll() {
      return asyncReturn(true)
    },

    getConversation(conversationId) {
      return asyncReturn(normalizeConversation(storeGetConversation(passengerState, conversationId, 'driver-001')))
    },

    getConversationMessages(conversationId) {
      return asyncReturn(
        sortByCreatedAt(storeListConversationMessages(passengerState, conversationId, 'driver-001')).map(normalizeMessage)
      )
    },

    sendMessage(conversationId, request) {
      return asyncReturn(normalizeMessage(storeSendConversationMessage(passengerState, conversationId, 'driver-001', request)))
    },

    markConversationRead(conversationId) {
      return asyncReturn(normalizeConversation(storeMarkConversationRead(passengerState, conversationId, 'driver-001')))
    },

    getConversationProposals(conversationId) {
      return asyncReturn(
        sortByNewestVersion(storeListNegotiationProposals(passengerState, conversationId, 'driver-001')).map(normalizeProposal)
      )
    },

    createProposal(conversationId, request) {
      return asyncReturn(normalizeProposal(storeCreateNegotiationProposal(passengerState, conversationId, 'driver-001', request)))
    },

    confirmProposal(conversationId, version) {
      return asyncReturn(normalizeProposal(storeConfirmNegotiationProposal(passengerState, conversationId, 'driver-001', version)))
    }
  }
}

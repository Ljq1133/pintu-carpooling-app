<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import {
  Car,
  ChevronRight,
  CheckCircle2,
  ClipboardList,
  CreditCard,
  MapPin,
  MessageCircle,
  PlugZap,
  Puzzle,
  ShieldCheck,
  Star,
  UserRound
} from 'lucide-vue-next'
import MatchCard from '../ui/MatchCard.vue'
import OrderCard from '../ui/OrderCard.vue'
import NegotiationPanel from '../Negotiation/NegotiationPanel.vue'
import PrimaryButton from '../ui/PrimaryButton.vue'
import {
  DRIVER_CREDIT_MIN,
  MATCH_REQUEST_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  REVIEW_STATUS,
  passengerApiNeeds,
  passengerIntegrationChecklist,
  passengerStatusContract
} from '../../api/passengerApi'
import { useSharedPlatform } from '../../platform/sharedPlatform.js'
import { mapApi } from '../../api/mapApi.js'

const activeTab = ref('ride')
const { passengerState: state, passengerApi } = useSharedPlatform()
const latestOrderId = ref('')
const ridePassengerOrderId = ref('')
const selectedPassengerOrderId = ref('')
const detailPassengerOrderId = ref('')
const activeOrderFilter = ref('ongoing')
const formError = ref('')
const orderError = ref('')
const rideError = ref('')
const isTimePickerOpen = ref(false)
const departureMode = ref('now')
const pickerBaseline = ref(new Date())
const reviewRating = ref(5)
const reviewComment = ref('司机很准时，路线也顺路')
const isRideReviewOpen = ref(false)
const mapSearchMessage = ref('地图已根据当前位置初始化')
const locationStatus = ref('idle')
const poiSuggestions = ref([])
const priceEstimate = ref(null)
const publishingOrder = ref(false)
const startPoint = reactive({ lng: null, lat: null })
const endPoint = reactive({ lng: null, lat: null })

const rideForm = reactive({
  start: state.currentLocation.address,
  end: 'SOHO',
  passengerCount: 1,
  note: '希望准时出发'
})

const pad = (value) => String(value).padStart(2, '0')

const weekLabels = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function formatDateValue(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateTime(date) {
  return `${formatDateValue(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function getInitialPickerTime() {
  const date = new Date()
  return getRoundedFutureTime(date)
}

function getRoundedFutureTime(baseDate) {
  const date = new Date(baseDate)
  const roundedMinute = Math.ceil(date.getMinutes() / 5) * 5
  if (roundedMinute >= 60) {
    date.setHours(date.getHours() + 1)
    date.setMinutes(0)
  } else {
    date.setMinutes(roundedMinute)
  }
  date.setSeconds(0, 0)
  return date
}

const initialPickerTime = getInitialPickerTime()
const selectedDate = ref(formatDateValue(initialPickerTime))
const selectedHour = ref(pad(initialPickerTime.getHours()))
const selectedMinute = ref(pad(initialPickerTime.getMinutes()))
const pickerDate = ref(selectedDate.value)
const pickerHour = ref(selectedHour.value)
const pickerMinute = ref(selectedMinute.value)

const tabIcons = {
  ride: Car,
  matches: Puzzle,
  orders: ClipboardList,
  profile: UserRound,
  integration: PlugZap
}

const activeTabInfo = computed(() => state.tabs.find((tab) => tab.key === activeTab.value) || state.tabs[0])

const phoneNavItems = computed(() => state.tabs.filter((tab) => tab.key !== 'integration'))

const dateOptions = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() + index)
    const monthDay = `${date.getMonth() + 1}月${pad(date.getDate())}日`
    const dayLabel = index === 0 ? '今天' : weekLabels[date.getDay()]
    return {
      value: formatDateValue(date),
      label: `${monthDay} ${dayLabel}`
    }
  })
})

const pickerDateOptions = computed(() => [
  {
    value: 'now',
    label: '现在出发',
    subLabel: ''
  },
  ...dateOptions.value.map((option) => ({
    ...option,
    subLabel: ''
  }))
])

const hourOptions = Array.from({ length: 24 }, (_, hour) => pad(hour))

const minuteOptions = Array.from({ length: 12 }, (_, index) => pad(index * 5))

const selectedDateLabel = computed(
  () => dateOptions.value.find((option) => option.value === selectedDate.value)?.label || selectedDate.value
)

const scheduledDepartureTime = computed(() => `${selectedDate.value} ${selectedHour.value}:${selectedMinute.value}`)

const departureTimeLabel = computed(() => {
  if (departureMode.value === 'now') return '现在出发'
  return `${selectedDateLabel.value} ${selectedHour.value}:${selectedMinute.value}`
})

const draftDepartureTime = computed(() =>
  departureMode.value === 'now' ? formatDateTime(new Date()) : scheduledDepartureTime.value
)

const latestPassengerOrder = computed(() => {
  const selectedOrder = state.passengerOrders.find((order) => order.id === latestOrderId.value)
  if (selectedOrder) return selectedOrder
  return [...state.passengerOrders].sort((left, right) =>
    String(right.createdAt || '').localeCompare(String(left.createdAt || ''))
  )[0] || null
})

const draftOrder = computed(() => ({
  start: rideForm.start,
  end: rideForm.end,
  departureTime: draftDepartureTime.value,
  passengerCount: Number(rideForm.passengerCount || 1),
  note: rideForm.note,
  status: '待匹配'
}))

const previewTrips = ref([])
const recommendedTrips = ref([])

async function refreshPreviewTrips() {
  previewTrips.value = await passengerApi.getRecommendedTrips(draftOrder.value)
}

async function refreshRecommendedTrips() {
  recommendedTrips.value = latestPassengerOrder.value
    ? await passengerApi.getRecommendedTrips(latestPassengerOrder.value.id)
    : []
}

const firstTrip = computed(() => recommendedTrips.value[0] || previewTrips.value[0])

watch(draftOrder, refreshPreviewTrips, { immediate: true })
watch(latestPassengerOrder, refreshRecommendedTrips, { immediate: true })
watch(
  () => state.driverTrips.map((trip) => `${trip.id}:${trip.status}:${trip.availableSeats}`).join('|'),
  () => {
    refreshPreviewTrips()
    refreshRecommendedTrips()
  }
)

const hasPassengerOrders = computed(() => state.passengerOrders.length > 0)

const orderFilters = [
  {
    key: 'ongoing',
    label: '进行中'
  },
  {
    key: 'completed',
    label: '已完成'
  },
  {
    key: 'cancelled',
    label: '已取消'
  }
]

const ongoingStatuses = [
  ORDER_STATUS.MATCHING,
  ORDER_STATUS.NEGOTIATING,
  ORDER_STATUS.CONFIRMED,
  ORDER_STATUS.IN_PROGRESS,
  ORDER_STATUS.PENDING_PAYMENT
]

const orderSummaries = computed(() =>
  state.passengerOrders.map((order) => {
    const record = state.orderRecords.find((item) => item.passengerOrderId === order.id)
    const requests = state.matchRequests.filter((request) => request.passengerOrderId === order.id)
    const timeline = record
      ? record.timeline
      : [
          {
            status: order.status,
            time: order.createdAt,
            description:
              order.status === ORDER_STATUS.CANCELLED
                ? order.cancelReason || '乘客取消订单'
                : '乘客发布拼车需求'
          }
        ]

    return {
      passengerOrderId: order.id,
      recordId: record?.id || '',
      start: record?.start || order.start,
      end: record?.end || order.end,
      route: record?.route || `${order.start} -> ${order.end}`,
      departureTime: record?.departureTime || order.departureTime,
      passengerCount: record?.passengerCount || order.passengerCount,
      status: record?.status || order.status,
      driverName: record?.driverName || '',
      driverPhone: record?.driverPhone || '',
      vehicle: record?.vehicle || '',
      price: record?.price || 0,
      paymentStatus: record?.paymentStatus || PAYMENT_STATUS.UNPAID,
      reviewStatus: record?.reviewStatus || REVIEW_STATUS.UNREVIEWED,
      canPay: Boolean(record?.canPay),
      live: record?.live || null,
      paidAt: record?.paidAt || '',
      cancelReason: record?.cancelReason || order.cancelReason || '',
      review: record?.review || null,
      createdAt: record?.createdAt || order.createdAt,
      timeline,
      matchRequests: requests
    }
  })
)

const filteredOrderSummaries = computed(() =>
  orderSummaries.value.filter((order) => {
    if (activeOrderFilter.value === 'completed') return order.status === ORDER_STATUS.COMPLETED
    if (activeOrderFilter.value === 'cancelled') return order.status === ORDER_STATUS.CANCELLED
    return ongoingStatuses.includes(order.status)
  })
)

const filteredOrderCards = computed(() =>
  filteredOrderSummaries.value.map((order) => ({
    summary: order,
    card: {
      date: order.departureTime.slice(5),
      route: order.route,
      status: order.status,
      count: `${order.passengerCount} 人同行`,
      price: order.price ? order.price : '待定',
      actions:
        order.status === ORDER_STATUS.MATCHING
          ? ['查看进程', '\u91cd\u65b0\u9009\u62e9']
          : order.status === ORDER_STATUS.NEGOTIATING
            ? ['查看进程', '\u8054\u7cfb\u53f8\u673a']
            : order.status === ORDER_STATUS.CONFIRMED
              ? ['查看进程', '\u8054\u7cfb\u53f8\u673a']
              : order.status === ORDER_STATUS.IN_PROGRESS
                ? ['\u8054\u7cfb\u53f8\u673a', '\u5bfc\u822a']
                : order.status === ORDER_STATUS.PENDING_PAYMENT
                  ? ['\u8ba2\u5355\u8be6\u60c5', '\u8054\u7cfb\u53f8\u673a']
                  : order.status === ORDER_STATUS.COMPLETED
                    ? ['\u8ba2\u5355\u8be6\u60c5', '\u518d\u6b21\u62fc\u8f66']
                    : ['\u8ba2\u5355\u8be6\u60c5', '\u518d\u6b21\u62fc\u8f66']
    }
  }))
)

const selectedOrderSummary = computed(
  () =>
    orderSummaries.value.find((order) => order.passengerOrderId === detailPassengerOrderId.value) || null
)

const rideOrderSummary = computed(() => {
  if (!ridePassengerOrderId.value) return null
  return orderSummaries.value.find((order) => order.passengerOrderId === ridePassengerOrderId.value) || null
})

const rideRequests = computed(() => rideOrderSummary.value?.matchRequests || [])

const pendingRideRequest = computed(
  () => rideRequests.value.find((request) => request.status === MATCH_REQUEST_STATUS.PENDING) || null
)

const rejectedRideRequest = computed(
  () => rideRequests.value.find((request) => request.status === MATCH_REQUEST_STATUS.REJECTED) || null
)

const acceptedRideRequest = computed(
  () => rideRequests.value.find((request) => request.status === MATCH_REQUEST_STATUS.ACCEPTED) || null
)

const activeRideRequest = computed(
  () => acceptedRideRequest.value || pendingRideRequest.value || rejectedRideRequest.value || null
)

const rideConversationId = computed(() => activeRideRequest.value?.conversationId || '')

const rideSessionActive = computed(() =>
  [ORDER_STATUS.MATCHING, ORDER_STATUS.NEGOTIATING, ORDER_STATUS.CONFIRMED, ORDER_STATUS.IN_PROGRESS, ORDER_STATUS.PENDING_PAYMENT].includes(
    rideOrderSummary.value?.status
  )
)

const rideNegotiationConversation = ref(null)
const rideNegotiationMessages = ref([])
const rideNegotiationProposals = ref([])
const rideNegotiationLoading = ref(false)
const rideNegotiationError = ref('')
let rideNegotiationTimer = null

const rideLatestProposal = computed(() => rideNegotiationProposals.value[0] || null)

const rideProposalSeed = computed(() => ({
  price: rideLatestProposal.value?.price ?? rideOrderSummary.value?.price ?? priceEstimate.value?.total ?? '',
  departureTime: rideLatestProposal.value?.departureTime ?? rideOrderSummary.value?.departureTime ?? scheduledDepartureTime.value,
  passengerCount: rideLatestProposal.value?.passengerCount ?? rideOrderSummary.value?.passengerCount ?? rideForm.passengerCount ?? 1,
  pickupLocation: rideLatestProposal.value?.pickupLocation ?? rideOrderSummary.value?.start ?? rideForm.start,
  note: rideLatestProposal.value?.note ?? rideOrderSummary.value?.cancelReason ?? rideForm.note
}))

function clearRideNegotiationTimer() {
  if (rideNegotiationTimer) {
    clearInterval(rideNegotiationTimer)
    rideNegotiationTimer = null
  }
}

async function refreshRideNegotiation({ markRead = false } = {}) {
  const conversationId = rideConversationId.value
  if (!conversationId) {
    rideNegotiationConversation.value = null
    rideNegotiationMessages.value = []
    rideNegotiationProposals.value = []
    rideNegotiationError.value = ''
    return
  }

  rideNegotiationLoading.value = true
  try {
    const [conversation, messages, proposals] = await Promise.all([
      passengerApi.getConversation(conversationId),
      passengerApi.getConversationMessages(conversationId),
      passengerApi.getConversationProposals(conversationId)
    ])
    rideNegotiationConversation.value = conversation
    rideNegotiationMessages.value = messages
    rideNegotiationProposals.value = proposals
    rideNegotiationError.value = ''
    if (markRead) {
      await passengerApi.markConversationRead(conversationId)
    }
  } catch (error) {
    rideNegotiationError.value = error.message || '协商状态同步失败'
  } finally {
    rideNegotiationLoading.value = false
  }
}

async function refreshRideSession() {
  await passengerApi.refreshState?.()
  await refreshRideNegotiation()
}

function syncRideSessionTimer() {
  clearRideNegotiationTimer()
  if (!rideSessionActive.value) return
  rideNegotiationTimer = globalThis.setInterval(() => {
    void refreshRideSession()
  }, 5000)
}

watch(rideSessionActive, () => {
  syncRideSessionTimer()
  void refreshRideSession()
}, { immediate: true })

watch(rideConversationId, () => {
  void refreshRideNegotiation({ markRead: true })
}, { immediate: true })

onBeforeUnmount(() => {
  clearRideNegotiationTimer()
})

const rideRequestTrip = computed(() => {
  const request = activeRideRequest.value
  if (!request) return null
  return state.driverTrips.find((trip) => trip.id === request.driverTripId) || null
})

const rideFlowStep = computed(() => {
  if (!rideOrderSummary.value) return 'form'
  if (rideOrderSummary.value.status === ORDER_STATUS.COMPLETED) return 'paid'
  if (rideOrderSummary.value.status === ORDER_STATUS.PENDING_PAYMENT || rideOrderSummary.value.status === ORDER_STATUS.IN_PROGRESS) return 'live'
  if (rideOrderSummary.value.status === ORDER_STATUS.CONFIRMED) return 'confirmed'
  if (rideOrderSummary.value.status === ORDER_STATUS.NEGOTIATING) {
    if (acceptedRideRequest.value) return 'negotiating'
    if (pendingRideRequest.value) return 'waiting'
  }
  if (pendingRideRequest.value) return 'waiting'
  if (rejectedRideRequest.value) return 'rejected'
  return 'form'
})

const rideStatusLabel = computed(() => {
  if (!rideOrderSummary.value) return ORDER_STATUS.MATCHING
  if (rideOrderSummary.value.status === ORDER_STATUS.COMPLETED) return ORDER_STATUS.COMPLETED
  if (rideOrderSummary.value.status === ORDER_STATUS.CANCELLED) return ORDER_STATUS.CANCELLED
  if (rideOrderSummary.value.status === ORDER_STATUS.NEGOTIATING) {
    if (acceptedRideRequest.value) return ORDER_STATUS.NEGOTIATING
    if (pendingRideRequest.value) return '待确认'
  }
  if (rideOrderSummary.value.status === ORDER_STATUS.CONFIRMED) return ORDER_STATUS.CONFIRMED
  if (rideOrderSummary.value.status === ORDER_STATUS.PENDING_PAYMENT) return ORDER_STATUS.PENDING_PAYMENT
  if (rideOrderSummary.value.status === ORDER_STATUS.IN_PROGRESS) return ORDER_STATUS.IN_PROGRESS
  if (pendingRideRequest.value) return '待确认'
  if (rejectedRideRequest.value) return ORDER_STATUS.MATCHING
  return ORDER_STATUS.CONFIRMED
})

const canPayRideOrder = computed(() =>
  rideOrderSummary.value?.recordId &&
  rideOrderSummary.value.status === ORDER_STATUS.PENDING_PAYMENT &&
  rideOrderSummary.value.canPay &&
  rideOrderSummary.value.paymentStatus === PAYMENT_STATUS.UNPAID
)

const canReviewRideOrder = computed(() =>
  rideOrderSummary.value?.recordId &&
  rideOrderSummary.value.status === ORDER_STATUS.COMPLETED &&
  rideOrderSummary.value.paymentStatus === PAYMENT_STATUS.PAID &&
  rideOrderSummary.value.reviewStatus === REVIEW_STATUS.UNREVIEWED
)

const stateCards = computed(() => [
  {
    label: '演示乘客',
    value: state.passenger.name,
    note: `${state.passenger.username} / 123456`
  },
  {
    label: '司机行程 mock',
    value: state.driverTrips.length,
    note: `信用门槛 ${DRIVER_CREDIT_MIN} 分`
  },
  {
    label: '乘客订单',
    value: state.passengerOrders.length,
    note: '含待匹配需求和正式订单'
  },
  {
    label: '正式订单',
    value: state.orderRecords.length,
    note: 'mock 司机同意后生成'
  }
])

const setupChecklist = [
  '页面动作统一调用 src/api/passengerApi.js',
  '当前 API 入口默认接 passengerApi.mock.js',
  '真实接口完成后优先替换 adapter，不改页面主流程'
]

function validateRideForm() {
  if (!rideForm.start.trim()) return '请填写出发地'
  if (!rideForm.end.trim()) return '请填写目的地'
  if (Number(rideForm.passengerCount) < 1) return '乘车人数不能小于 1'
  if (departureMode.value !== 'now') {
    const selected = new Date(scheduledDepartureTime.value.replace(' ', 'T'))
    if (Number.isNaN(selected.getTime()) || selected <= new Date()) return '预约时间必须晚于当前时间'
  }
  return ''
}

async function locateCurrentPosition() {
  locationStatus.value = 'loading'
  mapSearchMessage.value = '正在定位当前位置...'
  try {
    const point = await mapApi.locate()
    rideForm.start = point.name || point.address
    startPoint.lng = point.lng
    startPoint.lat = point.lat
    locationStatus.value = 'success'
    mapSearchMessage.value = `定位成功 · ${point.address}`
    await refreshPriceEstimate()
    formError.value = ''
  } catch (error) {
    locationStatus.value = 'error'
    mapSearchMessage.value = '定位失败，可输入地址后重新搜索'
    formError.value = error.message
  }
}

async function searchRideLocation(type) {
  const keyword = type === 'start' ? rideForm.start.trim() : rideForm.end.trim()
  if (!keyword) {
    formError.value = type === 'start' ? '请先输入出发地' : '请先输入目的地'
    return
  }
  try {
    mapSearchMessage.value = `正在搜索「${keyword}」...`
    const points = await mapApi.searchPois(keyword, startPoint.lng, startPoint.lat)
    poiSuggestions.value = points
    const point = points[0]
    if (!point) throw new Error('未找到相关地点')
    if (type === 'start') {
      rideForm.start = point.name
      startPoint.lng = point.lng
      startPoint.lat = point.lat
    } else {
      rideForm.end = point.name
      endPoint.lng = point.lng
      endPoint.lat = point.lat
    }
    mapSearchMessage.value = `已选中 ${point.name} · ${point.address}`
    await refreshPriceEstimate()
    formError.value = ''
  } catch (error) {
    mapSearchMessage.value = `未能定位「${keyword}」`
    formError.value = error.message
  }
}

async function refreshPriceEstimate() {
  if ([startPoint.lng, startPoint.lat, endPoint.lng, endPoint.lat].some((value) => value == null)) return
  try {
    priceEstimate.value = await mapApi.estimate({
      startLng: startPoint.lng,
      startLat: startPoint.lat,
      endLng: endPoint.lng,
      endLat: endPoint.lat,
      passengerCount: Number(rideForm.passengerCount || 1)
    })
  } catch {
    priceEstimate.value = null
  }
}

async function publishPassengerOrder() {
  if (publishingOrder.value) return
  const error = validateRideForm()
  if (error) {
    formError.value = error
    return
  }

  try {
    publishingOrder.value = true
    if (startPoint.lng == null) await searchRideLocation('start')
    if (endPoint.lng == null) await searchRideLocation('end')
    if ([startPoint.lng, startPoint.lat, endPoint.lng, endPoint.lat].some((value) => value == null)) return
    const orderPayload = {
      ...rideForm,
      startLng: startPoint.lng,
      startLat: startPoint.lat,
      endLng: endPoint.lng,
      endLat: endPoint.lat,
      departureTime: departureMode.value === 'now' ? formatDateTime(new Date()) : scheduledDepartureTime.value
    }
    const order = await passengerApi.createPassengerOrder(orderPayload)
    if (!order) {
      formError.value = state.apiError || '当前账号信用分过低或已被限制发单'
      return
    }
    latestOrderId.value = order.id
    ridePassengerOrderId.value = order.id
    selectedPassengerOrderId.value = order.id
    activeOrderFilter.value = 'ongoing'
    detailPassengerOrderId.value = ''
    isRideReviewOpen.value = false
    rideError.value = ''
    formError.value = ''
    await refreshRecommendedTrips()
    activeTab.value = 'matches'
  } finally {
    publishingOrder.value = false
  }
}

watch(() => rideForm.passengerCount, refreshPriceEstimate)

function openTimePicker() {
  pickerBaseline.value = new Date()
  if (departureMode.value === 'now') {
    pickerDate.value = 'now'
    pickerHour.value = ''
    pickerMinute.value = ''
  } else {
    pickerDate.value = selectedDate.value
    pickerHour.value = selectedHour.value
    pickerMinute.value = selectedMinute.value
  }
  isTimePickerOpen.value = true
}

function makePickerDateTime(dateValue, hourValue, minuteValue) {
  return new Date(`${dateValue}T${hourValue}:${minuteValue}:00`)
}

function isPastHourFor(dateValue, hourValue) {
  const latestMinuteInHour = makePickerDateTime(dateValue, hourValue, '55')
  return latestMinuteInHour < getRoundedFutureTime(pickerBaseline.value)
}

function isPastMinuteFor(dateValue, hourValue, minuteValue) {
  return makePickerDateTime(dateValue, hourValue, minuteValue) < getRoundedFutureTime(pickerBaseline.value)
}

function getFirstAvailableHour(dateValue) {
  return hourOptions.find((hour) => !isPastHourFor(dateValue, hour)) || '00'
}

function getFirstAvailableMinute(dateValue, hourValue) {
  return minuteOptions.find((minute) => !isPastMinuteFor(dateValue, hourValue, minute)) || '00'
}

function isPastHour(hour) {
  if (pickerDate.value === 'now') return true
  return isPastHourFor(pickerDate.value, hour)
}

function isPastMinute(minute) {
  if (pickerDate.value === 'now') return true
  return isPastMinuteFor(pickerDate.value, pickerHour.value, minute)
}

function selectPickerDate(value) {
  pickerDate.value = value
  if (value === 'now') {
    pickerHour.value = ''
    pickerMinute.value = ''
    return
  }

  pickerHour.value = getFirstAvailableHour(value)
  pickerMinute.value = getFirstAvailableMinute(value, pickerHour.value)
}

function selectPickerHour(hour) {
  if (isPastHour(hour)) return
  pickerHour.value = hour
  pickerMinute.value = getFirstAvailableMinute(pickerDate.value, hour)
}

function selectPickerMinute(minute) {
  if (isPastMinute(minute)) return
  pickerMinute.value = minute
}

function confirmScheduledTime() {
  if (pickerDate.value === 'now') {
    departureMode.value = 'now'
    isTimePickerOpen.value = false
    return
  }

  if (isPastHour(pickerHour.value)) pickerHour.value = getFirstAvailableHour(pickerDate.value)
  if (isPastMinute(pickerMinute.value)) pickerMinute.value = getFirstAvailableMinute(pickerDate.value, pickerHour.value)

  selectedDate.value = pickerDate.value
  selectedHour.value = pickerHour.value
  selectedMinute.value = pickerMinute.value
  departureMode.value = 'scheduled'
  isTimePickerOpen.value = false
}

function matchRequestForTrip(tripId) {
  const passengerOrderId = latestPassengerOrder.value?.id
  if (!passengerOrderId) return null
  return state.matchRequests.find(
    (request) =>
      request.passengerOrderId === passengerOrderId &&
      request.driverTripId === tripId &&
      [MATCH_REQUEST_STATUS.PENDING, MATCH_REQUEST_STATUS.ACCEPTED].includes(request.status)
  ) || null
}

function tripRequestActionLabel(tripId) {
  const request = matchRequestForTrip(tripId)
  if (request?.status === MATCH_REQUEST_STATUS.ACCEPTED) return '查看协商方案'
  if (request?.status === MATCH_REQUEST_STATUS.PENDING) return '等待司机确认'
  return '发送拼车请求'
}

function isTripRequestPending(tripId) {
  return matchRequestForTrip(tripId)?.status === MATCH_REQUEST_STATUS.PENDING
}

function handleTripRequest(tripId) {
  const request = matchRequestForTrip(tripId)
  if (request?.status === MATCH_REQUEST_STATUS.ACCEPTED) {
    const passengerOrderId = latestPassengerOrder.value?.id
    if (!passengerOrderId) return
    goToRideOrder(passengerOrderId)
    void refreshRideSession()
    return
  }
  if (request?.status === MATCH_REQUEST_STATUS.PENDING) return
  void requestTrip(tripId)
}

async function requestTrip(tripId) {
  if (!latestPassengerOrder.value) return
  const request = await passengerApi.sendMatchRequest({
    passengerOrderId: latestPassengerOrder.value.id,
    driverTripId: tripId
  })
  if (request) {
    ridePassengerOrderId.value = latestPassengerOrder.value.id
    selectedPassengerOrderId.value = latestPassengerOrder.value.id
    detailPassengerOrderId.value = ''
    activeOrderFilter.value = 'ongoing'
    activeTab.value = 'ride'
    rideError.value = ''
    orderError.value = ''
  }
}

function requestCountForOrder(orderId) {
  return state.matchRequests.filter((request) => request.passengerOrderId === orderId).length
}

function makeMatchCard(trip) {
  return {
    route: trip.route,
    time: trip.departureTime.slice(5),
    seats: `${trip.availableSeats} ???`,
    price: trip.price,
    score: `${trip.overlap}%`,
    driverName: trip.driverName,
    vehicle: trip.vehicle,
    driverCredit: trip.driverCredit
  }
}

async function sendRideNegotiationMessage(content) {
  if (!rideConversationId.value) return null
  const message = await passengerApi.sendMessage(rideConversationId.value, { content })
  await refreshRideNegotiation({ markRead: true })
  return message
}

async function createRideNegotiationProposal(payload) {
  if (!rideConversationId.value) return null
  const proposal = await passengerApi.createProposal(rideConversationId.value, payload)
  await refreshRideNegotiation({ markRead: true })
  return proposal
}

async function confirmRideNegotiationProposal(version) {
  if (!rideConversationId.value) return null
  const proposal = await passengerApi.confirmProposal(rideConversationId.value, version)
  await refreshRideSession()
  return proposal
}

function selectOrder(passengerOrderId) {
  selectedPassengerOrderId.value = passengerOrderId
  const order = orderSummaries.value.find((item) => item.passengerOrderId === passengerOrderId)
  if (order && ongoingStatuses.includes(order.status)) {
    goToRideOrder(passengerOrderId)
    return
  }
  detailPassengerOrderId.value = ''
  orderError.value = ''
}

function selectOrderFilter(filterKey) {
  activeOrderFilter.value = filterKey
  const firstOrder = filteredOrderSummaries.value[0]
  if (firstOrder) selectedPassengerOrderId.value = firstOrder.passengerOrderId
  detailPassengerOrderId.value = ''
  orderError.value = ''
}

function syncOrderFilter(status) {
  if (status === ORDER_STATUS.COMPLETED) activeOrderFilter.value = 'completed'
  else if (status === ORDER_STATUS.CANCELLED) activeOrderFilter.value = 'cancelled'
  else activeOrderFilter.value = 'ongoing'
}

async function payRideOrder() {
  if (!rideOrderSummary.value?.recordId) return
  const orderRecord = await passengerApi.payOrder(rideOrderSummary.value.recordId)
  if (!orderRecord) {
    rideError.value = '???????????'
    return
  }
  ridePassengerOrderId.value = orderRecord.passengerOrderId
  selectedPassengerOrderId.value = orderRecord.passengerOrderId
  detailPassengerOrderId.value = ''
  isRideReviewOpen.value = false
  syncOrderFilter(orderRecord.status)
  rideError.value = ''
}

async function submitRideReview() {
  if (!rideOrderSummary.value?.recordId) return
  const orderRecord = await passengerApi.submitReview(rideOrderSummary.value.recordId, {
    rating: reviewRating.value,
    comment: reviewComment.value
  })
  if (!orderRecord) {
    rideError.value = '????????????'
    return
  }
  isRideReviewOpen.value = false
  syncOrderFilter(orderRecord.status)
  rideError.value = ''
}

function goToRideOrder(passengerOrderId) {
  ridePassengerOrderId.value = passengerOrderId
  selectedPassengerOrderId.value = passengerOrderId
  detailPassengerOrderId.value = ''
  activeTab.value = 'ride'
  rideError.value = ''
  orderError.value = ''
}

function chooseAnotherDriver() {
  if (rideOrderSummary.value) {
    latestOrderId.value = rideOrderSummary.value.passengerOrderId
    selectedPassengerOrderId.value = rideOrderSummary.value.passengerOrderId
  }
  detailPassengerOrderId.value = ''
  activeTab.value = 'matches'
  rideError.value = ''
}

function resetRideFlow() {
  ridePassengerOrderId.value = ''
  latestOrderId.value = ''
  detailPassengerOrderId.value = ''
  isRideReviewOpen.value = false
  rideError.value = ''
}

function viewCompletedOrder() {
  if (!rideOrderSummary.value) return
  activeOrderFilter.value = 'completed'
  detailPassengerOrderId.value = ''
  selectedPassengerOrderId.value = rideOrderSummary.value.passengerOrderId
  activeTab.value = 'orders'
}

function openOrderDetail(passengerOrderId) {
  const order = orderSummaries.value.find((item) => item.passengerOrderId === passengerOrderId)
  if (order && ongoingStatuses.includes(order.status)) {
    goToRideOrder(passengerOrderId)
    return
  }
  selectedPassengerOrderId.value = passengerOrderId
  detailPassengerOrderId.value = passengerOrderId
  orderError.value = ''
}

function handleOrderAction(order, action) {
  if (ongoingStatuses.includes(order.status)) {
    if (action === '\u91cd\u65b0\u9009\u62e9') {
      goToRideOrder(order.passengerOrderId)
      chooseAnotherDriver()
      return
    }
    goToRideOrder(order.passengerOrderId)
    return
  }

  if (
    action === '\u8ba2\u5355\u8be6\u60c5' ||
    action === '\u5bfc\u822a' ||
    action === '\u884c\u7a0b\u7801' ||
    action === '\u67e5\u770b\u8fdb\u7a0b' ||
    action === '\u8054\u7cfb\u53f8\u673a'
  ) {
    openOrderDetail(order.passengerOrderId)
    return
  }

  if (action === '\u518d\u6b21\u62fc\u8f66') {
    rideForm.start = order.start
    rideForm.end = order.end
    rideForm.passengerCount = order.passengerCount
    rideForm.note = '\u518d\u6b21\u62fc\u8fd9\u6761\u8def\u7ebf'
    activeTab.value = 'ride'
    return
  }

  if (action === '\u8054\u7cfb\u53f8\u673a') {
    openOrderDetail(order.passengerOrderId)
  }
}

</script>

<template>
  <section class="Passenger-app">
    <div class="Passenger-appStage">
      <div class="Passenger-phoneColumn">
        <div class="Passenger-phoneShell">
          <div class="Passenger-statusBar">
            <span>9:41</span>
            <div>
              <i></i>
              <i></i>
              <i></i>
            </div>
          </div>

          <div class="Passenger-phoneScreen">
            <header class="Passenger-appHeader">
              <div>
                <span>PinTu 乘客端</span>
                <strong>{{ state.passenger.name }}，今天拼哪一程？</strong>
              </div>
              <div class="Passenger-creditBadge">
                <ShieldCheck :size="15" />
                {{ state.passenger.credit }}
              </div>
            </header>

            <main class="Passenger-appContent">
              <section v-if="activeTab === 'ride'" class="Passenger-mobileView">
                <template v-if="rideFlowStep === 'form'">
                  <article class="Passenger-mapPreview">
                    <div>
                      <MapPin :size="22" />
                      <span>{{ mapSearchMessage }}</span>
                    </div>
                    <strong>{{ rideForm.start || '选择出发地' }} -> {{ rideForm.end || '选择目的地' }}</strong>
                    <div class="Passenger-mapRouteLine" aria-hidden="true">
                      <i></i>
                      <i></i>
                      <i></i>
                    </div>
                  </article>

                  <form class="Passenger-formCard" @submit.prevent="publishPassengerOrder">
                    <div class="Passenger-routePicker">
                      <div class="Passenger-routeField">
                        <span>出发地</span>
                        <div class="Passenger-routeSelect">
                          <i class="start"></i>
                          <input v-model.trim="rideForm.start" aria-label="出发地" list="Passenger-pois" placeholder="选择上车点" @change="searchRideLocation('start')" />
                          <button type="button" :aria-label="locationStatus === 'error' ? '重新定位' : '使用当前位置'" @click="locateCurrentPosition">
                            <MapPin :size="17" />
                          </button>
                        </div>
                      </div>
                      <div class="Passenger-routeField">
                        <span>目的地</span>
                        <div class="Passenger-routeSelect">
                          <i></i>
                          <input v-model.trim="rideForm.end" aria-label="目的地" list="Passenger-pois" placeholder="你要去哪儿" @change="searchRideLocation('end')" />
                          <button type="button" aria-label="搜索目的地" @click="searchRideLocation('end')">
                            <ChevronRight :size="17" />
                          </button>
                        </div>
                      </div>
                      <datalist id="Passenger-pois">
                        <option v-for="point in poiSuggestions" :key="`${point.lng}-${point.lat}`" :value="point.name">{{ point.address }}</option>
                      </datalist>
                    </div>
                    <label class="Passenger-timeField">
                      <span>出发时间</span>
                      <button class="Passenger-timeButton" type="button" @click="openTimePicker">
                        <strong>{{ departureTimeLabel }}</strong>
                      </button>
                    </label>
                    <label>
                      <span>乘车人数</span>
                      <input v-model.number="rideForm.passengerCount" aria-label="乘车人数" min="1" type="number" />
                    </label>
                    <label class="Passenger-noteField">
                      <span>备注</span>
                      <textarea v-model="rideForm.note" aria-label="备注" rows="2"></textarea>
                    </label>
                    <p v-if="formError" class="Passenger-formError">{{ formError }}</p>
                    <p v-if="priceEstimate" class="Passenger-priceEstimate">
                      预计 {{ priceEstimate.distanceKm }} km / {{ priceEstimate.durationMinutes }} 分钟
                      <strong>¥ {{ priceEstimate.total }}</strong>
                    </p>

                    <PrimaryButton class="Passenger-submitButton" :label="publishingOrder ? '发布中...' : '发布拼车需求'" type="submit" />
                  </form>
                </template>

                <article v-else-if="rideFlowStep === 'waiting'" class="Passenger-flowCard">
                  <header>
                    <div>
                      <span>正在等待司机确认</span>
                      <strong>{{ pendingRideRequest?.driverName }}</strong>
                    </div>
                    <b>{{ rideStatusLabel }}</b>
                  </header>
                  <div class="Passenger-waitingAnimation" aria-hidden="true">
                    <i></i>
                    <i></i>
                    <i></i>
                  </div>
                  <p>{{ rideOrderSummary.route }} · {{ rideOrderSummary.departureTime.slice(5) }}</p>
                  <p v-if="rideRequestTrip">{{ rideRequestTrip.vehicle }} · 预计 ¥ {{ rideRequestTrip.price }}</p>
                  <p v-if="rideError" class="Passenger-formError">{{ rideError }}</p>
                  <div v-if="pendingRideRequest" class="Passenger-mockActionBox">
                    <span>请求已发送给司机</span>
                    <p>司机确认后，行程信息会自动更新。</p>
                  </div>
                </article>

                <article v-else-if="rideFlowStep === 'negotiating'" class="Passenger-flowCard negotiation-stageCard">
                  <header>
                    <div>
                      <span>司机已接单，正在协商细节</span>
                      <strong>{{ acceptedRideRequest?.driverName || rideOrderSummary.driverName || '待确认司机' }}</strong>
                    </div>
                    <b>{{ rideStatusLabel }}</b>
                  </header>
                  <p>{{ rideOrderSummary.route }} · {{ rideOrderSummary.departureTime.slice(5) }}</p>
                  <p v-if="rideRequestTrip">{{ rideRequestTrip.vehicle }} · 建议价 ¥ {{ rideRequestTrip.price }}</p>
                  <p v-if="rideNegotiationError || rideError" class="Passenger-formError">{{ rideNegotiationError || rideError }}</p>
                  <NegotiationPanel
                    title="拼车协商"
                    subtitle="乘客与司机"
                    :conversation="rideNegotiationConversation"
                    :messages="rideNegotiationMessages"
                    :proposals="rideNegotiationProposals"
                    peer-label="司机"
                    self-label="乘客"
                    self-id="passenger-001"
                    :status-label="rideStatusLabel"
                    :default-proposal="rideProposalSeed"
                    :can-message="rideNegotiationConversation?.status === ORDER_STATUS.NEGOTIATING"
                    :can-propose="rideNegotiationConversation?.status === ORDER_STATUS.NEGOTIATING"
                    :can-confirm="rideNegotiationConversation?.status === ORDER_STATUS.NEGOTIATING"
                    confirm-label="确认当前方案"
                    @send-message="sendRideNegotiationMessage"
                    @create-proposal="createRideNegotiationProposal"
                    @confirm-proposal="confirmRideNegotiationProposal"
                    @refresh="refreshRideSession"
                  />
                  <p v-if="rideNegotiationLoading" class="Passenger-supportNote">正在同步协商记录...</p>
                </article>

                <article v-else-if="rideFlowStep === 'confirmed'" class="Passenger-flowCard">
                  <header>
                    <div>
                      <span>方案已确认，等待司机出发</span>
                      <strong>{{ rideOrderSummary.driverName }} · {{ rideOrderSummary.vehicle }}</strong>
                    </div>
                    <b>{{ rideStatusLabel }}</b>
                  </header>
                  <p>上车点与时间已经锁定，司机开始行程后，这里会自动切换到实时行程。</p>
                  <div class="Passenger-tripSummary">
                    <div>
                      <span>路线</span>
                      <strong>{{ rideOrderSummary.route }}</strong>
                    </div>
                    <div>
                      <span>出发时间</span>
                      <strong>{{ rideOrderSummary.departureTime }}</strong>
                    </div>
                    <div>
                      <span>费用</span>
                      <strong>¥ {{ rideOrderSummary.price }}</strong>
                    </div>
                    <div>
                      <span>司机电话</span>
                      <strong>{{ rideOrderSummary.driverPhone }}</strong>
                    </div>
                  </div>
                  <NegotiationPanel
                    title="确认后的协商记录"
                    subtitle="已锁定版本"
                    :conversation="rideNegotiationConversation"
                    :messages="rideNegotiationMessages"
                    :proposals="rideNegotiationProposals"
                    peer-label="司机"
                    self-label="乘客"
                    self-id="passenger-001"
                    :status-label="rideStatusLabel"
                    :default-proposal="rideProposalSeed"
                    :can-message="false"
                    :can-propose="false"
                    :can-confirm="false"
                    confirm-label="方案已确认"
                    @send-message="sendRideNegotiationMessage"
                    @create-proposal="createRideNegotiationProposal"
                    @confirm-proposal="confirmRideNegotiationProposal"
                    @refresh="refreshRideSession"
                  />
                </article>

                <article v-else-if="rideFlowStep === 'rejected'" class="Passenger-flowCard">
                  <header>
                    <div>
                      <span>司机暂未接单</span>
                      <strong>{{ rejectedRideRequest?.driverName }} 拒绝了这次拼车请求</strong>
                    </div>
                    <b>{{ rideStatusLabel }}</b>
                  </header>
                  <p>{{ rejectedRideRequest?.rejectReason || '可以重新选择其他推荐司机。' }}</p>
                  <button class="Passenger-primaryAction" type="button" @click="chooseAnotherDriver">
                    重新选择司机
                    <ChevronRight :size="16" />
                  </button>
                </article>

                <article v-else-if="rideFlowStep === 'live'" class="Passenger-liveRideCard">
                  <header>
                    <div>
                      <span>实时行程</span>
                      <strong>{{ rideOrderSummary.driverName }} · {{ rideOrderSummary.live?.etaText }}</strong>
                    </div>
                    <b>{{ rideStatusLabel }}</b>
                  </header>
                  <div class="Passenger-mapMock">
                    <MapPin :size="22" />
                    <span>{{ rideOrderSummary.live?.mapText }}</span>
                  </div>
                  <div class="Passenger-liveGrid">
                    <span>司机位置：{{ rideOrderSummary.live?.driverLocation }}</span>
                    <span>距离：{{ rideOrderSummary.live?.distanceText }}</span>
                    <span>车辆：{{ rideOrderSummary.vehicle }}</span>
                    <span>预计费用：¥ {{ rideOrderSummary.price }}</span>
                    <span>路线：{{ rideOrderSummary.route }}</span>
                  </div>
                  <p v-if="rideError" class="Passenger-formError">{{ rideError }}</p>
                  <div v-if="!canPayRideOrder" class="Passenger-mockActionBox">
                    <span>行程状态实时更新</span>
                    <p>司机确认开始或送达后，当前页面会自动同步。</p>
                  </div>
                  <button v-if="canPayRideOrder" class="Passenger-primaryAction" type="button" @click="payRideOrder">
                    <CreditCard :size="15" />
                    确认付款
                  </button>
                </article>

                <article v-else class="Passenger-paymentDoneCard">
                  <CheckCircle2 :size="34" />
                  <div>
                    <span>付款完成</span>
                    <strong>{{ rideOrderSummary.route }}</strong>
                    <p>{{ rideOrderSummary.departureTime.slice(5) }} · {{ rideOrderSummary.driverName }} · ¥ {{ rideOrderSummary.price }}</p>
                  </div>
                  <div class="Passenger-completeActions">
                    <button v-if="canReviewRideOrder" type="button" @click="isRideReviewOpen = !isRideReviewOpen">
                      <Star :size="15" />
                      去评价
                    </button>
                    <button type="button" @click="resetRideFlow">返回乘车</button>
                    <button type="button" @click="viewCompletedOrder">查看订单</button>
                  </div>
                  <div v-if="isRideReviewOpen && canReviewRideOrder" class="Passenger-reviewBox">
                    <span>推荐评价</span>
                    <div class="Passenger-ratingGroup">
                      <button
                        v-for="rating in 5"
                        :key="rating"
                        :class="{ active: reviewRating === rating }"
                        type="button"
                        @click="reviewRating = rating"
                      >
                        <Star :size="13" />
                        {{ rating }}
                      </button>
                    </div>
                    <textarea v-model="reviewComment" rows="2" aria-label="订单评价"></textarea>
                    <button type="button" @click="submitRideReview">
                      <Star :size="15" />
                      提交评价
                    </button>
                  </div>
                  <div v-if="rideOrderSummary.review" class="Passenger-reviewResult">
                    <span>评价结果</span>
                    <strong>{{ rideOrderSummary.review.rating }} 星</strong>
                    <p>{{ rideOrderSummary.review.comment }}</p>
                  </div>
                </article>
              </section>

              <section v-else-if="activeTab === 'matches'" class="Passenger-mobileView">
                <div class="Passenger-screenTitle">
                  <span>推荐司机</span>
                  <strong v-if="latestPassengerOrder">{{ recommendedTrips.length }} 条推荐</strong>
                  <strong v-else>先发布需求</strong>
                </div>

                <div v-if="!latestPassengerOrder" class="Passenger-emptyState">
                  <Puzzle :size="34" />
                  <strong>先发布拼车需求</strong>
                  <p>在乘车页填写出发地、目的地、时间和人数后，这里会显示推荐司机。</p>
                </div>

                <div v-else-if="recommendedTrips.length === 0" class="Passenger-emptyState">
                  <Puzzle :size="34" />
                  <strong>暂无符合规则的司机</strong>
                  <p>当前规则会过滤满座、顺路度低于 60%、信用分低于 {{ DRIVER_CREDIT_MIN }} 的司机。</p>
                </div>

                <template v-else>
                  <article v-for="trip in recommendedTrips" :key="trip.id" class="Passenger-matchCardWrap">
                    <MatchCard :match="makeMatchCard(trip)" />
                    <button :disabled="isTripRequestPending(trip.id)" @click="handleTripRequest(trip.id)">
                      {{ tripRequestActionLabel(trip.id) }}
                    </button>
                  </article>
                </template>
              </section>

              <section v-else-if="activeTab === 'orders'" class="Passenger-mobileView">
                <div class="Passenger-orderTabs">
                  <button
                    v-for="filter in orderFilters"
                    :key="filter.key"
                    :class="{ active: activeOrderFilter === filter.key }"
                    type="button"
                    @click="selectOrderFilter(filter.key)"
                  >
                    {{ filter.label }}
                  </button>
                </div>

                <div v-if="!hasPassengerOrders" class="Passenger-emptyState">
                  <ClipboardList :size="34" />
                  <strong>还没有乘客订单</strong>
                  <p>发布拼车需求并发起请求后，这里会显示简洁订单卡片。</p>
                </div>

                <template v-else-if="filteredOrderCards.length > 0">
                  <article
                    v-for="item in filteredOrderCards"
                    :key="item.summary.passengerOrderId"
                    :class="{ active: selectedPassengerOrderId === item.summary.passengerOrderId }"
                    class="Passenger-orderCardWrap"
                    role="button"
                    tabindex="0"
                    @click="selectOrder(item.summary.passengerOrderId)"
                    @keydown.enter="selectOrder(item.summary.passengerOrderId)"
                    @keydown.space.prevent="selectOrder(item.summary.passengerOrderId)"
                  >
                    <OrderCard :order="item.card" @action="handleOrderAction(item.summary, $event)" />
                  </article>

                  <article v-if="selectedOrderSummary" class="Passenger-orderDetailCard">
                    <header>
                      <div>
                        <span>行程摘要</span>
                        <strong>{{ selectedOrderSummary.route }}</strong>
                      </div>
                      <b>{{ selectedOrderSummary.status }}</b>
                    </header>

                    <div class="Passenger-tripSummary">
                      <div>
                        <span>出发地</span>
                        <strong>{{ selectedOrderSummary.start }}</strong>
                      </div>
                      <div>
                        <span>目的地</span>
                        <strong>{{ selectedOrderSummary.end }}</strong>
                      </div>
                      <div>
                        <span>出发时间</span>
                        <strong>{{ selectedOrderSummary.departureTime }}</strong>
                      </div>
                      <div>
                        <span>同行人数</span>
                        <strong>{{ selectedOrderSummary.passengerCount }} 人</strong>
                      </div>
                      <div v-if="selectedOrderSummary.driverName">
                        <span>司机</span>
                        <strong>{{ selectedOrderSummary.driverName }}</strong>
                      </div>
                      <div v-if="selectedOrderSummary.vehicle">
                        <span>车辆</span>
                        <strong>{{ selectedOrderSummary.vehicle }}</strong>
                      </div>
                      <div v-if="selectedOrderSummary.price">
                        <span>费用</span>
                        <strong>¥ {{ selectedOrderSummary.price }}</strong>
                      </div>
                      <div>
                        <span>支付</span>
                        <strong>{{ selectedOrderSummary.paymentStatus }}</strong>
                      </div>
                    </div>

                    <p v-if="orderError" class="Passenger-formError">{{ orderError }}</p>

                    <div v-if="selectedOrderSummary.review" class="Passenger-reviewResult">
                      <span>评价结果</span>
                      <strong>{{ selectedOrderSummary.review.rating }} 星</strong>
                      <p>{{ selectedOrderSummary.review.comment }}</p>
                    </div>
                    <div v-if="selectedOrderSummary.cancelReason" class="Passenger-reviewResult">
                      <span>取消原因</span>
                      <p>{{ selectedOrderSummary.cancelReason }}</p>
                    </div>
                  </article>
                </template>

                <div v-else class="Passenger-emptyState">
                  <ClipboardList :size="34" />
                  <strong>当前分组暂无订单</strong>
                  <p>切换到其他状态分组，或先完成一次乘客端订单流程。</p>
                </div>
              </section>

              <section v-else-if="activeTab === 'profile'" class="Passenger-mobileView">
                <div class="Passenger-profileHead">
                  <div class="Passenger-avatar">
                    <UserRound :size="26" />
                  </div>
                  <div>
                    <strong>{{ state.passenger.name }}</strong>
                    <span>{{ state.passenger.phone }}</span>
                  </div>
                </div>

                <div class="Passenger-statStrip">
                  <div>
                    <strong>{{ state.passenger.tripCount }}</strong>
                    <span>出行</span>
                  </div>
                  <div>
                    <strong>{{ state.passenger.savedMoney }}</strong>
                    <span>节省</span>
                  </div>
                  <div>
                    <strong>{{ state.passenger.carbonSaved }}</strong>
                    <span>减碳</span>
                  </div>
                </div>

                <button class="Passenger-menuItem"><CreditCard :size="17" /> 我的钱包 <ChevronRight :size="16" /></button>
                <button class="Passenger-menuItem"><MessageCircle :size="17" /> 消息通知 <ChevronRight :size="16" /></button>
                <button class="Passenger-menuItem"><Star :size="17" /> 我的评价 <ChevronRight :size="16" /></button>
              </section>

              <section v-else class="Passenger-mobileView">
                <div class="Passenger-integrationCard">
                  <PlugZap :size="30" />
                  <strong>联调准备</strong>
                  <p>演示账号：{{ state.passenger.username }} / 123456</p>
                  <p>页面动作已统一走 src/api/passengerApi.js，当前默认接 mock adapter。</p>
                </div>

                <div class="Passenger-integrationSection">
                  <span>接口需求</span>
                  <div class="Passenger-apiRows">
                    <div v-for="item in passengerApiNeeds" :key="item.path" class="Passenger-apiRow">
                      <b>{{ item.method }}</b>
                      <div>
                        <strong>{{ item.path }}</strong>
                        <p>{{ item.desc }}</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="Passenger-integrationSection">
                  <span>状态字段</span>
                  <div class="Passenger-statusRows">
                    <div v-for="item in passengerStatusContract" :key="item.label">
                      <strong>{{ item.label }}</strong>
                      <p>{{ item.value }}</p>
                    </div>
                  </div>
                </div>

                <div class="Passenger-integrationSection">
                  <span>A / C 联调检查</span>
                  <ul class="Passenger-checklist compact">
                    <li v-for="item in passengerIntegrationChecklist" :key="item">{{ item }}</li>
                  </ul>
                </div>
              </section>
            </main>

            <div v-if="isTimePickerOpen" class="Passenger-pickerOverlay" role="dialog" aria-label="选择出发时间">
              <section class="Passenger-timePicker">
                <header>
                  <div>
                    <span>出发时间</span>
                    <strong>选择日期和时间</strong>
                  </div>
                  <button type="button" @click="isTimePickerOpen = false">关闭</button>
                </header>

                <div class="Passenger-pickerColumns">
                  <div class="Passenger-pickerColumn">
                    <span>日期</span>
                    <div class="Passenger-pickerList">
                      <button
                        v-for="option in pickerDateOptions"
                        :key="option.value"
                        :class="{ active: pickerDate === option.value }"
                        type="button"
                        @click="selectPickerDate(option.value)"
                      >
                        <strong>{{ option.label }}</strong>
                        <small v-if="option.subLabel">{{ option.subLabel }}</small>
                      </button>
                    </div>
                  </div>

                  <div class="Passenger-pickerColumn">
                    <span>小时</span>
                    <div class="Passenger-pickerList">
                      <button v-if="pickerDate === 'now'" class="active Passenger-dashOption" disabled type="button">
                        -
                      </button>
                      <template v-else>
                        <button
                          v-for="hour in hourOptions"
                          :key="hour"
                          :class="{ active: pickerHour === hour, disabled: isPastHour(hour) }"
                          :disabled="isPastHour(hour)"
                          type="button"
                          @click="selectPickerHour(hour)"
                        >
                          {{ Number(hour) }}点
                        </button>
                      </template>
                    </div>
                  </div>

                  <div class="Passenger-pickerColumn">
                    <span>分钟</span>
                    <div class="Passenger-pickerList">
                      <button v-if="pickerDate === 'now'" class="active Passenger-dashOption" disabled type="button">
                        -
                      </button>
                      <template v-else>
                        <button
                          v-for="minute in minuteOptions"
                          :key="minute"
                          :class="{ active: pickerMinute === minute, disabled: isPastMinute(minute) }"
                          :disabled="isPastMinute(minute)"
                          type="button"
                          @click="selectPickerMinute(minute)"
                        >
                          {{ Number(minute) }}分
                        </button>
                      </template>
                    </div>
                  </div>
                </div>

                <footer>
                  <button type="button" @click="isTimePickerOpen = false">取消</button>
                  <button type="button" @click="confirmScheduledTime">确定</button>
                </footer>
              </section>
            </div>

            <nav class="Passenger-bottomNav" aria-label="乘客端底部导航">
              <button
                v-for="tab in phoneNavItems"
                :key="tab.key"
                :class="{ active: activeTab === tab.key }"
                @click="activeTab = tab.key"
              >
                <component :is="tabIcons[tab.key]" :size="17" />
                <span>{{ tab.label }}</span>
              </button>
            </nav>
          </div>

          <div class="Passenger-homeIndicator"></div>
        </div>
      </div>

    </div>
  </section>
</template>

<style scoped>
.Passenger-app {
  width: 100%;
}

.Passenger-tabbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 22px;
  padding: 8px;
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-tabbar button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #757575;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.Passenger-tabbar button.active {
  background: #111111;
  color: #FFFFFF;
}

.Passenger-appStage {
  display: block;
}

.Passenger-phoneColumn {
  display: grid;
  justify-items: center;
  width: 100%;
}

.Passenger-phoneLabel {
  width: min(100%, 360px);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #757575;
  font-size: 12px;
  font-weight: 800;
}

.Passenger-phoneLabel strong {
  color: #111111;
}

.Passenger-phoneShell {
  width: min(100%, 430px);
  min-height: 720px;
  padding: 16px 14px 22px;
  border: 1px solid #E3E5E5;
  border-radius: 26px;
  background: #FFFFFF;
  box-shadow: 0 22px 60px rgba(17, 17, 17, 0.12);
}

.Passenger-statusBar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  color: #111111;
  font-size: 12px;
  font-weight: 800;
}

.Passenger-statusBar div {
  display: flex;
  gap: 4px;
}

.Passenger-statusBar i {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #111111;
}

.Passenger-phoneScreen {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 680px;
  max-height: calc(100vh - 156px);
  overflow: hidden;
  border-radius: 24px;
  background: #F7F8F7;
}

.Passenger-appHeader {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 22px 18px 14px;
}

.Passenger-appHeader div:first-child {
  display: grid;
  gap: 6px;
}

.Passenger-appHeader span,
.Passenger-formCard span,
.Passenger-screenTitle span,
.Passenger-tripCard span,
.Passenger-panelHeader span,
.Passenger-stateCard span,
.Passenger-location span,
.Passenger-miniTrip span {
  color: #757575;
  font-size: 12px;
}

.Passenger-appHeader strong {
  max-width: 210px;
  color: #111111;
  font-size: 20px;
  line-height: 1.25;
}

.Passenger-creditBadge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 0 9px;
  border-radius: 999px;
  background: #111111;
  color: #39FF14;
  font-size: 12px;
  font-weight: 900;
}

.Passenger-appContent {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 14px 76px;
}

.Passenger-mobileView {
  display: grid;
  gap: 12px;
}

.Passenger-mapPreview,
.Passenger-formCard,
.Passenger-tripCard,
.Passenger-matchCardWrap,
.Passenger-flowCard,
.Passenger-paymentDoneCard,
.Passenger-orderDetailCard,
.Passenger-emptyState,
.Passenger-profileHead,
.Passenger-statStrip,
.Passenger-menuItem,
.Passenger-integrationCard {
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-emptyState p,
.Passenger-integrationCard p {
  margin: 0;
  color: #757575;
  font-size: 12px;
  line-height: 1.6;
}

.Passenger-mapPreview {
  display: grid;
  gap: 10px;
  min-height: 170px;
  padding: 14px;
  background:
    linear-gradient(90deg, rgba(227, 229, 229, 0.7) 1px, transparent 1px),
    linear-gradient(0deg, rgba(227, 229, 229, 0.7) 1px, transparent 1px),
    #FFFFFF;
  background-size: 30px 30px;
}

.Passenger-mapPreview > div:first-child {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #555555;
  font-size: 12px;
  font-weight: 800;
}

.Passenger-mapPreview svg {
  color: #39FF14;
}

.Passenger-mapPreview strong {
  align-self: center;
  color: #111111;
  font-size: 18px;
  line-height: 1.25;
}

.Passenger-mapRouteLine {
  display: grid;
  grid-template-columns: 10px 1fr 10px;
  gap: 8px;
  align-items: center;
}

.Passenger-mapRouteLine i {
  display: block;
  height: 10px;
  border-radius: 999px;
  background: #39FF14;
}

.Passenger-mapRouteLine i:nth-child(2) {
  height: 3px;
  background: #111111;
}

.Passenger-formCard {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.Passenger-formCard label {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 10px;
  align-items: center;
  min-height: 36px;
}

.Passenger-timeField {
  align-items: start;
}

.Passenger-routePicker {
  display: grid;
  gap: 10px;
}

.Passenger-routeField {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.Passenger-routeField > span {
  color: #757575;
  font-size: 12px;
}

.Passenger-routeSelect {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 9px;
  align-items: center;
  min-width: 0;
  min-height: 54px;
  padding: 0 14px;
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #F7F8F7;
  color: #111111;
  text-align: left;
}

.Passenger-routeSelect i {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: #FF4055;
}

.Passenger-routeSelect i.start {
  background: #39FF14;
}

.Passenger-routeSelect strong {
  min-width: 0;
  overflow: hidden;
  color: #111111;
  font-size: 17px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.Passenger-routeSelect strong.muted {
  color: #757575;
}

.Passenger-routeSelect svg {
  color: #757575;
}

.Passenger-routeSelect input {
  min-height: 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  font-size: 17px;
  font-weight: 700;
}

.Passenger-routeSelect input:focus {
  box-shadow: none;
}

.Passenger-routeSelect button {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #ffffff;
}

.Passenger-priceEstimate {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0;
  padding: 10px 12px;
  border-left: 3px solid #39FF14;
  background: #f7f8f7;
  color: #626862;
  font-size: 12px;
}

.Passenger-priceEstimate strong {
  color: #111111;
  font-size: 18px;
}

.Passenger-formCard input,
.Passenger-formCard textarea {
  width: 100%;
  min-width: 0;
  border: 1px solid #E3E5E5;
  border-radius: 7px;
  background: #F7F8F7;
  color: #111111;
  font: inherit;
  outline: 0;
}

.Passenger-formCard input {
  min-height: 36px;
  padding: 0 10px;
}

.Passenger-formCard textarea {
  padding: 9px 10px;
  resize: none;
}

.Passenger-timeButton {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid #E3E5E5;
  border-radius: 7px;
  background: #F7F8F7;
  color: #111111;
  text-align: left;
}

.Passenger-timeButton strong {
  font-size: 15px;
}

.Passenger-noteField {
  grid-template-columns: 1fr;
  align-items: stretch;
}

.Passenger-submitButton.primary-button {
  width: 100%;
  min-height: 46px;
  margin-top: 2px;
  border-radius: 8px;
}

.Passenger-formError {
  margin: 0;
  padding: 9px 10px;
  border-radius: 7px;
  background: #111111;
  color: #39FF14;
  font-size: 12px;
  font-weight: 900;
}

.Passenger-primaryAction,
.Passenger-tripCard button,
.Passenger-matchCardWrap button,
.Passenger-actionStack > button,
.Passenger-mockActionBox button,
.Passenger-reviewBox > button,
.Passenger-completeActions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  border: 0;
  border-radius: 8px;
  background: #39FF14;
  color: #111111;
  font-weight: 900;
}

.Passenger-tripCard button:disabled,
.Passenger-matchCardWrap button:disabled {
  background: #EEEEEE;
  color: #757575;
  cursor: default;
}

.Passenger-pickerOverlay {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: flex-end;
  padding: 12px;
  background: rgba(17, 17, 17, 0.46);
}

.Passenger-timePicker {
  display: grid;
  gap: 14px;
  width: 100%;
  max-height: 560px;
  padding: 14px;
  border-radius: 18px;
  background: #F6F7F8;
  box-shadow: 0 18px 40px rgba(17, 17, 17, 0.18);
}

.Passenger-timePicker header,
.Passenger-timePicker footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 0 2px;
}

.Passenger-timePicker header div {
  display: grid;
  gap: 4px;
}

.Passenger-timePicker header span {
  color: #757575;
  font-size: 11px;
}

.Passenger-pickerColumn > span {
  display: none;
}

.Passenger-timePicker header strong {
  color: #111111;
  font-size: 16px;
}

.Passenger-timePicker header button,
.Passenger-timePicker footer button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: #FFFFFF;
  color: #111111;
  font-weight: 900;
}

.Passenger-timePicker footer button:last-child {
  border-color: #39FF14;
  background: #39FF14;
  color: #111111;
}

.Passenger-pickerColumns {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(58px, 0.7fr) minmax(58px, 0.7fr);
  gap: 0;
  padding: 8px;
  border-radius: 16px;
  background: #FFFFFF;
  box-shadow: inset 0 0 0 1px #F0F1F2;
}

.Passenger-pickerColumn {
  display: grid;
  min-width: 0;
}

.Passenger-pickerList {
  display: grid;
  gap: 2px;
  max-height: 188px;
  overflow: auto;
  padding: 0 2px;
  scrollbar-width: none;
}

.Passenger-pickerList::-webkit-scrollbar {
  display: none;
}

.Passenger-pickerList button {
  display: grid;
  place-items: center;
  gap: 1px;
  min-height: 38px;
  padding: 6px 4px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #A6A6A6;
  font-size: 14px;
  font-weight: 900;
  line-height: 1.1;
  white-space: nowrap;
}

.Passenger-pickerList button small {
  color: inherit;
  font-size: 9px;
  font-weight: 700;
  opacity: 0.72;
}

.Passenger-pickerList button.active {
  background: #F0F1F3;
  color: #292D33;
  font-size: 16px;
}

.Passenger-pickerList button:disabled {
  opacity: 1;
}

.Passenger-pickerList button.disabled {
  color: #D4D4D4;
  cursor: default;
}

.Passenger-dashOption {
  min-height: 38px;
}

.Passenger-screenTitle {
  display: flex;
  justify-content: space-between;
  align-items: end;
}

.Passenger-screenTitle strong {
  font-size: 18px;
}

.Passenger-tripCard {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.Passenger-matchCardWrap {
  display: grid;
  gap: 10px;
  padding: 10px;
}

.Passenger-tripCard > div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.Passenger-tripCard p {
  margin: 0;
  font-weight: 900;
}

.Passenger-tripMeta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  color: #757575;
  font-size: 12px;
}

.Passenger-tripMeta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.Passenger-tripMeta b {
  color: #12A900;
  white-space: nowrap;
}

.Passenger-orderTabs {
  display: flex;
  justify-content: space-around;
  gap: 8px;
}

.Passenger-orderTabs button {
  flex: 1;
  padding-bottom: 8px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: transparent;
  color: #757575;
  font: inherit;
  font-size: 13px;
  font-weight: 900;
}

.Passenger-orderTabs button.active {
  color: #111111;
  border-bottom: 3px solid #39FF14;
}

.Passenger-emptyState {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 210px;
  padding: 24px;
  text-align: center;
}

.Passenger-emptyState svg {
  color: #39FF14;
}

.Passenger-orderCardWrap {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.Passenger-orderCardWrap.active {
  outline: 2px solid rgba(57, 255, 20, 0.72);
  outline-offset: 2px;
}

.Passenger-orderDetailCard {
  display: grid;
  gap: 12px;
  padding: 14px;
}

.Passenger-orderDetailCard header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
}

.Passenger-orderDetailCard header div {
  display: grid;
  gap: 5px;
}

.Passenger-orderDetailCard header span,
.Passenger-requestList > span,
.Passenger-mockActionBox span,
.Passenger-reviewBox span,
.Passenger-reviewResult span {
  color: #757575;
  font-size: 12px;
  font-weight: 800;
}

.Passenger-orderDetailCard header strong {
  color: #111111;
  line-height: 1.35;
}

.Passenger-orderDetailCard header b {
  flex: 0 0 auto;
  padding: 6px 8px;
  border-radius: 6px;
  background: #111111;
  color: #39FF14;
  font-size: 12px;
}

.Passenger-liveRideCard {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-flowCard,
.Passenger-paymentDoneCard {
  display: grid;
  gap: 12px;
  padding: 14px;
}

.Passenger-flowCard header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.Passenger-flowCard header div,
.Passenger-paymentDoneCard > div:first-of-type {
  display: grid;
  gap: 5px;
}

.Passenger-flowCard header span,
.Passenger-paymentDoneCard span {
  color: #757575;
  font-size: 12px;
  font-weight: 800;
}

.Passenger-flowCard header b {
  flex: 0 0 auto;
  padding: 6px 8px;
  border-radius: 6px;
  background: #111111;
  color: #39FF14;
  font-size: 12px;
}

.Passenger-flowCard p,
.Passenger-paymentDoneCard p {
  margin: 0;
  color: #757575;
  font-size: 12px;
  line-height: 1.5;
}

.Passenger-waitingAnimation {
  display: flex;
  justify-content: center;
  gap: 8px;
  min-height: 78px;
  align-items: center;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-waitingAnimation i {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #39FF14;
  animation: Passenger-waitingPulse 1s ease-in-out infinite;
}

.Passenger-waitingAnimation i:nth-child(2) {
  animation-delay: 0.15s;
}

.Passenger-waitingAnimation i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes Passenger-waitingPulse {
  0%,
  100% {
    transform: scale(0.72);
    opacity: 0.45;
  }

  50% {
    transform: scale(1);
    opacity: 1;
  }
}

.Passenger-paymentDoneCard > svg {
  color: #39FF14;
}

.Passenger-completeActions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.Passenger-completeActions button {
  min-height: 38px;
  padding: 0 8px;
  font-size: 12px;
}

.Passenger-completeActions button:nth-child(2),
.Passenger-completeActions button:nth-child(3) {
  background: #F7F8F8;
}

.Passenger-liveRideCard.compact {
  padding: 0;
  border: 0;
  background: transparent;
}

.Passenger-liveRideCard header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
}

.Passenger-liveRideCard header div {
  display: grid;
  gap: 5px;
}

.Passenger-liveRideCard header span {
  color: #757575;
  font-size: 12px;
  font-weight: 800;
}

.Passenger-liveRideCard header strong {
  color: #111111;
  line-height: 1.35;
}

.Passenger-liveRideCard header b {
  flex: 0 0 auto;
  padding: 6px 8px;
  border-radius: 6px;
  background: #111111;
  color: #39FF14;
  font-size: 12px;
}

.Passenger-mapMock {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 118px;
  padding: 16px;
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(227, 229, 229, 0.6) 1px, transparent 1px),
    linear-gradient(0deg, rgba(227, 229, 229, 0.6) 1px, transparent 1px),
    #F7F8F8;
  background-size: 28px 28px;
  color: #111111;
  text-align: center;
  font-size: 13px;
  font-weight: 900;
}

.Passenger-mapMock svg {
  color: #39FF14;
}

.Passenger-liveGrid,
.Passenger-detailGrid,
.Passenger-tripSummary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.Passenger-liveGrid span,
.Passenger-detailGrid div,
.Passenger-tripSummary div {
  min-height: 28px;
  padding: 8px;
  border-radius: 7px;
  background: #F7F8F8;
  color: #555555;
  font-size: 12px;
  font-weight: 800;
}

.Passenger-liveGrid span:last-child,
.Passenger-detailGrid div:nth-child(6),
.Passenger-tripSummary div:nth-child(6) {
  grid-column: 1 / -1;
}

.Passenger-detailGrid div,
.Passenger-tripSummary div {
  display: grid;
  gap: 4px;
}

.Passenger-detailGrid span,
.Passenger-tripSummary span {
  color: #757575;
  font-size: 11px;
}

.Passenger-detailGrid strong,
.Passenger-tripSummary strong {
  color: #111111;
  line-height: 1.35;
}

.Passenger-timeline {
  display: grid;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-timeline > div {
  display: grid;
  grid-template-columns: 12px 1fr;
  gap: 8px;
}

.Passenger-timeline i {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 999px;
  background: #39FF14;
  box-shadow: 0 0 0 4px rgba(57, 255, 20, 0.16);
}

.Passenger-timeline strong {
  display: block;
  color: #111111;
  font-size: 13px;
}

.Passenger-timeline span,
.Passenger-timeline p,
.Passenger-requestList p,
.Passenger-reviewResult p {
  margin: 3px 0 0;
  color: #757575;
  font-size: 12px;
  line-height: 1.5;
}

.Passenger-requestList {
  display: grid;
  gap: 8px;
}

.Passenger-requestItem {
  display: grid;
  gap: 6px;
  padding: 10px;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-requestItem > div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.Passenger-requestItem b {
  color: #12A900;
  font-size: 12px;
}

.Passenger-miniActions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.Passenger-miniActions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  min-height: 34px;
  border: 0;
  border-radius: 7px;
  background: #FFFFFF;
  color: #111111;
  font-size: 12px;
  font-weight: 900;
}

.Passenger-actionStack {
  display: grid;
  gap: 9px;
}

.Passenger-actionStack > button:nth-child(2),
.Passenger-miniActions button:nth-child(2) {
  background: #111111;
  color: #FFFFFF;
}

.Passenger-mockActionBox,
.Passenger-reviewBox,
.Passenger-reviewResult {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-mockActionBox p {
  margin: 0;
  color: #757575;
  font-size: 12px;
  line-height: 1.5;
}

.Passenger-reviewBox textarea {
  width: 100%;
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #E3E5E5;
  border-radius: 7px;
  background: #FFFFFF;
  color: #111111;
  font: inherit;
  resize: none;
  outline: 0;
}

.Passenger-ratingGroup {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.Passenger-ratingGroup button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-height: 32px;
  border: 0;
  border-radius: 7px;
  background: #FFFFFF;
  color: #757575;
  font-size: 12px;
  font-weight: 900;
}

.Passenger-ratingGroup button.active {
  background: #111111;
  color: #39FF14;
}

.Passenger-reviewResult strong {
  color: #111111;
}

.Passenger-profileHead {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.Passenger-profileHead > div:last-child {
  display: grid;
  gap: 5px;
}

.Passenger-avatar {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border: 6px solid #39FF14;
  border-radius: 999px;
  background: #111111;
  color: #FFFFFF;
}

.Passenger-statStrip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: #111111;
  color: #FFFFFF;
}

.Passenger-statStrip div {
  display: grid;
  gap: 4px;
  padding: 14px 8px;
  text-align: center;
}

.Passenger-statStrip strong {
  color: #39FF14;
  font-size: 19px;
}

.Passenger-statStrip span {
  color: #FFFFFF;
  font-size: 12px;
}

.Passenger-menuItem {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  min-height: 46px;
  padding: 0 14px;
  color: #111111;
  text-align: left;
  font-weight: 800;
}

.Passenger-integrationCard {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.Passenger-integrationCard svg {
  color: #39FF14;
}

.Passenger-integrationSection {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-integrationSection > span,
.Passenger-apiRow p,
.Passenger-statusRows p {
  margin: 0;
  color: #757575;
  font-size: 12px;
  line-height: 1.45;
}

.Passenger-integrationSection > span {
  font-weight: 900;
  letter-spacing: 0;
}

.Passenger-apiRows,
.Passenger-statusRows {
  display: grid;
  gap: 8px;
}

.Passenger-apiRow {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 8px;
  padding: 9px;
  border-radius: 7px;
  background: #F7F8F8;
}

.Passenger-apiRow b {
  color: #111111;
  font-size: 11px;
}

.Passenger-apiRow strong,
.Passenger-statusRows strong {
  display: block;
  overflow-wrap: anywhere;
  color: #111111;
  font-size: 12px;
}

.Passenger-statusRows > div {
  display: grid;
  gap: 3px;
  padding: 8px 0;
  border-bottom: 1px solid #E3E5E5;
}

.Passenger-statusRows > div:last-child {
  border-bottom: 0;
}

.Passenger-bottomNav {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  min-height: 62px;
  border-top: 1px solid #E3E5E5;
  background: #FFFFFF;
}

.Passenger-bottomNav button {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 4px;
  border: 0;
  background: transparent;
  color: #757575;
  font-size: 11px;
  font-weight: 800;
}

.Passenger-bottomNav button.active {
  color: #19D800;
}

.Passenger-bottomNav button.active svg {
  color: #39FF14;
}

.Passenger-homeIndicator {
  width: 96px;
  height: 4px;
  margin: 10px auto 0;
  border-radius: 999px;
  background: #111111;
}

.Passenger-sidePanels {
  display: grid;
  gap: 14px;
}

.Passenger-panel,
.Passenger-stateCard {
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-panel {
  padding: 18px;
}

.Passenger-panelHeader strong {
  display: block;
  margin-top: 6px;
  color: #111111;
  font-size: 24px;
}

.Passenger-panelHeader p,
.Passenger-stateCard p {
  margin: 8px 0 0;
  color: #757575;
  line-height: 1.5;
}

.Passenger-panelHeader.compact strong {
  font-size: 20px;
}

.Passenger-location {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding: 12px;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-location strong {
  display: block;
  margin-top: 3px;
}

.Passenger-stateGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.Passenger-stateCard {
  padding: 16px;
}

.Passenger-stateCard strong {
  display: block;
  margin-top: 8px;
  color: #111111;
  font-size: 26px;
}

.Passenger-checklist {
  display: grid;
  gap: 8px;
  margin: 14px 0 0;
  padding-left: 18px;
  color: #333333;
  line-height: 1.5;
}

.Passenger-checklist.compact {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
}

.Passenger-miniTrip {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  padding: 12px;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-miniTrip p {
  margin: 0;
  color: #757575;
  font-size: 13px;
}

@media (max-width: 760px) {
  .Passenger-app,
  .Passenger-appStage,
  .Passenger-phoneColumn,
  .Passenger-phoneShell,
  .Passenger-phoneScreen {
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }

  .Passenger-phoneShell {
    min-height: calc(100vh - 118px);
    padding: 0;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .Passenger-statusBar,
  .Passenger-homeIndicator {
    display: none;
  }

  .Passenger-phoneScreen {
    min-height: calc(100vh - 118px);
    max-height: none;
    border-radius: 0;
  }

  .Passenger-appStage,
  .Passenger-stateGrid {
    grid-template-columns: 1fr;
  }

  .Passenger-tabbar {
    overflow-x: auto;
    flex-wrap: nowrap;
  }
}
</style>

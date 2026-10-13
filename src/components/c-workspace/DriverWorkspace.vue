<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { CarFront, Check, Clock, Flag, MapPin, Play, Plus, ShieldCheck, Users, X } from 'lucide-vue-next'
import { MATCH_REQUEST_STATUS, ORDER_STATUS } from '../../api/passengerApi.js'
import { mapApi } from '../../api/mapApi.js'
import NegotiationPanel from '../Negotiation/NegotiationPanel.vue'

const props = defineProps({
  driverApi: {
    type: Object,
    required: true
  }
})

const profile = computed(() => props.driverApi.getProfile())
const vehicles = computed(() => props.driverApi.getVehicles())
const trips = computed(() => props.driverApi.getTrips())
const requests = computed(() => props.driverApi.getMatchRequests())
const recommendations = computed(() => props.driverApi.getRecommendations?.() || [])
const orders = computed(() => props.driverApi.getOrders())
const actionMessage = ref('')
const locationStatus = ref('idle')
const publishing = ref(false)
const estimate = ref(null)

const selectedRequestId = ref('')
const negotiationConversation = ref(null)
const negotiationMessages = ref([])
const negotiationProposals = ref([])
const negotiationLoading = ref(false)
const negotiationError = ref('')
let negotiationTimer = null

const pendingRequests = computed(() =>
  requests.value.filter((request) => request.status === MATCH_REQUEST_STATUS.PENDING)
)

const acceptedRequests = computed(() =>
  requests.value.filter((request) => request.status === MATCH_REQUEST_STATUS.ACCEPTED)
)

const selectedAcceptedRequest = computed(() => {
  if (selectedRequestId.value) {
    const selected = acceptedRequests.value.find((request) => request.id === selectedRequestId.value)
    if (selected) return selected
  }
  return acceptedRequests.value[0] || null
})

const selectedConversationId = computed(() => selectedAcceptedRequest.value?.conversationId || '')
const selectedTrip = computed(() => {
  if (!selectedAcceptedRequest.value) return null
  return trips.value.find((trip) => trip.id === selectedAcceptedRequest.value.driverTripId) || null
})

const latestProposal = computed(() => negotiationProposals.value[0] || null)
const proposalSeed = computed(() => ({
  price: latestProposal.value?.price ?? selectedTrip.value?.price ?? '',
  departureTime: latestProposal.value?.departureTime ?? selectedAcceptedRequest.value?.time ?? selectedTrip.value?.departureTime ?? '',
  passengerCount: latestProposal.value?.passengerCount ?? 1,
  pickupLocation: latestProposal.value?.pickupLocation ?? selectedTrip.value?.start ?? '',
  note: latestProposal.value?.note ?? selectedAcceptedRequest.value?.message ?? ''
}))

const pad = (value) => String(value).padStart(2, '0')
const futureTime = new Date(Date.now() + 30 * 60 * 1000)
const defaultDeparture = `${futureTime.getFullYear()}-${pad(futureTime.getMonth() + 1)}-${pad(futureTime.getDate())} ${pad(futureTime.getHours())}:${pad(futureTime.getMinutes())}`

const form = reactive({
  start: '同济科技园',
  end: 'SOHO',
  startLng: null,
  startLat: null,
  endLng: null,
  endLat: null,
  time: defaultDeparture,
  vehicle: vehicles.value.find((vehicle) => vehicle.status === '已认证')?.plate || '',
  seats: 3,
  price: 18.6
})

watch(
  vehicles,
  (items) => {
    if (!form.vehicle) {
      form.vehicle = items.find((vehicle) => vehicle.status === '已认证')?.plate || ''
    }
  },
  { immediate: true }
)

watch(
  acceptedRequests,
  (items) => {
    if (!items.length) {
      selectedRequestId.value = ''
      return
    }
    if (!items.some((request) => request.id === selectedRequestId.value)) {
      selectedRequestId.value = items[0].id
    }
  },
  { immediate: true }
)

async function publishTrip() {
  if (publishing.value) return
  const departure = new Date(form.time.replace(' ', 'T'))
  if (Number.isNaN(departure.getTime()) || departure <= new Date()) {
    actionMessage.value = '出发时间必须晚于当前时间'
    return
  }
  try {
    publishing.value = true
    await resolvePlace('start')
    await resolvePlace('end')
    const trip = await props.driverApi.createTrip(form)
    actionMessage.value = trip ? `已发布行程：${trip.start} -> ${trip.end}` : '请检查车辆认证、司机信用或表单信息'
  } catch (error) {
    actionMessage.value = error.message
  } finally {
    publishing.value = false
  }
}

async function locateStart() {
  locationStatus.value = 'loading'
  actionMessage.value = '正在定位...'
  try {
    const point = await mapApi.locate()
    form.start = point.name
    form.startLng = point.lng
    form.startLat = point.lat
    locationStatus.value = 'success'
    actionMessage.value = `定位成功：${point.address}`
    await updateEstimate()
  } catch (error) {
    locationStatus.value = 'error'
    actionMessage.value = `定位失败：${error.message}`
  }
}

async function resolvePlace(type) {
  const keyword = String(form[type] || '').trim()
  if (!keyword) throw new Error(type === 'start' ? '请输入出发地' : '请输入目的地')
  const lngKey = `${type}Lng`
  const latKey = `${type}Lat`
  if (form[lngKey] != null && form[latKey] != null) return
  const points = await mapApi.searchPois(keyword, form.startLng, form.startLat)
  const point = points[0]
  if (!point) throw new Error(`未找到地点：${keyword}`)
  form[type] = point.name
  form[lngKey] = point.lng
  form[latKey] = point.lat
  await updateEstimate()
}

async function updateEstimate() {
  if ([form.startLng, form.startLat, form.endLng, form.endLat].some((value) => value == null)) return
  estimate.value = await mapApi.estimate({
    startLng: form.startLng,
    startLat: form.startLat,
    endLng: form.endLng,
    endLat: form.endLat,
    passengerCount: 1
  })
  if (!form.price) form.price = estimate.value.total
}

async function updateRequest(id, status) {
  const result =
    status === MATCH_REQUEST_STATUS.ACCEPTED
      ? await props.driverApi.acceptMatchRequest(id)
      : await props.driverApi.rejectMatchRequest(id)
  if (result?.conversationId) {
    selectedRequestId.value = result.id
    await refreshNegotiation()
  }
  actionMessage.value = result
    ? `请求已${status === MATCH_REQUEST_STATUS.ACCEPTED ? '同意，进入协商' : '拒绝'}`
    : '该请求已处理'
}

async function invitePassenger(candidate) {
  const result = await props.driverApi.invitePassenger?.(candidate)
  actionMessage.value = result ? `已邀请 ${candidate.passengerName || '乘客'} 同行` : '邀请发送失败或已存在有效请求'
}

async function startTrip(orderId) {
  const order = await props.driverApi.startTrip(orderId)
  actionMessage.value = order ? '行程已开始' : '当前订单不能开始行程'
}

async function finishTrip(orderId) {
  const order = await props.driverApi.finishTrip(orderId)
  actionMessage.value = order ? '已确认送达，等待乘客付款' : '当前订单不能确认送达'
}

function selectAcceptedRequest(requestId) {
  selectedRequestId.value = requestId
}

function clearNegotiationTimer() {
  if (negotiationTimer) {
    clearInterval(negotiationTimer)
    negotiationTimer = null
  }
}

async function refreshNegotiation() {
  const conversationId = selectedConversationId.value
  if (!conversationId) {
    negotiationConversation.value = null
    negotiationMessages.value = []
    negotiationProposals.value = []
    negotiationError.value = ''
    return
  }

  negotiationLoading.value = true
  try {
    const [conversation, messages, proposals] = await Promise.all([
      props.driverApi.getConversation(conversationId),
      props.driverApi.getConversationMessages(conversationId),
      props.driverApi.getConversationProposals(conversationId)
    ])
    negotiationConversation.value = conversation
    negotiationMessages.value = messages
    negotiationProposals.value = proposals
    negotiationError.value = ''
    await props.driverApi.markConversationRead(conversationId)
  } catch (error) {
    negotiationError.value = error.message || '协商记录同步失败'
  } finally {
    negotiationLoading.value = false
  }
}

async function refreshWorkspace() {
  await props.driverApi.refreshAll?.()
  await refreshNegotiation()
}

watch(
  selectedConversationId,
  () => {
    clearNegotiationTimer()
    if (!selectedConversationId.value) {
      void refreshNegotiation()
      return
    }
    void refreshNegotiation()
    negotiationTimer = globalThis.setInterval(() => {
      void refreshWorkspace()
    }, 5000)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearNegotiationTimer()
})

async function sendNegotiationMessage(content) {
  if (!selectedConversationId.value) return null
  const message = await props.driverApi.sendMessage(selectedConversationId.value, { content })
  await refreshNegotiation()
  return message
}

async function createNegotiationProposal(payload) {
  if (!selectedConversationId.value) return null
  const proposal = await props.driverApi.createProposal(selectedConversationId.value, payload)
  await refreshWorkspace()
  return proposal
}

async function confirmNegotiationProposal(version) {
  if (!selectedConversationId.value) return null
  const proposal = await props.driverApi.confirmProposal(selectedConversationId.value, version)
  await refreshWorkspace()
  return proposal
}

function displayOrderStatus(order) {
  if (order.status === ORDER_STATUS.PENDING_PAYMENT) return ORDER_STATUS.PENDING_PAYMENT
  if (order.canPay) return ORDER_STATUS.PENDING_PAYMENT
  if (order.live?.phase === 'trip') return ORDER_STATUS.IN_PROGRESS
  return order.status
}
</script>

<template>
  <div class="c-two-column">
    <section class="work-panel driver-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">司机概览</span>
          <h3>今日出行</h3>
        </div>
        <CarFront :size="30" />
      </div>

      <div class="driver-stats">
        <div>
          <strong>{{ profile.credit }}</strong>
          <span>信用分</span>
        </div>
        <div>
          <strong>{{ trips.length }}</strong>
          <span>已发布行程</span>
        </div>
        <div>
          <strong>{{ orders.length }}</strong>
          <span>正式订单</span>
        </div>
      </div>

      <div class="driver-form">
        <label>
          <span>出发地</span>
          <div class="driver-location-field">
            <input v-model="form.start" @input="form.startLng = form.startLat = null" @change="resolvePlace('start')" />
            <button type="button" :title="locationStatus === 'error' ? '重新定位' : '使用当前位置'" @click="locateStart">
              <MapPin :size="16" />
            </button>
          </div>
        </label>
        <label>
          <span>目的地</span>
          <input v-model="form.end" @input="form.endLng = form.endLat = null" @change="resolvePlace('end')" />
        </label>
        <label>
          <span>出发时间</span>
          <input v-model="form.time" />
        </label>
        <label>
          <span>车辆</span>
          <select v-model="form.vehicle">
            <option v-for="vehicle in vehicles" :key="vehicle.id" :disabled="vehicle.status !== '已认证'" :value="vehicle.plate">
              {{ vehicle.plate }} · {{ vehicle.model }} · {{ vehicle.status }}
            </option>
          </select>
        </label>
        <label>
          <span>可拼座位</span>
          <input v-model.number="form.seats" type="number" min="1" max="6" />
        </label>
        <label>
          <span>推荐价格</span>
          <input v-model.number="form.price" type="number" min="0" step="0.1" />
          <small v-if="estimate">{{ estimate.distanceKm }} km · {{ estimate.durationMinutes }} 分钟 · 系统估价 ¥{{ estimate.total }}</small>
        </label>
        <button class="c-primary-action" :disabled="publishing" @click="publishTrip">
          <Plus :size="17" />
          {{ publishing ? '发布中...' : '发布行程' }}
        </button>
      </div>
      <p v-if="actionMessage" class="work-note action-message">{{ actionMessage }}</p>
    </section>

    <section class="work-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">车辆与行程</span>
          <h3>我的行程</h3>
        </div>
        <ShieldCheck :size="28" />
      </div>

      <div class="mini-list">
        <article v-for="vehicle in vehicles" :key="vehicle.id" class="mini-row">
          <div>
            <strong>{{ vehicle.plate }}</strong>
            <span>{{ vehicle.model }} · {{ vehicle.color }} · {{ vehicle.seats }} 座</span>
          </div>
          <b :class="['status-pill', vehicle.status === '已认证' ? 'green' : 'gray']">{{ vehicle.status }}</b>
        </article>
      </div>

      <div class="mini-list trip-list">
        <article v-for="trip in trips" :key="trip.id" class="mini-row">
          <div>
            <strong>{{ trip.start }} -> {{ trip.end }}</strong>
            <span>{{ trip.departureTime }} · {{ trip.vehicle }} · 剩余 {{ trip.availableSeats }} 座</span>
          </div>
          <b class="status-pill green">{{ trip.status }}</b>
        </article>
      </div>
    </section>

    <section class="work-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">双向匹配</span>
          <h3>推荐乘客</h3>
        </div>
        <Users :size="28" />
      </div>
      <div v-if="recommendations.length" class="candidate-grid">
        <article v-for="candidate in recommendations" :key="`${candidate.driverTripId}-${candidate.passengerOrderId}`">
          <div>
            <strong>{{ candidate.start }} -> {{ candidate.end }}</strong>
            <span>{{ candidate.passengerName }} · {{ candidate.passengerCount }} 人 · 信用 {{ candidate.passengerCredit }}</span>
            <small>{{ candidate.reason }}</small>
          </div>
          <b class="status-pill green">{{ candidate.score }} 分</b>
          <button type="button" @click="invitePassenger(candidate)">邀请同行</button>
        </article>
      </div>
      <p v-else class="work-note">当前没有满足路线、时间、座位与信用条件的乘客需求。</p>
    </section>

    <section class="work-panel wide-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">拼车请求</span>
          <h3>待处理乘客</h3>
        </div>
        <Users :size="28" />
      </div>

      <div v-if="requests.length" class="candidate-grid">
        <article v-for="request in requests" :key="request.id" class="candidate-card">
          <div class="candidate-top">
            <strong>{{ request.passenger }}</strong>
            <span :class="['status-pill', request.status === MATCH_REQUEST_STATUS.PENDING ? 'black' : 'green']">{{ request.status }}</span>
          </div>
          <p>{{ request.route }}</p>
          <div class="candidate-meta">
            <span><Clock :size="14" /> {{ request.time }}</span>
            <span>顺路 {{ request.overlap }}%</span>
            <span>信用 {{ request.credit }}</span>
            <span>¥ {{ request.price }}</span>
          </div>
          <div class="candidate-actions">
            <button :disabled="request.status !== MATCH_REQUEST_STATUS.PENDING" @click="updateRequest(request.id, MATCH_REQUEST_STATUS.ACCEPTED)">
              <Check :size="15" />
              同意
            </button>
            <button :disabled="request.status !== MATCH_REQUEST_STATUS.PENDING" @click="updateRequest(request.id, MATCH_REQUEST_STATUS.REJECTED)">
              <X :size="15" />
              拒绝
            </button>
          </div>
        </article>
      </div>
      <div v-else class="c-empty-state">乘客端发起拼车请求后，会自动出现在这里。</div>
    </section>

    <section class="work-panel wide-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">协商工作台</span>
          <h3>已同意请求与方案确认</h3>
        </div>
        <Users :size="28" />
      </div>

      <div class="driver-negotiationLayout">
        <div class="driver-negotiationList">
          <button
            v-for="request in acceptedRequests"
            :key="request.id"
            type="button"
            class="driver-negotiationItem"
            :class="{ active: selectedAcceptedRequest?.id === request.id }"
            @click="selectAcceptedRequest(request.id)"
          >
            <strong>{{ request.passenger }}</strong>
            <span>{{ request.route }}</span>
            <small>{{ request.time }} · {{ request.price ? `¥ ${request.price}` : '价格待议' }}</small>
          </button>
          <div v-if="!acceptedRequests.length" class="c-empty-state driver-negotiationEmpty">
            司机同意请求后，协商会话会出现在这里。
          </div>
        </div>

        <div class="driver-negotiationPanelWrap">
          <p v-if="negotiationError" class="work-note action-message">{{ negotiationError }}</p>
          <NegotiationPanel
            title="司机与乘客协商"
            subtitle="确认价格、出发时间与上车点"
            :conversation="negotiationConversation"
            :messages="negotiationMessages"
            :proposals="negotiationProposals"
            peer-label="乘客"
            self-label="司机"
            self-id="driver-001"
            :status-label="negotiationConversation?.status || selectedAcceptedRequest?.status || '协商中'"
            :default-proposal="proposalSeed"
            :can-message="negotiationConversation?.status === ORDER_STATUS.NEGOTIATING"
            :can-propose="negotiationConversation?.status === ORDER_STATUS.NEGOTIATING"
            :can-confirm="negotiationConversation?.status === ORDER_STATUS.NEGOTIATING"
            confirm-label="确认当前方案"
            @send-message="sendNegotiationMessage"
            @create-proposal="createNegotiationProposal"
            @confirm-proposal="confirmNegotiationProposal"
            @refresh="refreshWorkspace"
          />
          <p v-if="negotiationLoading" class="work-note">正在同步协商内容...</p>
        </div>
      </div>
    </section>

    <section class="work-panel wide-panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">订单履约</span>
          <h3>进行中的订单</h3>
        </div>
        <Flag :size="28" />
      </div>

      <div v-if="orders.length" class="driver-order-grid">
        <article v-for="order in orders" :key="order.id">
          <div>
            <strong>{{ order.route }}</strong>
            <span>{{ order.passengerName }} · {{ order.vehicle }} · ¥ {{ order.price }}</span>
          </div>
          <b class="status-pill black">{{ displayOrderStatus(order) }}</b>
          <button v-if="order.live?.phase === 'pickup'" @click="startTrip(order.id)">
            <Play :size="15" />
            开始行程
          </button>
          <button v-else-if="order.live?.phase === 'trip'" @click="finishTrip(order.id)">
            <Flag :size="15" />
            确认送达
          </button>
          <span v-else>{{ order.status === ORDER_STATUS.COMPLETED ? '乘客已付款' : '等待乘客付款' }}</span>
        </article>
      </div>
      <div v-else class="c-empty-state">双方确认同一版协商方案后，这里会生成正式订单。</div>
    </section>
  </div>
</template>

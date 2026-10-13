<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { BadgeCheck, Clock3, MessageSquare, PencilLine, RefreshCw, Send, Users } from 'lucide-vue-next'

const props = defineProps({
  title: {
    type: String,
    default: '协商中'
  },
  subtitle: {
    type: String,
    default: '订单级协商'
  },
  conversation: {
    type: Object,
    default: null
  },
  messages: {
    type: Array,
    default: () => []
  },
  proposals: {
    type: Array,
    default: () => []
  },
  peerLabel: {
    type: String,
    default: '对方'
  },
  selfLabel: {
    type: String,
    default: '我'
  },
  selfId: {
    type: String,
    default: ''
  },
  statusLabel: {
    type: String,
    default: '协商中'
  },
  emptyHint: {
    type: String,
    default: '会话创建后，这里会显示消息记录、版本化方案和确认状态。'
  },
  canPropose: {
    type: Boolean,
    default: true
  },
  canMessage: {
    type: Boolean,
    default: true
  },
  canConfirm: {
    type: Boolean,
    default: true
  },
  confirmLabel: {
    type: String,
    default: '确认最新方案'
  },
  defaultProposal: {
    type: Object,
    default: () => ({})
  },
  messagePlaceholder: {
    type: String,
    default: '给对方发一条消息'
  }
})

const emit = defineEmits(['send-message', 'create-proposal', 'confirm-proposal', 'refresh'])

const messageText = ref('')
const proposalDraft = reactive({
  price: '',
  departureTime: '',
  passengerCount: 1,
  pickupLocation: '',
  note: ''
})

const latestProposal = computed(() => props.proposals?.[0] || null)

function applyProposalSeed(seed = {}) {
  proposalDraft.price = seed.price ?? ''
  proposalDraft.departureTime = seed.departureTime ?? ''
  proposalDraft.passengerCount = seed.passengerCount ?? 1
  proposalDraft.pickupLocation = seed.pickupLocation ?? ''
  proposalDraft.note = seed.note ?? ''
}

watch(
  () => props.defaultProposal,
  (seed) => {
    applyProposalSeed(seed || {})
  },
  { immediate: true, deep: true }
)

watch(
  () => props.conversation?.id,
  () => {
    messageText.value = ''
  },
  { immediate: true }
)

function sendMessage() {
  const content = messageText.value.trim()
  if (!content || !props.canMessage) return
  emit('send-message', content)
  messageText.value = ''
}

function submitProposal() {
  emit('create-proposal', {
    price: proposalDraft.price === '' || proposalDraft.price == null ? null : Number(proposalDraft.price),
    departureTime: proposalDraft.departureTime,
    passengerCount: Number(proposalDraft.passengerCount || 1),
    pickupLocation: proposalDraft.pickupLocation || '',
    note: proposalDraft.note || ''
  })
}

function confirmLatestProposal() {
  if (!latestProposal.value) return
  emit('confirm-proposal', latestProposal.value.version)
}

function messageRole(message) {
  if (!props.selfId) return message.senderId || 'system'
  if (message.senderId === props.selfId) return props.selfLabel
  if (message.senderId === 'system') return '系统'
  return props.peerLabel
}
</script>

<template>
  <section class="negotiation-panel" :class="{ compact: !conversation }">
    <div class="panel-head negotiation-head">
      <div>
        <span class="eyebrow">{{ subtitle }}</span>
        <h3>{{ title }}</h3>
      </div>
      <div class="negotiation-headActions">
        <b class="status-pill black">{{ statusLabel }}</b>
        <button type="button" class="negotiation-refresh" @click="emit('refresh')">
          <RefreshCw :size="16" />
        </button>
      </div>
    </div>

    <div v-if="!conversation" class="c-empty-state negotiation-empty">
      <MessageSquare :size="30" />
      <strong>等待协商会话建立</strong>
      <p>{{ emptyHint }}</p>
    </div>

    <template v-else>
      <div class="negotiation-summary">
        <div>
          <strong>{{ peerLabel }}</strong>
          <span v-if="conversation.matchRequestId">请求 {{ conversation.matchRequestId }}</span>
        </div>
        <div>
          <Users :size="14" />
          <span>{{ conversation.unreadCount }} 条未读</span>
        </div>
        <div>
          <Clock3 :size="14" />
          <span>{{ conversation.createdAt }}</span>
        </div>
      </div>

      <div class="negotiation-stream">
        <article v-for="message in messages" :key="message.id" class="negotiation-message" :class="{ self: message.senderId === selfId }">
          <div class="negotiation-messageTop">
            <strong>{{ messageRole(message) }}</strong>
            <span>{{ message.createdAt }}</span>
          </div>
          <p>{{ message.content || '[图片消息]' }}</p>
        </article>
        <div v-if="!messages.length" class="negotiation-emptyState">
          先发一条消息，开始协商。
        </div>
      </div>

      <div v-if="latestProposal" class="negotiation-latestProposal">
        <div class="negotiation-proposalHead">
          <div>
            <span>最新方案</span>
            <strong>v{{ latestProposal.version }}</strong>
          </div>
          <b>{{ latestProposal.confirmedRoles.length }}/2 已确认</b>
        </div>
        <div class="negotiation-proposalGrid">
          <div>
            <span>价格</span>
            <strong>￥{{ latestProposal.price }}</strong>
          </div>
          <div>
            <span>时间</span>
            <strong>{{ latestProposal.departureTime }}</strong>
          </div>
          <div>
            <span>人数</span>
            <strong>{{ latestProposal.passengerCount }} 人</strong>
          </div>
          <div v-if="latestProposal.pickupLocation">
            <span>上车点</span>
            <strong>{{ latestProposal.pickupLocation }}</strong>
          </div>
        </div>
        <p v-if="latestProposal.note">{{ latestProposal.note }}</p>
      </div>

      <div v-else class="negotiation-emptyState muted">
        还没有协商方案，可以先发起第一版。
      </div>

      <div class="negotiation-actions">
        <button type="button" class="negotiation-confirmButton" :disabled="!canConfirm || !latestProposal" @click="confirmLatestProposal">
          <BadgeCheck :size="15" />
          {{ confirmLabel }}
        </button>
      </div>

      <div class="negotiation-form">
        <div class="negotiation-formHead">
          <div>
            <span>消息沟通</span>
            <strong>给 {{ peerLabel }} 发消息</strong>
          </div>
          <MessageSquare :size="18" />
        </div>
        <textarea v-model="messageText" :placeholder="messagePlaceholder" rows="3" :disabled="!canMessage"></textarea>
        <button type="button" class="negotiation-sendButton" :disabled="!canMessage" @click="sendMessage">
          <Send :size="15" />
          发送消息
        </button>
      </div>

      <form class="negotiation-form proposal-form" @submit.prevent="submitProposal">
        <div class="negotiation-formHead">
          <div>
            <span>方案版本</span>
            <strong>提交新的协商方案</strong>
          </div>
          <PencilLine :size="18" />
        </div>
        <div class="negotiation-formGrid">
          <label>
            <span>价格</span>
            <input v-model="proposalDraft.price" type="number" min="0" step="0.1" :disabled="!canPropose" />
          </label>
          <label>
            <span>出发时间</span>
            <input v-model="proposalDraft.departureTime" :disabled="!canPropose" />
          </label>
          <label>
            <span>人数</span>
            <input v-model.number="proposalDraft.passengerCount" type="number" min="1" max="8" :disabled="!canPropose" />
          </label>
          <label>
            <span>上车点</span>
            <input v-model="proposalDraft.pickupLocation" :disabled="!canPropose" />
          </label>
        </div>
        <label class="negotiation-noteField">
          <span>备注</span>
          <textarea v-model="proposalDraft.note" rows="2" :disabled="!canPropose"></textarea>
        </label>
        <button type="submit" class="negotiation-sendButton" :disabled="!canPropose">
          <PencilLine :size="15" />
          提交方案
        </button>
      </form>
    </template>
  </section>
</template>

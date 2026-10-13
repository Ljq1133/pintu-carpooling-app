<script setup>
import { computed, reactive, ref, watch } from 'vue'
import {
  BadgeCheck,
  CarFront,
  ChevronDown,
  ChevronRight,
  ClipboardList,
  MessageSquareWarning,
  Save,
  Settings2,
  ShieldCheck,
  UserRound
} from 'lucide-vue-next'
import BlacklistPanel from '../ui/BlacklistPanel.vue'
import EmergencyContactsPanel from '../ui/EmergencyContactsPanel.vue'
import FeedbackNotice from '../ui/FeedbackNotice.vue'

const props = defineProps({
  passenger: {
    type: Object,
    required: true
  },
  identity: {
    type: Object,
    default: () => ({ status: '未申请' })
  },
  creditRecords: {
    type: Array,
    default: () => []
  },
  orders: {
    type: Array,
    default: () => []
  },
  appeals: {
    type: Array,
    default: () => []
  },
  preferences: {
    type: Object,
    default: () => ({})
  },
  bindings: {
    type: Array,
    default: () => []
  },
  supportSessions: {
    type: Array,
    default: () => []
  },
  supportMessages: {
    type: Object,
    default: () => ({})
  },
  retentionPolicies: {
    type: Array,
    default: () => []
  },
  blacklist: {
    type: Array,
    default: () => []
  },
  blacklistBusyId: {
    type: String,
    default: ''
  },
  blacklistError: {
    type: String,
    default: ''
  },
  emergencyContacts: {
    type: Array,
    default: () => []
  },
  contactBusyId: {
    type: String,
    default: ''
  },
  contactError: {
    type: String,
    default: ''
  },
  saving: {
    type: Boolean,
    default: false
  },
  identityBusy: {
    type: Boolean,
    default: false
  },
  switching: {
    type: Boolean,
    default: false
  },
  settingsBusy: {
    type: Boolean,
    default: false
  },
  message: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
  },
  activeSection: {
    type: String,
    default: 'account'
  }
})

const emit = defineEmits([
  'save-profile',
  'update-preferences',
  'create-binding',
  'delete-binding',
  'create-support-session',
  'send-support-message',
  'apply-driver',
  'switch-driver',
  'open-history',
  'unblock-user',
  'save-contact',
  'delete-contact',
  'section-change'
])

const profileForm = reactive({
  name: '',
  gender: '',
  avatarUrl: ''
})
const preferenceForm = reactive({
  darkMode: false,
  fontScale: 'standard',
  locale: 'zh-CN',
  protocolVersion: '2026-06'
})
const bindingForm = reactive({
  bindingType: 'PHONE',
  provider: '',
  accountLabel: '',
  accountValue: ''
})
const supportForm = reactive({
  subject: '',
  message: '',
  attachmentKey: '',
  attachmentFile: null
})
const supportReplies = reactive({})
const expandedSections = reactive({
  account: true,
  identity: false,
  orders: false,
  safety: true,
  appeals: false,
  support: false,
  bindings: false,
  preferences: false,
  protocol: false
})

const profileSections = computed(() => [
  { key: 'account', label: '账号资料', desc: '昵称、性别和头像', icon: UserRound },
  { key: 'identity', label: '身份与车辆', desc: `司机认证：${identityStatus.value}`, icon: BadgeCheck },
  { key: 'orders', label: '行程历史', desc: `${historyCounts.value.ongoing} 进行中 · ${historyCounts.value.completed} 已完成`, icon: ClipboardList },
  { key: 'safety', label: '安全隐私', desc: `${props.emergencyContacts.length} 个联系人 · ${props.blacklist.length} 个拉黑`, icon: ShieldCheck },
  { key: 'appeals', label: '申诉中心', desc: `${props.appeals.length} 条处理记录`, icon: MessageSquareWarning },
  { key: 'support', label: '客服中心', desc: `${props.supportSessions.length} 个会话`, icon: MessageSquareWarning },
  { key: 'bindings', label: '账号绑定', desc: `${props.bindings.filter((item) => item.status === 'ACTIVE').length} 项生效`, icon: BadgeCheck },
  { key: 'preferences', label: '偏好设置', desc: `${props.preferences.locale || 'zh-CN'} · ${props.preferences.fontScale || 'standard'}`, icon: Settings2 },
  { key: 'protocol', label: '协议与数据保留', desc: props.preferences.protocolVersion || '2026-06', icon: ShieldCheck }
])

watch(
  () => props.activeSection,
  (section) => {
    if (!section || !(section in expandedSections)) return
    expandedSections[section] = true
  },
  { immediate: true }
)

watch(
  () => props.passenger,
  (passenger) => {
    profileForm.name = passenger?.name || ''
    profileForm.gender = passenger?.gender || ''
    profileForm.avatarUrl = passenger?.avatarUrl || ''
  },
  { immediate: true, deep: true }
)

watch(
  () => props.preferences,
  (preferences) => {
    preferenceForm.darkMode = Boolean(preferences?.darkMode)
    preferenceForm.fontScale = preferences?.fontScale || 'standard'
    preferenceForm.locale = preferences?.locale || 'zh-CN'
    preferenceForm.protocolVersion = preferences?.protocolVersion || '2026-06'
  },
  { immediate: true, deep: true }
)

const identityStatus = computed(() => props.identity?.status || '未申请')
const canApplyDriver = computed(() => ['未申请', '已驳回'].includes(identityStatus.value))
const canSwitchDriver = computed(() => identityStatus.value === '已通过')

const historyCounts = computed(() => ({
  ongoing: props.orders.filter((order) => ['待匹配', '进行中'].includes(order.status)).length,
  completed: props.orders.filter((order) => order.status === '已完成').length,
  cancelled: props.orders.filter((order) => order.status === '已取消').length
}))

function submitProfile() {
  emit('save-profile', { ...profileForm })
}

function submitPreferences() {
  emit('update-preferences', { ...preferenceForm })
}

function submitBinding() {
  if (!bindingForm.accountValue.trim()) return
  emit('create-binding', { ...bindingForm })
  bindingForm.accountValue = ''
  bindingForm.accountLabel = ''
}

function submitSupportSession() {
  if (!supportForm.subject.trim() || (!supportForm.message.trim() && !supportForm.attachmentKey.trim() && !supportForm.attachmentFile)) return
  emit('create-support-session', { ...supportForm })
  supportForm.subject = ''
  supportForm.message = ''
  supportForm.attachmentKey = ''
  supportForm.attachmentFile = null
}

function submitSupportReply(sessionId) {
  const draft = supportReplies[sessionId] || { content: '', attachmentKey: '', attachmentFile: null }
  if (!String(draft.content || '').trim() && !String(draft.attachmentKey || '').trim() && !draft.attachmentFile) return
  emit('send-support-message', {
    sessionId,
    payload: {
      messageType: draft.attachmentKey || draft.attachmentFile ? 'image' : 'text',
      content: draft.content || '',
      attachmentKey: draft.attachmentKey || '',
      attachmentFile: draft.attachmentFile || null
    }
  })
  supportReplies[sessionId] = { content: '', attachmentKey: '', attachmentFile: null }
}

function supportDraft(sessionId) {
  if (!supportReplies[sessionId]) supportReplies[sessionId] = { content: '', attachmentKey: '', attachmentFile: null }
  return supportReplies[sessionId]
}

function selectSupportAttachment(event, draft) {
  const file = event.target.files?.[0] || null
  draft.attachmentFile = file
  if (file && !draft.attachmentKey) draft.attachmentKey = file.name
}

function toggleSection(key) {
  expandedSections[key] = !expandedSections[key]
  if (expandedSections[key]) emit('section-change', key)
}

function formatMoney(value) {
  const number = Number(value || 0)
  return Number.isInteger(number) ? number : number.toFixed(1)
}

function appealStatusLabel(status) {
  return {
    OPEN: '待受理',
    CLAIMED: '已认领',
    IN_PROGRESS: '处理中',
    WAITING_SUPPLEMENT: '待补充',
    CLOSED: '已关闭'
  }[status] || status || '待受理'
}

function appealEvidenceItems(appeal) {
  return (appeal.evidenceKeys || appeal.evidences || [])
    .map((item) => typeof item === 'string' ? item : item.storageKey || item.attachmentKey || item.fileName || item.id)
    .filter(Boolean)
}

function appealTransitionText(appeal) {
  return (appeal.transitions || [])
    .map((item) => `${appealStatusLabel(item.toStatus)} ${item.createdAt || ''}`.trim())
    .join(' / ')
}

function appealArbitrationText(appeal) {
  return (appeal.arbitrations || [])
    .map((item) => item.remark || item.actionType)
    .filter(Boolean)
    .join(' / ')
}
</script>

<template>
  <div
    :class="[
      'Passenger-profilePanel',
      { 'Passenger-profilePanel--dark': preferences.darkMode },
      `Passenger-profilePanel--font-${preferences.fontScale || 'standard'}`
    ]"
  >
    <header class="Passenger-profileTopBar">
      <div>
        <span>我的</span>
        <strong>账户与安全</strong>
      </div>
      <b class="Passenger-profileCredit">
        <ShieldCheck :size="15" />
        {{ passenger.credit || 0 }}
      </b>
    </header>

    <section class="Passenger-profileSummary">
      <div class="Passenger-avatar">
        <img v-if="passenger.avatarUrl" :src="passenger.avatarUrl" alt="">
        <UserRound v-else :size="25" />
      </div>
      <div class="Passenger-profileIdentity">
        <strong>{{ passenger.name }}</strong>
        <span>{{ passenger.phone }}</span>
      </div>

      <div class="Passenger-profileMetrics">
        <span><b>{{ passenger.tripCount || 0 }}</b>出行</span>
        <span><b>{{ formatMoney(passenger.savedMoney) }}</b>节省</span>
        <span><b>{{ formatMoney(passenger.carbonSaved) }}</b>减碳</span>
      </div>
    </section>

    <FeedbackNotice type="success" :message="message" />
    <FeedbackNotice type="error" :message="error" />

    <section class="Passenger-settingsList" aria-label="我的功能列表">
      <article
        v-for="section in profileSections"
        :key="section.key"
        :class="{ expanded: expandedSections[section.key] }"
        class="Passenger-settingsItem"
      >
        <button
          class="Passenger-settingsRow"
          type="button"
          :aria-expanded="expandedSections[section.key]"
          @click="toggleSection(section.key)"
        >
          <span class="Passenger-settingsIcon"><component :is="section.icon" :size="18" /></span>
          <span class="Passenger-settingsCopy">
            <strong>{{ section.label }}</strong>
            <small>{{ section.desc }}</small>
          </span>
          <ChevronDown :class="{ rotated: expandedSections[section.key] }" :size="18" />
        </button>

        <div v-if="expandedSections[section.key]" class="Passenger-settingsContent">
          <form v-if="section.key === 'account'" class="Passenger-listForm" @submit.prevent="submitProfile">
            <label class="Passenger-formRow">
              <span>昵称</span>
              <input v-model.trim="profileForm.name" required maxlength="64" placeholder="同行者如何称呼你">
            </label>
            <label class="Passenger-formRow">
              <span>性别</span>
              <select v-model="profileForm.gender">
                <option value="">未设置</option>
                <option value="男">男</option>
                <option value="女">女</option>
                <option value="其他">其他</option>
              </select>
            </label>
            <label class="Passenger-formRow">
              <span>头像地址</span>
              <input v-model.trim="profileForm.avatarUrl" placeholder="可选">
            </label>
            <button class="Passenger-rowAction" type="submit" :disabled="saving">
              <Save :size="16" />
              <span>{{ saving ? '保存中' : '保存资料' }}</span>
            </button>
          </form>

          <template v-else-if="section.key === 'identity'">
            <div class="Passenger-infoRows">
              <div>
                <span>认证状态</span>
                <b :class="['Passenger-profilePill', identityStatus]">{{ identityStatus }}</b>
              </div>
              <p v-if="identity.rejectReason">驳回原因：{{ identity.rejectReason }}</p>
              <p v-else-if="identity.submittedAt">提交时间：{{ identity.submittedAt }}</p>
              <p v-else>当前账号尚未提交司机认证。</p>
            </div>
            <button v-if="canApplyDriver" class="Passenger-rowAction" type="button" :disabled="identityBusy" @click="emit('apply-driver')">
              <ShieldCheck :size="17" />
              <span>{{ identityBusy ? '提交中' : identityStatus === '已驳回' ? '重新申请司机认证' : '申请司机认证' }}</span>
              <ChevronRight :size="16" />
            </button>
            <button v-else-if="canSwitchDriver" class="Passenger-rowAction" type="button" :disabled="switching" @click="emit('switch-driver')">
              <CarFront :size="17" />
              <span>{{ switching ? '切换中' : '进入司机端管理车辆' }}</span>
              <ChevronRight :size="16" />
            </button>
            <p v-else class="Passenger-mutedLine">司机认证审核通过后，可进入司机端管理车辆。</p>
          </template>

          <template v-else-if="section.key === 'safety'">
            <EmergencyContactsPanel
              :items="emergencyContacts"
              :busy-id="contactBusyId"
              :error="contactError"
              @save="emit('save-contact', $event)"
              @delete="emit('delete-contact', $event)"
            />

            <BlacklistPanel
              :items="blacklist"
              :busy-id="blacklistBusyId"
              :error="blacklistError"
              @unblock="emit('unblock-user', $event)"
            />
          </template>

          <template v-else-if="section.key === 'orders'">
            <div class="Passenger-historyActions">
              <button type="button" @click="emit('open-history', 'completed')">
                <span>已完成</span>
                <b>{{ historyCounts.completed }}</b>
              </button>
              <button type="button" @click="emit('open-history', 'cancelled')">
                <span>已取消</span>
                <b>{{ historyCounts.cancelled }}</b>
              </button>
              <button type="button" @click="emit('open-history', 'ongoing')">
                <span>进行中</span>
                <b>{{ historyCounts.ongoing }}</b>
              </button>
            </div>
          </template>

          <template v-else-if="section.key === 'appeals'">
            <div v-if="appeals.length" class="Passenger-appealList">
              <article v-for="appeal in appeals" :key="appeal.id">
                <div>
                  <strong>{{ appeal.route || appeal.orderId }}</strong>
                  <span>{{ appeal.reasonType }} · {{ appeal.createdAt }}</span>
                </div>
                <b>{{ appealStatusLabel(appeal.status) }}</b>
                <p>{{ appeal.resolution || appeal.description || '管理员处理中' }}</p>
                <section class="Passenger-appealDetail">
                  <span>证据：{{ appealEvidenceItems(appeal).join('、') || '暂无附件' }}</span>
                  <span>流转：{{ appealTransitionText(appeal) || '等待客服处理' }}</span>
                  <span>仲裁：{{ appealArbitrationText(appeal) || appeal.resolution || '暂无仲裁结果' }}</span>
                </section>
              </article>
            </div>
            <div v-else class="Passenger-infoRows">
              <div>
                <span>当前申诉</span>
                <b>0</b>
              </div>
              <p>暂无申诉记录。订单详情页发起申诉后，会在这里展示处理进度。</p>
            </div>
          </template>

          <template v-else-if="section.key === 'preferences'">
            <form class="Passenger-listForm Passenger-settingsForm" @submit.prevent="submitPreferences">
              <label class="Passenger-formRow Passenger-toggleRow">
                <span>深色模式</span>
                <input v-model="preferenceForm.darkMode" type="checkbox">
              </label>
              <label class="Passenger-formRow">
                <span>字号</span>
                <select v-model="preferenceForm.fontScale">
                  <option value="compact">紧凑</option>
                  <option value="standard">标准</option>
                  <option value="large">大字</option>
                </select>
              </label>
              <label class="Passenger-formRow">
                <span>语言</span>
                <select v-model="preferenceForm.locale">
                  <option value="zh-CN">中文</option>
                  <option value="en-US">English</option>
                </select>
              </label>
              <button class="Passenger-rowAction" type="submit" :disabled="settingsBusy">
                <Save :size="16" />
                <span>{{ settingsBusy ? '保存中' : '保存偏好' }}</span>
              </button>
            </form>
          </template>

          <template v-else-if="section.key === 'bindings'">
            <div class="Passenger-subPanel">
              <header>
                <strong>账号绑定</strong>
                <span>{{ bindings.filter((item) => item.status === 'ACTIVE').length }} 项</span>
              </header>
              <article v-for="binding in bindings" :key="binding.id" class="Passenger-miniRow">
                <div>
                  <strong>{{ binding.accountLabel || binding.bindingType }}</strong>
                  <span>{{ binding.maskedValue }} · {{ binding.status }}</span>
                </div>
                <button
                  type="button"
                  :disabled="settingsBusy || binding.status !== 'ACTIVE'"
                  @click="emit('delete-binding', binding.id)"
                >
                  解绑
                </button>
              </article>
              <form class="Passenger-inlineForm" @submit.prevent="submitBinding">
                <select v-model="bindingForm.bindingType" :disabled="settingsBusy">
                  <option value="PHONE">手机号</option>
                  <option value="PAYMENT">支付账号</option>
                  <option value="SOCIAL">社交账号</option>
                </select>
                <input v-model.trim="bindingForm.accountLabel" :disabled="settingsBusy" placeholder="标签">
                <input v-model.trim="bindingForm.accountValue" :disabled="settingsBusy" required placeholder="绑定值">
                <button type="submit" :disabled="settingsBusy">绑定</button>
              </form>
            </div>
          </template>

          <template v-else-if="section.key === 'support'">
            <div class="Passenger-subPanel">
              <header>
                <strong>在线客服</strong>
                <span>{{ supportSessions.length }} 个会话</span>
              </header>
              <form class="Passenger-inlineForm Passenger-supportCreate" @submit.prevent="submitSupportSession">
                <input v-model.trim="supportForm.subject" :disabled="settingsBusy" required placeholder="主题">
                <input v-model.trim="supportForm.message" :disabled="settingsBusy" placeholder="消息">
                <input v-model.trim="supportForm.attachmentKey" :disabled="settingsBusy" placeholder="图片 key">
                <input type="file" accept="image/*" :disabled="settingsBusy" @change="selectSupportAttachment($event, supportForm)">
                <button type="submit" :disabled="settingsBusy">发送</button>
              </form>
              <article v-for="session in supportSessions" :key="session.id" class="Passenger-supportSession">
                <div>
                  <strong>{{ session.subject }}</strong>
                  <span>{{ session.status }} · {{ session.updatedAt || session.createdAt }}</span>
                </div>
                <p v-for="messageItem in (supportMessages[session.id] || []).slice(-3)" :key="messageItem.id">
                  {{ messageItem.senderRole }}：{{ messageItem.content || messageItem.attachmentKey }}
                </p>
                <form class="Passenger-inlineForm" @submit.prevent="submitSupportReply(session.id)">
                  <input
                    v-model.trim="supportDraft(session.id).content"
                    :disabled="settingsBusy || session.status === 'CLOSED'"
                    placeholder="回复内容"
                  >
                  <input
                    v-model.trim="supportDraft(session.id).attachmentKey"
                    :disabled="settingsBusy || session.status === 'CLOSED'"
                    placeholder="图片 key"
                  >
                  <input
                    type="file"
                    accept="image/*"
                    :disabled="settingsBusy || session.status === 'CLOSED'"
                    @change="selectSupportAttachment($event, supportDraft(session.id))"
                  >
                  <button type="submit" :disabled="settingsBusy || session.status === 'CLOSED'">回复</button>
                </form>
              </article>
            </div>
          </template>

          <template v-else-if="section.key === 'protocol'">
            <div class="Passenger-subPanel">
              <header>
                <strong>协议与数据保留</strong>
                <span>{{ preferences.protocolVersion || '2026-06' }}</span>
              </header>
              <article v-for="policy in retentionPolicies" :key="policy.id || policy.dataType" class="Passenger-miniRow">
                <div>
                  <strong>{{ policy.dataType }}</strong>
                  <span>{{ policy.retentionDays }} 天 · {{ policy.deleteMode }}</span>
                </div>
              </article>
            </div>
          </template>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.Passenger-profilePanel {
  display: grid;
  gap: 10px;
}

.Passenger-profilePanel--font-compact {
  font-size: 12px;
}

.Passenger-profilePanel--font-large {
  font-size: 15px;
}

.Passenger-profilePanel--dark {
  color: #F4F5F5;
}

.Passenger-profilePanel--dark .Passenger-profileSummary,
.Passenger-profilePanel--dark .Passenger-settingsList,
.Passenger-profilePanel--dark .Passenger-settingsRow,
.Passenger-profilePanel--dark .Passenger-formRow,
.Passenger-profilePanel--dark .Passenger-infoRows div,
.Passenger-profilePanel--dark .Passenger-subPanel,
.Passenger-profilePanel--dark .Passenger-miniRow,
.Passenger-profilePanel--dark .Passenger-supportSession {
  border-color: #2E3435;
  background: #141818;
}

.Passenger-profilePanel--dark .Passenger-profileTopBar strong,
.Passenger-profilePanel--dark .Passenger-settingsCopy strong,
.Passenger-profilePanel--dark .Passenger-profileIdentity strong,
.Passenger-profilePanel--dark .Passenger-miniRow strong,
.Passenger-profilePanel--dark .Passenger-supportSession strong,
.Passenger-profilePanel--dark .Passenger-infoRows b {
  color: #F4F5F5;
}

.Passenger-profileTopBar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 2px 0;
}

.Passenger-profileTopBar div {
  display: grid;
  gap: 2px;
}

.Passenger-profileTopBar span,
.Passenger-profileIdentity span,
.Passenger-settingsCopy small,
.Passenger-formRow span,
.Passenger-infoRows span,
.Passenger-infoRows p,
.Passenger-mutedLine {
  color: #70747A;
  font-size: 12px;
  line-height: 1.45;
}

.Passenger-profileTopBar strong {
  color: #111111;
  font-size: 21px;
  line-height: 1.15;
}

.Passenger-profileCredit {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  gap: 5px;
  padding: 0 12px;
  border-radius: 999px;
  background: #111111;
  color: #39FF14;
  font-size: 16px;
}

.Passenger-profileSummary {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-avatar {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 999px;
  background: #F2F4F4;
  color: #111111;
}

.Passenger-avatar img {
  width: 100%;
  height: 100%;
  border-radius: inherit;
  object-fit: cover;
}

.Passenger-profileIdentity {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.Passenger-profileIdentity strong {
  overflow: hidden;
  color: #111111;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.Passenger-profileMetrics {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.Passenger-profileMetrics span {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border-radius: 7px;
  background: #F7F8F8;
  color: #555555;
  font-size: 12px;
}

.Passenger-profileMetrics b {
  color: #111111;
  font-size: 15px;
}

.Passenger-settingsList {
  overflow: hidden;
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-settingsItem + .Passenger-settingsItem {
  border-top: 1px solid #E8EAEA;
}

.Passenger-settingsRow {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 22px;
  gap: 10px;
  width: 100%;
  min-height: 62px;
  align-items: center;
  border: 0;
  padding: 9px 10px;
  background: #FFFFFF;
  text-align: left;
  cursor: pointer;
}

.Passenger-settingsIcon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #F2F4F4;
  color: #111111;
}

.Passenger-settingsCopy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.Passenger-settingsCopy strong {
  color: #111111;
  font-size: 15px;
}

.Passenger-settingsRow svg:last-child {
  color: #757575;
  transition: transform 0.18s ease;
}

.Passenger-settingsRow svg.rotated {
  transform: rotate(180deg);
}

.Passenger-settingsItem.expanded .Passenger-settingsIcon {
  background: #111111;
  color: #39FF14;
}

.Passenger-settingsContent {
  display: grid;
  gap: 10px;
  padding: 0 10px 12px 54px;
}

.Passenger-listForm {
  display: grid;
  gap: 0;
  overflow: hidden;
  border: 1px solid #E8EAEA;
  border-radius: 8px;
}

.Passenger-formRow,
.Passenger-infoRows div {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 10px;
  background: #FDFDFD;
}

.Passenger-formRow + .Passenger-formRow,
.Passenger-infoRows div + div {
  border-top: 1px solid #E8EAEA;
}

.Passenger-formRow input,
.Passenger-formRow select {
  min-width: 0;
  min-height: 36px;
  border: 0;
  background: transparent;
  color: #111111;
  font-weight: 700;
  outline: none;
}

.Passenger-rowAction,
.Passenger-historyActions button {
  border: 0;
  background: #111111;
  color: #FFFFFF;
  font-weight: 900;
  cursor: pointer;
}

.Passenger-rowAction {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
  color: #FFFFFF;
}

.Passenger-rowAction:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.Passenger-toggleRow input {
  justify-self: end;
  width: 18px;
  height: 18px;
}

.Passenger-settingsForm {
  gap: 0;
}

.Passenger-settingsForm .Passenger-rowAction {
  margin: 10px;
}

.Passenger-subPanel {
  display: grid;
  gap: 8px;
  border: 1px solid #E8EAEA;
  border-radius: 8px;
  padding: 10px;
  background: #FDFDFD;
}

.Passenger-subPanel header,
.Passenger-miniRow,
.Passenger-supportSession > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.Passenger-subPanel header strong,
.Passenger-miniRow strong,
.Passenger-supportSession strong {
  color: #111111;
  font-size: 13px;
}

.Passenger-subPanel header span,
.Passenger-miniRow span,
.Passenger-supportSession span,
.Passenger-supportSession p {
  margin: 0;
  color: #70747A;
  font-size: 12px;
  line-height: 1.45;
}

.Passenger-miniRow {
  min-height: 42px;
  border-top: 1px solid #E8EAEA;
  padding-top: 8px;
}

.Passenger-miniRow div,
.Passenger-supportSession div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.Passenger-miniRow button,
.Passenger-inlineForm button {
  min-height: 32px;
  border: 0;
  border-radius: 7px;
  background: #111111;
  color: #FFFFFF;
  font-weight: 900;
  cursor: pointer;
}

.Passenger-miniRow button:disabled,
.Passenger-inlineForm button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.Passenger-inlineForm {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 6px;
}

.Passenger-inlineForm input,
.Passenger-inlineForm select {
  min-width: 0;
  min-height: 34px;
  border: 1px solid #E3E5E5;
  border-radius: 7px;
  padding: 0 8px;
  background: #FFFFFF;
  color: #111111;
}

.Passenger-supportCreate {
  grid-template-columns: minmax(0, 0.75fr) minmax(0, 1fr) minmax(0, 0.75fr) minmax(0, 0.85fr) auto;
}

.Passenger-supportSession {
  display: grid;
  gap: 7px;
  border-top: 1px solid #E8EAEA;
  padding-top: 8px;
}

.Passenger-supportSession .Passenger-inlineForm {
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.7fr) minmax(0, 0.8fr) auto;
}

.Passenger-infoRows {
  display: grid;
  gap: 8px;
}

.Passenger-appealList {
  display: grid;
  gap: 8px;
}

.Passenger-appealList article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 10px;
  padding: 10px;
  border: 1px solid #E8EAEA;
  border-radius: 8px;
  background: #FDFDFD;
}

.Passenger-appealList article div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.Passenger-appealList strong {
  overflow: hidden;
  color: #111111;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.Passenger-appealList span,
.Passenger-appealList p {
  margin: 0;
  color: #70747A;
  font-size: 12px;
  line-height: 1.45;
}

.Passenger-appealList b {
  color: #111111;
  font-size: 12px;
}

.Passenger-appealList p {
  grid-column: 1 / -1;
}

.Passenger-appealDetail {
  grid-column: 1 / -1;
  display: grid;
  gap: 5px;
  padding: 8px;
  border-radius: 6px;
  background: #F3F5F5;
}

.Passenger-appealDetail span {
  overflow-wrap: anywhere;
}

.Passenger-infoRows {
  overflow: hidden;
  border: 1px solid #E8EAEA;
  border-radius: 8px;
}

.Passenger-infoRows p {
  margin: 0;
  padding: 0 10px 10px;
}

.Passenger-infoRows b,
.Passenger-profilePill {
  justify-self: end;
  color: #111111;
  font-size: 13px;
}

.Passenger-mutedLine {
  margin: 0;
}

.Passenger-profilePill.已通过 {
  color: #16610A;
}

.Passenger-profilePill.已驳回 {
  color: #B42318;
}

.Passenger-historyActions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.Passenger-historyActions button {
  display: grid;
  gap: 4px;
  min-height: 54px;
  place-items: center;
}

.Passenger-historyActions span {
  color: #FFFFFF;
  font-size: 12px;
}

.Passenger-historyActions b {
  color: #39FF14;
  font-size: 18px;
}

:deep(.contact-panel),
:deep(.blacklist-panel) {
  border: 1px solid #E8EAEA;
  border-radius: 8px;
  padding: 10px;
}

:deep(.contact-panel header strong),
:deep(.blacklist-panel header strong) {
  font-size: 13px;
}

@media (max-width: 420px) {
  .Passenger-settingsContent {
    padding-left: 10px;
  }

  .Passenger-inlineForm,
  .Passenger-supportCreate,
  .Passenger-supportSession .Passenger-inlineForm {
    grid-template-columns: 1fr;
  }
}
</style>

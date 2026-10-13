<script setup>
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import {
  BadgeCheck,
  ClipboardCheck,
  Eye,
  Filter,
  Gavel,
  Gauge,
  MessageSquareWarning,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  UserRoundCheck
} from 'lucide-vue-next'
import FeedbackNotice from '../ui/FeedbackNotice.vue'

const props = defineProps({
  adminApi: {
    type: Object,
    required: true
  }
})

const userAuditStatus = ref('')
const vehicleAuditStatus = ref('')
const orderFilters = reactive({
  status: '',
  paymentStatus: '',
  keyword: '',
  dateFrom: '',
  dateTo: ''
})
const appealStatus = ref('')
const rejectReasons = reactive({})
const appealComments = reactive({})
const arbitrationForms = reactive({})
const supportReplies = reactive({})
const ruleForm = reactive({
  id: '',
  ruleType: 'MATCHING',
  code: 'DEFAULT',
  name: '',
  grayPercent: 100,
  content: '{"radiusKm":5,"timeWindowMinutes":120,"minimumOverlap":60}'
})
const retentionForm = reactive({
  dataType: 'LOCATION',
  retentionDays: 30,
  deleteMode: 'ARCHIVE_THEN_DELETE',
  description: ''
})
const selectedAudit = ref(null)
const selectedOrder = ref(null)
const retentionCleanupDryRun = ref(true)
const actionMessage = ref('')
const actionError = ref('')
const activeAdminModule = ref('audit')
const SAFETY_REFRESH_INTERVAL_MS = 5000
let safetyRefreshTimer = null

const adminModules = [
  { key: 'audit', label: '审核中心' },
  { key: 'orders', label: '订单中心' },
  { key: 'safety', label: '安全监控' },
  { key: 'support', label: '客服会话' },
  { key: 'rules', label: '规则配置' },
  { key: 'credit', label: '信用风控' },
  { key: 'appeals', label: '申诉仲裁' },
  { key: 'stats', label: '运营统计' }
]

const adminMe = computed(() => props.adminApi.getMe?.() || {})
const permissions = computed(() => new Set((adminMe.value.permissions || []).map((item) => item.code)))
const canAudit = computed(() => permissions.value.has('AUDIT_REVIEW'))
const canViewOrders = computed(() => permissions.value.has('ORDER_VIEW'))
const canManageCredit = computed(() => permissions.value.has('CREDIT_MANAGE'))
const canManageAppeals = computed(() => permissions.value.has('APPEAL_MANAGE'))
const canManageArbitration = computed(() => permissions.value.has('ARBITRATION_MANAGE'))
const canMonitorSafety = computed(() => permissions.value.has('SAFETY_MONITOR'))
const canManageRules = computed(() => permissions.value.has('RULE_MANAGE'))
const canManageSupport = computed(() => permissions.value.has('SUPPORT_MANAGE'))
const canRestrictUsers = computed(() => permissions.value.has('USER_RESTRICT'))

const auditUsers = computed(() => props.adminApi.getUserAudits())
const auditVehicles = computed(() => props.adminApi.getVehicleAudits())
const orders = computed(() => props.adminApi.getOrders())
const creditUsers = computed(() => props.adminApi.getCreditUsers())
const appeals = computed(() => props.adminApi.getAppeals?.() || [])
const orderSummary = computed(() => props.adminApi.getOrderSummary?.() || fallbackSummary(orders.value))
const safetyOrders = computed(() => props.adminApi.getSafetyOrders?.() || [])
const safetyAlerts = computed(() => props.adminApi.getSafetyAlerts?.() || [])
const ruleConfigs = computed(() => props.adminApi.getRules?.() || [])
const supportSessions = computed(() => props.adminApi.getSupportSessions?.() || [])
const retentionPolicies = computed(() => props.adminApi.getRetentionPolicies?.() || [])
const retentionCleanupResult = computed(() => props.adminApi.getRetentionCleanupResult?.() || null)
const opsStatistics = computed(() => props.adminApi.getOpsStatistics?.() || fallbackOpsStatistics())

const summaryCards = computed(() => [
  {
    icon: UserRoundCheck,
    value: auditUsers.value.filter((item) => item.status === '待审核').length,
    label: '实名待审核'
  },
  {
    icon: ClipboardCheck,
    value: auditVehicles.value.filter((item) => item.status === '待审核').length,
    label: '车辆待审核'
  },
  {
    icon: Gauge,
    value: orderSummary.value.total || orders.value.length,
    label: '平台订单'
  },
  {
    icon: ShieldAlert,
    value: creditUsers.value.filter((user) => user.credit < 60).length,
    label: '限制账号'
  }
])

const opsCards = computed(() => [
  { label: '转化率', value: `${opsStatistics.value.conversionRate || 0}%` },
  { label: '完成率', value: `${opsStatistics.value.completionRate || 0}%` },
  { label: '取消率', value: `${opsStatistics.value.cancellationRate || 0}%` },
  { label: '平均匹配', value: `${opsStatistics.value.averageMatchMinutes || 0} 分钟` },
  { label: '异常率', value: `${opsStatistics.value.abnormalRate || 0}%` },
  { label: '未结申诉', value: opsStatistics.value.openAppealCount || 0 }
])

function fallbackSummary(items) {
  return {
    total: items.length,
    pendingPayment: items.filter((order) => order.status === '待付款').length,
    completed: items.filter((order) => order.status === '已完成').length,
    cancelled: items.filter((order) => order.status === '已取消').length,
    statusCounts: {},
    paymentStatusCounts: {}
  }
}

function clearFeedback() {
  actionMessage.value = ''
  actionError.value = ''
}

async function refreshUserAudits() {
  clearFeedback()
  await props.adminApi.refreshUserAudits?.({ status: userAuditStatus.value, page: 1, size: 20 })
}

async function refreshVehicleAudits() {
  clearFeedback()
  await props.adminApi.refreshVehicleAudits?.({ status: vehicleAuditStatus.value, page: 1, size: 20 })
}

async function refreshOrders(options = {}) {
  if (!options?.silent) clearFeedback()
  const filters = { ...orderFilters }
  await Promise.all([
    props.adminApi.refreshOrders?.(filters),
    props.adminApi.refreshOrderSummary?.(filters)
  ])
}

function fallbackOpsStatistics() {
  const total = orders.value.length
  const completed = orders.value.filter((order) => order.status === '已完成').length
  const cancelled = orders.value.filter((order) => order.status === '已取消').length
  return {
    totalOrders: total,
    completedOrders: completed,
    cancelledOrders: cancelled,
    activeOrders: orders.value.filter((order) => ['已确认', '履约中', '待付款'].includes(order.status)).length,
    safetyAlertCount: safetyAlerts.value.length,
    openAppealCount: appeals.value.filter((appeal) => appeal.status !== 'CLOSED').length,
    conversionRate: total ? Math.round((completed / total) * 100) : 0,
    completionRate: total ? Math.round((completed / total) * 100) : 0,
    cancellationRate: total ? Math.round((cancelled / total) * 100) : 0,
    averageMatchMinutes: 0,
    abnormalRate: total ? Math.round((safetyAlerts.value.length / total) * 100) : 0
  }
}

async function exportOrdersCsv() {
  clearFeedback()
  if (!canViewOrders.value) {
    actionError.value = '当前管理员没有订单导出权限'
    return
  }
  const csv = await props.adminApi.exportOrdersCsv?.({ ...orderFilters })
  if (!csv) {
    actionError.value = '订单导出失败'
    return
  }
  actionMessage.value = `已导出 ${csv.split('\n').filter(Boolean).length - 1} 条订单 CSV`
}

async function openOrderDetail(order) {
  clearFeedback()
  if (!canViewOrders.value) {
    actionError.value = '当前管理员没有订单详情权限'
    return
  }
  selectedOrder.value = await props.adminApi.getOrderDetail?.(order.id) || order
}

async function refreshSafety(options = {}) {
  if (!options?.silent) clearFeedback()
  await Promise.all([
    props.adminApi.refreshSafetyOrders?.(),
    props.adminApi.refreshSafetyAlerts?.()
  ])
}

function startSafetyPolling() {
  if (safetyRefreshTimer || !canMonitorSafety.value) return
  safetyRefreshTimer = window.setInterval(() => {
    if (activeAdminModule.value === 'safety') refreshSafety({ silent: true })
  }, SAFETY_REFRESH_INTERVAL_MS)
}

function stopSafetyPolling() {
  if (!safetyRefreshTimer) return
  window.clearInterval(safetyRefreshTimer)
  safetyRefreshTimer = null
}

async function refreshRules() {
  clearFeedback()
  await props.adminApi.refreshRules?.()
}

function editRule(rule) {
  ruleForm.id = rule.id
  ruleForm.ruleType = rule.ruleType
  ruleForm.code = rule.code
  ruleForm.name = rule.name
  ruleForm.grayPercent = rule.grayPercent || 100
  ruleForm.content = rule.versions?.[0]?.content || '{}'
}

async function saveRule() {
  clearFeedback()
  if (!canManageRules.value) {
    actionError.value = '当前管理员没有规则管理权限'
    return
  }
  try {
    const payload = { ...ruleForm, grayPercent: Number(ruleForm.grayPercent || 100) }
    const result = ruleForm.id
      ? await props.adminApi.updateRule?.(ruleForm.id, payload)
      : await props.adminApi.createRule?.(payload)
    if (!result) {
      actionError.value = '规则保存失败'
      return
    }
    ruleForm.id = ''
    actionMessage.value = '规则已保存'
  } catch (error) {
    actionError.value = error.message || '规则保存失败'
  }
}

async function publishRule(rule) {
  clearFeedback()
  if (!window.confirm(`确认发布规则「${rule.name}」？`)) return
  const result = await props.adminApi.publishRule?.(rule.id)
  actionMessage.value = result ? `${rule.name} 已发布` : '规则发布失败'
}

async function rollbackRule(rule) {
  clearFeedback()
  const targetVersion = rule.versions?.find((item) => item.version !== rule.activeVersion)?.version || rule.activeVersion
  if (!window.confirm(`确认将「${rule.name}」回滚到 v${targetVersion}？`)) return
  const result = await props.adminApi.rollbackRule?.(rule.id, {
    toVersion: targetVersion,
    reason: '管理员回滚'
  })
  actionMessage.value = result ? `${rule.name} 已回滚到 v${targetVersion}` : '规则回滚失败'
}

async function refreshSupport() {
  clearFeedback()
  await props.adminApi.refreshSupportSessions?.()
}

function supportSessionMessages(sessionId) {
  return props.adminApi.getSupportMessages?.(sessionId) || []
}

function supportDraft(sessionId) {
  if (!supportReplies[sessionId]) supportReplies[sessionId] = { content: '', attachmentKey: '', attachmentFile: null }
  return supportReplies[sessionId]
}

function selectAdminSupportAttachment(event, draft) {
  draft.attachmentFile = event.target.files?.[0] || null
}

async function uploadAdminAttachmentFile(file) {
  if (!file || !props.adminApi.uploadAttachment) return null
  try {
    return await props.adminApi.uploadAttachment(file)
  } catch (error) {
    actionError.value = error.message || '图片上传失败'
    return null
  }
}

async function resolveAdminSupportDraft(draft) {
  let attachmentKey = String(draft.attachmentKey || '').trim()
  if (draft.attachmentFile) {
    const uploaded = await uploadAdminAttachmentFile(draft.attachmentFile)
    if (uploaded?.attachmentKey) attachmentKey = uploaded.attachmentKey
  }
  return {
    messageType: attachmentKey ? 'image' : 'text',
    content: String(draft.content || '').trim(),
    attachmentKey
  }
}

async function claimSupport(session) {
  clearFeedback()
  const result = await props.adminApi.claimSupportSession?.(session.id)
  actionMessage.value = result ? `${session.subject} 已认领` : '客服会话认领失败'
}

async function replySupport(session) {
  clearFeedback()
  const draft = supportDraft(session.id)
  if (!String(draft.content || '').trim() && !String(draft.attachmentKey || '').trim() && !draft.attachmentFile) {
    actionError.value = '回复内容不能为空'
    return
  }
  const payload = await resolveAdminSupportDraft(draft)
  if (!payload.content && !payload.attachmentKey) {
    actionError.value = actionError.value || '回复内容不能为空'
    return
  }
  const message = await props.adminApi.replySupportSession?.(session.id, payload)
  if (!message) {
    actionError.value = '客服回复失败'
    return
  }
  supportReplies[session.id] = { content: '', attachmentKey: '', attachmentFile: null }
  actionMessage.value = '客服回复已发送'
}

async function closeSupport(session) {
  clearFeedback()
  const result = await props.adminApi.closeSupportSession?.(session.id, { reason: '处理完成' })
  actionMessage.value = result ? `${session.subject} 已关闭` : '客服会话关闭失败'
}

async function saveRetentionPolicy() {
  clearFeedback()
  const result = await props.adminApi.saveRetentionPolicy?.({
    ...retentionForm,
    retentionDays: Number(retentionForm.retentionDays || 30)
  })
  actionMessage.value = result ? '数据保留策略已保存' : '数据保留策略保存失败'
}

async function cleanupRetention() {
  clearFeedback()
  if (!canMonitorSafety.value) {
    actionError.value = '当前管理员没有数据保留执行权限'
    return
  }
  const result = await props.adminApi.cleanupRetention?.({ dryRun: retentionCleanupDryRun.value })
  actionMessage.value = result
    ? retentionCleanupDryRun.value ? '数据保留清理预览已生成' : '数据保留清理已执行'
    : '数据保留清理执行失败'
}

async function refreshStats() {
  clearFeedback()
  await props.adminApi.refreshOpsStatistics?.()
}

async function openAuditDetail(item) {
  clearFeedback()
  selectedAudit.value = await props.adminApi.getAuditDetail?.(item.id) || {
    ...item,
    histories: item.histories || []
  }
}

async function reviewUser(item, status) {
  await reviewAudit(() => props.adminApi.reviewUserAudit(item.id, status, rejectReasons[item.id] || ''), item, status)
  await refreshUserAudits()
}

async function reviewVehicle(item, status) {
  await reviewAudit(() => props.adminApi.reviewVehicleAudit(item.id, status, rejectReasons[item.id] || ''), item, status)
  await refreshVehicleAudits()
}

async function reviewAudit(action, item, status) {
  clearFeedback()
  if (!canAudit.value) {
    actionError.value = '当前管理员没有审核处理权限'
    return
  }
  if (status === '已驳回' && !String(rejectReasons[item.id] || '').trim()) {
    actionError.value = '驳回前请填写原因'
    return
  }
  try {
    const result = await action()
    if (!result) {
      actionError.value = '审核操作未完成'
      return
    }
    actionMessage.value = `${item.id} 已更新为${status}`
    rejectReasons[item.id] = ''
    selectedAudit.value = result
  } catch (error) {
    actionError.value = error.message || '审核操作失败'
  }
}

async function adjustCredit(user, delta) {
  clearFeedback()
  if (!canManageCredit.value) {
    actionError.value = '当前管理员没有信用管理权限'
    return
  }
  const result = await props.adminApi.adjustCredit(user.id, delta, delta > 0 ? '管理员奖励' : '管理员处罚')
  if (!result) {
    actionError.value = '信用分调整失败'
    return
  }
  actionMessage.value = `${user.name} 信用分已调整`
}

async function restrictUser(user, type) {
  clearFeedback()
  if (!canRestrictUsers.value) {
    actionError.value = '当前管理员没有用户限制权限'
    return
  }
  if (!window.confirm(`确认对 ${user.name} 执行 ${type}？`)) return
  const result = await props.adminApi.restrictUser?.(user.id, {
    type,
    reason: '管理员风控'
  })
  actionMessage.value = result ? `${user.name} 已限制` : '用户限制失败'
}

async function releaseRestriction(user, type) {
  clearFeedback()
  if (!canRestrictUsers.value) {
    actionError.value = '当前管理员没有用户限制权限'
    return
  }
  if (!window.confirm(`确认解除 ${user.name} 的 ${type}？`)) return
  const ok = await props.adminApi.releaseRestriction?.(user.id, type)
  actionMessage.value = ok ? `${user.name} 已解除限制` : '解除限制失败'
}

async function retryNotification(notice) {
  clearFeedback()
  if (!canAudit.value) {
    actionError.value = '当前管理员没有通知重试权限'
    return
  }
  const updated = await props.adminApi.retryNotification?.(notice.id)
  if (!updated) {
    actionError.value = '通知重试失败'
    return
  }
  if (selectedAudit.value?.notifications) {
    selectedAudit.value.notifications = selectedAudit.value.notifications.map((item) =>
      item.id === updated.id ? { ...item, ...updated } : item
    )
  }
  actionMessage.value = `${updated.title || '审核通知'} 已重试`
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

function ensureArbitrationForm(appeal) {
  if (!arbitrationForms[appeal.id]) {
    arbitrationForms[appeal.id] = {
      actionType: 'ORDER_NOTE',
      amount: '',
      creditDelta: 0,
      targetUserId: appeal.applicantId || '',
      remark: ''
    }
  }
  return arbitrationForms[appeal.id]
}

async function refreshAppeals() {
  clearFeedback()
  await props.adminApi.refreshAppeals?.({ status: appealStatus.value })
}

async function transitionAppeal(appeal, status, defaultComment = '', assigneeId = '') {
  clearFeedback()
  if (!canManageAppeals.value) {
    actionError.value = '当前管理员没有申诉处理权限'
    return
  }
  const comment = assigneeId ? defaultComment : (appealComments[appeal.id] || defaultComment)
  const result = await props.adminApi.transitionAppeal?.(appeal.id, {
    status,
    comment,
    assigneeId: assigneeId || (status === 'CLAIMED' ? adminMe.value.id : appeal.assigneeId)
  })
  if (!result) {
    actionError.value = '申诉流转失败'
    return
  }
  appealComments[appeal.id] = ''
  actionMessage.value = `${appeal.id} 已更新为${appealStatusLabel(status)}`
}

async function arbitrateAppeal(appeal) {
  clearFeedback()
  if (!canManageArbitration.value) {
    actionError.value = '当前管理员没有仲裁处理权限'
    return
  }
  const form = ensureArbitrationForm(appeal)
  const confirmed = window.confirm('确认提交仲裁动作？该操作会写入仲裁记录。')
  if (!confirmed) return
  try {
    const record = await props.adminApi.arbitrateAppeal?.(appeal.id, {
      actionType: form.actionType,
      amount: form.amount === '' ? null : Number(form.amount),
      creditDelta: Number(form.creditDelta || 0),
      targetUserId: form.targetUserId || appeal.applicantId,
      remark: form.remark,
      confirmed: true
    })
    if (!record) {
      actionError.value = '仲裁提交失败'
      return
    }
    form.remark = ''
    actionMessage.value = `${appeal.id} 仲裁记录已写入`
  } catch (error) {
    actionError.value = error.message || '仲裁提交失败'
  }
}

watch(
  [activeAdminModule, canMonitorSafety, canViewOrders],
  ([module, canMonitor, canView]) => {
    if (module === 'orders' && canView) {
      refreshOrders({ silent: true })
    }
    if (module === 'safety' && canMonitor) {
      refreshSafety({ silent: true })
      startSafetyPolling()
    } else {
      stopSafetyPolling()
    }
  },
  { immediate: true }
)

onUnmounted(stopSafetyPolling)
</script>

<template>
  <div class="admin-workspace">
    <section class="admin-rbac-strip">
      <div>
        <span class="eyebrow">当前后台身份</span>
        <strong>{{ adminMe.name || '平台管理员' }}</strong>
      </div>
      <div class="admin-role-tags">
        <b v-for="role in adminMe.subRoleLabels || []" :key="role">{{ role }}</b>
      </div>
      <div class="admin-permission-tags">
        <span v-for="permission in adminMe.permissions || []" :key="permission.code">
          <ShieldCheck :size="14" />
          {{ permission.label }}
        </span>
      </div>
    </section>

    <FeedbackNotice type="success" :message="actionMessage" />
    <FeedbackNotice type="error" :message="actionError" />

    <section class="admin-summary">
      <article v-for="card in summaryCards" :key="card.label">
        <component :is="card.icon" :size="24" />
        <strong>{{ card.value }}</strong>
        <span>{{ card.label }}</span>
      </article>
    </section>

    <nav class="admin-module-tabs" aria-label="管理端模块">
      <button
        v-for="module in adminModules"
        :key="module.key"
        :class="{ active: activeAdminModule === module.key }"
        type="button"
        @click="activeAdminModule = module.key"
      >
        {{ module.label }}
      </button>
    </nav>

    <div class="admin-grid">
      <template v-if="activeAdminModule === 'audit'">
      <section class="work-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">用户资料</span>
            <h3>实名审核</h3>
          </div>
          <label class="admin-filter">
            <Filter :size="15" />
            <select v-model="userAuditStatus" @change="refreshUserAudits">
              <option value="">全部</option>
              <option value="待审核">待审核</option>
              <option value="已通过">已通过</option>
              <option value="已驳回">已驳回</option>
            </select>
          </label>
        </div>
        <div v-if="auditUsers.length" class="admin-list">
          <article v-for="item in auditUsers" :key="item.id" class="admin-row audit-row">
            <div>
              <strong>{{ item.name }} · {{ item.role }}</strong>
              <span>{{ item.type }} · {{ item.submittedAt }}</span>
              <input
                v-if="item.status === '待审核' && canAudit"
                v-model.trim="rejectReasons[item.id]"
                class="admin-reject-input"
                placeholder="驳回原因"
              >
            </div>
            <div class="row-actions">
              <button type="button" @click="openAuditDetail(item)">
                <Eye :size="14" />
                详情
              </button>
              <template v-if="item.status === '待审核'">
                <button type="button" :disabled="!canAudit" @click="reviewUser(item, '已通过')">通过</button>
                <button type="button" :disabled="!canAudit" @click="reviewUser(item, '已驳回')">驳回</button>
              </template>
            </div>
            <b :class="['status-pill', item.status === '待审核' ? 'black' : item.status === '已驳回' ? 'gray' : 'green']">{{ item.status }}</b>
          </article>
        </div>
        <div v-else class="c-empty-state">当前筛选下暂无实名审核。</div>
      </section>

      <section class="work-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">车辆资料</span>
            <h3>车辆资质审核</h3>
          </div>
          <label class="admin-filter">
            <Filter :size="15" />
            <select v-model="vehicleAuditStatus" @change="refreshVehicleAudits">
              <option value="">全部</option>
              <option value="待审核">待审核</option>
              <option value="已通过">已通过</option>
              <option value="已驳回">已驳回</option>
            </select>
          </label>
        </div>
        <div v-if="auditVehicles.length" class="admin-list">
          <article v-for="item in auditVehicles" :key="item.id" class="admin-row audit-row">
            <div>
              <strong>{{ item.plate }} · {{ item.driver }}</strong>
              <span>{{ item.model }} · {{ item.submittedAt }}</span>
              <input
                v-if="item.status === '待审核' && canAudit"
                v-model.trim="rejectReasons[item.id]"
                class="admin-reject-input"
                placeholder="驳回原因"
              >
            </div>
            <div class="row-actions">
              <button type="button" @click="openAuditDetail(item)">
                <Eye :size="14" />
                详情
              </button>
              <template v-if="item.status === '待审核'">
                <button type="button" :disabled="!canAudit" @click="reviewVehicle(item, '已通过')">通过</button>
                <button type="button" :disabled="!canAudit" @click="reviewVehicle(item, '已驳回')">驳回</button>
              </template>
            </div>
            <b :class="['status-pill', item.status === '待审核' ? 'black' : item.status === '已驳回' ? 'gray' : 'green']">{{ item.status }}</b>
          </article>
        </div>
        <div v-else class="c-empty-state">当前筛选下暂无车辆审核。</div>
      </section>

      <section v-if="selectedAudit" class="work-panel wide-panel audit-detail-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">审核详情</span>
            <h3>{{ selectedAudit.id }}</h3>
          </div>
          <BadgeCheck :size="22" />
        </div>
        <div class="audit-detail-grid">
          <span>对象</span><strong>{{ selectedAudit.name || selectedAudit.plate || selectedAudit.targetId }}</strong>
          <span>状态</span><strong>{{ selectedAudit.status }}</strong>
          <span>提交时间</span><strong>{{ selectedAudit.submittedAt || '暂无' }}</strong>
          <span>处理时间</span><strong>{{ selectedAudit.handledAt || '暂无' }}</strong>
          <span>原因</span><strong>{{ selectedAudit.reason || '暂无' }}</strong>
        </div>
        <div class="audit-history-list">
          <article v-for="attachment in selectedAudit.attachments || []" :key="attachment.id">
            <span>{{ attachment.attachmentType || '附件' }}</span>
            <strong>{{ attachment.storageKey }}</strong>
            <small>{{ attachment.contentType || '未标注类型' }} · {{ attachment.createdAt || '暂无时间' }}</small>
          </article>
          <p v-if="!(selectedAudit.attachments || []).length">暂无附件 key。</p>
        </div>
        <div class="audit-history-list">
          <article v-for="notice in selectedAudit.notifications || []" :key="notice.id">
            <span>{{ notice.createdAt }}</span>
            <strong>{{ notice.title || '审核通知' }}</strong>
            <small>{{ notice.sendStatus || '待发送' }} · 重试 {{ notice.retryCount || 0 }} 次</small>
            <button type="button" :disabled="!canAudit" @click="retryNotification(notice)">重试通知</button>
          </article>
          <p v-if="!(selectedAudit.notifications || []).length">暂无通知记录。</p>
        </div>
        <div class="audit-history-list">
          <article v-for="history in selectedAudit.histories || []" :key="history.id">
            <span>{{ history.createdAt }}</span>
            <strong>{{ history.fromStatus || '初始' }} -> {{ history.toStatus }}</strong>
            <small>{{ history.reason || '无原因' }}</small>
          </article>
          <p v-if="!(selectedAudit.histories || []).length">暂无审核历史。</p>
        </div>
      </section>

      </template>

      <template v-else-if="activeAdminModule === 'orders'">
      <section class="work-panel wide-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">订单中心</span>
            <h3>订单履约看板</h3>
          </div>
        </div>
        <form class="admin-filterbar" @submit.prevent="refreshOrders">
          <select v-model="orderFilters.status" :disabled="!canViewOrders">
            <option value="">全部状态</option>
            <option value="待确认">待确认</option>
            <option value="已确认">已确认</option>
            <option value="履约中">履约中</option>
            <option value="待付款">待付款</option>
            <option value="已完成">已完成</option>
            <option value="已取消">已取消</option>
          </select>
          <select v-model="orderFilters.paymentStatus" :disabled="!canViewOrders">
            <option value="">全部支付</option>
            <option value="未支付">未支付</option>
            <option value="已支付">已支付</option>
            <option value="已退款">已退款</option>
          </select>
          <input v-model.trim="orderFilters.keyword" :disabled="!canViewOrders" placeholder="订单/路线/用户关键词">
          <input v-model="orderFilters.dateFrom" :disabled="!canViewOrders" type="date">
          <input v-model="orderFilters.dateTo" :disabled="!canViewOrders" type="date">
          <button type="submit" :disabled="!canViewOrders">筛选</button>
          <button type="button" :disabled="!canViewOrders" @click="exportOrdersCsv">导出 CSV</button>
        </form>
        <div class="admin-order-metrics">
          <span>待付款 {{ orderSummary.pendingPayment || 0 }}</span>
          <span>已完成 {{ orderSummary.completed || 0 }}</span>
          <span>已取消 {{ orderSummary.cancelled || 0 }}</span>
        </div>
        <div v-if="canViewOrders && orders.length" class="order-board">
          <article v-for="order in orders" :key="order.id">
            <b>{{ order.id }}</b>
            <strong>{{ order.route }}</strong>
            <span>{{ order.passenger }} / {{ order.driver }}</span>
            <span>¥ {{ order.price }}</span>
            <em :class="order.risk === '正常' ? 'normal' : 'warn'">{{ order.status }} · {{ order.paymentStatus || order.risk }}</em>
            <button type="button" @click="openOrderDetail(order)">详情</button>
          </article>
        </div>
        <div v-else class="c-empty-state">{{ canViewOrders ? '当前筛选下暂无订单。' : '当前管理员没有订单看板权限。' }}</div>
        <div v-if="selectedOrder" class="order-detail-panel">
          <div class="panel-head">
            <div>
              <span class="eyebrow">订单详情</span>
              <h3>{{ selectedOrder.id }}</h3>
            </div>
            <BadgeCheck :size="22" />
          </div>
          <div class="audit-detail-grid">
            <span>路线</span><strong>{{ selectedOrder.route }}</strong>
            <span>状态</span><strong>{{ selectedOrder.status }} · {{ selectedOrder.paymentStatus || '暂无支付' }}</strong>
            <span>乘客</span><strong>{{ selectedOrder.passenger || selectedOrder.passengerId }}</strong>
            <span>司机</span><strong>{{ selectedOrder.driver || selectedOrder.driverId || '待分配' }}</strong>
            <span>金额</span><strong>¥ {{ selectedOrder.price || 0 }}</strong>
            <span>备注</span><strong>{{ selectedOrder.adminNote || selectedOrder.cancelReason || '暂无' }}</strong>
          </div>
          <div class="detail-columns">
            <section>
              <strong>状态时间线</strong>
              <p v-for="event in selectedOrder.timeline || []" :key="event.id">
                {{ event.fromStatus || 'INIT' }} -> {{ event.toStatus }} · {{ event.reason || event.createdAt }}
              </p>
              <p v-if="!(selectedOrder.timeline || []).length">暂无时间线。</p>
            </section>
            <section>
              <strong>支付与评价</strong>
              <p v-for="payment in selectedOrder.payments || []" :key="payment.id">
                {{ payment.status }} · ¥ {{ payment.amount }} · {{ payment.paidAt || payment.createdAt }}
              </p>
              <p v-for="review in selectedOrder.reviews || []" :key="review.id">
                评分 {{ review.rating }} · {{ review.comment || '无评价内容' }}
              </p>
              <p v-if="!(selectedOrder.payments || []).length && !(selectedOrder.reviews || []).length">暂无支付或评价记录。</p>
            </section>
            <section>
              <strong>协商与申诉</strong>
              <p v-for="proposal in selectedOrder.negotiations || []" :key="proposal.id">
                v{{ proposal.version }} · ¥ {{ proposal.price }} · {{ proposal.note || proposal.pickupLocation }}
              </p>
              <p v-for="appeal in selectedOrder.appeals || []" :key="appeal.id">
                {{ appeal.status }} · {{ appeal.reasonType }} · {{ appeal.resolution || appeal.description }}
              </p>
              <p v-if="!(selectedOrder.negotiations || []).length && !(selectedOrder.appeals || []).length">暂无协商或申诉记录。</p>
            </section>
            <section>
              <strong>轨迹与告警</strong>
              <p>轨迹 {{ (selectedOrder.trajectory || []).length }} 点</p>
              <p v-for="alert in selectedOrder.alerts || []" :key="alert.id">
                {{ alert.alertType }} · {{ alert.status }} · {{ alert.details || '无说明' }}
              </p>
              <p v-if="!(selectedOrder.alerts || []).length">暂无异常告警。</p>
            </section>
            <section>
              <strong>信用事件</strong>
              <p v-for="credit in selectedOrder.creditEvents || []" :key="credit.id">
                {{ credit.userId }} · {{ credit.delta }} · {{ credit.reason }}
              </p>
              <p v-if="!(selectedOrder.creditEvents || []).length">暂无信用变更。</p>
            </section>
          </div>
        </div>
      </section>

      </template>

      <template v-else-if="activeAdminModule === 'safety'">
      <section class="work-panel wide-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">安全监控</span>
            <h3>履约订单、轨迹摘要与异常告警</h3>
          </div>
          <ShieldAlert :size="22" />
        </div>
        <form class="admin-filterbar" @submit.prevent="refreshSafety">
          <button type="submit" :disabled="!canMonitorSafety">
            <RefreshCw :size="14" />
            刷新监控
          </button>
        </form>
        <div v-if="canMonitorSafety && safetyOrders.length" class="admin-list safety-list">
          <article v-for="order in safetyOrders" :key="order.orderId" class="admin-row">
            <div>
              <strong>{{ order.route }}</strong>
              <span>{{ order.orderId }} · {{ order.status }}</span>
              <span>乘客 {{ order.passenger }} · 司机 {{ order.driver }}</span>
              <p>
                轨迹 {{ order.trajectoryCount || 0 }} 点 · 告警 {{ order.alertCount || 0 }} 条
                · 分享 {{ order.shareStatus || 'INACTIVE' }} · 虚拟号 {{ order.virtualPhoneStatus || 'UNBOUND' }}
                <template v-if="order.latestLocation || order.latestLocationAt">
                  · 最新 {{ order.latestLocationAt || order.latestLocation?.reportedAt }}
                </template>
              </p>
              <p v-if="order.noLocationUpdate" class="warn-copy">长时间无位置更新，已生成 NO_LOCATION_UPDATE。</p>
            </div>
            <b class="status-pill black">{{ order.noLocationUpdate || order.alertCount ? '需关注' : '正常' }}</b>
          </article>
        </div>
        <div v-else class="c-empty-state">{{ canMonitorSafety ? '当前没有履约中的安全监控订单。' : '当前管理员没有安全监控权限。' }}</div>
        <div v-if="canMonitorSafety && safetyAlerts.length" class="admin-list compact-list">
          <article v-for="alert in safetyAlerts.slice(0, 6)" :key="alert.id" class="admin-row">
            <div>
              <strong>{{ alert.alertType }} · {{ alert.orderId }}</strong>
              <span>{{ alert.status }} · {{ alert.createdAt }}</span>
              <p>{{ alert.details || '系统异常告警' }}</p>
            </div>
          </article>
        </div>
        <form class="admin-filterbar retention-form" @submit.prevent="saveRetentionPolicy">
          <select v-model="retentionForm.dataType" :disabled="!canMonitorSafety">
            <option value="LOCATION">位置</option>
            <option value="CHAT">聊天</option>
            <option value="ATTACHMENT">附件</option>
            <option value="AUDIT">审计</option>
          </select>
          <input v-model.number="retentionForm.retentionDays" :disabled="!canMonitorSafety" type="number" min="1">
          <select v-model="retentionForm.deleteMode" :disabled="!canMonitorSafety">
            <option value="ARCHIVE_THEN_DELETE">归档后删除</option>
            <option value="DELETE_OBJECT">删除对象</option>
            <option value="KEEP_AUDIT">保留审计</option>
          </select>
          <input v-model.trim="retentionForm.description" :disabled="!canMonitorSafety" placeholder="策略说明">
          <button type="submit" :disabled="!canMonitorSafety">保存策略</button>
          <label class="inline-check">
            <input v-model="retentionCleanupDryRun" type="checkbox" :disabled="!canMonitorSafety">
            仅预览
          </label>
          <button type="button" :disabled="!canMonitorSafety" @click="cleanupRetention">执行清理</button>
        </form>
        <div class="admin-order-metrics retention-metrics">
          <span v-for="policy in retentionPolicies" :key="policy.id || policy.dataType">
            {{ policy.dataType }} {{ policy.retentionDays }} 天
          </span>
        </div>
        <div v-if="retentionCleanupResult" class="retention-result">
          <strong>{{ retentionCleanupResult.dryRun ? '清理预览' : '清理结果' }}</strong>
          <span>位置 {{ retentionCleanupResult.locationReports || 0 }} 条</span>
          <span>轨迹 {{ retentionCleanupResult.trajectoryPoints || 0 }} 点</span>
          <p v-for="note in retentionCleanupResult.notes || []" :key="note">{{ note }}</p>
        </div>
      </section>
      </template>

      <template v-else-if="activeAdminModule === 'support'">
      <section class="work-panel wide-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">在线客服</span>
            <h3>会话认领、回复与关闭</h3>
          </div>
          <MessageSquareWarning :size="22" />
        </div>
        <form class="admin-filterbar" @submit.prevent="refreshSupport">
          <button type="submit" :disabled="!canManageSupport">
            <RefreshCw :size="14" />
            刷新会话
          </button>
        </form>
        <div v-if="canManageSupport && supportSessions.length" class="admin-list support-list">
          <article v-for="session in supportSessions" :key="session.id" class="admin-row support-row">
            <div>
              <strong>{{ session.subject }}</strong>
              <span>{{ session.id }} · {{ session.status }} · {{ session.updatedAt || session.createdAt }}</span>
              <span>乘客 {{ session.userName || session.userId }} · 处理人 {{ session.assigneeId || '未认领' }}</span>
              <p v-for="messageItem in supportSessionMessages(session.id).slice(-3)" :key="messageItem.id">
                {{ messageItem.senderRole }}：{{ messageItem.content || messageItem.attachmentKey }}
              </p>
              <form class="appeal-arbitration-form" @submit.prevent="replySupport(session)">
                <input
                  v-model.trim="supportDraft(session.id).content"
                  :disabled="!canManageSupport || session.status === 'CLOSED'"
                  placeholder="客服回复"
                >
                <input
                  v-model.trim="supportDraft(session.id).attachmentKey"
                  :disabled="!canManageSupport || session.status === 'CLOSED'"
                  placeholder="图片 key"
                >
                <input
                  type="file"
                  accept="image/*"
                  :disabled="!canManageSupport || session.status === 'CLOSED'"
                  @change="selectAdminSupportAttachment($event, supportDraft(session.id))"
                >
                <button type="submit" :disabled="!canManageSupport || session.status === 'CLOSED'">回复</button>
              </form>
            </div>
            <div class="row-actions">
              <button type="button" :disabled="!canManageSupport || session.status === 'CLOSED'" @click="claimSupport(session)">认领</button>
              <button type="button" :disabled="!canManageSupport || session.status === 'CLOSED'" @click="closeSupport(session)">关闭</button>
            </div>
            <b :class="['status-pill', session.status === 'CLOSED' ? 'green' : session.status === 'OPEN' ? 'black' : 'gray']">
              {{ session.status }}
            </b>
          </article>
        </div>
        <div v-else class="c-empty-state">{{ canManageSupport ? '当前暂无客服会话。' : '当前管理员没有客服处理权限。' }}</div>
      </section>
      </template>

      <template v-else-if="activeAdminModule === 'credit'">
      <section class="work-panel wide-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">账号风控</span>
            <h3>信用分与限制管理</h3>
          </div>
        </div>
        <div v-if="canManageCredit" class="credit-grid">
          <article v-for="user in creditUsers" :key="user.id">
            <div>
              <strong>{{ user.name }}</strong>
              <span>{{ user.role }} · {{ user.status }}</span>
            </div>
            <b>{{ user.credit }}</b>
            <button type="button" @click="adjustCredit(user, 5)">+5</button>
            <button type="button" @click="adjustCredit(user, -10)">-10</button>
            <button type="button" :disabled="!canRestrictUsers" @click="restrictUser(user, 'LIMIT_POST')">限发</button>
            <button type="button" :disabled="!canRestrictUsers" @click="restrictUser(user, 'LIMIT_ACCEPT')">限接</button>
            <button type="button" :disabled="!canRestrictUsers" @click="restrictUser(user, 'LIMIT_COMMUNICATION')">限聊</button>
            <button type="button" :disabled="!canRestrictUsers" @click="restrictUser(user, 'FULL_BAN')">封禁</button>
            <button type="button" :disabled="!canRestrictUsers" @click="releaseRestriction(user, 'FULL_BAN')">解禁</button>
          </article>
        </div>
        <div v-else class="c-empty-state">当前管理员没有信用管理权限。</div>
      </section>
      </template>

      <section v-else-if="activeAdminModule === 'rules'" class="work-panel wide-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">规则配置</span>
            <h3>匹配与计价规则</h3>
          </div>
          <Gauge :size="22" />
        </div>
        <form class="rule-editor" @submit.prevent="saveRule">
          <select v-model="ruleForm.ruleType" :disabled="!canManageRules">
            <option value="MATCHING">匹配</option>
            <option value="PRICING">计价</option>
            <option value="CREDIT">信用</option>
          </select>
          <input v-model.trim="ruleForm.code" :disabled="!canManageRules" required placeholder="编码">
          <input v-model.trim="ruleForm.name" :disabled="!canManageRules" required placeholder="名称">
          <input v-model.number="ruleForm.grayPercent" :disabled="!canManageRules" type="number" min="0" max="100">
          <textarea v-model.trim="ruleForm.content" :disabled="!canManageRules" required rows="4" placeholder="JSON 内容"></textarea>
          <button type="submit" :disabled="!canManageRules">{{ ruleForm.id ? '保存版本' : '新增规则' }}</button>
          <button type="button" :disabled="!canManageRules" @click="ruleForm.id = ''">清空选择</button>
        </form>
        <div v-if="canManageRules && ruleConfigs.length" class="admin-list rule-list">
          <article v-for="rule in ruleConfigs" :key="rule.id" class="admin-row rule-row">
            <div>
              <strong>{{ rule.name }} · {{ rule.code }}</strong>
              <span>{{ rule.ruleType }} · {{ rule.status }} · v{{ rule.activeVersion || '-' }} · 灰度 {{ rule.grayPercent }}%</span>
              <p>{{ rule.versions?.[0]?.content }}</p>
            </div>
            <div class="row-actions">
              <button type="button" :disabled="!canManageRules" @click="editRule(rule)">编辑</button>
              <button type="button" :disabled="!canManageRules" @click="publishRule(rule)">发布</button>
              <button type="button" :disabled="!canManageRules || (rule.versions || []).length < 2" @click="rollbackRule(rule)">回滚</button>
            </div>
            <b :class="['status-pill', rule.status === 'ACTIVE' ? 'green' : 'gray']">{{ rule.status }}</b>
          </article>
        </div>
        <div v-else class="c-empty-state">{{ canManageRules ? '暂无规则配置。' : '当前管理员没有规则管理权限。' }}</div>
      </section>

      <section v-else-if="activeAdminModule === 'appeals'" class="work-panel wide-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">申诉仲裁</span>
            <h3>工单处理与仲裁记录</h3>
          </div>
          <MessageSquareWarning :size="22" />
        </div>
        <form class="admin-filterbar appeal-filterbar" @submit.prevent="refreshAppeals">
          <select v-model="appealStatus" :disabled="!canManageAppeals">
            <option value="">全部工单</option>
            <option value="OPEN">待受理</option>
            <option value="CLAIMED">已认领</option>
            <option value="IN_PROGRESS">处理中</option>
            <option value="WAITING_SUPPLEMENT">待补充</option>
            <option value="CLOSED">已关闭</option>
          </select>
          <button type="submit" :disabled="!canManageAppeals">
            <RefreshCw :size="14" />
            刷新
          </button>
        </form>
        <div v-if="canManageAppeals && appeals.length" class="admin-list appeal-list">
          <article v-for="appeal in appeals" :key="appeal.id" class="admin-row appeal-row">
            <div>
              <strong>{{ appeal.route || appeal.orderId }} · {{ appeal.reasonType }}</strong>
              <span>{{ appeal.id }} · {{ appealStatusLabel(appeal.status) }} · {{ appeal.createdAt }}</span>
              <span>乘客 {{ appeal.applicantName || appeal.applicantId }} · 处理人 {{ appeal.assigneeId || '未认领' }}</span>
              <p>{{ appeal.resolution || appeal.description || '暂无补充说明' }}</p>
              <input v-model.trim="appealComments[appeal.id]" class="admin-reject-input" placeholder="流转备注 / 关闭结论 / 转交处理人">
            </div>
            <div class="row-actions">
              <button type="button" :disabled="!canManageAppeals || appeal.status === 'CLOSED'" @click="transitionAppeal(appeal, 'CLAIMED', '管理员认领工单')">认领</button>
              <button type="button" :disabled="!canManageAppeals || appeal.status === 'CLOSED'" @click="transitionAppeal(appeal, 'IN_PROGRESS', '进入处理中')">处理中</button>
              <button type="button" :disabled="!canManageAppeals || appeal.status === 'CLOSED'" @click="transitionAppeal(appeal, 'WAITING_SUPPLEMENT', '等待乘客补充材料')">待补充</button>
              <button type="button" :disabled="!canManageAppeals || appeal.status === 'CLOSED'" @click="transitionAppeal(appeal, 'CLAIMED', '工单已转交', appealComments[appeal.id])">转交</button>
              <button type="button" :disabled="!canManageAppeals || appeal.status === 'CLOSED'" @click="transitionAppeal(appeal, 'CLOSED', '处理完成')">关闭</button>
            </div>
            <b :class="['status-pill', appeal.status === 'CLOSED' ? 'green' : appeal.status === 'OPEN' ? 'black' : 'gray']">{{ appealStatusLabel(appeal.status) }}</b>
            <form class="appeal-arbitration-form" @submit.prevent="arbitrateAppeal(appeal)">
              <Gavel :size="16" />
              <select v-model="ensureArbitrationForm(appeal).actionType" :disabled="!canManageArbitration">
                <option value="ORDER_NOTE">订单备注</option>
                <option value="REFUND">退款记录</option>
                <option value="FORCE_SETTLEMENT">强制结算</option>
                <option value="CREDIT_CORRECTION">信用修正</option>
              </select>
              <input v-model="ensureArbitrationForm(appeal).amount" :disabled="!canManageArbitration" type="number" min="0" step="0.1" placeholder="金额">
              <input v-model.number="ensureArbitrationForm(appeal).creditDelta" :disabled="!canManageArbitration" type="number" step="1" placeholder="信用变更">
              <input v-model.trim="ensureArbitrationForm(appeal).targetUserId" :disabled="!canManageArbitration" placeholder="目标用户">
              <input v-model.trim="ensureArbitrationForm(appeal).remark" :disabled="!canManageArbitration" placeholder="仲裁备注">
              <button type="submit" :disabled="!canManageArbitration">提交仲裁</button>
            </form>
            <div v-if="(appeal.transitions || []).length" class="appeal-history">
              <span v-for="transition in appeal.transitions.slice(-3)" :key="transition.id">
                {{ transition.fromStatus || 'INIT' }} -> {{ transition.toStatus }} · {{ transition.comment || '无备注' }}
              </span>
            </div>
            <div v-if="(appeal.arbitrations || []).length" class="appeal-history">
              <span v-for="record in appeal.arbitrations.slice(-3)" :key="record.id">
                {{ record.actionType }} · {{ record.remark || record.amount || record.creditDelta || '已记录' }}
              </span>
            </div>
          </article>
        </div>
        <div v-else class="c-empty-state">{{ canManageAppeals ? '当前筛选下暂无申诉工单。' : '当前管理员没有申诉处理权限。' }}</div>
      </section>

      <section v-else-if="false && activeAdminModule === 'appeals'" class="work-panel wide-panel admin-placeholder-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">申诉仲裁</span>
            <h3>暂无申诉仲裁数据</h3>
          </div>
        </div>
        <p>申诉流转、仲裁退款、信用处理和订单备注能力接入后会在这里显示。</p>
      </section>

      <section v-else class="work-panel wide-panel">
        <div class="panel-head">
          <div>
            <span class="eyebrow">运营统计</span>
            <h3>订单转化、完成与异常指标</h3>
          </div>
          <button type="button" :disabled="!canViewOrders" @click="refreshStats">
            <RefreshCw :size="14" />
            刷新
          </button>
        </div>
        <div class="stats-grid">
          <article v-for="card in opsCards" :key="card.label">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </article>
        </div>
        <div class="detail-columns">
          <section>
            <strong>订单量</strong>
            <p>总订单 {{ opsStatistics.totalOrders || 0 }}</p>
            <p>履约中/待处理 {{ opsStatistics.activeOrders || 0 }}</p>
            <p>已完成 {{ opsStatistics.completedOrders || 0 }}</p>
            <p>已取消 {{ opsStatistics.cancelledOrders || 0 }}</p>
          </section>
          <section>
            <strong>治理信号</strong>
            <p>安全告警 {{ opsStatistics.safetyAlertCount || 0 }}</p>
            <p>未结申诉 {{ opsStatistics.openAppealCount || 0 }}</p>
            <p>异常率 {{ opsStatistics.abnormalRate || 0 }}%</p>
          </section>
        </div>
      </section>
    </div>
  </div>
</template>

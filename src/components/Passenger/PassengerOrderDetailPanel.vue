<script setup>
import { reactive, ref } from 'vue'
import {
  ClipboardList,
  CreditCard,
  Flag,
  Link,
  MessageSquareWarning,
  Phone,
  ShieldAlert,
  Star,
  UserRound
} from 'lucide-vue-next'
import FeedbackNotice from '../ui/FeedbackNotice.vue'

const props = defineProps({
  order: {
    type: Object,
    required: true
  },
  error: {
    type: String,
    default: ''
  },
  shareLink: {
    type: Object,
    default: null
  },
  virtualPhone: {
    type: Object,
    default: null
  },
  trajectory: {
    type: Array,
    default: () => []
  },
  appealBusy: {
    type: Boolean,
    default: false
  },
  shareBusy: {
    type: Boolean,
    default: false
  },
  virtualPhoneBusy: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['block-driver', 'create-appeal', 'create-share-link', 'refresh-virtual-phone'])

const isAppealOpen = ref(false)
const appealForm = reactive({
  reasonType: 'SERVICE_DISPUTE',
  description: '',
  evidenceKeys: '',
  evidenceFile: null
})

function submitAppeal() {
  emit('create-appeal', {
    orderId: props.order.recordId || props.order.id,
    reasonType: appealForm.reasonType,
    description: appealForm.description,
    evidenceFile: appealForm.evidenceFile,
    evidenceKeys: appealForm.evidenceKeys
      .split(/[\n,，]/)
      .map((item) => item.trim())
      .filter(Boolean)
  })
}

function selectEvidenceFile(event) {
  appealForm.evidenceFile = event.target.files?.[0] || null
}
</script>

<template>
  <article class="Passenger-orderDetailCard">
    <header>
      <div>
        <span>履约中心</span>
        <strong>{{ order.route }}</strong>
      </div>
      <b>{{ order.status }}</b>
    </header>

    <section class="Passenger-detailSection">
      <div class="Passenger-detailSectionHead">
        <Flag :size="16" />
        <strong>行程进度</strong>
      </div>
      <div class="Passenger-timelineList">
        <article v-for="item in order.timeline || []" :key="`${item.status}-${item.time}-${item.description}`">
          <i></i>
          <div>
            <strong>{{ item.status }}</strong>
            <span>{{ item.time || order.createdAt || '暂无时间' }}</span>
            <p>{{ item.description || '状态已更新' }}</p>
          </div>
        </article>
      </div>
    </section>

    <section class="Passenger-detailSection">
      <div class="Passenger-detailSectionHead">
        <ClipboardList :size="16" />
        <strong>行程信息</strong>
      </div>
      <div class="Passenger-tripSummary">
        <div>
          <span>出发地</span>
          <strong>{{ order.start }}</strong>
        </div>
        <div>
          <span>目的地</span>
          <strong>{{ order.end }}</strong>
        </div>
        <div>
          <span>出发时间</span>
          <strong>{{ order.departureTime }}</strong>
        </div>
        <div>
          <span>同行人数</span>
          <strong>{{ order.passengerCount }} 人</strong>
        </div>
      </div>
    </section>

    <section class="Passenger-detailSection">
      <div class="Passenger-detailSectionHead">
        <UserRound :size="16" />
        <strong>同行信息</strong>
      </div>
      <div class="Passenger-tripSummary">
        <div>
          <span>司机</span>
          <strong>{{ order.driverName || '暂未匹配' }}</strong>
        </div>
        <div>
          <span>车辆</span>
          <strong>{{ order.vehicle || '待确认' }}</strong>
        </div>
        <div v-if="order.driverPhone">
          <span>联系方式</span>
          <strong>{{ order.driverPhone }}</strong>
        </div>
      </div>
    </section>

    <section class="Passenger-detailSection">
      <div class="Passenger-detailSectionHead">
        <CreditCard :size="16" />
        <strong>费用支付</strong>
      </div>
      <div class="Passenger-tripSummary">
        <div>
          <span>费用</span>
          <strong>{{ order.price ? `¥ ${order.price}` : '待定' }}</strong>
        </div>
        <div>
          <span>支付状态</span>
          <strong>{{ order.paymentStatus }}</strong>
        </div>
        <div v-if="order.paidAt">
          <span>支付时间</span>
          <strong>{{ order.paidAt }}</strong>
        </div>
      </div>
    </section>

    <section class="Passenger-detailSection">
      <div class="Passenger-detailSectionHead">
        <ShieldAlert :size="16" />
        <strong>安全与服务</strong>
      </div>
      <FeedbackNotice v-if="error" type="error" :message="error" />
      <div v-if="order.recordId || order.id" class="Passenger-serviceGrid">
        <article>
          <Phone :size="15" />
          <span>虚拟号</span>
          <strong>{{ virtualPhone?.virtualNumber || order.virtualPhone || '未绑定' }}</strong>
          <button type="button" :disabled="virtualPhoneBusy" @click="emit('refresh-virtual-phone', order.recordId || order.id)">
            {{ virtualPhoneBusy ? '刷新中' : '刷新' }}
          </button>
        </article>
        <article>
          <Link :size="15" />
          <span>行程分享</span>
          <strong>{{ shareLink?.expiresAt || '未生成' }}</strong>
          <button type="button" :disabled="shareBusy" @click="emit('create-share-link', order.recordId || order.id)">
            {{ shareBusy ? '生成中' : '生成链接' }}
          </button>
        </article>
      </div>
      <p v-if="shareLink?.publicPath" class="Passenger-mutedText">{{ shareLink.publicPath }}</p>
      <p v-if="trajectory.length" class="Passenger-mutedText">已记录 {{ trajectory.length }} 个轨迹点。</p>
      <button
        v-if="order.driverId"
        class="Passenger-blockAction"
        type="button"
        @click="emit('block-driver', order.driverId)"
      >
        拉黑该司机
      </button>
      <p v-else class="Passenger-mutedText">匹配司机后会显示安全操作。</p>
      <button v-if="order.recordId || order.id" class="Passenger-appealToggle" type="button" @click="isAppealOpen = !isAppealOpen">
        <MessageSquareWarning :size="15" />
        {{ isAppealOpen ? '收起申诉' : '发起申诉' }}
      </button>
      <form v-if="isAppealOpen && (order.recordId || order.id)" class="Passenger-appealForm" @submit.prevent="submitAppeal">
        <select v-model="appealForm.reasonType">
          <option value="SERVICE_DISPUTE">服务争议</option>
          <option value="PAYMENT_DISPUTE">费用争议</option>
          <option value="SAFETY_CONCERN">安全问题</option>
          <option value="OTHER">其他</option>
        </select>
        <textarea v-model.trim="appealForm.description" required maxlength="2000" rows="3" placeholder="请补充申诉说明"></textarea>
        <input v-model.trim="appealForm.evidenceKeys" placeholder="证据 URL 或附件 key，多个用逗号分隔">
        <input type="file" accept="image/*" @change="selectEvidenceFile">
        <button type="submit" :disabled="appealBusy">
          {{ appealBusy ? '提交中' : '提交申诉' }}
        </button>
      </form>
    </section>

    <section v-if="order.review || order.cancelReason" class="Passenger-detailSection">
      <div class="Passenger-detailSectionHead">
        <Star :size="16" />
        <strong>评价与记录</strong>
      </div>
      <div v-if="order.review" class="Passenger-reviewResult">
        <span>评价结果</span>
        <strong>{{ order.review.rating }} 星</strong>
        <p>{{ order.review.comment }}</p>
      </div>
      <div v-if="order.cancelReason" class="Passenger-reviewResult">
        <span>取消原因</span>
        <p>{{ order.cancelReason }}</p>
      </div>
    </section>
  </article>
</template>

<style scoped>
.Passenger-orderDetailCard {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  background: #FFFFFF;
}

.Passenger-orderDetailCard header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.Passenger-orderDetailCard header div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.Passenger-orderDetailCard header span,
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

.Passenger-detailSection {
  display: grid;
  gap: 10px;
}

.Passenger-detailSectionHead {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  color: #111111;
}

.Passenger-detailSectionHead strong {
  font-size: 14px;
}

.Passenger-timelineList {
  display: grid;
  gap: 8px;
}

.Passenger-timelineList article {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-timelineList i {
  width: 9px;
  height: 9px;
  margin-top: 5px;
  border-radius: 999px;
  background: #39FF14;
  box-shadow: 0 0 0 4px rgba(57, 255, 20, 0.16);
}

.Passenger-timelineList div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.Passenger-timelineList strong {
  color: #111111;
  font-size: 13px;
}

.Passenger-timelineList span,
.Passenger-timelineList p,
.Passenger-mutedText {
  margin: 0;
  color: #757575;
  font-size: 12px;
  line-height: 1.45;
}

.Passenger-tripSummary,
.Passenger-serviceGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.Passenger-tripSummary div,
.Passenger-serviceGrid article {
  display: grid;
  gap: 4px;
  min-height: 28px;
  padding: 8px;
  border-radius: 7px;
  background: #F7F8F8;
  color: #555555;
}

.Passenger-serviceGrid article {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
}

.Passenger-tripSummary div:nth-child(6) {
  grid-column: 1 / -1;
}

.Passenger-tripSummary span,
.Passenger-serviceGrid span {
  color: #757575;
  font-size: 11px;
}

.Passenger-tripSummary strong,
.Passenger-serviceGrid strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #111111;
  line-height: 1.35;
}

.Passenger-serviceGrid button {
  grid-column: 1 / -1;
  min-height: 30px;
  border: 0;
  border-radius: 7px;
  background: #111111;
  color: #FFFFFF;
  font-weight: 900;
  cursor: pointer;
}

.Passenger-blockAction,
.Passenger-appealToggle,
.Passenger-appealForm button {
  min-height: 38px;
  border: 0;
  border-radius: 8px;
  background: #111111;
  color: #FFFFFF;
  font-weight: 900;
  cursor: pointer;
}

.Passenger-appealToggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
}

.Passenger-appealForm {
  display: grid;
  gap: 8px;
}

.Passenger-appealForm select,
.Passenger-appealForm textarea,
.Passenger-appealForm input {
  width: 100%;
  min-width: 0;
  border: 1px solid #E3E5E5;
  border-radius: 8px;
  padding: 9px 10px;
  background: #FFFFFF;
  color: #111111;
  font: inherit;
  outline: none;
}

.Passenger-reviewResult {
  display: grid;
  gap: 6px;
  padding: 10px;
  border-radius: 8px;
  background: #F7F8F8;
}

.Passenger-reviewResult strong {
  color: #111111;
}

.Passenger-reviewResult p {
  margin: 0;
  color: #555555;
  font-size: 12px;
  line-height: 1.45;
}
</style>

<script setup>
import { FileText, MessageSquare, Navigation, QrCode, RefreshCw, Repeat2, Users } from 'lucide-vue-next'

defineProps({
  order: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['action'])

const actionIcons = {
  联系乘客: MessageSquare,
  联系司机: MessageSquare,
  导航: Navigation,
  订单详情: FileText,
  查看进程: FileText,
  再次拼车: Repeat2,
  重新选择: RefreshCw,
  行程码: QrCode
}

const formatPrice = (price) => {
  const numericPrice = Number(price)
  return Number.isFinite(numericPrice) ? `¥ ${price}` : price
}
</script>

<template>
  <article class="order-card">
    <div class="order-head">
      <span>{{ order.date }}</span>
      <b :class="{ live: order.status === '进行中' }">{{ order.status }}</b>
    </div>
    <strong>{{ order.route }}</strong>
    <div class="order-meta">
      <span><Users :size="12" /> {{ order.count }}</span>
      <span>{{ formatPrice(order.price) }}</span>
    </div>
    <div class="order-actions">
      <button v-for="action in order.actions" :key="action" type="button" @click.stop="emit('action', action)">
        <component :is="actionIcons[action] || FileText" :size="15" />
        {{ action }}
      </button>
    </div>
  </article>
</template>

<script setup>
import { Car, CarFront, ClipboardList, UserRound } from 'lucide-vue-next'

defineProps({
  active: {
    type: String,
    required: true
  }
})

const items = [
  ['ride', '乘车', Car],
  ['drive', '开车', CarFront],
  ['orders', '订单', ClipboardList],
  ['profile', '我的', UserRound]
]
</script>

<template>
  <nav class="bottom-nav">
    <a v-for="[key, label, Icon] in items" :key="key" :class="{ active: active === key }">
      <component :is="Icon" :size="18" />
      <span>{{ label }}</span>
    </a>
  </nav>
</template>


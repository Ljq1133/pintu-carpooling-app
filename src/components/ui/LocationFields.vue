<script setup>
import { Circle, MapPin } from 'lucide-vue-next'

defineProps({
  startLabel: {
    type: String,
    default: '我的位置'
  },
  endLabel: {
    type: String,
    default: '输入目的地'
  }
})
</script>

<template>
  <div class="location-fields">
    <div class="field-row">
      <Circle :size="10" fill="#39FF14" color="#39FF14" />
      <span>{{ startLabel }}</span>
    </div>
    <div class="field-line"></div>
    <div class="field-row muted">
      <MapPin :size="13" />
      <span>{{ endLabel }}</span>
    </div>
  </div>
</template>


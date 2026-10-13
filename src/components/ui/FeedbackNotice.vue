<script setup>
import { AlertCircle, CheckCircle2, Info, Loader2 } from 'lucide-vue-next'

const props = defineProps({
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['info', 'success', 'error', 'loading'].includes(value)
  },
  message: {
    type: String,
    default: ''
  }
})

const iconMap = {
  info: Info,
  success: CheckCircle2,
  error: AlertCircle,
  loading: Loader2
}
</script>

<template>
  <p v-if="message" :class="['feedback-notice', `feedback-notice--${props.type}`]" role="status">
    <component :is="iconMap[props.type]" :class="{ spinning: props.type === 'loading' }" :size="15" />
    <span>{{ message }}</span>
  </p>
</template>

<style scoped>
.feedback-notice {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.feedback-notice--info,
.feedback-notice--loading {
  border-color: #D8DBDD;
  background: #F7F8F7;
  color: #39413C;
}

.feedback-notice--success {
  border-color: #BFFFB4;
  background: #E9FFE4;
  color: #16610A;
}

.feedback-notice--error {
  border-color: #FFD2D2;
  background: #FFECEC;
  color: #B42318;
}

.spinning {
  animation: feedback-spin 0.9s linear infinite;
}

@keyframes feedback-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

<script setup>
/**
 * 通用二次确认弹窗。
 * 用于不可逆操作：取消订单、付款、封禁、强制结算、信用调整等。
 * 通过 v-model 控制显隐，确认/取消通过 emit 抛出。
 */
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '请确认操作' },
  message: { type: String, default: '此操作不可撤销，确定要继续吗？' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  danger: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

function handleConfirm() {
  if (props.loading) return
  emit('confirm')
}

function handleCancel() {
  if (props.loading) return
  visible.value = false
  emit('cancel')
}
</script>

<template>
  <div v-if="visible" class="confirm-mask" role="dialog" aria-modal="true" @click.self="handleCancel">
    <div class="confirm-card">
      <h3 class="confirm-title">{{ title }}</h3>
      <p class="confirm-message"><slot>{{ message }}</slot></p>
      <div class="confirm-actions">
        <button type="button" class="btn-cancel" :disabled="loading" @click="handleCancel">
          {{ cancelText }}
        </button>
        <button
          type="button"
          class="btn-confirm"
          :class="{ danger }"
          :disabled="loading"
          @click="handleConfirm"
        >
          {{ loading ? '处理中…' : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.confirm-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.confirm-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: min(360px, 90vw);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
}
.confirm-title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #111;
}
.confirm-message {
  margin: 0 0 20px;
  color: #444;
  line-height: 1.6;
  white-space: pre-wrap;
}
.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.btn-cancel,
.btn-confirm {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-cancel {
  background: #f3f4f6;
  border-color: #e5e7eb;
  color: #111;
}
.btn-confirm {
  background: #39ff14;
  color: #111;
  font-weight: 600;
}
.btn-confirm.danger {
  background: #ef4444;
  color: #fff;
}
.btn-cancel:disabled,
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

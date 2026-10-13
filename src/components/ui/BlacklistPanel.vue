<script setup>
import { ShieldOff, UserRoundX, X } from 'lucide-vue-next'

defineProps({
  items: {
    type: Array,
    default: () => []
  },
  busyId: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
  }
})

defineEmits(['unblock'])

const roleLabel = (role) => role === 'driver' ? '司机' : role === 'passenger' ? '乘客' : '用户'
</script>

<template>
  <section class="blacklist-panel">
    <header>
      <ShieldOff :size="18" />
      <strong>安全与隐私 · 黑名单</strong>
      <b>{{ items.length }}</b>
    </header>
    <p v-if="error" class="blacklist-error">{{ error }}</p>
    <div v-if="items.length" class="blacklist-list">
      <article v-for="item in items" :key="item.blockedUserId">
        <UserRoundX :size="20" />
        <div>
          <strong>{{ item.blockedName || '已拉黑用户' }}</strong>
          <span>{{ roleLabel(item.role) }} · {{ item.reason || '用户主动拉黑' }}</span>
          <small>{{ item.createdAt }}</small>
        </div>
        <button
          type="button"
          title="解除拉黑"
          :disabled="busyId === item.blockedUserId"
          @click="$emit('unblock', item.blockedUserId)"
        >
          <X :size="16" />
        </button>
      </article>
    </div>
    <p v-else class="blacklist-empty">暂无黑名单用户。</p>
  </section>
</template>

<style scoped>
.blacklist-panel {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid #E3E5E5;
  background: #FFFFFF;
}

.blacklist-panel header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
}

.blacklist-panel header b {
  color: #B42318;
}

.blacklist-list {
  display: grid;
  gap: 8px;
}

.blacklist-list article {
  display: grid;
  grid-template-columns: auto 1fr 36px;
  gap: 10px;
  align-items: center;
  padding: 10px;
  background: #F7F8F8;
}

.blacklist-list div {
  display: grid;
  gap: 2px;
}

.blacklist-list span,
.blacklist-list small,
.blacklist-empty {
  color: #70747A;
  font-size: 12px;
}

.blacklist-list button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 0;
  background: #111111;
  color: #FFFFFF;
  cursor: pointer;
}

.blacklist-list button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.blacklist-error {
  margin: 0;
  color: #B42318;
  font-size: 12px;
  font-weight: 800;
}

.blacklist-empty {
  margin: 0;
}
</style>

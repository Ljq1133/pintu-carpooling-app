<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Clock3, MapPin, Route, ShieldCheck } from 'lucide-vue-next'
import LogoMark from '../components/ui/LogoMark.vue'
import { getPublicShareView } from '../api/publicSafetyApi.js'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const share = ref(null)

const points = computed(() => {
  const sharedPoints = share.value?.trajectory || share.value?.points || []
  if (sharedPoints.length) return sharedPoints
  const startLng = share.value?.startLng
  const startLat = share.value?.startLat
  const endLng = share.value?.endLng
  const endLat = share.value?.endLat
  return [
    startLng != null && startLat != null ? { longitude: startLng, latitude: startLat, reportedAt: '起点已模糊' } : null,
    endLng != null && endLat != null ? { longitude: endLng, latitude: endLat, reportedAt: '终点已模糊' } : null
  ].filter(Boolean)
})
const statusText = computed(() => share.value?.orderStatus || share.value?.status || '进行中')
const passengerName = computed(() => share.value?.passengerName || share.value?.passenger || '乘客')
const driverName = computed(() => share.value?.driverName || share.value?.driver || '司机')
const isInactive = computed(() => {
  const status = String(statusText.value || '').toUpperCase()
  return Boolean(error.value) || ['COMPLETED', 'CANCELLED', 'EXPIRED', 'REVOKED', '已完成', '已取消'].includes(status)
})

function formatPoint(point) {
  const lng = point.lng ?? point.longitude
  const lat = point.lat ?? point.latitude
  if (lng == null || lat == null) return '坐标已脱敏'
  return `${Number(lng).toFixed(4)}, ${Number(lat).toFixed(4)}`
}

onMounted(async () => {
  try {
    share.value = await getPublicShareView(route.params.token)
  } catch (requestError) {
    error.value = requestError.message || '分享链接已失效'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="share-page">
    <section class="share-shell">
      <header class="share-header">
        <RouterLink class="share-brand" to="/login">
          <LogoMark size="small" />
          <strong>拼途 PinTu</strong>
        </RouterLink>
        <span :class="['share-status', { inactive: isInactive }]">
          {{ isInactive ? '共享已停止' : '安全共享中' }}
        </span>
      </header>

      <section v-if="loading" class="share-panel">
        <ShieldCheck :size="22" />
        <h1>正在校验分享链接</h1>
        <p>我们只会展示脱敏后的行程信息。</p>
      </section>

      <section v-else-if="error" class="share-panel">
        <ShieldCheck :size="22" />
        <h1>分享链接不可用</h1>
        <p>{{ error }}</p>
      </section>

      <template v-else>
        <section class="share-hero">
          <span>PUBLIC TRIP WATCH</span>
          <h1>{{ share.route || `${share.start || '出发地'} → ${share.end || '目的地'}` }}</h1>
          <p>{{ passengerName }} 与 {{ driverName }} 的行程守护页。精确身份与坐标已隐藏。</p>
        </section>

        <section class="share-grid">
          <article>
            <Route :size="18" />
            <span>行程状态</span>
            <strong>{{ statusText }}</strong>
          </article>
          <article>
            <Clock3 :size="18" />
            <span>链接有效期</span>
            <strong>{{ share.expiresAt || '已按行程状态控制' }}</strong>
          </article>
          <article>
            <ShieldCheck :size="18" />
            <span>隐私等级</span>
            <strong>姓名与轨迹均已脱敏</strong>
          </article>
        </section>

        <section class="share-panel">
          <div class="share-panel-head">
            <MapPin :size="18" />
            <strong>脱敏轨迹</strong>
          </div>
          <ol v-if="points.length" class="share-timeline">
            <li v-for="(point, index) in points" :key="`${formatPoint(point)}-${index}`">
              <i></i>
              <div>
                <strong>{{ formatPoint(point) }}</strong>
                <span>{{ point.reportedAt || point.createdAt || '时间已隐藏' }}</span>
              </div>
            </li>
          </ol>
          <p v-else>暂无可公开展示的轨迹点。</p>
        </section>
      </template>
    </section>
  </main>
</template>

<style scoped>
.share-page {
  min-height: 100vh;
  background: #F4F5F5;
  color: #111111;
}

.share-shell {
  width: min(920px, calc(100% - 32px));
  margin: 0 auto;
  padding: 28px 0 40px;
}

.share-header,
.share-brand,
.share-grid,
.share-panel-head,
.share-timeline li {
  display: flex;
  align-items: center;
}

.share-header {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.share-brand {
  gap: 10px;
  color: inherit;
  text-decoration: none;
}

.share-status {
  padding: 7px 10px;
  border-radius: 6px;
  background: #111111;
  color: #39FF14;
  font-size: 12px;
  font-weight: 900;
}

.share-status.inactive {
  background: #E4E6E6;
  color: #555555;
}

.share-hero,
.share-panel,
.share-grid article {
  border: 1px solid #E1E4E4;
  border-radius: 8px;
  background: #FFFFFF;
}

.share-hero {
  display: grid;
  gap: 10px;
  padding: 24px;
}

.share-hero span {
  color: #5F6767;
  font-size: 12px;
  font-weight: 900;
}

.share-hero h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.2;
}

.share-hero p,
.share-panel p,
.share-timeline span {
  margin: 0;
  color: #606666;
  line-height: 1.5;
}

.share-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 12px 0;
}

.share-grid article {
  display: grid;
  gap: 6px;
  min-height: 96px;
  padding: 14px;
}

.share-grid span {
  color: #666666;
  font-size: 12px;
}

.share-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.share-panel h1 {
  margin: 0;
}

.share-panel-head {
  gap: 8px;
}

.share-timeline {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.share-timeline li {
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #F7F8F8;
}

.share-timeline i {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #39FF14;
  box-shadow: 0 0 0 4px rgba(57, 255, 20, 0.16);
}

.share-timeline div {
  display: grid;
  gap: 3px;
}

@media (max-width: 640px) {
  .share-grid {
    grid-template-columns: 1fr;
  }

  .share-hero h1 {
    font-size: 22px;
  }
}
</style>

<script setup>
import { ref } from 'vue'
import { CarFront, ClipboardList, PlugZap } from 'lucide-vue-next'
import DriverWorkspace from './DriverWorkspace.vue'
import AdminWorkspace from './AdminWorkspace.vue'
import IntegrationWorkspace from './IntegrationWorkspace.vue'
import { useSharedPlatform } from '../../platform/sharedPlatform.js'

const activeTab = ref('driver')
const { cState: state, driverApi, adminApi } = useSharedPlatform()

const tabs = [
  ['driver', '司机端', CarFront],
  ['admin', '管理端', ClipboardList],
  ['integration', '联调准备', PlugZap]
]

</script>

<template>
  <section class="kit-section c-workbench">
    <div class="section-heading">
      <h2>07 C 组跨端工作台</h2>
      <p>司机端与乘客端共享请求和订单状态，管理端实时读取同一份 mock 平台数据。</p>
    </div>

    <div class="c-tabbar" role="tablist" aria-label="C 组工作模块">
      <button
        v-for="[key, label, Icon] in tabs"
        :key="key"
        :class="{ active: activeTab === key }"
        @click="activeTab = key"
      >
        <component :is="Icon" :size="18" />
        {{ label }}
      </button>
    </div>

    <DriverWorkspace v-if="activeTab === 'driver'" :driver-api="driverApi" />
    <AdminWorkspace v-else-if="activeTab === 'admin'" :admin-api="adminApi" />
    <IntegrationWorkspace v-else :state="state" />
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LogOut } from 'lucide-vue-next'
import LogoMark from '../components/ui/LogoMark.vue'
import Passenger from '../components/Passenger/Passenger.vue'
import DriverWorkspace from '../components/c-workspace/DriverWorkspace.vue'
import AdminWorkspace from '../components/c-workspace/AdminWorkspace.vue'
import { useSharedPlatform } from '../platform/sharedPlatform.js'
import { clearSession, getSession } from '../auth/session.js'

const route = useRoute()
const router = useRouter()
const session = getSession()
const role = computed(() => route.params.role)
const { driverApi, adminApi } = useSharedPlatform()

function logout() {
  clearSession()
  router.replace('/login')
}
</script>

<template>
  <div class="pintu-app">
    <header class="pintu-header">
      <RouterLink class="pintu-brand" :to="`/app/${role}`">
        <LogoMark size="small" />
        <span><strong>拼途 PinTu</strong><small>可信的私人拼车社区</small></span>
      </RouterLink>
      <div class="workspace-identity">
        <span>{{ session?.user?.name }}</span>
        <b>{{ role }}</b>
        <button type="button" title="退出登录" @click="logout"><LogOut :size="17" /></button>
      </div>
    </header>
    <main :class="['pintu-main', `role-${role}`]">
      <Passenger v-if="role === 'passenger'" />
      <DriverWorkspace v-else-if="role === 'driver'" :driver-api="driverApi" />
      <AdminWorkspace v-else :admin-api="adminApi" />
    </main>
  </div>
</template>


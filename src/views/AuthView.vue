<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  KeyRound,
  MessageSquareText,
  ShieldCheck,
  Smartphone
} from 'lucide-vue-next'
import LogoMark from '../components/ui/LogoMark.vue'
import { authApi } from '../api/authApi.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const sendingCode = ref(false)
const error = ref('')
const notice = ref('')
const countdown = ref(0)
const form = reactive({
  username: '',
  phone: '',
  password: '',
  newPassword: '',
  name: '',
  code: '',
  role: 'passenger'
})

const mode = computed(() => route.meta.mode)
const adminOnly = computed(() => Boolean(route.meta.adminOnly))
const content = computed(() => ({
  login: adminOnly.value
    ? { eyebrow: 'ADMIN ACCESS', title: '管理后台登录', action: '进入后台', icon: ShieldCheck }
    : { eyebrow: 'WELCOME BACK', title: '继续你的拼途', action: '登录', icon: KeyRound },
  register: { eyebrow: 'NEW ROUTE', title: '创建拼途账号', action: '注册并出发', icon: Smartphone },
  code: { eyebrow: 'QUICK PASS', title: '验证码登录', action: '安全登录', icon: MessageSquareText },
  forgot: { eyebrow: 'ACCOUNT RECOVERY', title: '重设你的密码', action: '确认重置', icon: ShieldCheck }
}[mode.value]))
const needsCode = computed(() => ['register', 'code', 'forgot'].includes(mode.value))

function startCountdown() {
  countdown.value = 60
  const timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) window.clearInterval(timer)
  }, 1000)
}

async function sendCode() {
  error.value = ''
  notice.value = ''
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    error.value = '请输入有效的 11 位手机号'
    return
  }
  try {
    sendingCode.value = true
    const purpose = mode.value === 'register'
      ? 'REGISTER'
      : mode.value === 'code' ? 'LOGIN' : 'RESET_PASSWORD'
    const result = mode.value === 'forgot'
      ? await authApi.forgotPassword(form.phone)
      : await authApi.sendCode(form.phone, purpose)
    notice.value = result?.debugCode
      ? `课程环境验证码：${result.debugCode}`
      : '验证码已发送，请查看短信'
    startCountdown()
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    sendingCode.value = false
  }
}

async function submit() {
  if (loading.value) return
  error.value = ''
  notice.value = ''
  try {
    loading.value = true
    let result
    if (mode.value === 'login') {
      result = await authApi.passwordLogin({
        username: form.username,
        password: form.password,
        role: adminOnly.value ? 'admin' : form.role
      })
    } else if (mode.value === 'register') {
      result = await authApi.register({
        phone: form.phone,
        password: form.password,
        name: form.name,
        code: form.code
      })
    } else if (mode.value === 'code') {
      result = await authApi.codeLogin({ phone: form.phone, code: form.code })
    } else {
      await authApi.resetPassword({
        phone: form.phone,
        code: form.code,
        newPassword: form.newPassword
      })
      await router.push({ path: '/login', query: { reset: 'success' } })
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    await router.push(redirect || (adminOnly.value ? '/app/admin' : `/app/${result.user.role}`))
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-stage">
    <section class="auth-story">
      <div class="auth-brand">
        <LogoMark size="small" />
        <strong>拼途 PinTu</strong>
      </div>
      <div class="auth-story-copy">
        <span>PRIVATE CARPOOLING / SHANGHAI</span>
        <h1>让顺路的人，<br>在城市里相遇。</h1>
        <p>实名、双向确认、行程守护。每一次同行都有清晰的边界与可信的记录。</p>
      </div>
      <div class="route-art" aria-hidden="true">
        <i class="route-dot route-dot-a"></i>
        <i class="route-dot route-dot-b"></i>
        <i class="route-dot route-dot-c"></i>
      </div>
      <div class="auth-proof">
        <ShieldCheck :size="18" />
        <span>端到端身份校验</span>
        <span>AES-GCM 隐私保护</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-heading">
          <component :is="content.icon" :size="22" />
          <span>{{ content.eyebrow }}</span>
          <h2>{{ content.title }}</h2>
        </div>

        <p v-if="route.query.reset === 'success'" class="auth-message success">
          密码已重置，请使用新密码登录。
        </p>
        <p v-if="adminOnly" class="auth-message success">
          管理后台入口仅允许管理员账号登录，权限会按后台角色自动收敛。
        </p>

        <form @submit.prevent="submit">
          <label v-if="mode === 'login'">
            <span>账号或手机号</span>
            <input v-model.trim="form.username" required autocomplete="username" placeholder="输入账号或手机号">
          </label>
          <label v-if="mode === 'register'">
            <span>昵称</span>
            <input v-model.trim="form.name" required maxlength="64" placeholder="同行者如何称呼你">
          </label>
          <label v-if="mode !== 'login'">
            <span>手机号</span>
            <input v-model.trim="form.phone" required inputmode="numeric" maxlength="11" placeholder="11 位手机号">
          </label>
          <label v-if="mode === 'login' || mode === 'register'">
            <span>密码</span>
            <input
              v-model="form.password"
              required
              type="password"
              :minlength="mode === 'login' ? 6 : 8"
              :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              :placeholder="mode === 'login' ? '输入登录密码' : '至少 8 位'"
            >
          </label>
          <label v-if="mode === 'forgot'">
            <span>新密码</span>
            <input v-model="form.newPassword" required type="password" minlength="8" autocomplete="new-password" placeholder="至少 8 位">
          </label>
          <label v-if="mode === 'login' && !adminOnly">
            <span>登录身份</span>
            <select v-model="form.role">
              <option value="passenger">乘客</option>
              <option value="driver">司机</option>
              <option value="admin">管理员</option>
            </select>
          </label>
          <label v-if="needsCode">
            <span>验证码</span>
            <div class="code-field">
              <input v-model.trim="form.code" required inputmode="numeric" maxlength="6" placeholder="6 位验证码">
              <button type="button" :disabled="sendingCode || countdown > 0" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s` : sendingCode ? '发送中' : '发送验证码' }}
              </button>
            </div>
          </label>

          <p v-if="error" class="auth-message error" role="alert">{{ error }}</p>
          <p v-if="notice" class="auth-message success">{{ notice }}</p>

          <button class="auth-submit" type="submit" :disabled="loading">
            <span>{{ loading ? '处理中...' : content.action }}</span>
            <ArrowRight :size="18" />
          </button>
        </form>

        <nav v-if="!adminOnly" class="auth-links" aria-label="认证方式">
          <RouterLink v-if="mode !== 'login'" to="/login">密码登录</RouterLink>
          <RouterLink v-if="mode !== 'code'" to="/code-login">验证码登录</RouterLink>
          <RouterLink v-if="mode !== 'register'" to="/register">注册账号</RouterLink>
          <RouterLink v-if="mode !== 'forgot'" to="/forgot-password">忘记密码</RouterLink>
        </nav>
        <nav v-else class="auth-links" aria-label="后台认证方式">
          <RouterLink to="/login">返回普通登录</RouterLink>
        </nav>
      </div>
    </section>
  </main>
</template>

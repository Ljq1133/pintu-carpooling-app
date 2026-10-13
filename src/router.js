import { createRouter, createWebHistory } from 'vue-router'
import AuthView from './views/AuthView.vue'
import TripShareView from './views/TripShareView.vue'
import WorkspaceView from './views/WorkspaceView.vue'
import { getSession, isAuthenticated } from './auth/session.js'

const routes = [
  { path: '/', redirect: '/app/passenger' },
  { path: '/login', component: AuthView, meta: { guest: true, mode: 'login' } },
  { path: '/admin/login', component: AuthView, meta: { guest: true, mode: 'login', adminOnly: true } },
  { path: '/register', component: AuthView, meta: { guest: true, mode: 'register' } },
  { path: '/code-login', component: AuthView, meta: { guest: true, mode: 'code' } },
  { path: '/forgot-password', component: AuthView, meta: { guest: true, mode: 'forgot' } },
  { path: '/share/:token', component: TripShareView },
  {
    path: '/app/passenger/profile/:section',
    component: WorkspaceView,
    meta: { requiresAuth: true }
  },
  {
    path: '/app/:role(passenger|driver|admin)',
    component: WorkspaceView,
    meta: { requiresAuth: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const session = getSession()
  if (to.meta.requiresAuth && !isAuthenticated()) {
    const loginPath = to.params.role === 'admin' ? '/admin/login' : '/login'
    return { path: loginPath, query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && isAuthenticated()) {
    return `/app/${session?.user?.role || 'passenger'}`
  }
  const targetRole = to.path.startsWith('/app/passenger/profile/') ? 'passenger' : to.params.role
  if (to.meta.requiresAuth && targetRole !== session?.user?.role) {
    return `/app/${session?.user?.role || 'passenger'}`
  }
  return true
})


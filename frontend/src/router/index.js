import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 / 注册', public: true }
  },
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { title: '智能对话', icon: 'ChatDotRound' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: '数据看板', icon: 'DataAnalysis' }
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('@/views/DietRecordView.vue'),
    meta: { title: '饮食记录', icon: 'Bowl' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '健康档案', icon: 'User' }
  },
  // 404 兜底
  {
    path: '/:pathMatch(.*)*',
    redirect: '/chat'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫：未登录跳转到 /login
router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.public) return true          // 公开页面直接放行
  if (!authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} · 小膳`
    : '小膳 · 智能饮食管理助手'
})

export default router

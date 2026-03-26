<template>
  <!-- 登录页不显示侧边栏 -->
  <router-view v-if="isLoginPage" />

  <!-- 主应用框架 -->
  <div v-else class="app-shell">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="logo-icon">🥗</div>
        <div class="logo-text">
          <span class="logo-name">小膳</span>
          <span class="logo-sub">智能饮食助手</span>
        </div>
      </div>

      <!-- 用户信息区 -->
      <div class="sidebar-user" @click="router.push('/profile')">
        <div class="user-avatar">{{ avatarChar }}</div>
        <div class="user-info">
          <span class="user-name">{{ displayName }}</span>
          <span class="user-id">ID: {{ authStore.userId }}</span>
        </div>
        <el-tooltip content="前往健康档案" placement="right">
          <el-icon class="user-edit-icon"><EditPen /></el-icon>
        </el-tooltip>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- 底部：健康提示 + 退出 -->
      <div class="sidebar-footer">
        <div class="health-tip">
          <el-icon color="var(--color-primary)"><InfoFilled /></el-icon>
          <span>{{ dailyTip }}</span>
        </div>

        <!-- 退出登录按钮 -->
        <el-popconfirm
          title="确定要退出登录吗？"
          confirm-button-text="退出"
          cancel-button-text="取消"
          confirm-button-type="danger"
          width="200"
          @confirm="doLogout"
        >
          <template #reference>
            <button class="logout-btn">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </button>
          </template>
        </el-popconfirm>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <!-- keep-alive 保活 ChatView，切换路由再回来对话记录不丢失 -->
          <keep-alive include="ChatView">
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'

const route     = useRoute()
const router    = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

const isLoginPage = computed(() => route.path === '/login')

const navItems = [
  { path: '/chat',      icon: 'ChatDotRound', label: '智能对话' },
  { path: '/dashboard', icon: 'DataAnalysis', label: '数据看板' },
  { path: '/records',   icon: 'Bowl',         label: '饮食记录' },
  { path: '/profile',   icon: 'User',         label: '健康档案' },
]

const displayName = computed(() =>
  userStore.profile?.nickname || authStore.nickname || authStore.userId || '用户'
)
const avatarChar = computed(() => displayName.value.charAt(0).toUpperCase())

const tips = [
  '每日喝水 8 杯，保持身体活力',
  '早餐吃好，午餐吃饱，晚餐吃少',
  '细嚼慢咽，有助消化和控制食量',
  '蔬菜水果要多吃，维生素不可少',
  '少油少盐，健康每一餐',
  '规律进食，保持血糖稳定',
]
const dailyTip = ref(tips[new Date().getDate() % tips.length])

async function doLogout() {
  await authStore.logout()
  userStore.profile = null
  ElMessage.success('已退出登录')
  router.replace('/login')
}

onMounted(() => {
  if (authStore.isLoggedIn && !userStore.profile) {
    userStore.fetchProfile()
  }
})
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ---- 侧边栏 ---- */
.sidebar {
  width: var(--sidebar-width);
  background: var(--color-card);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 2px 0 16px rgba(0,0,0,0.04);
  z-index: 10;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px 16px;
  border-bottom: 1px solid var(--color-border);
}
.logo-icon { font-size: 28px; }
.logo-text  { display: flex; flex-direction: column; }
.logo-name  { font-size: 17px; font-weight: 700; color: var(--color-primary); letter-spacing: 0.5px; }
.logo-sub   { font-size: 11px; color: var(--color-text-muted); }

/* 用户信息 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-radius: 10px;
  margin: 10px 10px 4px;
  transition: var(--transition);
  position: relative;
}
.sidebar-user:hover { background: var(--color-primary-light); }
.sidebar-user:hover .user-edit-icon { opacity: 1; }

.user-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #00c9a7);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}
.user-info    { flex: 1; overflow: hidden; }
.user-name    { display: block; font-size: 13px; font-weight: 600; color: var(--color-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-id      { display: block; font-size: 10.5px; color: var(--color-text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-edit-icon { color: var(--color-primary); opacity: 0; transition: var(--transition); font-size: 14px; flex-shrink: 0; }

/* 导航 */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  text-decoration: none;
  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 500;
  transition: var(--transition);
}
.nav-item:hover  { background: var(--color-primary-light); color: var(--color-primary); }
.nav-item.active {
  background: linear-gradient(135deg, var(--color-primary), #00c9a7);
  color: #fff;
  box-shadow: 0 4px 14px rgba(66,184,131,0.3);
}
.nav-icon { font-size: 18px; }

/* 底部 */
.sidebar-footer {
  padding: 12px 14px 18px;
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.health-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 11.5px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

/* 退出按钮 */
.logout-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  padding: 8px 12px;
  border: 1.5px solid #fde8e8;
  border-radius: 8px;
  background: #fff5f5;
  color: var(--color-danger);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}
.logout-btn:hover {
  background: #fee2e2;
  border-color: var(--color-danger);
  box-shadow: 0 2px 8px rgba(245,108,108,0.2);
}

/* 主内容区 */
.main-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>

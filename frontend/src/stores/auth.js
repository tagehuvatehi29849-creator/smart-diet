import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { useChatStore } from '@/stores/chat'

/**
 * 认证状态管理（Pinia）
 * 持久化到 localStorage，刷新不丢失登录状态。
 *
 * 存储 Key：
 *   sd_token    — 访问令牌
 *   sd_userId   — 业务 userId
 *   sd_username — 用户名
 *   sd_nickname — 昵称
 *   sd_expire   — 过期时间戳（ms）
 */
export const useAuthStore = defineStore('auth', () => {
  const token    = ref(localStorage.getItem('sd_token')    || '')
  const userId   = ref(localStorage.getItem('sd_userId')   || '')
  const username = ref(localStorage.getItem('sd_username') || '')
  const nickname = ref(localStorage.getItem('sd_nickname') || '')
  const expireAt = ref(Number(localStorage.getItem('sd_expire') || 0))

  const isLoggedIn = computed(() => {
    if (!token.value) return false
    // 本地过期检查（精准控制由后端 Redis TTL 决定，这里做前置拦截）
    if (expireAt.value && Date.now() > expireAt.value) {
      _clearStorage()
      return false
    }
    return true
  })

  /** 注册：调后端 /v1/auth/register */
  async function register(dto) {
    const res = await authApi.register(dto)
    const loginVO = res.data?.data
    _persist(loginVO)
    return loginVO
  }

  /** 登录：调后端 /v1/auth/login */
  async function login(dto) {
    const res = await authApi.login(dto)
    const loginVO = res.data?.data
    _persist(loginVO)
    return loginVO
  }

  /** 退出：通知后端销毁 Token，清除本地状态（包含对话历史） */
  async function logout() {
    try { await authApi.logout() } catch (_) {}
    useChatStore().clearStorage()   // 清除本账号的对话持久化数据
    _clearStorage()
  }

  /** 静默退出（Token 已过期，只清本地） */
  function silentLogout() {
    useChatStore().clearStorage()
    _clearStorage()
  }

  function _persist(loginVO) {
    token.value    = loginVO.token
    userId.value   = loginVO.userId
    username.value = loginVO.username
    nickname.value = loginVO.nickname
    expireAt.value = loginVO.expireAt

    localStorage.setItem('sd_token',    loginVO.token)
    localStorage.setItem('sd_userId',   loginVO.userId)
    localStorage.setItem('sd_username', loginVO.username)
    localStorage.setItem('sd_nickname', loginVO.nickname)
    localStorage.setItem('sd_expire',   String(loginVO.expireAt))
    // 兼容 userStore 用到的 key
    localStorage.setItem('userId', loginVO.userId)
  }

  function _clearStorage() {
    token.value    = ''
    userId.value   = ''
    username.value = ''
    nickname.value = ''
    expireAt.value = 0
    ;['sd_token','sd_userId','sd_username','sd_nickname','sd_expire','userId']
      .forEach(k => localStorage.removeItem(k))
  }

  return {
    token, userId, username, nickname, expireAt,
    isLoggedIn,
    register, login, logout, silentLogout,
  }
})

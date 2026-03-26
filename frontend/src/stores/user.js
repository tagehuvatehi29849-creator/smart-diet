import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { profileApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

export const useUserStore = defineStore('user', () => {
  const profile = ref(null)
  const loading = ref(false)

  // 动态读取 authStore 的 userId，避免循环依赖（懒引用）
  const userId = computed(() => {
    try { return useAuthStore().userId } catch { return '' }
  })

  async function fetchProfile() {
    if (!userId.value) return
    loading.value = true
    try {
      const res = await profileApi.get(userId.value)
      profile.value = res.data?.data
    } catch (_) {
      // 未创建画像时静默忽略
    } finally {
      loading.value = false
    }
  }

  async function saveProfile(dto) {
    // 注册时已自动创建 user_profile 记录，所以这里始终走 update。
    // 若 Redis 缓存/网络原因导致 profile.value 为空，先尝试 update；
    // 若后端明确返回 USER_PROFILE_NOT_FOUND（画像不存在）才降级 create。
    const payload = { ...dto, userId: userId.value }
    let res
    try {
      res = await profileApi.update(payload)
    } catch (err) {
      // 仅当 update 因画像不存在而 404/业务失败时，才降级 create
      const code = err?.response?.data?.code
      if (code === 20201 /* USER_PROFILE_NOT_FOUND */ || err?.response?.status === 404) {
        res = await profileApi.create(payload)
      } else {
        throw err
      }
    }
    profile.value = res.data?.data
    return profile.value
  }

  return { userId, profile, loading, fetchProfile, saveProfile }
})

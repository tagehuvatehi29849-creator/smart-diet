import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// ---- 请求拦截：自动注入 Token ----
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('sd_token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// ---- 响应拦截：统一错误处理 ----
http.interceptors.response.use(
  (res) => {
    // 业务码 20504 = Token 过期
    if (res.data?.code === 20504) {
      ElMessage.error(res.data.message || '登录已过期，请重新登录')
      localStorage.removeItem('sd_token')
      localStorage.removeItem('sd_userId')
      localStorage.removeItem('sd_nickname')
      window.location.href = '/login'
      return Promise.reject(new Error('Token invalid'))
    }
    return res
  },
  async (err) => {
    const data = err.response?.data
    // responseType:'blob' 时 data 是 Blob，需要异步读出来再解析 JSON
    if (data instanceof Blob) {
      // blob 请求的错误交给各调用方自己处理（如 downloadReport），这里只静默 reject
      return Promise.reject(err)
    }
    const msg = data?.message || err.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

// ==========================================
// 认证接口
// ==========================================
export const authApi = {
  register: (dto)  => http.post('/v1/auth/register', dto),
  login:    (dto)  => http.post('/v1/auth/login', dto),
  logout:   ()     => http.post('/v1/auth/logout'),
  info:     ()     => http.get('/v1/auth/info'),
  changePassword: (body) => http.post('/v1/auth/change-password', body),
}

// ==========================================
// 用户画像
// ==========================================
export const profileApi = {
  get:    (userId)  => http.get(`/v1/profile/${userId}`),
  create: (dto)     => http.post('/v1/profile', dto),
  update: (dto)     => http.put('/v1/profile', dto),
  delete: (userId)  => http.delete(`/v1/profile/${userId}`),
}

// ==========================================
// 饮食记录
// ==========================================
export const dietApi = {
  add:       (dto)                        => http.post('/v1/diet/record', dto),
  daily:     (userId, date)               => http.get('/v1/diet/daily',  { params: { userId, date } }),
  page:      (userId, pageNum, pageSize)  => http.get('/v1/diet/page',   { params: { userId, pageNum, pageSize } }),
  range:     (userId, startDate, endDate) => http.get('/v1/diet/range',  { params: { userId, startDate, endDate } }),
  getById:   (id)                         => http.get(`/v1/diet/${id}`),
  update:    (id, dto)                    => http.put(`/v1/diet/${id}`, dto),
  delete:    (id)                         => http.delete(`/v1/diet/${id}`),
  reanalyze: (id)                         => http.post(`/v1/diet/${id}/reanalyze`),
}

// ==========================================
// AI 对话（SSE 流式，使用原生 fetch）
// ==========================================
// ==========================================
// 报告下载
// ==========================================
export const reportApi = {
  /**
   * 下载饮食 PDF 报告（返回 blob）
   * @param {string} startDate yyyy-MM-dd
   * @param {string} endDate   yyyy-MM-dd
   */
  download: (startDate, endDate) =>
    http.get('/v1/report/download', {
      params: { startDate, endDate },
      responseType: 'blob',
      timeout: 60000,
    }),
}

export const agentApi = {
  async streamChat(payload, onDelta, onDone, onError) {
    const token = localStorage.getItem('sd_token')
    const response = await fetch('/api/v1/agent/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(payload),
    })

    if (!response.ok) {
      onError?.(`HTTP ${response.status}`)
      return
    }

    const reader  = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop()
      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const raw = line.slice(5).trim()
        if (!raw) continue
        try {
          const evt = JSON.parse(raw)
          if (evt.type === 'delta')      onDelta?.(evt.content)
          else if (evt.type === 'done')  onDone?.(evt.conversationId)
          else if (evt.type === 'error') onError?.(evt.content)
        } catch (_) {}
      }
    }
  },

  clearConversation: (convId) => http.delete(`/v1/agent/conversation/${convId}`),
}

export default http

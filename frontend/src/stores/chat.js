import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import dayjs from 'dayjs'

const STORAGE_PREFIX   = 'sd_chat_v1_'   // 版本号便于未来迁移
const MAX_CONVS        = 30              // 最多保留 30 条会话
const MAX_MSGS_PER_CONV = 200            // 每条会话最多保留 200 条消息

// ---- 存储工具函数 ----

function storageKey() {
  // 按 userId 隔离，防止多账号混用
  const userId = localStorage.getItem('sd_userId') || 'anonymous'
  return STORAGE_PREFIX + userId
}

/**
 * 序列化时：
 * - 丢弃 imageUrl（Base64 很大，超过 localStorage 限制）
 * - 将 hadImage 标记保留，让气泡能显示"[图片]"占位
 * - 将未完成的 streaming 消息标记为完成
 */
function serialize(convs, activeId) {
  const truncatedConvs = convs.slice(0, MAX_CONVS).map(conv => ({
    id:       conv.id,
    title:    conv.title,
    time:     conv.time,
    messages: conv.messages.slice(-MAX_MSGS_PER_CONV).map(msg => {
      const { imageUrl, streaming, ...rest } = msg
      return {
        ...rest,
        streaming: false,                     // 重置未完成的流
        hadImage:  !!imageUrl || !!msg.hadImage, // 记录曾有图片
      }
    }),
  }))
  return { conversations: truncatedConvs, activeConvId: activeId }
}

function save(convs, activeId) {
  try {
    localStorage.setItem(storageKey(), JSON.stringify(serialize(convs, activeId)))
  } catch (e) {
    // QuotaExceededError：尝试只保留最近 5 条会话的最近 50 条消息
    try {
      const mini = convs.slice(0, 5).map(conv => ({
        ...conv,
        messages: conv.messages.slice(-50).map(({ imageUrl, streaming, ...r }) =>
          ({ ...r, streaming: false, hadImage: !!imageUrl || !!r.hadImage })
        ),
      }))
      localStorage.setItem(storageKey(), JSON.stringify({ conversations: mini, activeConvId: activeId }))
    } catch (_) {
      console.warn('[ChatStore] localStorage 已满，对话记录无法持久化')
    }
  }
}

function load() {
  try {
    const raw = localStorage.getItem(storageKey())
    if (!raw) return null
    const data = JSON.parse(raw)
    if (!Array.isArray(data?.conversations)) return null
    return data
  } catch {
    return null
  }
}

// ---- Store ----

export const useChatStore = defineStore('chat', () => {
  /** 会话列表 [{ id, title, time, messages[] }] */
  const conversations = ref([])
  /** 当前激活的会话 ID */
  const activeConvId  = ref('')

  /** 从 localStorage 恢复 */
  const saved = load()
  if (saved) {
    conversations.value = saved.conversations
    activeConvId.value  = saved.activeConvId || ''
  }

  /** 当前会话的消息列表 */
  const messages = computed(() => {
    const conv = conversations.value.find(c => c.id === activeConvId.value)
    return conv ? conv.messages : []
  })

  /** 当前会话对象 */
  const activeConv = computed(() =>
    conversations.value.find(c => c.id === activeConvId.value) || null
  )

  // 任意状态变化时自动持久化（防抖 300ms，避免流式追加时频繁写入）
  let saveTimer = null
  function scheduleSave() {
    clearTimeout(saveTimer)
    saveTimer = setTimeout(() => save(conversations.value, activeConvId.value), 300)
  }
  watch([conversations, activeConvId], scheduleSave, { deep: true })

  // ---- 工具 ----

  function genId() {
    return Math.random().toString(36).slice(2) + Date.now().toString(36)
  }

  /** 新建对话 */
  function newChat() {
    const id = genId()
    conversations.value.unshift({
      id,
      title:    '新对话',
      time:     dayjs().format('HH:mm'),
      messages: [],
    })
    activeConvId.value = id
    return id
  }

  /** 确保存在一个活跃会话（首次进入时调用） */
  function ensureActiveConv() {
    if (!activeConvId.value || !conversations.value.find(c => c.id === activeConvId.value)) {
      if (conversations.value.length > 0) {
        activeConvId.value = conversations.value[0].id
      } else {
        newChat()
      }
    }
  }

  /** 切换到指定会话 */
  function switchConv(convId) {
    if (conversations.value.find(c => c.id === convId)) {
      activeConvId.value = convId
    }
  }

  /** 删除会话 */
  function deleteConv(convId) {
    const idx = conversations.value.findIndex(c => c.id === convId)
    if (idx !== -1) conversations.value.splice(idx, 1)
    if (activeConvId.value === convId) {
      if (conversations.value.length > 0) {
        activeConvId.value = conversations.value[0].id
      } else {
        newChat()
      }
    }
  }

  /** 向当前会话追加消息 */
  function pushMessage(msg) {
    const conv = conversations.value.find(c => c.id === activeConvId.value)
    if (conv) conv.messages.push(msg)
  }

  /** 更新会话标题和时间 */
  function updateConvMeta(convId, firstUserText) {
    const conv = conversations.value.find(c => c.id === convId)
    if (conv) {
      if (conv.title === '新对话') {
        conv.title = firstUserText.length > 18
          ? firstUserText.slice(0, 18) + '…'
          : firstUserText
      }
      conv.time = dayjs().format('HH:mm')
    }
  }

  /** 清空当前会话的消息（不删除会话本身） */
  function clearMessages(convId) {
    const conv = conversations.value.find(c => c.id === (convId || activeConvId.value))
    if (conv) conv.messages = []
  }

  /**
   * 用户登出时清除本账号的持久化数据
   * （在 auth store / logout 时调用）
   */
  function clearStorage() {
    try { localStorage.removeItem(storageKey()) } catch (_) {}
    conversations.value = []
    activeConvId.value  = ''
  }

  return {
    conversations, activeConvId, messages, activeConv,
    newChat, ensureActiveConv, switchConv, deleteConv,
    pushMessage, updateConvMeta, clearMessages, clearStorage,
  }
})

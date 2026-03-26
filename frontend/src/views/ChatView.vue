<template>
  <div class="chat-layout">
    <!-- 左侧：会话列表 -->
    <aside class="chat-sidebar">
      <div class="chat-sidebar-header">
        <span class="chat-sidebar-title">对话记录</span>
        <el-button type="primary" size="small" round @click="newChat">
          <el-icon><Plus /></el-icon> 新对话
        </el-button>
      </div>
      <div class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === activeConvId }"
          @click="switchConv(conv)"
        >
          <el-icon class="conv-icon"><ChatDotRound /></el-icon>
          <div class="conv-meta">
            <span class="conv-title">{{ conv.title }}</span>
            <span class="conv-time">{{ conv.time }}</span>
          </div>
          <el-icon class="conv-del" @click.stop="deleteConv(conv.id)"><Delete /></el-icon>
        </div>
        <div v-if="!conversations.length" class="conv-empty">
          <el-icon size="32" color="#c8d3e0"><ChatRound /></el-icon>
          <p>暂无对话记录</p>
        </div>
      </div>
    </aside>

    <!-- 右侧：对话区 -->
    <div class="chat-main">
      <!-- 顶栏 -->
      <header class="chat-header">
        <div class="chat-header-left">
          <div class="ai-avatar">🥗</div>
          <div>
            <div class="ai-name">小膳</div>
            <div class="ai-status">
              <span class="status-dot" :class="{ active: !isStreaming }"></span>
              {{ isStreaming ? '正在思考...' : '在线' }}
            </div>
          </div>
        </div>
        <div class="chat-header-actions">
          <el-tooltip content="清除当前对话上下文" placement="bottom">
            <el-button circle size="small" @click="clearContext">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="开启 RAG 知识库检索" placement="bottom">
            <el-switch v-model="enableRag" size="small" active-text="RAG" />
          </el-tooltip>
        </div>
      </header>

      <!-- 消息列表 -->
      <div class="messages-wrap" ref="messagesRef">
        <!-- 欢迎引导 -->
        <transition name="fade">
          <div v-if="messages.length === 0" class="welcome-panel">
            <div class="welcome-avatar">🥗</div>
            <h2>你好，我是小膳！</h2>
            <p>我是你的专属智能饮食管理助手，可以帮你：</p>
            <div class="welcome-tags">
              <el-tag v-for="tag in welcomeTags" :key="tag" round effect="plain" type="success"
                      class="welcome-tag" @click="quickAsk(tag)">
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </transition>

        <!-- 消息气泡 -->
        <transition-group name="slide-up" tag="div">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-row"
            :class="msg.role"
          >
            <!-- 头像 -->
            <div class="msg-avatar">
              <template v-if="msg.role === 'assistant'">🥗</template>
              <template v-else>👤</template>
            </div>
            <!-- 气泡内容 -->
            <div class="msg-bubble" :class="{ streaming: msg.streaming }">
              <!-- 用户上传的图片（显示在文字上方） -->
              <img
                v-if="msg.imageUrl"
                :src="msg.imageUrl"
                class="msg-image"
                alt="用户上传的图片"
              />
              <!-- 图片在 localStorage 中不保存，刷新后显示占位提示 -->
              <div v-else-if="msg.hadImage" class="msg-image-placeholder">
                🖼️ 图片（刷新后不保留）
              </div>
              <div
                v-if="msg.content"
                class="msg-content md-content"
                :class="{ 'typing-cursor': msg.streaming }"
                v-html="renderMd(msg.content)"
                @click="handleMsgClick"
              />
              <div v-if="msg.role === 'assistant' && !msg.streaming" class="msg-actions">
                <el-tooltip content="复制" placement="top">
                  <el-button link size="small" @click="copyMsg(msg.content)">
                    <el-icon><CopyDocument /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </div>
        </transition-group>

        <!-- 加载中气泡 -->
        <div v-if="isStreaming && !currentStreamMsg" class="message-row assistant">
          <div class="msg-avatar">🥗</div>
          <div class="msg-bubble loading-bubble">
            <span class="dot-typing"></span>
          </div>
        </div>
      </div>

      <!-- 快捷提问 -->
      <div v-if="messages.length > 0" class="quick-asks">
        <el-button
          v-for="q in quickQuestions"
          :key="q"
          size="small"
          round
          plain
          @click="quickAsk(q)"
        >{{ q }}</el-button>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <!-- 图片预览条 -->
        <div v-if="selectedImage" class="image-preview-bar">
          <div class="image-preview-wrap">
            <img :src="selectedImage" class="preview-thumb" alt="预览" />
            <el-button class="remove-img-btn" circle size="small" @click="removeImage">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <span class="preview-hint">已附加图片，可直接发送或输入文字说明</span>
        </div>

        <div class="input-wrap">
          <!-- 隐藏的文件选择框 -->
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            style="display:none"
            @change="onImageSelected"
          />
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            :placeholder="isStreaming ? '小膳正在回复中...' : (selectedImage ? '可直接发送图片，或补充文字说明...' : '描述你今天吃了什么，或者向我提问...')  "
            :disabled="isStreaming"
            resize="none"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact="() => {}"
          />
          <div class="input-footer">
            <div class="input-footer-left">
              <el-tooltip content="上传图片" placement="top">
                <el-button
                  link
                  size="small"
                  :disabled="isStreaming"
                  class="upload-img-btn"
                  @click="fileInputRef.click()"
                >
                  <el-icon size="18"><Picture /></el-icon>
                </el-button>
              </el-tooltip>
              <span class="input-hint">Enter 发送 · Shift+Enter 换行</span>
            </div>
            <el-button
              type="primary"
              :disabled="(!inputText.trim() && !selectedImage) || isStreaming"
              :loading="isStreaming"
              circle
              @click="sendMessage"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'ChatView' })

import { ref, reactive, nextTick, onMounted, computed } from 'vue'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'
import { agentApi, reportApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

marked.setOptions({ breaks: true, gfm: true })

const userStore  = useUserStore()
const chatStore  = useChatStore()
const messagesRef = ref(null)

// 从 store 读取状态（切换路由再回来，这些数据还在）
const conversations = computed(() => chatStore.conversations)
const activeConvId  = computed(() => chatStore.activeConvId)
const messages      = computed(() => chatStore.messages)

const inputText    = ref('')
const isStreaming  = ref(false)
const enableRag    = ref(true)
const selectedImage = ref('')   // Base64 Data URL
const fileInputRef  = ref(null)

const currentStreamMsg = computed(() => messages.value.find(m => m.streaming))

const welcomeTags = [
  '📊 分析我今天的饮食',
  '🥗 给我一份减脂食谱',
  '💪 增肌期该怎么吃',
  '🩺 高血压的饮食禁忌',
  '📄 生成本周饮食报告',
  '💧 我每天需要喝多少水',
]

const quickQuestions = [
  '分析这顿饭的营养',
  '给我今日健康提醒',
  '有什么清淡的晚餐推荐',
]

const DOWNLOAD_RE = /\[DOWNLOAD:(\d{4}-\d{2}-\d{2}):(\d{4}-\d{2}-\d{2})\]/gi

function downloadButtonHtml(start, end) {
  return (
    `<button type="button" class="pdf-download-btn" data-start="${start}" data-end="${end}">` +
    `📥 下载 PDF 报告（${start} ~ ${end}）</button>`
  )
}

/**
 * 将模型可能改写的多种「下载占位符」统一为 [DOWNLOAD:start:end]。
 * 避免 Markdown 链接 [文字](%%DOWNLOAD:...%%) 生成错误 href，导致整页 GET 到 /%%DOWNLOAD:...%%。
 */
function normalizeDownloadMarkers(raw) {
  if (!raw) return ''
  let t = raw
  t = t.replace(
    /\[[^\]]*?\]\(\s*%%DOWNLOAD:(\d{4}-\d{2}-\d{2})\s*[~:]\s*(\d{4}-\d{2}-\d{2})\s*%%\s*\)/gi,
    '[DOWNLOAD:$1:$2]'
  )
  t = t.replace(
    /%%DOWNLOAD:(\d{4}-\d{2}-\d{2})\s*[~:]\s*(\d{4}-\d{2}-\d{2})%%/gi,
    '[DOWNLOAD:$1:$2]'
  )
  t = t.replace(
    /\[DOWNLOAD:(\d{4}-\d{2}-\d{2})\s*[~:]\s*(\d{4}-\d{2}-\d{2})\]/gi,
    '[DOWNLOAD:$1:$2]'
  )
  return t
}

/** 从错误链接的 href（含 URL 编码）中解析出起止日期，用于点击拦截 */
function parseDownloadFromHref(href) {
  if (!href || href === '#' || /^\s*javascript:/i.test(href)) return null
  let s = href.trim()
  try {
    if (/^https?:\/\//i.test(s)) {
      s = new URL(s).pathname
    } else {
      s = s.split(/[?#]/)[0]
    }
    let prev = ''
    for (let i = 0; i < 5 && s !== prev; i++) {
      prev = s
      try {
        s = decodeURIComponent(s)
      } catch {
        break
      }
    }
  } catch {
    return null
  }
  const m =
    s.match(/%%DOWNLOAD:(\d{4}-\d{2}-\d{2})\s*[~:]\s*(\d{4}-\d{2}-\d{2})%%/i) ||
    s.match(/DOWNLOAD:(\d{4}-\d{2}-\d{2})\s*[~:]\s*(\d{4}-\d{2}-\d{2})/i)
  return m ? [m[1], m[2]] : null
}

/** 用户选择图片文件后转 Base64 */
function onImageSelected(event) {
  const file = event.target.files?.[0]
  if (!file) return
  // 限制大小（5MB），Base64 后体积约 1.33 倍
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    event.target.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => { selectedImage.value = e.target.result }
  reader.readAsDataURL(file)
  event.target.value = '' // 清空 input，允许重复选同一文件
}

function removeImage() {
  selectedImage.value = ''
}

function renderMd(text) {
  if (!text) return ''
  try {
    const normalized = normalizeDownloadMarkers(text)
    const mdHtml = marked.parse(normalized)
    return mdHtml
      .replace(DOWNLOAD_RE, (_, s, e) => downloadButtonHtml(s, e))
      .replace(
        /%%DOWNLOAD:(\d{4}-\d{2}-\d{2})\s*[~:]\s*(\d{4}-\d{2}-\d{2})%%/gi,
        (_, s, e) => downloadButtonHtml(s, e)
      )
  } catch { return text }
}

/** 事件委托：下载按钮 + 拦截错误的「伪链接」href */
function handleMsgClick(event) {
  const btn = event.target.closest('.pdf-download-btn')
  if (btn) {
    downloadReport(btn.dataset.start, btn.dataset.end)
    return
  }
  const a = event.target.closest('a[href]')
  if (!a) return
  const dates = parseDownloadFromHref(a.getAttribute('href'))
  if (!dates) return
  event.preventDefault()
  event.stopPropagation()
  downloadReport(dates[0], dates[1])
}

const isDownloading = ref(false)

async function downloadReport(startDate, endDate) {
  if (!startDate || !endDate) {
    ElMessage.error('日期参数缺失，请重新让 AI 生成报告')
    return
  }
  if (isDownloading.value) return
  isDownloading.value = true
  try {
    const res = await reportApi.download(startDate, endDate)

    // 检查响应是否真的是 PDF（防止服务端返回 JSON 错误被当成 PDF）
    const contentType = res.headers?.['content-type'] || ''
    if (!contentType.includes('application/pdf')) {
      // 服务端返回了 JSON 错误，把 Blob 转成文字读出来
      const text = await res.data.text()
      let msg = '下载失败'
      try { msg = JSON.parse(text)?.message || msg } catch (_) {}
      ElMessage.error(msg)
      return
    }

    const blob = new Blob([res.data], { type: 'application/pdf' })
    const url  = URL.createObjectURL(blob)
    const a    = document.createElement('a')
    a.href     = url
    a.download = `diet-report_${startDate}_${endDate}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('报告下载成功！')
  } catch (e) {
    // 网络错误或 4xx/5xx：尝试从 blob 里读错误信息
    const blobData = e?.response?.data
    if (blobData instanceof Blob) {
      try {
        const text = await blobData.text()
        const json = JSON.parse(text)
        ElMessage.error(json?.message || '下载失败，请稍后重试')
        return
      } catch (_) {}
    }
    ElMessage.error('下载失败，请稍后重试')
  } finally {
    isDownloading.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function genId() {
  return Math.random().toString(36).slice(2) + Date.now().toString(36)
}

function newChat() {
  chatStore.newChat()
}

function switchConv(conv) {
  chatStore.switchConv(conv.id)
  nextTick(scrollToBottom)
}

function deleteConv(id) {
  chatStore.deleteConv(id)
}

async function clearContext() {
  const convId = chatStore.activeConvId
  if (!convId) return
  try {
    await agentApi.clearConversation(convId)
    chatStore.clearMessages(convId)
    ElMessage.success('对话上下文已清除')
  } catch (_) {}
}

function quickAsk(q) {
  inputText.value = q
  sendMessage()
}

async function copyMsg(text) {
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

async function sendMessage() {
  const text  = inputText.value.trim()
  const image = selectedImage.value

  // 文字和图片至少有一个
  if (!text && !image) return
  if (isStreaming.value) return

  chatStore.ensureActiveConv()
  const convId = chatStore.activeConvId

  // 显示用户消息（含图片）
  const userMsg = reactive({ id: genId(), role: 'user', content: text, imageUrl: image || null })
  chatStore.pushMessage(userMsg)
  chatStore.updateConvMeta(convId, text || '【图片】')

  // 清空输入和图片预览
  inputText.value    = ''
  selectedImage.value = ''
  isStreaming.value  = true
  scrollToBottom()

  const aiMsg = reactive({ id: genId(), role: 'assistant', content: '', streaming: true })
  chatStore.pushMessage(aiMsg)

  try {
    await agentApi.streamChat(
      {
        userId:         userStore.userId,
        conversationId: convId,
        message:        text,
        imageUrl:       image || undefined,
        enableRag:      enableRag.value,
        enableProfile:  true,
      },
      (chunk) => {
        aiMsg.content += chunk
        scrollToBottom()
      },
      (serverConvId) => {
        aiMsg.streaming = false
        isStreaming.value = false
        if (serverConvId && serverConvId !== convId) {
          const conv = chatStore.conversations.find(c => c.id === convId)
          if (conv) conv.id = serverConvId
          chatStore.activeConvId = serverConvId
        }
      },
      (errMsg) => {
        aiMsg.content = `⚠️ 出错了：${errMsg}`
        aiMsg.streaming = false
        isStreaming.value = false
      }
    )
  } catch (e) {
    aiMsg.content = `⚠️ 请求失败：${e.message}`
    aiMsg.streaming = false
    isStreaming.value = false
  }
}

onMounted(() => {
  // 仅在没有任何会话时才初始化新对话，已有会话则保持不动
  chatStore.ensureActiveConv()
  nextTick(scrollToBottom)
})
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ---- 会话侧边栏 ---- */
.chat-sidebar {
  width: 200px;
  background: #f8fafc;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.chat-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 12px 10px;
}
.chat-sidebar-title { font-size: 13px; font-weight: 600; color: var(--color-text-muted); }

.conv-list { flex: 1; overflow-y: auto; padding: 4px 8px; }
.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: var(--transition);
}
.conv-item:hover { background: var(--color-primary-light); }
.conv-item.active { background: var(--color-primary-light); }
.conv-icon { color: var(--color-primary); flex-shrink: 0; }
.conv-meta { flex: 1; overflow: hidden; }
.conv-title { display: block; font-size: 12.5px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time  { display: block; font-size: 11px; color: var(--color-text-muted); }
.conv-del   { color: #ccc; opacity: 0; transition: var(--transition); }
.conv-item:hover .conv-del { opacity: 1; color: var(--color-danger); }

.conv-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 0; gap: 8px; }
.conv-empty p { font-size: 12px; color: var(--color-text-muted); }

/* ---- 主对话区 ---- */
.chat-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: var(--color-bg); }

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
}
.chat-header-left { display: flex; align-items: center; gap: 12px; }
.ai-avatar { font-size: 30px; }
.ai-name { font-size: 15px; font-weight: 700; }
.ai-status { font-size: 12px; color: var(--color-text-muted); display: flex; align-items: center; gap: 5px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #ccc; }
.status-dot.active { background: var(--color-primary); animation: pulse 2s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
.chat-header-actions { display: flex; align-items: center; gap: 10px; }

/* ---- 消息列表 ---- */
.messages-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 40px 20px;
  text-align: center;
}
.welcome-avatar { font-size: 64px; margin-bottom: 16px; }
.welcome-panel h2 { font-size: 20px; font-weight: 700; color: var(--color-text); margin-bottom: 8px; }
.welcome-panel p  { color: var(--color-text-muted); margin-bottom: 20px; }
.welcome-tags { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 500px; }
.welcome-tag { cursor: pointer; transition: var(--transition); }
.welcome-tag:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(66,184,131,0.2); }

/* ---- 消息气泡 ---- */
.message-row {
  display: flex;
  gap: 10px;
  max-width: 820px;
  width: 100%;
}
.message-row.user { flex-direction: row-reverse; align-self: flex-end; }
.message-row.assistant { align-self: flex-start; }

.msg-avatar {
  font-size: 24px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-bubble {
  max-width: calc(100% - 50px);
  background: var(--color-card);
  border-radius: 14px;
  padding: 12px 16px;
  box-shadow: var(--shadow);
  position: relative;
}
.message-row.user .msg-bubble {
  background: linear-gradient(135deg, var(--color-primary), #00c9a7);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.message-row.assistant .msg-bubble { border-bottom-left-radius: 4px; }
.message-row.user .msg-bubble .md-content * { color: #fff !important; }
.message-row.user .msg-bubble .md-content code { background: rgba(255,255,255,0.15); }

.msg-content { font-size: 14px; line-height: 1.7; word-break: break-word; }

.msg-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
  opacity: 0;
  transition: var(--transition);
}
.msg-bubble:hover .msg-actions { opacity: 1; }

.loading-bubble {
  display: flex;
  align-items: center;
  min-width: 60px;
  min-height: 40px;
  justify-content: center;
}

/* 三点加载动画 */
.dot-typing {
  position: relative;
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 16px 0 0 0 var(--color-primary), 32px 0 0 0 var(--color-primary);
  animation: dot-typing 1.2s infinite linear;
}
@keyframes dot-typing {
  0%   { box-shadow: 16px 0 0 0px var(--color-primary), 32px 0 0 0px var(--color-primary); }
  25%  { box-shadow: 16px 0 0 3px var(--color-primary), 32px 0 0 0px var(--color-primary); }
  50%  { box-shadow: 16px 0 0 3px var(--color-primary), 32px 0 0 3px var(--color-primary); }
  75%  { box-shadow: 16px 0 0 0px var(--color-primary), 32px 0 0 3px var(--color-primary); }
  100% { box-shadow: 16px 0 0 0px var(--color-primary), 32px 0 0 0px var(--color-primary); }
}

/* ---- 快捷提问 ---- */
.quick-asks {
  display: flex;
  gap: 8px;
  padding: 6px 20px;
  overflow-x: auto;
  flex-wrap: nowrap;
}
.quick-asks::-webkit-scrollbar { height: 0; }

/* ---- 输入区 ---- */
.input-area {
  padding: 12px 20px 20px;
  background: var(--color-card);
  border-top: 1px solid var(--color-border);
}
.input-wrap {
  background: var(--color-bg);
  border-radius: 14px;
  border: 1.5px solid var(--color-border);
  padding: 10px 12px 8px;
  transition: var(--transition);
}
.input-wrap:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(66,184,131,0.12);
}
.input-wrap :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none !important;
  padding: 0;
  font-size: 14px;
  resize: none;
}
.input-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.input-footer-left { display: flex; align-items: center; gap: 4px; }
.input-hint { font-size: 11px; color: var(--color-text-muted); }
.upload-img-btn { color: var(--color-text-muted) !important; padding: 4px !important; }
.upload-img-btn:hover { color: var(--color-primary) !important; }

/* ---- 图片预览条 ---- */
.image-preview-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--color-primary-light);
  border-radius: 10px;
  margin-bottom: 6px;
}
.image-preview-wrap { position: relative; flex-shrink: 0; }
.preview-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  display: block;
  border: 2px solid var(--color-primary);
}
.remove-img-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px !important;
  height: 20px !important;
  min-height: 20px !important;
  font-size: 10px !important;
  background: var(--color-danger) !important;
  color: #fff !important;
  border: none !important;
}
.preview-hint { font-size: 12px; color: var(--color-text-muted); }

/* ---- 消息气泡内的图片 ---- */
.msg-image {
  max-width: 240px;
  max-height: 180px;
  width: 100%;
  object-fit: contain;
  border-radius: 10px;
  display: block;
  margin-bottom: 6px;
}

/* ---- 图片占位（刷新后图片数据丢失） ---- */
.msg-image-placeholder {
  font-size: 12px;
  color: rgba(255,255,255,0.75);
  background: rgba(255,255,255,0.15);
  border-radius: 6px;
  padding: 4px 10px;
  margin-bottom: 6px;
  display: inline-block;
}
.message-row.assistant .msg-image-placeholder {
  color: var(--color-text-muted);
  background: var(--color-bg);
}

/* ---- PDF 下载按钮（由 renderMd 注入到气泡 HTML 中） ---- */
.msg-content :deep(.pdf-download-btn) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 8px 18px;
  background: linear-gradient(135deg, var(--color-primary), #00c9a7);
  color: #fff !important;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 3px 12px rgba(66, 184, 131, 0.35);
  transition: transform 0.15s, box-shadow 0.15s;
  white-space: nowrap;
}
.msg-content :deep(.pdf-download-btn:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(66, 184, 131, 0.45);
}
.msg-content :deep(.pdf-download-btn:active) {
  transform: translateY(0);
}
</style>

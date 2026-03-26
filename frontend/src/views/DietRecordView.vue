<template>
  <div class="records-page">
    <!-- 顶部栏 -->
    <header class="page-header">
      <div>
        <h1 class="page-title">饮食记录</h1>
        <p class="page-sub">记录每餐饮食，AI 自动分析营养成分</p>
      </div>
      <div class="header-actions">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          size="default"
          :clearable="false"
          @change="loadRecords"
        />
        <el-button type="primary" round @click="showAddDialog = true">
          <el-icon><Plus /></el-icon> 记录饮食
        </el-button>
      </div>
    </header>

    <!-- 餐次 Tab -->
    <div class="meal-tabs">
      <div
        v-for="tab in mealTabs"
        :key="tab.type"
        class="meal-tab"
        :class="{ active: activeMealType === tab.type }"
        @click="activeMealType = tab.type"
      >
        <span class="tab-emoji">{{ tab.emoji }}</span>
        <span class="tab-label">{{ tab.label }}</span>
        <span v-if="countByType(tab.type) > 0" class="tab-count">{{ countByType(tab.type) }}</span>
      </div>
    </div>

    <!-- 记录列表 -->
    <div class="records-container">
      <div v-if="filteredRecords.length === 0" class="empty-full">
        <div class="empty-icon">🍽️</div>
        <h3>今日 {{ activeMealType === -1 ? '暂无' : currentMealLabel }} 记录</h3>
        <p>点击"记录饮食"按钮，记录你的每一餐 ✨</p>
        <el-button type="primary" round @click="showAddDialog = true">
          <el-icon><Plus /></el-icon> 立即记录
        </el-button>
      </div>

      <transition-group name="slide-up" tag="div" class="record-grid">
        <div
          v-for="rec in filteredRecords"
          :key="rec.id"
          class="record-card"
          :class="{ abnormal: rec.isAbnormal }"
        >
          <!-- 卡片头 -->
          <div class="record-card-header">
            <div class="record-meal-tag" :style="mealTagStyle(rec.mealType)">
              {{ rec.mealTypeLabel }}
            </div>
            <div class="record-card-actions">
              <el-button link size="small" @click="reanalyze(rec)" :loading="rec.loading">
                <el-icon><Refresh /></el-icon>
              </el-button>
              <el-popconfirm title="确定删除这条记录？" @confirm="deleteRecord(rec.id)">
                <template #reference>
                  <el-button link size="small" type="danger">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>

          <!-- 餐食图片（URL 或 base64 均支持） -->
          <div v-if="rec.imageUrl" class="record-img-wrap">
            <img :src="rec.imageUrl" class="record-img" alt="餐食图片" />
          </div>

          <!-- 餐食描述 -->
          <div class="record-desc">{{ rec.mealDescription }}</div>

          <!-- 异常警告 -->
          <el-alert
            v-if="rec.isAbnormal"
            :title="rec.abnormalReason"
            type="warning"
            :closable="false"
            show-icon
            class="record-alert"
          />

          <!-- 营养数据 -->
          <div v-if="rec.totalCalories" class="nutrition-grid">
            <div class="nutrition-item" v-for="n in getNutrition(rec)" :key="n.label">
              <div class="nutrition-value" :style="{ color: n.color }">{{ n.value }}</div>
              <div class="nutrition-label">{{ n.label }}</div>
            </div>
          </div>

          <!-- 健康评分 -->
          <div v-if="rec.healthScore != null" class="health-score-row">
            <span class="score-label">健康评分</span>
            <el-progress
              :percentage="rec.healthScore"
              :color="scoreColor(rec.healthScore)"
              :stroke-width="6"
              class="score-bar"
            />
          </div>

          <!-- AI 建议 -->
          <div v-if="rec.aiSuggestion" class="ai-suggestion">
            <el-icon color="var(--color-primary)" size="13"><Cpu /></el-icon>
            <span>{{ rec.aiSuggestion }}</span>
          </div>
        </div>
      </transition-group>
    </div>

    <!-- 新增对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="记录饮食"
      width="500px"
      border-radius="16px"
      :before-close="() => { showAddDialog = false; resetForm() }"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" size="default">
        <el-form-item label="餐次" prop="mealType">
          <el-radio-group v-model="form.mealType">
            <el-radio-button v-for="tab in mealTabs.slice(1)" :key="tab.type" :value="tab.type">
              {{ tab.emoji }} {{ tab.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker
            v-model="form.recordDate"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>

        <el-form-item label="餐食描述" prop="mealDescription">
          <el-input
            v-model="form.mealDescription"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请描述你吃了什么，例如：早饭吃了两个煎蛋、一碗小米粥、一根香蕉..."
          />
        </el-form-item>

        <el-form-item label="餐食图片">
          <!-- 模式切换 -->
          <div class="image-mode-bar">
            <button
              :class="['img-mode-btn', { active: imageMode === 'url' }]"
              type="button"
              @click="imageMode = 'url'"
            >🔗 图片链接</button>
            <button
              :class="['img-mode-btn', { active: imageMode === 'local' }]"
              type="button"
              @click="imageMode = 'local'"
            >📁 本地上传</button>
          </div>

          <!-- URL 输入 -->
          <el-input
            v-if="imageMode === 'url'"
            v-model="form.imageUrl"
            placeholder="可选，粘贴餐食图片 URL"
            clearable
            @clear="form.imageUrl = ''"
          />

          <!-- 本地上传 -->
          <div v-else class="local-upload-wrap">
            <label class="upload-zone" :class="{ 'has-img': !!localPreview }">
              <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                class="file-hidden"
                @change="handleLocalFile"
              />
              <template v-if="!localPreview">
                <el-icon size="32" color="#c0cfd8"><UploadFilled /></el-icon>
                <p class="upload-tip">点击或拖拽上传<br /><span>支持 JPG / PNG / WebP，建议小于 2MB</span></p>
              </template>
              <template v-else>
                <img :src="localPreview" class="preview-img" />
                <div class="preview-mask">
                  <el-icon size="20" color="#fff"><Edit /></el-icon>
                  <span>重新选择</span>
                </div>
              </template>
            </label>
            <el-button
              v-if="localPreview"
              link
              type="danger"
              size="small"
              class="clear-img-btn"
              @click.stop="clearLocalImg"
            >
              <el-icon><Delete /></el-icon> 移除图片
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="AI 分析">
          <el-switch v-model="form.analyzeNow" active-text="立即分析" inactive-text="仅保存" />
          <span class="form-hint">开启后 AI 将自动分析营养成分</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false; resetForm()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ form.analyzeNow ? '记录并分析' : '保存记录' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { dietApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const selectedDate   = ref(dayjs().format('YYYY-MM-DD'))
const activeMealType = ref(-1)
const records        = ref([])
const showAddDialog  = ref(false)
const submitting     = ref(false)
const formRef        = ref()
const fileInputRef   = ref()

// 图片上传模式：'url' | 'local'
const imageMode   = ref('url')
// 本地图片预览（base64 data URL）
const localPreview = ref('')

const mealTabs = [
  { type: -1, emoji: '🍽️', label: '全部' },
  { type: 0,  emoji: '🌅', label: '早餐' },
  { type: 1,  emoji: '🌞', label: '午餐' },
  { type: 2,  emoji: '🌙', label: '晚餐' },
  { type: 3,  emoji: '🍎', label: '加餐' },
  { type: 4,  emoji: '🌃', label: '夜宵' },
]

const mealColors = ['#42b883', '#409eff', '#e6a23c', '#909399', '#f56c6c']
const mealBgs    = ['#e8f8f0', '#ecf5ff', '#fdf6ec', '#f4f4f5', '#fef0f0']

const form = reactive({
  userId: userStore.userId,
  mealType: 0,
  recordDate: dayjs().format('YYYY-MM-DD'),
  mealDescription: '',
  imageUrl: '',
  analyzeNow: true,
})

const rules = {
  mealType:        [{ required: true, message: '请选择餐次' }],
  mealDescription: [{ required: true, message: '请描述你吃了什么', trigger: 'blur' }],
}

const filteredRecords = computed(() => {
  if (activeMealType.value === -1) return records.value
  return records.value.filter(r => r.mealType === activeMealType.value)
})

const currentMealLabel = computed(() => mealTabs.find(t => t.type === activeMealType.value)?.label || '')

function countByType(t) {
  if (t === -1) return records.value.length
  return records.value.filter(r => r.mealType === t).length
}

function mealTagStyle(t) {
  return { background: mealBgs[t || 0], color: mealColors[t || 0] }
}

function getNutrition(rec) {
  return [
    { label: '热量',   value: `${Math.round(rec.totalCalories || 0)}`, unit: 'kcal', color: '#42b883' },
    { label: '蛋白质', value: `${(rec.proteinG || 0).toFixed(1)}g`,    color: '#409eff' },
    { label: '碳水',   value: `${(rec.carbohydratesG || 0).toFixed(1)}g`, color: '#e6a23c' },
    { label: '脂肪',   value: `${(rec.fatG || 0).toFixed(1)}g`,        color: '#f56c6c' },
    { label: '纤维',   value: `${(rec.fiberG || 0).toFixed(1)}g`,      color: '#909399' },
  ]
}

function scoreColor(s) {
  if (s >= 80) return '#42b883'
  if (s >= 60) return '#e6a23c'
  return '#f56c6c'
}

async function loadRecords() {
  try {
    const res = await dietApi.daily(userStore.userId, selectedDate.value)
    records.value = (res.data?.data || []).map(r => ({ ...r, loading: false }))
  } catch (_) { records.value = [] }
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await dietApi.add({ ...form, userId: userStore.userId })
      ElMessage.success(form.analyzeNow ? '已记录并触发 AI 分析！' : '记录成功！')
      showAddDialog.value = false
      resetForm()
      await loadRecords()
    } catch (_) {}
    finally { submitting.value = false }
  })
}

/** 本地文件选择 → 转 base64 → 存入 form.imageUrl */
function handleLocalFile(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 4 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 4MB，请压缩后再上传')
    e.target.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = (ev) => {
    const base64 = ev.target.result
    localPreview.value  = base64
    form.imageUrl       = base64   // 直接存 base64 到 imageUrl 字段
  }
  reader.readAsDataURL(file)
}

/** 移除本地图片 */
function clearLocalImg() {
  localPreview.value = ''
  form.imageUrl      = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function resetForm() {
  Object.assign(form, {
    mealType: 0, mealDescription: '', imageUrl: '',
    recordDate: dayjs().format('YYYY-MM-DD'), analyzeNow: true,
  })
  formRef.value?.resetFields()
  imageMode.value    = 'url'
  localPreview.value = ''
}

async function reanalyze(rec) {
  rec.loading = true
  try {
    const res = await dietApi.reanalyze(rec.id)
    const updated = res.data?.data
    const idx = records.value.findIndex(r => r.id === rec.id)
    if (idx !== -1) records.value[idx] = { ...updated, loading: false }
    ElMessage.success('重新分析完成')
  } catch (_) { rec.loading = false }
}

async function deleteRecord(id) {
  try {
    await dietApi.delete(id)
    records.value = records.value.filter(r => r.id !== id)
    ElMessage.success('已删除')
  } catch (_) {}
}

onMounted(loadRecords)
</script>

<style scoped>
.records-page { height: 100vh; overflow-y: auto; padding: 0 24px 32px; }

.page-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 0 16px;
  position: sticky; top: 0; background: var(--color-bg); z-index: 5;
}
.page-title { font-size: 20px; font-weight: 700; }
.page-sub   { font-size: 13px; color: var(--color-text-muted); }
.header-actions { display: flex; gap: 10px; align-items: center; }

/* 餐次 Tabs */
.meal-tabs { display: flex; gap: 8px; margin-bottom: 20px; overflow-x: auto; padding-bottom: 4px; }
.meal-tabs::-webkit-scrollbar { height: 0; }
.meal-tab {
  display: flex; align-items: center; gap: 5px;
  padding: 7px 16px; border-radius: 20px;
  background: var(--color-card); box-shadow: var(--shadow);
  cursor: pointer; font-size: 13px; font-weight: 500;
  white-space: nowrap; color: var(--color-text-muted);
  transition: var(--transition);
}
.meal-tab:hover { box-shadow: var(--shadow-hover); color: var(--color-primary); }
.meal-tab.active {
  background: linear-gradient(135deg, var(--color-primary), #00c9a7);
  color: #fff; box-shadow: 0 4px 14px rgba(66,184,131,0.3);
}
.tab-count {
  background: rgba(255,255,255,0.3);
  border-radius: 10px; padding: 0 6px;
  font-size: 11px;
}
.meal-tab:not(.active) .tab-count { background: var(--color-primary-light); color: var(--color-primary); }

/* 记录网格 */
.records-container { min-height: 300px; }
.record-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 16px; }

.record-card {
  background: var(--color-card); border-radius: var(--radius);
  padding: 16px; box-shadow: var(--shadow);
  transition: var(--transition); position: relative;
  border: 1.5px solid transparent;
}
.record-card:hover { box-shadow: var(--shadow-hover); transform: translateY(-2px); }
.record-card.abnormal { border-color: #fad3ae; }

.record-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.record-meal-tag { padding: 3px 10px; border-radius: 10px; font-size: 12px; font-weight: 600; }
.record-card-actions { display: flex; gap: 2px; }

.record-img-wrap {
  width: 100%;
  height: 140px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 10px;
}
.record-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.record-desc { font-size: 13.5px; color: var(--color-text); line-height: 1.5; margin-bottom: 10px; }

.record-alert { margin-bottom: 10px; border-radius: 8px; }
.record-alert :deep(.el-alert__title) { font-size: 12px; }

.nutrition-grid {
  display: grid; grid-template-columns: repeat(5, 1fr);
  gap: 6px; margin-bottom: 10px;
  background: var(--color-bg); border-radius: 8px; padding: 10px;
}
.nutrition-item { text-align: center; }
.nutrition-value { font-size: 14px; font-weight: 700; }
.nutrition-label { font-size: 10px; color: var(--color-text-muted); margin-top: 2px; }

.health-score-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.score-label { font-size: 12px; color: var(--color-text-muted); white-space: nowrap; }
.score-bar { flex: 1; }

.ai-suggestion {
  display: flex; align-items: flex-start; gap: 5px;
  background: var(--color-primary-light); border-radius: 8px;
  padding: 8px 10px; font-size: 12.5px; color: var(--color-text); line-height: 1.5;
}

/* 空状态 */
.empty-full {
  display: flex; flex-direction: column; align-items: center;
  padding: 80px 20px; text-align: center; gap: 10px;
}
.empty-icon { font-size: 60px; }
.empty-full h3 { font-size: 16px; font-weight: 600; }
.empty-full p  { font-size: 13px; color: var(--color-text-muted); }

.form-hint { font-size: 11px; color: var(--color-text-muted); margin-left: 8px; }

/* ---- 图片上传 ---- */
.image-mode-bar {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  width: 100%;
}

.img-mode-btn {
  flex: 1;
  height: 32px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  background: transparent;
  font-size: 12.5px;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all .2s;
}
.img-mode-btn:hover  { border-color: var(--color-primary); color: var(--color-primary); }
.img-mode-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.local-upload-wrap { width: 100%; display: flex; flex-direction: column; gap: 6px; }

.upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 130px;
  border: 2px dashed var(--color-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all .2s;
  position: relative;
  overflow: hidden;
  background: var(--color-bg);
}
.upload-zone:hover { border-color: var(--color-primary); background: var(--color-primary-light); }
.upload-zone.has-img { border-style: solid; border-color: var(--color-primary); }

.file-hidden { display: none; }

.upload-tip {
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
  margin-top: 6px;
  line-height: 1.6;
}
.upload-tip span { font-size: 11px; color: #bcc4ce; }

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.preview-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity .2s;
  color: #fff;
  font-size: 12px;
  border-radius: 8px;
}
.upload-zone:hover .preview-mask { opacity: 1; }

.clear-img-btn { align-self: flex-end; }
</style>

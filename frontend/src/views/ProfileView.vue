<template>
  <div class="profile-page">
    <header class="page-header">
      <div>
        <h1 class="page-title">健康档案</h1>
        <p class="page-sub">完善你的健康信息，获得更精准的个性化建议</p>
      </div>
      <el-button type="primary" round :loading="saving" @click="saveProfile">
        <el-icon><Check /></el-icon> 保存档案
      </el-button>
    </header>

    <div class="profile-grid">
      <!-- 左列：健康指标看板 -->
      <div class="profile-left">
        <!-- 用户卡 -->
        <div class="user-card card">
          <div class="user-card-avatar">
            <div class="avatar-circle">{{ form.nickname?.charAt(0) || 'U' }}</div>
          </div>
          <div class="user-card-info">
            <div class="user-card-name">{{ form.nickname || '未设置昵称' }}</div>
            <div class="user-card-id">ID: {{ userStore.userId }}</div>
          </div>
          <div class="user-card-stats">
            <div class="ucs-item" v-for="s in userStats" :key="s.label">
              <div class="ucs-value" :style="{ color: s.color }">{{ s.value }}</div>
              <div class="ucs-label">{{ s.label }}</div>
            </div>
          </div>
        </div>

        <!-- BMI 卡 -->
        <div class="bmi-card card" v-if="bmi">
          <div class="bmi-header">
            <span class="card-title">BMI 体质指数</span>
            <span class="bmi-badge" :style="bmiBadgeStyle">{{ bmiLabel }}</span>
          </div>
          <div class="bmi-value">{{ bmi }}</div>
          <div class="bmi-bar-wrap">
            <div class="bmi-bar">
              <div class="bmi-range under" title="偏瘦 (<18.5)"></div>
              <div class="bmi-range normal" title="正常 (18.5-23.9)"></div>
              <div class="bmi-range over" title="超重 (24-27.9)"></div>
              <div class="bmi-range obese" title="肥胖 (≥28)"></div>
              <div class="bmi-pointer" :style="{ left: bmiPointerLeft }"></div>
            </div>
            <div class="bmi-scale">
              <span>16</span><span>18.5</span><span>24</span><span>28</span><span>35</span>
            </div>
          </div>
          <div class="bmi-tips">
            <div class="bmi-tip-row" v-for="tip in bmiTips" :key="tip.range">
              <span class="bmi-dot" :style="{ background: tip.color }"></span>
              <span>{{ tip.range }}</span><span style="color:#8a9ab0">{{ tip.label }}</span>
            </div>
          </div>
        </div>

        <!-- 代谢指标 -->
        <div class="card" v-if="form.bmr || form.tdee">
          <div class="card-title" style="margin-bottom:14px">代谢指标</div>
          <div class="meta-grid">
            <div class="meta-item">
              <div class="meta-label">基础代谢 BMR</div>
              <div class="meta-value green">{{ form.bmr ? Math.round(form.bmr) : '--' }}</div>
              <div class="meta-unit">kcal/天</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">总消耗 TDEE</div>
              <div class="meta-value blue">{{ form.tdee ? Math.round(form.tdee) : '--' }}</div>
              <div class="meta-unit">kcal/天</div>
            </div>
          </div>
          <div class="meta-note">* 数据由 Mifflin-St Jeor 公式自动计算，保存后更新</div>
        </div>
      </div>

      <!-- 右列：表单 -->
      <div class="profile-right">
        <el-form :model="form" label-width="90px" size="default" class="profile-form">
          <!-- 基础信息 -->
          <div class="form-section">
            <div class="form-section-title">基础信息</div>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="昵称">
                  <el-input v-model="form.nickname" placeholder="给自己起个昵称" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别">
                  <el-radio-group v-model="form.gender">
                    <el-radio :value="1">男</el-radio>
                    <el-radio :value="2">女</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="年龄">
                  <el-input-number v-model="form.age" :min="1" :max="120" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="身高(cm)">
                  <el-input-number v-model="form.heightCm" :min="100" :max="250" :precision="1" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="体重(kg)">
                  <el-input-number v-model="form.weightKg" :min="20" :max="300" :precision="1" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="目标(kg)">
                  <el-input-number v-model="form.targetWeightKg" :min="20" :max="300" :precision="1" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- 活动水平 -->
          <div class="form-section">
            <div class="form-section-title">活动水平</div>
            <div class="activity-grid">
              <div
                v-for="lvl in activityLevels"
                :key="lvl.value"
                class="activity-item"
                :class="{ active: form.activityLevel === lvl.value }"
                @click="form.activityLevel = lvl.value"
              >
                <span class="activity-emoji">{{ lvl.emoji }}</span>
                <span class="activity-label">{{ lvl.label }}</span>
                <span class="activity-desc">{{ lvl.desc }}</span>
              </div>
            </div>
          </div>

          <!-- 健康目标 -->
          <div class="form-section">
            <div class="form-section-title">健康目标</div>
            <el-checkbox-group v-model="form.healthGoals" class="goal-group">
              <el-checkbox v-for="g in goalOptions" :key="g" :value="g" class="goal-item">{{ g }}</el-checkbox>
            </el-checkbox-group>
          </div>

          <!-- 饮食禁忌 -->
          <div class="form-section">
            <div class="form-section-title">食物禁忌 / 过敏</div>
            <el-select
              v-model="form.foodAllergies"
              multiple
              filterable
              allow-create
              placeholder="选择或输入食物禁忌"
              style="width:100%"
            >
              <el-option v-for="a in allergyOptions" :key="a" :value="a" :label="a" />
            </el-select>
          </div>

          <!-- 健康状况 -->
          <div class="form-section">
            <div class="form-section-title">健康状况</div>
            <el-select
              v-model="form.healthConditions"
              multiple
              filterable
              allow-create
              placeholder="选择或输入健康状况"
              style="width:100%"
            >
              <el-option v-for="c in conditionOptions" :key="c" :value="c" :label="c" />
            </el-select>
          </div>

          <!-- AI 摘要 -->
          <div class="form-section" v-if="form.aiProfileSummary">
            <div class="form-section-title">AI 健康画像摘要</div>
            <div class="ai-summary">
              <el-icon color="var(--color-primary)"><Cpu /></el-icon>
              <p>{{ form.aiProfileSummary }}</p>
            </div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const saving = ref(false)

const form = reactive({
  userId: userStore.userId,
  nickname: '', gender: 1, age: null,
  heightCm: null, weightKg: null, targetWeightKg: null,
  activityLevel: 2, healthGoals: [], foodAllergies: [],
  healthConditions: [], aiProfileSummary: '', bmr: null, tdee: null,
})

const activityLevels = [
  { value: 1, emoji: '🪑', label: '久坐', desc: '几乎不运动' },
  { value: 2, emoji: '🚶', label: '轻度', desc: '每周1-3天' },
  { value: 3, emoji: '🏃', label: '中度', desc: '每周3-5天' },
  { value: 4, emoji: '🏋️', label: '高度', desc: '每周6-7天' },
  { value: 5, emoji: '⚡', label: '极高', desc: '体力劳动者' },
]

const goalOptions = ['减重', '增肌', '维持体重', '控糖', '降压', '降脂', '增强体质', '改善睡眠']
const allergyOptions = ['海鲜', '花生', '牛奶', '鸡蛋', '大豆', '小麦（麸质）', '坚果', '芒果']
const conditionOptions = ['高血压', '糖尿病', '高血脂', '高尿酸/痛风', '胃病', '贫血', '骨质疏松', '肾脏疾病']

// BMI 计算
const bmi = computed(() => {
  if (!form.heightCm || !form.weightKg) return null
  const h = form.heightCm / 100
  return (form.weightKg / (h * h)).toFixed(1)
})

const bmiLabel = computed(() => {
  const v = parseFloat(bmi.value)
  if (!v) return ''
  if (v < 18.5) return '偏瘦'
  if (v < 24)   return '正常'
  if (v < 28)   return '超重'
  return '肥胖'
})

const bmiBadgeStyle = computed(() => {
  const v = parseFloat(bmi.value)
  const map = { '偏瘦': '#409eff', '正常': '#42b883', '超重': '#e6a23c', '肥胖': '#f56c6c' }
  return { background: map[bmiLabel.value] || '#ccc', color: '#fff', padding: '2px 10px', borderRadius: '10px', fontSize: '12px' }
})

const bmiPointerLeft = computed(() => {
  const v = parseFloat(bmi.value)
  if (!v) return '0%'
  // 映射到 16-35 范围
  const pct = Math.min(100, Math.max(0, ((v - 16) / (35 - 16)) * 100))
  return pct + '%'
})

const bmiTips = [
  { range: '< 18.5', label: '偏瘦', color: '#409eff' },
  { range: '18.5-23.9', label: '正常', color: '#42b883' },
  { range: '24-27.9', label: '超重', color: '#e6a23c' },
  { range: '≥ 28', label: '肥胖', color: '#f56c6c' },
]

const userStats = computed(() => [
  { label: 'BMI',  value: bmi.value || '--', color: '#42b883' },
  { label: 'BMR',  value: form.bmr ? Math.round(form.bmr) : '--', color: '#409eff' },
  { label: 'TDEE', value: form.tdee ? Math.round(form.tdee) : '--', color: '#e6a23c' },
])

async function saveProfile() {
  saving.value = true
  try {
    const dto = {
      userId: userStore.userId,
      nickname: form.nickname || undefined,
      gender: form.gender,
      age: form.age || undefined,
      heightCm: form.heightCm || undefined,
      weightKg: form.weightKg || undefined,
      targetWeightKg: form.targetWeightKg || undefined,
      activityLevel: form.activityLevel,
      healthGoals: form.healthGoals,
      foodAllergies: form.foodAllergies,
      healthConditions: form.healthConditions,
    }
    await userStore.saveProfile(dto)
    // 更新表单中的 BMR/TDEE（后端计算后返回）
    if (userStore.profile) {
      form.bmr  = userStore.profile.bmr
      form.tdee = userStore.profile.tdee
    }
    ElMessage.success('健康档案已保存 ✨')
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || '保存失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

function syncFromProfile(p) {
  if (!p) return
  Object.assign(form, {
    nickname: p.nickname,
    gender: p.gender || 1,
    age: p.age,
    heightCm: p.heightCm,
    weightKg: p.weightKg,
    targetWeightKg: p.targetWeightKg,
    activityLevel: p.activityLevel || 2,
    healthGoals: p.healthGoals || [],
    foodAllergies: p.foodAllergies || [],
    healthConditions: p.healthConditions || [],
    aiProfileSummary: p.aiProfileSummary,
    bmr: p.bmr,
    tdee: p.tdee,
  })
}

onMounted(async () => {
  // 若 profile 尚未加载（从其他页跳转过来时可能为空），主动拉取一次
  if (!userStore.profile) {
    await userStore.fetchProfile()
  }
  syncFromProfile(userStore.profile)
})
watch(() => userStore.profile, syncFromProfile)
</script>

<style scoped>
.profile-page { height: 100vh; overflow-y: auto; padding: 0 24px 40px; }

.page-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 0 16px;
  position: sticky; top: 0; background: var(--color-bg); z-index: 5;
}
.page-title { font-size: 20px; font-weight: 700; }
.page-sub   { font-size: 13px; color: var(--color-text-muted); }

.profile-grid { display: grid; grid-template-columns: 300px 1fr; gap: 20px; }

/* ---- 左侧看板 ---- */
.profile-left { display: flex; flex-direction: column; gap: 16px; }
.card { background: var(--color-card); border-radius: var(--radius); padding: 18px; box-shadow: var(--shadow); }
.card-title { font-size: 14px; font-weight: 600; }

.user-card { text-align: center; }
.avatar-circle {
  width: 64px; height: 64px; border-radius: 50%; margin: 0 auto 8px;
  background: linear-gradient(135deg, var(--color-primary), #00c9a7);
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 700; color: #fff;
}
.user-card-name { font-size: 17px; font-weight: 700; }
.user-card-id   { font-size: 12px; color: var(--color-text-muted); margin-bottom: 14px; }
.user-card-stats { display: flex; justify-content: space-around; }
.ucs-item { text-align: center; }
.ucs-value { font-size: 18px; font-weight: 700; }
.ucs-label { font-size: 11px; color: var(--color-text-muted); }

/* BMI */
.bmi-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.bmi-value  { font-size: 36px; font-weight: 800; color: var(--color-primary); margin-bottom: 10px; }
.bmi-bar-wrap { margin-bottom: 8px; }
.bmi-bar {
  display: flex; height: 8px; border-radius: 4px; overflow: visible;
  position: relative; margin-bottom: 4px;
}
.bmi-range { height: 100%; }
.bmi-range.under  { flex: 2.5; background: #409eff; border-radius: 4px 0 0 4px; }
.bmi-range.normal { flex: 5.5; background: #42b883; }
.bmi-range.over   { flex: 4;   background: #e6a23c; }
.bmi-range.obese  { flex: 7;   background: #f56c6c; border-radius: 0 4px 4px 0; }
.bmi-pointer {
  position: absolute; top: -3px; width: 14px; height: 14px;
  border-radius: 50%; background: #fff;
  border: 3px solid var(--color-text);
  transform: translateX(-50%);
  transition: left 0.5s;
}
.bmi-scale { display: flex; justify-content: space-between; font-size: 10px; color: var(--color-text-muted); }
.bmi-tips { display: flex; flex-direction: column; gap: 4px; }
.bmi-tip-row { display: flex; align-items: center; gap: 6px; font-size: 11.5px; }
.bmi-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

/* 代谢 */
.meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 8px; }
.meta-item { text-align: center; background: var(--color-bg); border-radius: 10px; padding: 10px; }
.meta-label { font-size: 11px; color: var(--color-text-muted); margin-bottom: 4px; }
.meta-value { font-size: 22px; font-weight: 700; }
.meta-value.green { color: var(--color-primary); }
.meta-value.blue  { color: #409eff; }
.meta-unit  { font-size: 10px; color: var(--color-text-muted); }
.meta-note  { font-size: 10px; color: var(--color-text-muted); margin-top: 4px; }

/* ---- 右侧表单 ---- */
.profile-right { }
.profile-form { }
.form-section { background: var(--color-card); border-radius: var(--radius); padding: 18px; box-shadow: var(--shadow); margin-bottom: 16px; }
.form-section-title { font-size: 14px; font-weight: 600; color: var(--color-text); margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid var(--color-border); }

.activity-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.activity-item {
  display: flex; flex-direction: column; align-items: center;
  padding: 10px 6px; border-radius: 10px;
  border: 1.5px solid var(--color-border);
  cursor: pointer; transition: var(--transition);
}
.activity-item:hover { border-color: var(--color-primary); background: var(--color-primary-light); }
.activity-item.active {
  border-color: var(--color-primary); background: var(--color-primary-light);
  box-shadow: 0 0 0 2px rgba(66,184,131,0.2);
}
.activity-emoji { font-size: 22px; }
.activity-label { font-size: 12px; font-weight: 600; margin-top: 4px; }
.activity-desc  { font-size: 10px; color: var(--color-text-muted); text-align: center; }

.goal-group { display: flex; flex-wrap: wrap; gap: 8px; }
.goal-item { margin: 0 !important; }

.ai-summary {
  display: flex; align-items: flex-start; gap: 8px;
  background: var(--color-primary-light); border-radius: 8px;
  padding: 12px; font-size: 13px; line-height: 1.6;
}
</style>

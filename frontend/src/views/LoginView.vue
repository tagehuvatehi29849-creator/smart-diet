<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-inner">
        <div class="logo-wrap">
          <span class="logo-icon">🥗</span>
        </div>
        <h1 class="app-title">智能饮食助手</h1>
        <p class="app-desc">AI 驱动的个性化健康管理平台</p>

        <ul class="feature-list">
          <li v-for="f in features" :key="f.text">
            <span class="feat-icon">{{ f.icon }}</span>
            <span>{{ f.text }}</span>
          </li>
        </ul>

        <div class="brand-footer">
          <span>健康生活，从记录开始</span>
        </div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="form-card">
        <!-- Tab 切换 -->
        <div class="tab-bar">
          <button
            :class="['tab-btn', { active: activeTab === 'login' }]"
            @click="switchTab('login')"
          >登录</button>
          <button
            :class="['tab-btn', { active: activeTab === 'register' }]"
            @click="switchTab('register')"
          >注册</button>
          <span class="tab-indicator" :class="activeTab" />
        </div>

        <!-- ========== 登录表单 ========== -->
        <el-form
          v-if="activeTab === 'login'"
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          size="large"
          @submit.prevent="doLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              clearable
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              autocomplete="current-password"
              @keyup.enter="doLogin"
            />
          </el-form-item>

          <el-form-item>
            <div class="row-between">
              <el-checkbox v-model="loginForm.rememberMe">记住我（30天）</el-checkbox>
            </div>
          </el-form-item>

          <el-button
            class="submit-btn"
            type="primary"
            :loading="loading"
            @click="doLogin"
          >
            登 录
          </el-button>

          <p class="switch-tip">
            还没有账号？
            <a @click="switchTab('register')">立即注册</a>
          </p>
        </el-form>

        <!-- ========== 注册表单 ========== -->
        <el-form
          v-else
          ref="regFormRef"
          :model="regForm"
          :rules="regRules"
          label-position="top"
          size="large"
          scroll-to-error
        >
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="regForm.username"
                  placeholder="3~30位，字母数字下划线"
                  prefix-icon="User"
                  clearable
                  autocomplete="username"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="昵称" prop="nickname">
                <el-input
                  v-model="regForm.nickname"
                  placeholder="留空则同用户名"
                  prefix-icon="StarFilled"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="regForm.password"
                  type="password"
                  placeholder="至少 6 位"
                  prefix-icon="Lock"
                  show-password
                  autocomplete="new-password"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="regForm.confirmPassword"
                  type="password"
                  placeholder="再次输入密码"
                  prefix-icon="Lock"
                  show-password
                  autocomplete="new-password"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="section-divider">健康信息（可选，注册后随时补充）</div>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="性别" prop="gender">
                <el-select v-model="regForm.gender" placeholder="请选择" style="width:100%">
                  <el-option label="男" :value="1" />
                  <el-option label="女" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="年龄" prop="age">
                <el-input-number
                  v-model="regForm.age"
                  :min="10" :max="120"
                  controls-position="right"
                  style="width:100%"
                  placeholder="岁"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="活动水平">
                <el-select v-model="regForm.activityLevel" placeholder="请选择" style="width:100%">
                  <el-option label="久坐不动" :value="1" />
                  <el-option label="轻度活动" :value="2" />
                  <el-option label="中度活动" :value="3" />
                  <el-option label="高度活动" :value="4" />
                  <el-option label="极度活跃" :value="5" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="身高（cm）" prop="heightCm">
                <el-input-number
                  v-model="regForm.heightCm"
                  :min="50" :max="250" :precision="1"
                  controls-position="right"
                  style="width:100%"
                  placeholder="cm"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="体重（kg）" prop="weightKg">
                <el-input-number
                  v-model="regForm.weightKg"
                  :min="20" :max="300" :precision="1"
                  controls-position="right"
                  style="width:100%"
                  placeholder="kg"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="健康目标">
            <el-checkbox-group v-model="regForm.healthGoals" class="goal-group">
              <el-checkbox v-for="g in healthGoalOptions" :key="g" :value="g">{{ g }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-button
            class="submit-btn"
            type="primary"
            :loading="loading"
            @click="doRegister"
          >
            注 册
          </el-button>

          <p class="switch-tip">
            已有账号？
            <a @click="switchTab('login')">立即登录</a>
          </p>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router    = useRouter()
const authStore = useAuthStore()
const loading   = ref(false)
const activeTab = ref('login')

const loginFormRef = ref()
const regFormRef   = ref()

// ---- 登录表单 ----
const loginForm = reactive({ username: '', password: '', rememberMe: false })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码',   trigger: 'blur' }],
}

// ---- 注册表单 ----
const regForm = reactive({
  username: '', password: '', confirmPassword: '',
  nickname: '', gender: null, age: null,
  heightCm: null, weightKg: null,
  activityLevel: 2,
  healthGoals: [],
})

const validateConfirmPwd = (_, value, callback) => {
  if (value !== regForm.password) callback(new Error('两次密码不一致'))
  else callback()
}

const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '长度 3~30 位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只允许字母、数字和下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' },
  ],
}

const healthGoalOptions = ['减重', '增肌', '维持体重', '控血糖', '降血脂', '增强体质', '改善睡眠']

const features = [
  { icon: '🤖', text: 'AI 智能饮食分析与建议' },
  { icon: '📊', text: '热量与营养素数据看板' },
  { icon: '📝', text: '餐食记录与健康评分' },
  { icon: '🧬', text: '个性化 BMR / TDEE 计算' },
  { icon: '📄', text: '一键生成 PDF 健康报告' },
]

function switchTab(tab) { activeTab.value = tab }

async function doLogin() {
  if (!await loginFormRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    await authStore.login({
      username:   loginForm.username,
      password:   loginForm.password,
      rememberMe: loginForm.rememberMe,
    })
    ElMessage.success('登录成功，欢迎回来！')
    router.replace('/dashboard')
  } catch (e) {
    // 错误已在 api/index.js 拦截器中弹窗
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!await regFormRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    const dto = {
      username:      regForm.username,
      password:      regForm.password,
      nickname:      regForm.nickname || undefined,
      gender:        regForm.gender   || undefined,
      age:           regForm.age      || undefined,
      heightCm:      regForm.heightCm || undefined,
      weightKg:      regForm.weightKg || undefined,
      activityLevel: regForm.activityLevel,
      healthGoals:   regForm.healthGoals.length ? regForm.healthGoals : undefined,
    }
    await authStore.register(dto)
    ElMessage.success('注册成功，已自动登录！')
    router.replace('/profile')  // 引导到画像页补全信息
  } catch (e) {
    // 已在拦截器中弹窗
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

/* ======== 左侧品牌 ======== */
.brand-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44%;
  background: linear-gradient(145deg, #1a7c5e 0%, #26a785 60%, #3bbf99 100%);
  padding: 48px 40px;
}

.brand-inner {
  text-align: center;
  color: #fff;
  max-width: 360px;
}

.logo-wrap {
  width: 88px; height: 88px;
  background: rgba(255,255,255,.18);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 24px;
  font-size: 40px;
  box-shadow: 0 8px 32px rgba(0,0,0,.18);
}

.app-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 10px;
  letter-spacing: 2px;
}

.app-desc {
  font-size: 15px;
  opacity: .85;
  margin: 0 0 36px;
}

.feature-list {
  list-style: none;
  padding: 0; margin: 0 0 40px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  background: rgba(255,255,255,.12);
  border-radius: 10px;
  padding: 10px 16px;
  backdrop-filter: blur(4px);
}

.feat-icon { font-size: 20px; }

.brand-footer {
  font-size: 13px;
  opacity: .65;
  border-top: 1px solid rgba(255,255,255,.25);
  padding-top: 24px;
}

/* ======== 右侧表单 ======== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  overflow-y: auto;
}

.form-card {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 20px;
  padding: 40px 44px 36px;
  box-shadow: 0 8px 40px rgba(0,0,0,.1);
}

/* ---- Tab ---- */
.tab-bar {
  position: relative;
  display: flex;
  gap: 0;
  background: #f4f6f8;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 32px;
}

.tab-btn {
  flex: 1;
  height: 40px;
  border: none;
  background: transparent;
  border-radius: 9px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  color: #888;
  transition: all .25s;
  position: relative;
  z-index: 1;
}

.tab-btn.active {
  color: #1a7c5e;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,.1);
}

/* ---- 表单元素 ---- */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #333;
  font-size: 13px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 10px !important;
}

:deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px rgba(26, 124, 94, .25) !important;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 12px;
  margin-top: 4px;
  background: linear-gradient(135deg, #1a7c5e, #26a785);
  border: none;
  letter-spacing: 4px;
  box-shadow: 0 4px 16px rgba(26, 124, 94, .35);
  transition: transform .15s, box-shadow .15s;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(26, 124, 94, .45);
}

.switch-tip {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-top: 20px;
}

.switch-tip a {
  color: #1a7c5e;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.section-divider {
  color: #aaa;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  text-align: center;
  position: relative;
  margin: 8px 0 16px;
}

.section-divider::before,
.section-divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 28%;
  height: 1px;
  background: #e8ecef;
}
.section-divider::before { left: 0; }
.section-divider::after  { right: 0; }

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.goal-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.goal-group .el-checkbox) {
  margin: 0;
  padding: 4px 14px;
  border: 1.5px solid #e0e6ef;
  border-radius: 20px;
  font-size: 13px;
  transition: all .2s;
}

:deep(.goal-group .el-checkbox.is-checked) {
  border-color: #1a7c5e;
  background: #f0faf6;
  color: #1a7c5e;
}

:deep(.goal-group .el-checkbox__input) { display: none; }

/* 响应式 */
@media (max-width: 900px) {
  .brand-panel { display: none; }
  .form-panel  { background: #fff; }
  .form-card   { box-shadow: none; border-radius: 0; padding: 32px 20px; }
}
</style>

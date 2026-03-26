<template>
  <div class="dashboard">
    <!-- 顶部标题栏 -->
    <header class="page-header">
      <div>
        <h1 class="page-title">数据看板</h1>
        <p class="page-sub">{{ todayStr }}，保持健康饮食 💪</p>
      </div>
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="切换日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        size="default"
        @change="loadDailyData"
      />
    </header>

    <!-- 今日统计卡片 -->
    <section class="stat-cards">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-icon" :style="{ background: card.bg }">
          <el-icon :size="22" :color="card.color"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ card.value }}<span class="stat-unit">{{ card.unit }}</span>
          </div>
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-progress">
            <el-progress
              :percentage="card.pct"
              :stroke-width="4"
              :color="card.color"
              :show-text="false"
            />
            <span class="stat-pct-text">{{ card.pct }}%</span>
          </div>
        </div>
      </div>
    </section>

    <div class="dashboard-grid">
      <!-- 左列 -->
      <div class="dashboard-col">
        <!-- 今日餐次热量分布 -->
        <div class="card">
          <div class="card-header">
            <span class="card-title">今日餐次热量</span>
            <el-tag type="success" size="small" round>{{ selectedDate }}</el-tag>
          </div>
          <div class="meal-bars">
            <div v-if="dailyRecords.length === 0" class="empty-state">
              <el-icon size="40" color="#e0e7ef"><Bowl /></el-icon>
              <p>今日暂无记录，快去打卡吧～</p>
              <el-button type="primary" size="small" round @click="$router.push('/records')">去记录</el-button>
            </div>
            <div v-else v-for="rec in dailyRecords" :key="rec.id" class="meal-bar-item">
              <div class="meal-bar-info">
                <span class="meal-tag" :style="mealStyle(rec.mealType)">{{ rec.mealTypeLabel }}</span>
                <span class="meal-desc">{{ rec.mealDescription }}</span>
              </div>
              <div class="meal-bar-right">
                <el-progress
                  :percentage="calPct(rec.totalCalories)"
                  :stroke-width="8"
                  :color="mealColor(rec.mealType)"
                  :show-text="false"
                  style="width:100px"
                />
                <span class="meal-kcal">{{ rec.totalCalories ? Math.round(rec.totalCalories) : '--' }} kcal</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 近7日热量趋势 -->
        <div class="card chart-card">
          <div class="card-header">
            <span class="card-title">近 7 日热量趋势</span>
          </div>
          <div ref="trendChartRef" class="echarts-box"></div>
        </div>
      </div>

      <!-- 右列 -->
      <div class="dashboard-col">
        <!-- 今日营养环形图 -->
        <div class="card chart-card">
          <div class="card-header">
            <span class="card-title">今日营养分配</span>
          </div>
          <div ref="pieChartRef" class="echarts-box"></div>
        </div>

        <!-- 近期打卡 -->
        <div class="card">
          <div class="card-header">
            <span class="card-title">近期饮食记录</span>
            <el-button link type="primary" size="small" @click="$router.push('/records')">查看全部 →</el-button>
          </div>
          <div v-if="!recentRecords.length" class="empty-state small">
            <p>暂无记录</p>
          </div>
          <div v-else class="recent-list">
            <div v-for="rec in recentRecords" :key="rec.id" class="recent-item">
              <div class="recent-icon" :style="{ background: mealBg(rec.mealType) }">
                {{ mealEmoji(rec.mealType) }}
              </div>
              <div class="recent-meta">
                <div class="recent-title">{{ rec.mealTypeLabel }} · {{ rec.mealDescription?.slice(0, 20) }}...</div>
                <div class="recent-sub">
                  <el-tag v-if="rec.healthScore" size="small" :type="scoreType(rec.healthScore)" round>
                    {{ rec.healthScore }}分
                  </el-tag>
                  <span>{{ rec.totalCalories ? Math.round(rec.totalCalories) + ' kcal' : '' }}</span>
                  <span class="recent-date">{{ formatDate(rec.createdAt) }}</span>
                </div>
              </div>
              <el-icon v-if="rec.isAbnormal" color="#f56c6c" :title="rec.abnormalReason"><Warning /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { dietApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const todayStr = ref(dayjs().format('YYYY年MM月DD日'))

const dailyRecords  = ref([])
const recentRecords = ref([])
const trendChartRef = ref(null)
const pieChartRef   = ref(null)
let trendChart, pieChart

// ---- 统计卡片数据 ----
const todayCalories  = computed(() => dailyRecords.value.reduce((s, r) => s + (r.totalCalories || 0), 0))
const todayProtein   = computed(() => dailyRecords.value.reduce((s, r) => s + (r.proteinG || 0), 0))
const todayCarbs     = computed(() => dailyRecords.value.reduce((s, r) => s + (r.carbohydratesG || 0), 0))
const todayFat       = computed(() => dailyRecords.value.reduce((s, r) => s + (r.fatG || 0), 0))
const tdee           = computed(() => userStore.profile?.tdee || 2000)

const statCards = computed(() => [
  { label: '今日热量', value: Math.round(todayCalories.value), unit: 'kcal', icon: 'Histogram',    color: '#42b883', bg: '#e8f8f0', pct: Math.min(100, Math.round(todayCalories.value / tdee.value * 100)) },
  { label: '蛋白质',   value: Math.round(todayProtein.value),  unit: 'g',    icon: 'Trophy',       color: '#409eff', bg: '#ecf5ff', pct: Math.min(100, Math.round(todayProtein.value / 60 * 100)) },
  { label: '碳水化合物',value: Math.round(todayCarbs.value),   unit: 'g',    icon: 'Grid',         color: '#e6a23c', bg: '#fdf6ec', pct: Math.min(100, Math.round(todayCarbs.value / 250 * 100)) },
  { label: '脂肪',     value: Math.round(todayFat.value),      unit: 'g',    icon: 'Help',         color: '#f56c6c', bg: '#fef0f0', pct: Math.min(100, Math.round(todayFat.value / 65 * 100)) },
])

// ---- 加载数据 ----
async function loadDailyData() {
  try {
    const res = await dietApi.daily(userStore.userId, selectedDate.value)
    dailyRecords.value = res.data?.data || []
    updatePieChart()
  } catch (_) { dailyRecords.value = [] }
}

async function loadRecentData() {
  try {
    const end = dayjs().format('YYYY-MM-DD')
    const start = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
    const res = await dietApi.range(userStore.userId, start, end)
    const all = res.data?.data || []
    recentRecords.value = all.slice(-8).reverse()
    updateTrendChart(all)
  } catch (_) { recentRecords.value = [] }
}

// ---- 图表 ----
function initCharts() {
  if (trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (pieChartRef.value)   pieChart   = echarts.init(pieChartRef.value)
}

function updateTrendChart(records) {
  if (!trendChart) return
  const days = Array.from({ length: 7 }, (_, i) =>
    dayjs().subtract(6 - i, 'day').format('MM-DD'))
  const calMap = {}
  records.forEach(r => {
    const d = dayjs(r.recordDate).format('MM-DD')
    calMap[d] = (calMap[d] || 0) + (r.totalCalories || 0)
  })
  trendChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>热量：{c} kcal' },
    grid: { top: 20, right: 10, bottom: 30, left: 46 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#e0e7ef' } }, axisLabel: { color: '#8a9ab0', fontSize: 11 } },
    yAxis: { type: 'value', name: 'kcal', nameTextStyle: { color: '#8a9ab0', fontSize: 10 }, axisLabel: { color: '#8a9ab0', fontSize: 10 }, splitLine: { lineStyle: { color: '#f0f4f8' } } },
    series: [{
      type: 'line',
      data: days.map(d => Math.round(calMap[d] || 0)),
      smooth: true,
      symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#42b883', width: 2.5 },
      itemStyle: { color: '#42b883' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(66,184,131,0.3)' }, { offset: 1, color: 'rgba(66,184,131,0)' }] } },
      markLine: { silent: true, data: [{ yAxis: tdee.value, label: { formatter: 'TDEE', position: 'insideEndTop', fontSize: 10 }, lineStyle: { color: '#f56c6c', type: 'dashed' } }] }
    }]
  })
}

function updatePieChart() {
  if (!pieChart) return
  const p = Math.round(todayProtein.value * 4)
  const c = Math.round(todayCarbs.value * 4)
  const f = Math.round(todayFat.value * 9)
  const total = p + c + f || 1
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} kcal ({d}%)' },
    legend: { bottom: 8, textStyle: { color: '#8a9ab0', fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['40%', '68%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: [
        { value: p, name: '蛋白质', itemStyle: { color: '#409eff' } },
        { value: c, name: '碳水',   itemStyle: { color: '#e6a23c' } },
        { value: f, name: '脂肪',   itemStyle: { color: '#f56c6c' } },
      ]
    }]
  })
}

// ---- 工具函数 ----
const mealColors = ['#42b883', '#409eff', '#e6a23c', '#909399', '#f56c6c']
const mealEmojis = ['🌅', '🌞', '🌙', '🍎', '🌃']
const mealBgs    = ['#e8f8f0', '#ecf5ff', '#fdf6ec', '#f4f4f5', '#fef0f0']

function mealColor(t)  { return mealColors[t || 0] }
function mealEmoji(t)  { return mealEmojis[t || 0] }
function mealBg(t)     { return mealBgs[t || 0] }
function mealStyle(t)  { return { background: mealBgs[t || 0], color: mealColors[t || 0], padding: '2px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 } }
function calPct(cal)   { return cal ? Math.min(100, Math.round(cal / (tdee.value / 3) * 100)) : 0 }
function scoreType(s)  { return s >= 80 ? 'success' : s >= 60 ? 'warning' : 'danger' }
function formatDate(d) { return d ? dayjs(d).format('MM-DD HH:mm') : '' }

onMounted(async () => {
  initCharts()
  await Promise.all([loadDailyData(), loadRecentData()])
})
</script>

<style scoped>
.dashboard { height: 100vh; overflow-y: auto; padding: 0 24px 32px; }

.page-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 0 16px;
  position: sticky; top: 0; background: var(--color-bg); z-index: 5;
}
.page-title { font-size: 20px; font-weight: 700; color: var(--color-text); }
.page-sub   { font-size: 13px; color: var(--color-text-muted); margin-top: 2px; }

/* 统计卡片 */
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px; }

.stat-card {
  background: var(--color-card); border-radius: var(--radius);
  padding: 16px; box-shadow: var(--shadow);
  display: flex; align-items: flex-start; gap: 12px;
  transition: var(--transition);
}
.stat-card:hover { box-shadow: var(--shadow-hover); transform: translateY(-2px); }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-body { flex: 1; min-width: 0; }
.stat-value { font-size: 22px; font-weight: 700; color: var(--color-text); line-height: 1.2; }
.stat-unit  { font-size: 12px; font-weight: 400; color: var(--color-text-muted); margin-left: 2px; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin: 2px 0 6px; }
.stat-progress { display: flex; align-items: center; gap: 6px; }
.stat-pct-text { font-size: 11px; color: var(--color-text-muted); white-space: nowrap; }

/* 网格布局 */
.dashboard-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.dashboard-col  { display: flex; flex-direction: column; gap: 16px; }

.card { background: var(--color-card); border-radius: var(--radius); padding: 18px; box-shadow: var(--shadow); }
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.card-title { font-size: 14px; font-weight: 600; color: var(--color-text); }
.chart-card { min-height: 240px; }
.echarts-box { width: 100%; height: 200px; }

/* 空状态 */
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 24px 0; color: var(--color-text-muted); font-size: 13px; }
.empty-state.small { padding: 12px 0; }

/* 餐次条 */
.meal-bars { display: flex; flex-direction: column; gap: 10px; }
.meal-bar-item { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.meal-bar-info { display: flex; align-items: center; gap: 8px; flex: 1; overflow: hidden; }
.meal-tag { padding: 2px 8px; border-radius: 10px; font-size: 12px; font-weight: 600; white-space: nowrap; }
.meal-desc { font-size: 12.5px; color: var(--color-text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meal-bar-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.meal-kcal { font-size: 12px; color: var(--color-text-muted); white-space: nowrap; min-width: 60px; text-align: right; }

/* 近期列表 */
.recent-list { display: flex; flex-direction: column; gap: 8px; }
.recent-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px; border-radius: 8px;
  transition: var(--transition); cursor: default;
}
.recent-item:hover { background: var(--color-bg); }
.recent-icon { width: 34px; height: 34px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.recent-meta { flex: 1; overflow: hidden; }
.recent-title { font-size: 12.5px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recent-sub { display: flex; align-items: center; gap: 6px; margin-top: 2px; font-size: 11px; color: var(--color-text-muted); }
.recent-date { margin-left: auto; }
</style>

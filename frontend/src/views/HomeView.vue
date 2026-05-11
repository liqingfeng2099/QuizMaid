<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { healthCheck, getSignInDays, getUserSignData, userSignIn, getSignInDays1 } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

type EChartsOption = echarts.EChartsOption

const loginUserStore = useLoginUserStore()
const chartContainer = ref<HTMLElement | null>(null)
let myChart: echarts.ECharts | null = null

const continuousDays = ref(0)
const signData = ref<Record<string, boolean>>({})
const value = ref(dayjs())
const onPanelChange = (value: dayjs.Dayjs, mode: string) => {
  console.log(value, mode);
};

const correctRate = computed(() => {
  const { answerNum = 0, correctNum = 0 } = loginUserStore.loginUser
  if (answerNum === 0) return 0
  return Math.round((correctNum / answerNum) * 100)
})

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
  initChart()
  loadData()
  loadSignData()
})

const initChart = () => {
  if (chartContainer.value) {
    myChart = echarts.init(chartContainer.value)
  }
}

const loadData = async () => {
  try {
    const userId = loginUserStore.loginUser.id
    if (userId) {
      const currentYear = new Date().getFullYear().toString()
      const res = await getSignInDays({ userId: userId })
      if (res.data.code === 0 && res.data.data) {
        const heatmapData: [string, number][] = res.data.data.map((item: API.UserHeatMapVO) => [
          item.date || '',
          item.level || 0 // 使用 level 字段作为热力图等级
        ])
        updateChart(heatmapData, currentYear)
      } else {
        // 没有数据时显示空热力图
        updateChart([], currentYear)
      }
    }
  } catch (error) {
    console.error('加载热力图数据失败', error)
    // 出错时显示空热力图
    const currentYear = new Date().getFullYear().toString()
    updateChart([], currentYear)
  }
}

const loadSignData = async () => {
  try {
    const res = await getSignInDays1()
    if (res.data.code === 0) {
      continuousDays.value = res.data.data || 0
    }
    const currentYear = new Date().getFullYear()
    const signRes = await getUserSignData({ year: currentYear })
    if (signRes.data.code === 0 && signRes.data.data) {
      // 将 Record<string, unknown> 转换为 Record<string, boolean>
      signData.value = Object.fromEntries(
        Object.entries(signRes.data.data).map(([key, value]) => [key, Boolean(value)])
      )
    }
  } catch (error) {
    console.error('加载签到数据失败', error)
  }
}

const handleSignIn = async () => {
  try {
    const res = await userSignIn()
    if (res.data.code === 0) {
      message.success('签到成功')
      continuousDays.value++
      await loadSignData()
    } else {
      message.error('签到失败：' + res.data.message)
    }
  } catch {
    message.error('签到请求失败')
  }
}

const updateChart = (data: [string, number][], year: string) => {
  if (!myChart) return
  
  const option: EChartsOption = {
    title: {
      top: 30,
      left: 'center',
      text: '学习热力图',
    },
    tooltip: {
      position: 'top',
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      formatter: function(params: any) {
        if (Array.isArray(params)) {
          params = params[0]
        }
        return params.value[0] + '<br/>学习天数: ' + params.value[1]
      }
    },
    visualMap: {
      min: 0,
      max: 5,
      type: 'piecewise',
      orient: 'horizontal',
      left: 'center',
      top: 65,
      pieces: [
        { min: 1, max: 1, label: '1天', color: '#e6f7ff' },
        { min: 2, max: 2, label: '2天', color: '#91d5ff' },
        { min: 3, max: 3, label: '3天', color: '#40a9ff' },
        { min: 4, max: 4, label: '4天', color: '#1890ff' },
        { min: 5, label: '5+天', color: '#096dd9' }
      ]
    },
    calendar: {
      top: 120,
      left: 30,
      right: 30,
      cellSize: ['auto', 13],
      range: year,
      itemStyle: {
        borderWidth: 0.5,
      },
      yearLabel: { show: false },
    },
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: data,
    },
  }
  
  myChart.setOption(option)
}

healthCheck().then((res) => {
  console.log(res)
})
</script>

<template>
  <div class="home-view">
    <h1>欢迎使用 AI 题库管理系统</h1>
    
    <!-- 学习统计部分 -->
    <div class="stats-container">
      <a-card class="stats-card">
        <template #title>
          <div class="card-title">学习统计</div>
        </template>
        
        <a-row :gutter="16" class="stats-row">
          <a-col :span="8">
            <a-statistic title="答题总数" :value="loginUserStore.loginUser.answerNum || 0" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="正确数量" :value="loginUserStore.loginUser.correctNum || 0" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="正确率" :value="correctRate" suffix="%" />
          </a-col>
        </a-row>
        
        <a-divider />
        
        <div class="sign-in-section">
          <div class="sign-in-header">
            <h3>签到记录</h3>
            <a-button type="primary" @click="handleSignIn">
              立即签到
            </a-button>
          </div>
          <a-row :gutter="16" class="sign-in-content">
            <a-col :span="8">
              <div class="continuous-days-card">
                <div class="continuous-days">
                  已连续签到 <span class="days-count">{{ continuousDays }}</span> 天
                </div>
              </div>
            </a-col>
            <a-col :span="16">
              <div class="calendar-card">
                <a-calendar 
                  v-model:value="value" 
                  :fullscreen="false" 
                  @panelChange="onPanelChange"
                  :showHeader="false"
                >
                  <template #header>
                    <div style="padding: 8px; text-align: center;">
                      <span style="font-size: 14px; font-weight: 500;">
                        {{ value.year() }}年{{ value.month() + 1 }}月
                      </span>
                    </div>
                  </template>
                </a-calendar>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-card>
    </div>
    
    <!-- 学习热力图 -->
    <div class="chart-container">
      <div ref="chartContainer" id="main" class="chart"></div>
    </div>
  </div>
</template>

<style scoped>
.home-view {
  padding: 24px;
  text-align: center;
  min-height: 600px;
}

.home-view h1 {
  font-size: 28px;
  color: #333;
  font-weight: 500;
  margin-bottom: 32px;
}

.stats-container {
  max-width: 1000px;
  margin: 0 auto 32px;
}

.stats-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 18px;
  font-weight: 500;
}

.stats-row {
  margin-bottom: 24px;
}

.sign-in-section {
  margin-top: 24px;
}

.sign-in-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sign-in-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.sign-in-content {
  display: flex;
  align-items: stretch;
}

.continuous-days-card {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 24px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.continuous-days {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.days-count {
  font-size: 24px;
  font-weight: bold;
  color: #1890ff;
  margin: 0 4px;
}

.calendar-card {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 16px;
  background: #fff;
  height: 100%;
}

.calendar-card :deep(.ant-picker-calendar-header) {
  margin-bottom: 16px;
}

.calendar-card :deep(.ant-picker-calendar-date) {
  height: 40px;
  line-height: 40px;
}

.calendar-card :deep(.ant-picker-cell-in-view.ant-picker-cell-selected .ant-picker-cell-inner) {
  background: #1890ff;
  color: #fff;
}

.calendar-card :deep(.ant-picker-cell-today .ant-picker-cell-inner::before) {
  border-color: #1890ff;
}

.chart-container {
  max-width: 1000px;
  margin: 0 auto;
}

.chart {
  width: 100%;
  height: 400px;
}
</style>

<template>
  <!-- 题型正确率统计页面主容器 .hml -->
  <div class="question-stats-page">
    <h2 style="margin-bottom: 16px;">题型正确率统计</h2>

    <!-- 试卷选择器卡片 .hml -->
    <a-card style="margin-bottom: 16px;">
      <a-space>
        <span>选择试卷：</span>
        <a-select
          v-model:value="selectedPaperId"
          style="width: 320px;"
          placeholder="选择试卷（不选则统计全部）"
          allow-clear
          :loading="paperLoading"
          @change="handlePaperChange"
        >
          <!-- 全部题型选项（不选时统计全部数据） .hml -->
          <a-select-option :value="undefined">全部题型</a-select-option>
          <!-- 遍历渲染可用的试卷列表 .hml -->
          <a-select-option v-for="p in availablePapers" :key="p.paperId!" :value="p.paperId!">
            {{ p.paperName }}
          </a-select-option>
        </a-select>
      </a-space>
    </a-card>

    <!-- 加载中状态包裹层 .hml -->
    <a-spin :spinning="loading">
      <template v-if="typeStats.length > 0">
        <!-- ECharts柱状图区域 .hml -->
        <a-row :gutter="16">
          <a-col :span="24">
            <a-card title="题型正确率对比" size="small" style="margin-bottom: 16px;">
              <div ref="barChartRef" style="width: 100%; height: 400px;"></div>
            </a-card>
          </a-col>
        </a-row>

        <!-- 统计明细表格卡片 .hml -->
        <a-card title="题型统计明细" size="small">
          <a-table
            :dataSource="typeStats"
            :columns="columns"
            :pagination="false"
            rowKey="questionType"
            size="middle"
          >
            <!-- 自定义列渲染：正确率和得分率用进度条展示 .hml -->
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'correctRate'">
                <a-progress
                  :percent="record.correctRate || 0"
                  :stroke-color="getColor(record.correctRate)"
                  size="small"
                />
              </template>
              <template v-if="column.key === 'scoreRate'">
                <a-progress
                  :percent="record.scoreRate || 0"
                  :stroke-color="getColor(record.scoreRate)"
                  size="small"
                />
              </template>
            </template>
          </a-table>
        </a-card>
      </template>
      <!-- 无数据时的空状态提示 .hml -->
      <a-empty v-else-if="!loading" description="暂无统计数据" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts' // 引入ECharts图表库 .hml
import { getTypeAccuracy, getPaperTypeAccuracy, getAvailablePapers } from '@/api/tongji' // 导入统计相关API .hml

// 当前选中的试卷ID（undefined表示全部） .hml
const selectedPaperId = ref<number | undefined>(undefined)
// 可用的试卷列表 .hml
const availablePapers = ref<API.PaperStatisticsVO[]>([])
// 题型统计列表数据 .hml
const typeStats = ref<API.QuestionTypeStatVO[]>([])
// 加载状态标记 .hml
const loading = ref(false)
// 试卷列表加载状态标记 .hml
const paperLoading = ref(false)

// ECharts柱状图容器引用和实例 .hml
const barChartRef = ref<HTMLElement | null>(null)
let barChart: echarts.ECharts | null = null

// 统计明细表格列定义 .hml
const columns = [
  { title: '题型', dataIndex: 'questionTypeName', key: 'questionTypeName', width: 120 },
  { title: '总题数', dataIndex: 'totalCount', key: 'totalCount', width: 100 },
  { title: '正确数', dataIndex: 'correctCount', key: 'correctCount', width: 100 },
  { title: '正确率', key: 'correctRate', width: 200 },
  { title: '总得分', dataIndex: 'totalActualScore', key: 'totalActualScore', width: 100 },
  { title: '总分值', dataIndex: 'totalQuestionScore', key: 'totalQuestionScore', width: 100 },
  { title: '得分率', key: 'scoreRate', width: 200 },
]

// 根据百分比返回对应颜色（红黄蓝绿） .hml
const getColor = (rate?: number) => {
  if (!rate) return '#f5222d'
  if (rate >= 80) return '#52c41a'
  if (rate >= 60) return '#1890ff'
  if (rate >= 40) return '#faad14'
  return '#f5222d'
}

// 加载可用的试卷列表 .hml
const loadPapers = async () => {
  paperLoading.value = true
  try {
    const res = await getAvailablePapers()
    if (res.data.code === 0) {
      availablePapers.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载试卷列表失败', e)
  } finally {
    paperLoading.value = false
  }
}

// 试卷选择变更时重新加载统计 .hml
const handlePaperChange = async () => {
  await loadStats()
}

// 加载题型统计（根据是否选中试卷决定调用哪个接口） .hml
const loadStats = async () => {
  loading.value = true
  try {
    let res: any
    if (selectedPaperId.value) {
      res = await getPaperTypeAccuracy(selectedPaperId.value)
    } else {
      res = await getTypeAccuracy()
    }
    if (res.data.code === 0) {
      typeStats.value = res.data.data || []
      nextTick(() => renderChart())
    } else {
      typeStats.value = []
    }
  } catch (e) {
    console.error('加载题型统计失败', e)
    typeStats.value = []
  } finally {
    loading.value = false
  }
}

// 渲染ECharts柱状图（正确率和得分率对比） .hml
const renderChart = () => {
  if (!barChartRef.value || typeStats.value.length === 0) return

  barChart?.dispose()
  barChart = echarts.init(barChartRef.value)

  const names = typeStats.value.map(t => t.questionTypeName || '未知')
  const correctRates = typeStats.value.map(t => t.correctRate || 0)
  const scoreRates = typeStats.value.map(t => t.scoreRate || 0)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['正确率', '得分率'],
      top: 10
    },
    toolbox: {
      feature: {
        saveAsImage: { title: '保存为图片', name: '题型正确率统计' }
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { rotate: 0 }
    },
    yAxis: {
      type: 'value',
      name: '百分比 (%)',
      max: 100,
      axisLabel: { formatter: '{value}%' }
    },
    series: [
      {
        name: '正确率',
        type: 'bar',
        data: correctRates,
        barWidth: '30%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#52c41a' },
            { offset: 1, color: '#b7eb8f' }
          ])
        },
        label: {
          show: true,
          position: 'top',
          formatter: (p: any) => p.value + '%'
        }
      },
      {
        name: '得分率',
        type: 'bar',
        data: scoreRates,
        barWidth: '30%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#1890ff' },
            { offset: 1, color: '#69c0ff' }
          ])
        },
        label: {
          show: true,
          position: 'top',
          formatter: (p: any) => p.value + '%'
        }
      }
    ]
  }

  barChart.setOption(option)
}

// 页面挂载时加载试卷列表和统计数据 .hml
onMounted(async () => {
  await loadPapers()
  await loadStats()
})

// 页面卸载时销毁ECharts实例释放内存 .hml
onUnmounted(() => {
  barChart?.dispose()
})
</script>

<style scoped>
.question-stats-page {
  padding: 0;
}
</style>

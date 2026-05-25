<template>
  <div>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-card title="各题型得分率与正确率" size="small">
          <div ref="typeChartRef" style="width: 100%; height: 350px;"></div>
          <a-empty v-if="!hasTypeData" description="暂无题型数据" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="各难度得分率与正确率" size="small">
          <div ref="diffChartRef" style="width: 100%; height: 350px;"></div>
          <a-empty v-if="!hasDiffData" description="暂无难度数据" />
        </a-card>
      </a-col>
    </a-row>
    <a-row :gutter="16" style="margin-top: 16px;">
      <a-col :span="24">
        <a-card title="各知识点正确率" size="small">
          <div ref="kpChartRef" style="width: 100%; height: 400px;"></div>
          <a-empty v-if="!hasKpData" description="暂无知识点数据" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { message } from 'ant-design-vue'

const props = defineProps<{
  stats: API.PaperStatisticsVO
}>()

const typeChartRef = ref<HTMLElement | null>(null)
const diffChartRef = ref<HTMLElement | null>(null)
const kpChartRef = ref<HTMLElement | null>(null)
let typeChart: echarts.ECharts | null = null
let diffChart: echarts.ECharts | null = null
let kpChart: echarts.ECharts | null = null

const hasTypeData = computed(() =>
  props.stats.questionTypeStats && props.stats.questionTypeStats.length > 0
)
const hasDiffData = computed(() =>
  props.stats.difficultyStats && props.stats.difficultyStats.length > 0
)
const hasKpData = computed(() =>
  props.stats.knowledgePointStats && props.stats.knowledgePointStats.length > 0
)

const initTypeChart = () => {
  if (!typeChartRef.value || !hasTypeData.value) return
  typeChart = echarts.init(typeChartRef.value)
  const types = props.stats.questionTypeStats!
  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    toolbox: { feature: { saveAsImage: { title: '保存', name: '题型统计' } } },
    legend: { data: ['正确率', '得分率'] },
    xAxis: { type: 'category', data: types.map(t => t.questionTypeName || '') },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: '正确率', type: 'bar',
        data: types.map(t => t.correctRate || 0),
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '得分率', type: 'bar',
        data: types.map(t => t.scoreRate || 0),
        itemStyle: { color: '#52c41a' }
      }
    ]
  }
  typeChart.setOption(option)
  // A7: 题型点击钻取
  typeChart.off('click')
  typeChart.on('click', (params: any) => {
    const t = types[params.dataIndex]
    if (t) {
      message.info(`${t.questionTypeName}: 正确率 ${t.correctRate}%, 答题 ${t.totalCount} 次, 得分率 ${t.scoreRate}%`)
    }
  })
}

const initDiffChart = () => {
  if (!diffChartRef.value || !hasDiffData.value) return
  diffChart = echarts.init(diffChartRef.value)
  const diffs = props.stats.difficultyStats!
  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    toolbox: { feature: { saveAsImage: { title: '保存', name: '难度统计' } } },
    legend: { data: ['正确率', '得分率'] },
    xAxis: { type: 'category', data: diffs.map(d => d.difficultyName || '') },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: '正确率', type: 'bar',
        data: diffs.map(d => d.correctRate || 0),
        itemStyle: { color: '#faad14' }
      },
      {
        name: '得分率', type: 'bar',
        data: diffs.map(d => d.scoreRate || 0),
        itemStyle: { color: '#52c41a' }
      }
    ]
  }
  diffChart.setOption(option)
}

const initKpChart = () => {
  if (!kpChartRef.value || !hasKpData.value) return
  kpChart = echarts.init(kpChartRef.value)
  const kps = (props.stats.knowledgePointStats || []).slice(0, 20)
  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    toolbox: { feature: { saveAsImage: { title: '保存', name: '知识点正确率' } } },
    grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: kps.map(k => k.knowledgePoint || '').reverse(),
      axisLabel: { width: 100, overflow: 'truncate' } },
    series: [{
      type: 'bar', data: kps.map(k => k.correctRate || 0).reverse(),
      itemStyle: { color: '#722ed1' },
      label: { show: true, position: 'right', formatter: '{c}%' }
    }]
  }
  kpChart.setOption(option)
  // A7: 图表点击钻取
  kpChart.off('click')
  kpChart.on('click', (params: any) => {
    const kp = kps[kps.length - 1 - params.dataIndex]
    if (kp) {
      message.info(`${kp.knowledgePoint}: 正确率 ${kp.correctRate}%, 答题 ${kp.totalCount} 次, 得分率 ${kp.scoreRate}%`)
    }
  })
}

watch(() => props.stats.paperId, () => {
  nextTick(() => {
    typeChart?.dispose()
    diffChart?.dispose()
    kpChart?.dispose()
    initTypeChart()
    initDiffChart()
    initKpChart()
  })
})

onMounted(() => {
  nextTick(() => {
    initTypeChart()
    initDiffChart()
    initKpChart()
  })
})

onUnmounted(() => {
  typeChart?.dispose()
  diffChart?.dispose()
  kpChart?.dispose()
})
</script>

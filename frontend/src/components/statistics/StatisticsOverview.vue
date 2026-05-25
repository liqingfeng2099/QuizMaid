<template>
  <div>
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="4">
        <a-statistic title="参考人数" :value="stats.totalExaminees || 0" suffix="人" />
      </a-col>
      <a-col :span="4">
        <a-statistic title="最高分" :value="stats.maxScore || 0" :value-style="{ color: '#3f8600' }" />
      </a-col>
      <a-col :span="4">
        <a-statistic title="最低分" :value="stats.minScore || 0" :value-style="{ color: '#cf1322' }" />
      </a-col>
      <a-col :span="4">
        <a-statistic title="平均分" :value="stats.avgScore || 0" :precision="2" />
      </a-col>
      <a-col :span="4">
        <a-statistic title="中位数" :value="stats.medianScore || 0" />
      </a-col>
      <a-col :span="4">
        <a-statistic title="试卷总分" :value="stats.totalScore || 0" />
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <a-card size="small">
          <a-statistic
            title="及格率 (>=60%)"
            :value="stats.passRate || 0"
            :precision="1"
            suffix="%"
            :value-style="{ color: (stats.passRate || 0) >= 60 ? '#3f8600' : '#cf1322' }"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <a-statistic
            title="高分率 (>=90%)"
            :value="stats.highScoreRate || 0"
            :precision="1"
            suffix="%"
            :value-style="{ color: (stats.highScoreRate || 0) >= 20 ? '#3f8600' : '#faad14' }"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="分数段分布" size="small">
      <div ref="distChartRef" style="width: 100%; height: 350px;"></div>
      <a-empty v-if="!hasDistData" description="暂无分数分布数据" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  stats: API.PaperStatisticsVO
}>()

const distChartRef = ref<HTMLElement | null>(null)
let distChart: echarts.ECharts | null = null

const hasDistData = computed(() =>
  props.stats.scoreDistribution && props.stats.scoreDistribution.length > 0
)

const initDistChart = () => {
  if (!distChartRef.value || !hasDistData.value) return
  distChart = echarts.init(distChartRef.value)

  const dist = props.stats.scoreDistribution!
  const xData = dist.map(d => `${d.scoreBucket}-${(d.scoreBucket || 0) + 10}`)
  const yData = dist.map(d => d.count || 0)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    toolbox: {
      feature: {
        saveAsImage: { title: '保存为图片', name: '分数段分布' },
        dataZoom: { title: { zoom: '区域缩放', back: '还原' } }
      }
    },
    xAxis: {
      type: 'category',
      data: xData,
      name: '分数段',
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value',
      name: '人数'
    },
    series: [{
      type: 'bar',
      data: yData,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#1890ff' },
          { offset: 1, color: '#69c0ff' }
        ])
      },
      barWidth: '60%'
    }]
  }
  distChart.setOption(option)
}

watch(() => props.stats.paperId, () => {
  nextTick(() => {
    distChart?.dispose()
    initDistChart()
  })
})

onMounted(() => {
  nextTick(() => initDistChart())
})

onUnmounted(() => {
  distChart?.dispose()
})
</script>

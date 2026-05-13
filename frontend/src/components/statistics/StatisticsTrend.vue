<template>
  <div>
    <a-card size="small" style="margin-bottom: 16px;">
      <a-row :gutter="16" align="middle">
        <a-col :span="8">
          <span style="margin-right: 8px;">学科筛选：</span>
          <a-select
            v-model:value="subjectFilter"
            style="width: 160px;"
            placeholder="全部学科"
            allow-clear
            @change="load"
          >
            <a-select-option value="数学">数学</a-select-option>
            <a-select-option value="语文">语文</a-select-option>
            <a-select-option value="英语">英语</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-button type="primary" @click="load">查询趋势</a-button>
        </a-col>
      </a-row>
    </a-card>

    <a-card title="个人成绩趋势" size="small">
      <div ref="trendChartRef" style="width: 100%; height: 400px;"></div>
      <a-empty v-if="!hasData" description="暂无成绩趋势数据" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import { getScoreTrend } from '@/api/tongji'

const subjectFilter = ref<string | undefined>(undefined)
const trendData = ref<API.TrendDataPointVO[]>([])
const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null

const hasData = computed(() => trendData.value.length > 0)

const load = async () => {
  try {
    const res = await getScoreTrend({ subject: subjectFilter.value, limit: 50 })
    if (res.data.code === 0) {
      trendData.value = res.data.data || []
      nextTick(() => initTrendChart())
    } else {
      message.error(res.data.message || '加载趋势数据失败')
    }
  } catch (e) {
    message.error('加载趋势数据失败')
  }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const data = trendData.value
  if (data.length === 0) return

  const xData = data.map(d => d.paperName || d.examTime || '')
  const yData = data.map(d => d.score || 0)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params
        return `${p.name}<br/>分数: ${p.value}`
      }
    },
    toolbox: {
      feature: {
        saveAsImage: { title: '保存为图片', name: '成绩趋势' },
        dataZoom: { title: { zoom: '区域缩放', back: '还原' } }
      }
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value',
      name: '分数'
    },
    series: [{
      type: 'line',
      data: yData,
      smooth: true,
      lineStyle: { color: '#1890ff', width: 2 },
      itemStyle: { color: '#1890ff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24,144,255,0.3)' },
          { offset: 1, color: 'rgba(24,144,255,0.05)' }
        ])
      },
      markPoint: {
        data: [
          { type: 'max', name: '最高' },
          { type: 'min', name: '最低' }
        ]
      }
    }]
  }
  trendChart.setOption(option)
}

onMounted(() => {
  load()
})

onUnmounted(() => {
  trendChart?.dispose()
})
</script>

<template>
  <div>
    <a-card title="个人成绩 vs 整体对比" size="small">
      <a-spin :spinning="loading">
        <template v-if="hasData">
          <a-row :gutter="16" style="margin-bottom: 16px;">
            <a-col :span="6">
              <a-statistic title="您的分数" :value="stats?.maxScore || 0" :value-style="{ color: '#1890ff' }" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="整体平均分" :value="stats?.avgScore || 0" :precision="2" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="整体最高分" :value="stats?.maxScore || 0" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="参考人数" :value="stats?.totalExaminees || 0" suffix="人" />
            </a-col>
          </a-row>
          <div ref="compChartRef" style="width: 100%; height: 350px;"></div>
        </template>
        <a-empty v-else-if="!loading" description="暂无该试卷的考试记录" />
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import { getComparison } from '@/api/tongji'

const props = defineProps<{
  paperId: number | undefined
}>()

const stats = ref<API.PaperStatisticsVO | null>(null)
const loading = ref(false)
const compChartRef = ref<HTMLElement | null>(null)
let compChart: echarts.ECharts | null = null

const hasData = computed(() => stats.value && (stats.value.totalExaminees || 0) > 0)

const load = async () => {
  if (!props.paperId) return
  loading.value = true
  try {
    const res = await getComparison(props.paperId)
    if (res.data.code === 0) {
      stats.value = res.data.data || null
      nextTick(() => initCompChart())
    } else {
      message.error(res.data.message || '加载对比数据失败')
    }
  } catch (e) {
    message.error('加载对比数据失败')
  } finally {
    loading.value = false
  }
}

const initCompChart = () => {
  if (!compChartRef.value || !stats.value) return
  compChart?.dispose()
  compChart = echarts.init(compChartRef.value)

  const s = stats.value
  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    toolbox: { feature: { saveAsImage: { title: '保存', name: '个人对比' } } },
    xAxis: { type: 'category', data: ['最高分', '最低分', '平均分', '中位数'] },
    yAxis: { type: 'value' },
    legend: { data: ['整体', '您'] },
    series: [
      {
        name: '整体', type: 'bar',
        data: [s.maxScore || 0, s.minScore || 0, s.avgScore || 0, s.medianScore || 0],
        itemStyle: { color: '#91d5ff' }
      }
    ]
  }
  compChart.setOption(option)
}

watch(() => props.paperId, () => {
  if (props.paperId) load()
})

onMounted(() => {
  if (props.paperId) load()
})

onUnmounted(() => {
  compChart?.dispose()
})
</script>

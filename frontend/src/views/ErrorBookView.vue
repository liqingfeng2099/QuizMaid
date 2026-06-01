<template>
  <div class="error-book-page">
    <h2>错题本</h2>

    <a-card size="small" style="margin-bottom: 16px;">
      <a-row :gutter="12" align="middle">
        <a-col :span="3">
          <a-select v-model:value="filterErrorType" placeholder="错误类型" allow-clear @change="loadErrors">
            <a-select-option :value="1">概念错误</a-select-option>
            <a-select-option :value="2">计算错误</a-select-option>
            <a-select-option :value="3">思路错误</a-select-option>
            <a-select-option :value="4">审题错误</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="3">
          <a-input v-model:value="filterKnowledgePoint" placeholder="知识点" allow-clear @change="loadErrors" />
        </a-col>
        <a-col :span="3">
          <a-select v-model:value="filterReviewStatus" placeholder="复习状态" allow-clear @change="loadErrors">
            <a-select-option :value="0">未复习</a-select-option>
            <a-select-option :value="1">复习中</a-select-option>
            <a-select-option :value="2">已掌握</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="3">
          <a-select v-model:value="sortBy" @change="loadErrors">
            <a-select-option value="lastErrorTime">最近错误</a-select-option>
            <a-select-option value="firstErrorTime">首次错误</a-select-option>
            <a-select-option value="errorCount">错误次数</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="6">
          <a-space>
            <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
              <a-radio-button value="list">列表</a-radio-button>
              <a-radio-button value="charts">图表</a-radio-button>
              <a-radio-button value="recommend">推荐</a-radio-button>
            </a-radio-group>
            <a-button size="small" @click="handlePreview">预览</a-button>
            <a-button size="small" @click="handleExportExcel">导出</a-button>
            <a-button size="small" type="primary" @click="showAssemblyModal = true">强化组卷</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <template v-if="viewMode === 'list'">
      <div style="margin-bottom:8px;">
        <a-space>
          <a-button size="small" danger :disabled="selectedIds.length===0" @click="handleBatchDelete">批量删除</a-button>
          <a-button size="small" :disabled="selectedIds.length===0" @click="handleBatchReview(1)">批量标复习中</a-button>
          <a-button size="small" :disabled="selectedIds.length===0" @click="handleBatchReview(2)">批量标已掌握</a-button>
        </a-space>
      </div>
      <a-table :columns="columns" :data-source="errorList" :loading="loading"
        :pagination="pagination" @change="handleTableChange" row-key="id" size="small"
        :row-selection="{ selectedRowKeys: selectedIds, onChange: (keys: any) => selectedIds = keys }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'errorType'">
            <a-tag :color="getErrorTypeColor(record.errorType)">{{ getErrorTypeName(record.errorType) }}</a-tag>
          </template>
          <template v-if="column.key === 'reviewStatus'">
            <a-select v-model:value="record.reviewStatus" size="small" style="width: 90px;"
              @change="(v: number) => handleReviewStatus(record.id, v)">
              <a-select-option :value="0">未复习</a-select-option>
              <a-select-option :value="1">复习中</a-select-option>
              <a-select-option :value="2">已掌握</a-select-option>
            </a-select>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-select v-model:value="record.errorType" size="small" style="width: 70px;"
                @change="(v: number) => handleErrorType(record.id, v)">
                <a-select-option :value="1">概念</a-select-option>
                <a-select-option :value="2">计算</a-select-option>
                <a-select-option :value="3">思路</a-select-option>
                <a-select-option :value="4">审题</a-select-option>
              </a-select>
              <a-button size="small" @click="handleArchive(record.id)">归档</a-button>
              <a-button size="small" danger @click="handleDelete(record.id)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>

    <template v-if="viewMode === 'charts'">
      <a-spin :spinning="statsLoading">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-card size="small"><a-statistic title="错题总数" :value="errorStats?.totalErrors || 0" /></a-card>
          </a-col>
          <a-col :span="16">
            <a-card size="small" title="错误类型分布">
              <div ref="typePieRef" style="height: 250px;"></div>
            </a-card>
          </a-col>
        </a-row>
        <a-row :gutter="16" style="margin-top: 16px;">
          <a-col :span="12">
            <a-card size="small" title="薄弱知识点雷达图">
              <div ref="radarRef" style="height: 350px;"></div>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card size="small" title="高频错误知识点">
              <div ref="barRef" style="height: 350px;"></div>
            </a-card>
          </a-col>
        </a-row>
      </a-spin>
    </template>

    <!-- 推荐模式 -->
    <template v-if="viewMode === 'recommend'">
      <a-card size="small" style="margin-bottom: 12px;">
        <a-row :gutter="12" align="middle">
          <a-col :span="3"><span>推荐数量：</span><a-input-number v-model:value="recCount" :min="5" :max="30" size="small" /></a-col>
          <a-col :span="3">
            <a-select v-model:value="recTendency" size="small">
              <a-select-option value="basic">基础</a-select-option>
              <a-select-option value="balanced">均衡</a-select-option>
              <a-select-option value="advanced">进阶</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="3"><a-checkbox v-model:checked="recIncludeAnalysis">含解析</a-checkbox></a-col>
          <a-col :span="7">
            <a-checkbox v-model:checked="recFilterRecentEnabled">过滤近期做对</a-checkbox>
            <a-input-number
              v-if="recFilterRecentEnabled"
              v-model:value="recFilterRecentDays"
              :min="1" :max="365" size="small"
              style="width:70px;margin-left:4px"
              addon-after="天"
            />
          </a-col>
          <a-col :span="4"><a-button type="primary" size="small" @click="loadRecommend" :loading="recLoading">获取推荐</a-button></a-col>
        </a-row>
      </a-card>
      <a-table :columns="recColumns" :data-source="recList" :loading="recLoading" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag color="blue">{{ getTypeName(record.type) }}</a-tag>
          </template>
          <template v-if="column.key === 'difficulty'">
            <a-tag :color="record.difficulty===1?'green':record.difficulty===3?'red':'orange'">{{ getDiffName(record.difficulty) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button size="small" type="primary" @click="handleRecFeedback(record.id, true)">已掌握</a-button>
            <a-button size="small" @click="handleRecFeedback(record.id, false)" style="margin-left:4px;">仍困难</a-button>
          </template>
        </template>
      </a-table>
      <a-empty v-if="!recLoading && recList.length===0" description="暂无推荐题目，请先积累错题后重试" />
    </template>

    <!-- 强化组卷弹窗 -->
    <a-modal v-model:open="showAssemblyModal" title="错题专项强化组卷" @ok="handleAssembly" :ok-text="'开始组卷'" :confirm-loading="assemblyLoading">
      <a-form layout="vertical">
        <a-form-item label="试卷名称"><a-input v-model:value="asmPaperName" placeholder="错题强化卷" /></a-form-item>
        <a-form-item label="目标题数"><a-input-number v-model:value="asmQuestionCount" :min="5" :max="50" style="width:100%;" /></a-form-item>
        <a-form-item label="目标难度"><a-select v-model:value="asmDifficulty">
          <a-select-option :value="1">简单</a-select-option>
          <a-select-option :value="2">中等</a-select-option>
          <a-select-option :value="3">困难</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="答题时长(分钟)"><a-input-number v-model:value="asmDuration" :min="15" :max="180" style="width:100%;" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 预览弹窗 -->
    <a-modal v-model:open="previewVisible" title="错题集预览" width="800px" :footer="null">
      <div class="preview-container" v-html="previewHtml"></div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import { listErrors, updateReviewStatus, updateErrorType, toggleArchive, deleteError, getErrorStats, getWeakKnowledgePoints, recommendQuestions, recommendFeedback, batchDeleteErrors, batchUpdateReviewStatus, exportErrorBookExcel, previewErrorBook, reinforceAssemble } from '@/api/gerentongji'

const loading = ref(false)
const statsLoading = ref(false)
const viewMode = ref('list')
const errorList = ref<any[]>([])
const errorStats = ref<any>(null)
const weakPoints = ref<API.PersonalDimensionVO[]>([])
const filterErrorType = ref<number | undefined>(undefined)
const filterKnowledgePoint = ref('')
const filterReviewStatus = ref<number | undefined>(undefined)
const sortBy = ref('lastErrorTime')
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const selectedIds = ref<number[]>([])
const typePieRef = ref<HTMLElement | null>(null)
const radarRef = ref<HTMLElement | null>(null)
const barRef = ref<HTMLElement | null>(null)
let chartInstances: echarts.ECharts[] = []

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '题号', dataIndex: 'questionId', width: 70 },
  { title: '错误类型', key: 'errorType', width: 100 },
  { title: '错误次数', dataIndex: 'errorCount', width: 80 },
  { title: '最近错误', dataIndex: 'lastErrorTime', width: 160 },
  { title: '复习状态', key: 'reviewStatus', width: 110 },
  { title: '操作', key: 'action', width: 220 },
]

const getErrorTypeName = (t: number) => ({ 1:'概念错误', 2:'计算错误', 3:'思路错误', 4:'审题错误' }[t] || '未知')
const getErrorTypeColor = (t: number) => ({ 1:'orange', 2:'red', 3:'purple', 4:'blue' }[t] || 'default')

const loadErrors = async () => {
  loading.value = true
  try {
    const res = await listErrors({
      pageNum: pagination.current, pageSize: pagination.pageSize,
      errorType: filterErrorType.value,
      knowledgePoint: filterKnowledgePoint.value || undefined,
      reviewStatus: filterReviewStatus.value,
      sortBy: sortBy.value,
    })
    if (res.data.code === 0 && res.data.data) {
      errorList.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    }
  } catch (e) { message.error('加载失败') }
  loading.value = false
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const [sRes, kpRes] = await Promise.all([getErrorStats(), getWeakKnowledgePoints()])
    if (sRes.data.code === 0) errorStats.value = sRes.data.data
    weakPoints.value = kpRes.data.code === 0 ? (kpRes.data.data || []) : []
    nextTick(() => renderStatsCharts())
  } catch (e) { /* ignore */ }
  statsLoading.value = false
}

const renderStatsCharts = () => {
  chartInstances.forEach(c => c.dispose()); chartInstances = []
  if (typePieRef.value && errorStats.value?.byErrorType) {
    const c = echarts.init(typePieRef.value); chartInstances.push(c)
    c.setOption({
      tooltip:{}, toolbox:{feature:{saveAsImage:{title:'保存'}}},
      series:[{type:'pie',radius:'70%',
        data:errorStats.value.byErrorType.map((d:any)=>({name:d.dimensionKey,value:d.totalCount}))}]
    })
  }
  if (radarRef.value && weakPoints.value.length) {
    const c = echarts.init(radarRef.value); chartInstances.push(c)
    const top = weakPoints.value.slice(0,8)
    const maxVal = Math.max(...top.map(t=>t.totalCount||0))
    c.setOption({
      tooltip:{}, toolbox:{feature:{saveAsImage:{title:'保存'}}},
      radar:{ indicator: top.map(k=>({name:k.dimensionKey,max:maxVal*1.3})) },
      series:[{type:'radar',data:[{value:top.map(k=>k.totalCount||0),name:'错误次数',
        areaStyle:{color:'rgba(255,77,79,0.3)'}}]}]
    })
  }
  if (barRef.value && weakPoints.value.length) {
    const c = echarts.init(barRef.value); chartInstances.push(c)
    const top = weakPoints.value.slice(0,15)
    c.setOption({
      tooltip:{}, toolbox:{feature:{saveAsImage:{title:'保存'}}},
      grid:{left:'3%',right:'8%',containLabel:true},
      xAxis:{type:'value'}, yAxis:{type:'category',data:top.map(k=>k.dimensionKey).reverse()},
      series:[{type:'bar',data:top.map(k=>k.totalCount||0).reverse(),itemStyle:{color:'#ff4d4f'}}]
    })
  }
}

const handleTableChange = (pag:any) => { pagination.current=pag.current; pagination.pageSize=pag.pageSize; loadErrors() }
const handleReviewStatus = async (id:number,s:number) => { try{await updateReviewStatus(id,s)}catch{message.error('失败')} }
const handleErrorType = async (id:number,t:number) => { try{await updateErrorType(id,t)}catch{message.error('失败')} }
const handleArchive = async (id:number) => { try{await toggleArchive(id);message.success('已归档');loadErrors()}catch{message.error('失败')} }
const handleDelete = async (id:number) => { try{await deleteError(id);message.success('已删除');loadErrors()}catch{message.error('失败')} }

watch(viewMode, (v) => { if (v==='charts') loadStats() })
// ===== 推荐 =====
const recCount = ref(15)
const recTendency = ref('balanced')
const recIncludeAnalysis = ref(true)
const recFilterRecentEnabled = ref(true)
const recFilterRecentDays = ref(30)
const recLoading = ref(false)
const recList = ref<any[]>([])
const recColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 }, { title: '题型', key: 'type', width: 80 },
  { title: '难度', key: 'difficulty', width: 70 }, { title: '题干', dataIndex: 'content', ellipsis: true },
  { title: '知识点', dataIndex: 'knowledgePoints', width: 150, ellipsis: true },
  { title: '操作', key: 'action', width: 160 }
]
const getTypeName = (t: number) => ({ 1:'单选题', 2:'多选题', 3:'填空题', 4:'简答题' }[t] || '?')
const getDiffName = (d: number) => ({ 1:'简单', 2:'中等', 3:'困难' }[d] || '?')

const loadRecommend = async () => {
  recLoading.value = true
  try {
    const res = await recommendQuestions({ count: recCount.value, difficultyTendency: recTendency.value, includeAnalysis: recIncludeAnalysis.value, filterRecentEnabled: recFilterRecentEnabled.value, filterRecentDays: recFilterRecentDays.value })
    if (res.data.code === 0) recList.value = res.data.data || []
  } catch { recList.value = [] }
  recLoading.value = false
}
const handleRecFeedback = async (qid: number, correct: boolean) => {
  try { await recommendFeedback(qid, correct); message.success(correct ? '已记录' : '已记录') } catch {}
}

// ===== 批量操作 =====
const handleBatchDelete = async () => {
  try { await batchDeleteErrors(selectedIds.value); message.success('已删除'); selectedIds.value = []; loadErrors() } catch { message.error('失败') }
}
const handleBatchReview = async (status: number) => {
  try { await batchUpdateReviewStatus(selectedIds.value, status); message.success('已更新'); loadErrors() } catch { message.error('失败') }
}

// ===== 导出/预览 =====
const previewVisible = ref(false)
const previewHtml = ref('')
/** 剥离预览 HTML 中的 style/body/html 标签，防止污染外层页面样式 */
function sanitizePreviewHtml(raw: string): string {
  let s = raw
  // 去掉 <style>...</style>
  s = s.replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
  // 去掉 <html> / <head> / <body> 标签本身（保留内容）
  s = s.replace(/<\/?(html|head|body)[^>]*>/gi, '')
  // 去掉 DOCTYPE
  s = s.replace(/<!DOCTYPE[^>]*>/gi, '')
  return s
}

const handlePreview = async () => {
  try {
    const res = await previewErrorBook()
    if (res.data.code === 0) {
      previewHtml.value = sanitizePreviewHtml(res.data.data || '')
      previewVisible.value = true
    }
  } catch { /* ignore */ }
}
const handleExportExcel = async () => {
  try { const res=await exportErrorBookExcel(); const b=new Blob([res.data as any]); const a=document.createElement('a'); a.href=URL.createObjectURL(b); a.download='错题集.csv'; a.click() } catch { message.error('导出失败') }
}

// ===== 强化组卷 =====
const showAssemblyModal = ref(false)
const asmPaperName = ref('错题强化卷')
const asmQuestionCount = ref(15)
const asmDifficulty = ref<number|undefined>(2)
const asmDuration = ref(45)
const assemblyLoading = ref(false)
const handleAssembly = async () => {
  if (assemblyLoading.value) return
  assemblyLoading.value = true
  try {
    const res = await reinforceAssemble({ paperName: asmPaperName.value, questionCount: asmQuestionCount.value, difficultyAvg: asmDifficulty.value, duration: asmDuration.value })
    if (res.data.code === 0) {
      message.success('组卷成功！试卷ID: ' + res.data.data?.id)
      showAssemblyModal.value = false
    } else {
      message.error(res.data.message || '组卷失败')
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '组卷请求失败，请稍后重试'
    message.error(msg)
  } finally {
    assemblyLoading.value = false
  }
}

onMounted(() => { loadErrors() })
onUnmounted(() => { chartInstances.forEach(c=>c.dispose()) })
</script>

<style scoped>
.error-book-page { padding: 0; }

/* 预览容器：隔离预览内容，防止污染外层样式 */
.preview-container {
  max-height: 65vh;
  overflow-y: auto;
  font-family: 'Microsoft YaHei', sans-serif;
  line-height: 1.8;
}
/* 恢复预览内基础样式（原 style 标签被剥离后用 scoped 补充） */
.preview-container :deep(.q-item) {
  border: 1px solid #e8e8e8;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 4px;
}
.preview-container :deep(.q-title) {
  font-weight: bold;
  margin-bottom: 4px;
}
.preview-container :deep(.q-content) {
  margin: 8px 0;
}
.preview-container :deep(.q-answer) {
  color: #52c41a;
  margin-top: 4px;
}
.preview-container :deep(.q-meta) {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}
.preview-container :deep(h2) {
  font-size: 18px;
  margin-bottom: 16px;
}
.preview-container :deep(.q-content) {
  margin: 8px 0;
}
.preview-container :deep(.q-answer) {
  color: #52c41a;
  margin-top: 4px;
}
.preview-container :deep(.q-meta) {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}
.preview-container :deep(h2) {
  font-size: 18px;
  margin-bottom: 16px;
}
</style>

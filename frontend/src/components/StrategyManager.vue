<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  listPaperStrategyByPage, addPaperStrategy, updatePaperStrategy,
  deletePaperStrategy, getPaperStrategyById, setDefaultStrategy, copyPaperStrategy,
} from '@/api/shijuanguanli'

// ========== 列表 ==========
const loading = ref(false)
const strategyList = ref<API.PaperStrategyVO[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '策略名称', dataIndex: 'strategyName', ellipsis: true },
  { title: '总分', dataIndex: 'totalScore', width: 80 },
  { title: '难度', key: 'difficultyAvg', width: 60 },
  { title: '时长(分)', dataIndex: 'duration', width: 80 },
  { title: '默认', key: 'isDefault', width: 60 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 280 },
]

async function loadList() {
  loading.value = true
  try {
    const res = await listPaperStrategyByPage({ pageNum: pagination.current, pageSize: pagination.pageSize })
    if (res.data.code === 0 && res.data.data) {
      strategyList.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    }
  } catch { message.error('加载策略列表失败') }
  loading.value = false
}

function handleTableChange(pag: { current: number; pageSize: number }) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadList()
}

// ========== 详情 ==========
const detailVisible = ref(false)
const detailStrategy = ref<API.PaperStrategyVO | null>(null)
const detailLoading = ref(false)

interface ParsedTypeConfig { type: number; count: number; score: number }
interface ParsedDiffConfig { level: number; ratio: number }
const detailTypes = ref<ParsedTypeConfig[]>([])
const detailDiffs = ref<ParsedDiffConfig[]>([])

function getTypeName(t?: number) {
  return { 1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题' }[t || 0] || '未知'
}
function getDiffName(d?: number) {
  return { 1: '简单', 2: '中等', 3: '困难', 4: '较难', 5: '极难' }[d || 0] || '未知'
}
function getWeightLabel(code: string) {
  const map: Record<string, string> = {
    difficulty: '难度', accuracy: '正确率', discrimination: '区分度',
    calcLevel: '计算量', examFrequency: '考频', knowledgeCount: '考点覆盖',
  }
  return map[code] || code
}

async function handleViewDetail(record: API.PaperStrategyVO) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    const res = await getPaperStrategyById(record.id!)
    if (res.data.code === 0 && res.data.data) {
      detailStrategy.value = res.data.data
      // 解析配置
      try { detailTypes.value = JSON.parse(res.data.data.questionTypeConfig || '[]') }
      catch { detailTypes.value = [] }
      try { detailDiffs.value = JSON.parse(res.data.data.difficultyConfig || '[]') }
      catch { detailDiffs.value = [] }
    }
  } catch { message.error('加载详情失败') }
  detailLoading.value = false
}

// ========== 新增/编辑弹窗 ==========
const editVisible = ref(false)
const editTitle = ref('')
const editLoading = ref(false)
const editId = ref<number | undefined>()
const editForm = reactive({
  strategyName: '',
  totalScore: 100,
  difficultyAvg: 3,
  duration: 90,
  questionTypeConfig: [
    { type: 1, count: 10, score: 5 },
    { type: 2, count: 5, score: 6 },
    { type: 3, count: 5, score: 8 },
    { type: 4, count: 3, score: 10 },
  ],
  difficultyConfig: [
    { level: 1, ratio: 0.2 },
    { level: 2, ratio: 0.5 },
    { level: 3, ratio: 0.3 },
  ],
  weights: [
    { weightType: 'difficulty', weightValue: 30 },
    { weightType: 'accuracy', weightValue: 15 },
    { weightType: 'discrimination', weightValue: 20 },
    { weightType: 'calcLevel', weightValue: 10 },
    { weightType: 'examFrequency', weightValue: 10 },
    { weightType: 'knowledgeCount', weightValue: 15 },
  ],
})

const weightSum = ref(100)

function recalcWeightSum() {
  weightSum.value = editForm.weights.reduce((s, w) => s + (w.weightValue || 0), 0)
}

function handleAdd() {
  editId.value = undefined
  editTitle.value = '新建策略'
  resetEditForm()
  editVisible.value = true
}

function handleEdit(record: API.PaperStrategyVO) {
  editId.value = record.id
  editTitle.value = '编辑策略: ' + record.strategyName
  editForm.strategyName = record.strategyName || ''
  editForm.totalScore = record.totalScore || 100
  editForm.difficultyAvg = record.difficultyAvg || 3
  editForm.duration = record.duration || 90
  try { editForm.questionTypeConfig = JSON.parse(record.questionTypeConfig || '[]') } catch { resetEditForm() }
  try { editForm.difficultyConfig = JSON.parse(record.difficultyConfig || '[]') } catch {}
  editForm.weights = record.weights?.map(w => ({ weightType: w.weightType || '', weightValue: w.weightValue || 0 })) || []
  recalcWeightSum()
  editVisible.value = true
}

function resetEditForm() {
  editForm.strategyName = ''
  editForm.totalScore = 100
  editForm.difficultyAvg = 3
  editForm.duration = 90
  editForm.questionTypeConfig = [
    { type: 1, count: 10, score: 5 },
    { type: 2, count: 5, score: 6 },
    { type: 3, count: 5, score: 8 },
    { type: 4, count: 3, score: 10 },
  ]
  editForm.difficultyConfig = [
    { level: 1, ratio: 0.2 },
    { level: 2, ratio: 0.5 },
    { level: 3, ratio: 0.3 },
  ]
  editForm.weights = [
    { weightType: 'difficulty', weightValue: 30 },
    { weightType: 'accuracy', weightValue: 15 },
    { weightType: 'discrimination', weightValue: 20 },
    { weightType: 'calcLevel', weightValue: 10 },
    { weightType: 'examFrequency', weightValue: 10 },
    { weightType: 'knowledgeCount', weightValue: 15 },
  ]
  recalcWeightSum()
}

async function handleEditOk() {
  if (!editForm.strategyName) { message.warning('请输入策略名称'); return }
  if (weightSum.value !== 100) { message.warning('6个指标权重之和必须为100%，当前' + weightSum.value + '%'); return }

  editLoading.value = true
  try {
    const body = {
      strategyName: editForm.strategyName,
      totalScore: editForm.totalScore,
      difficultyAvg: editForm.difficultyAvg,
      duration: editForm.duration,
      questionTypeConfig: JSON.stringify(editForm.questionTypeConfig),
      difficultyConfig: JSON.stringify(editForm.difficultyConfig),
      weights: editForm.weights,
    }
    if (editId.value) {
      const res = await updatePaperStrategy({ id: editId.value, ...body })
      if (res.data.code === 0) { message.success('更新成功'); editVisible.value = false; loadList() }
      else message.error(res.data.message || '更新失败')
    } else {
      const res = await addPaperStrategy(body as any)
      if (res.data.code === 0) { message.success('创建成功'); editVisible.value = false; loadList() }
      else message.error(res.data.message || '创建失败')
    }
  } catch { message.error('保存失败') }
  editLoading.value = false
}

// ========== 删除 ==========
function handleDelete(record: API.PaperStrategyVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除策略「${record.strategyName}」吗？`,
    onOk: async () => {
      const res = await deletePaperStrategy({ id: record.id! })
      if (res.data.code === 0) { message.success('已删除'); loadList() }
      else message.error(res.data.message || '删除失败')
    },
  })
}

// ========== 复制 ==========
async function handleCopy(record: API.PaperStrategyVO) {
  const res = await copyPaperStrategy(record.id!)
  if (res.data.code === 0) { message.success('已复制为: ' + res.data.data?.strategyName); loadList() }
  else message.error(res.data.message || '复制失败')
}

// ========== 设为默认 ==========
async function handleSetDefault(record: API.PaperStrategyVO) {
  const res = await setDefaultStrategy(record.id!)
  if (res.data.code === 0) { message.success('已设为默认策略'); loadList() }
  else message.error(res.data.message || '设置失败')
}

onMounted(loadList)
</script>

<template>
  <div class="strategy-manager">
    <a-card title="组卷策略管理">
      <template #extra>
        <a-button type="primary" @click="handleAdd">新建策略</a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="strategyList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'difficultyAvg'">
            <a-tag :color="record.difficultyAvg <= 2 ? 'green' : record.difficultyAvg >= 4 ? 'red' : 'orange'">
              {{ getDiffName(record.difficultyAvg) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'isDefault'">
            <a-tag v-if="record.isDefault === 1" color="gold">默认</a-tag>
            <span v-else style="color:#ccc">-</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">详情</a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleCopy(record)">复制</a-button>
              <a-button v-if="record.isDefault !== 1" type="link" size="small" @click="handleSetDefault(record)">设默认</a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- ========== 详情弹窗 ========== -->
    <a-modal v-model:open="detailVisible" title="策略详情" :footer="null" width="650px">
      <a-spin :spinning="detailLoading">
        <template v-if="detailStrategy">
          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="名称">{{ detailStrategy.strategyName }}</a-descriptions-item>
            <a-descriptions-item label="总分">{{ detailStrategy.totalScore }}</a-descriptions-item>
            <a-descriptions-item label="平均难度">{{ getDiffName(detailStrategy.difficultyAvg) }}</a-descriptions-item>
            <a-descriptions-item label="时长">{{ detailStrategy.duration }}分钟</a-descriptions-item>
            <a-descriptions-item label="默认策略">
              <a-tag v-if="detailStrategy.isDefault === 1" color="gold">是</a-tag>
              <span v-else>否</span>
            </a-descriptions-item>
            <a-descriptions-item label="权重和">{{ detailStrategy.weightSum }}%</a-descriptions-item>
          </a-descriptions>

          <a-divider>题型配置</a-divider>
          <a-table
            v-if="detailTypes.length"
            :columns="[
              { title: '题型', key: 'type', width: 100 },
              { title: '数量', dataIndex: 'count', width: 80 },
              { title: '单题分值', dataIndex: 'score', width: 100 },
              { title: '小计', key: 'subtotal', width: 80 },
            ]"
            :data-source="detailTypes"
            :pagination="false"
            size="small"
            row-key="type"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'">{{ getTypeName(record.type) }}</template>
              <template v-if="column.key === 'subtotal'">{{ (record.count || 0) * (record.score || 0) }}</template>
            </template>
          </a-table>
          <a-empty v-else description="未配置" />

          <a-divider>难度分布</a-divider>
          <div v-if="detailDiffs.length" style="display:flex;gap:8px">
            <a-tag v-for="d in detailDiffs" :key="d.level"
              :color="d.level === 1 ? 'green' : d.level === 2 ? 'blue' : d.level === 3 ? 'orange' : 'red'"
            >
              {{ getDiffName(d.level) }}: {{ Math.round((d.ratio || 0) * 100) }}%
            </a-tag>
          </div>
          <a-empty v-else description="未配置" />

          <a-divider>权重分配</a-divider>
          <div v-if="detailStrategy.weights?.length" style="display:flex;gap:8px;flex-wrap:wrap">
            <a-tag v-for="w in detailStrategy.weights" :key="w.weightType" color="purple">
              {{ getWeightLabel(w.weightType || '') }}: {{ w.weightValue }}%
            </a-tag>
          </div>
          <a-empty v-else description="未配置" />
        </template>
      </a-spin>
    </a-modal>

    <!-- ========== 新增/编辑弹窗 ========== -->
    <a-modal v-model:open="editVisible" :title="editTitle" @ok="handleEditOk" :confirm-loading="editLoading" width="700px">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="策略名称" required>
          <a-input v-model:value="editForm.strategyName" placeholder="如: 高一数学期中策略" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="总分">
              <a-input-number v-model:value="editForm.totalScore" :min="1" :max="500" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="难度">
              <a-select v-model:value="editForm.difficultyAvg">
                <a-select-option :value="1">简单</a-select-option>
                <a-select-option :value="2">中等</a-select-option>
                <a-select-option :value="3">困难</a-select-option>
                <a-select-option :value="4">较难</a-select-option>
                <a-select-option :value="5">极难</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="时长(分)">
              <a-input-number v-model:value="editForm.duration" :min="10" :max="300" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>题型配置</a-divider>
        <a-row :gutter="8" v-for="(tc, i) in editForm.questionTypeConfig" :key="tc.type" style="margin-bottom:8px">
          <a-col :span="6">
            <a-select v-model:value="tc.type" size="small" disabled>
              <a-select-option :value="1">单选题</a-select-option>
              <a-select-option :value="2">多选题</a-select-option>
              <a-select-option :value="3">填空题</a-select-option>
              <a-select-option :value="4">简答题</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="6">
            <a-input-number v-model:value="tc.count" :min="0" :max="50" size="small" placeholder="数量" style="width:100%" />
          </a-col>
          <a-col :span="6">
            <a-input-number v-model:value="tc.score" :min="1" :max="50" size="small" placeholder="每题分" style="width:100%" />
          </a-col>
          <a-col :span="6">
            <span style="color:#999;font-size:12px">= {{ (tc.count || 0) * (tc.score || 0) }}分</span>
          </a-col>
        </a-row>

        <a-divider>难度占比</a-divider>
        <a-row :gutter="12" v-for="(dc, i) in editForm.difficultyConfig" :key="dc.level" style="margin-bottom:8px">
          <a-col :span="6">
            <a-select v-model:value="dc.level" size="small" disabled>
              <a-select-option :value="1">简单</a-select-option>
              <a-select-option :value="2">中等</a-select-option>
              <a-select-option :value="3">困难</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="12">
            <a-slider v-model:value="dc.ratio" :min="0" :max="1" :step="0.05" />
          </a-col>
          <a-col :span="6">
            <span>{{ Math.round(dc.ratio * 100) }}%</span>
          </a-col>
        </a-row>

        <a-divider>维度权重（总和: {{ weightSum }}%）</a-divider>
        <a-row :gutter="12" v-for="w in editForm.weights" :key="w.weightType" style="margin-bottom:8px">
          <a-col :span="8">
            <span style="line-height:32px">{{ getWeightLabel(w.weightType || '') }}</span>
          </a-col>
          <a-col :span="12">
            <a-slider v-model:value="w.weightValue" :min="0" :max="100" @change="recalcWeightSum" />
          </a-col>
          <a-col :span="4">
            <span style="line-height:32px">{{ w.weightValue }}%</span>
          </a-col>
        </a-row>
        <a-alert v-if="weightSum !== 100" :message="'权重和=' + weightSum + '%，必须等于100%'" type="error" show-icon />
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.strategy-manager {
  padding: 0;
}
</style>

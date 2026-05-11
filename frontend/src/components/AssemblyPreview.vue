<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { saveAssembleResult, addPaperStrategy } from '@/api/shijuanguanli'
import { addQuestionToPaper, removeQuestionFromPaper, updatePaperQuestion } from '@/api/shijuanshitiguanlianguanli'
import { listQuestionByPage } from '@/api/shitiguanli'
import { useLoginUserStore } from '@/stores/loginUser'

const props = defineProps<{
  result: API.AssemblyResultVO
  paperName: string
  subject: string
  paperStatus: number
  strategyId?: number
  saveAsTemplate: boolean
}>()

const emit = defineEmits(['back', 'done'])
const loginUserStore = useLoginUserStore()

// ========== 题目列表 ==========
interface QuestionItem {
  questionId: number
  type: number
  content: string
  difficulty: number
  score: number
  compositeScore: number
}
const questions = ref<QuestionItem[]>([])
const totalScore = ref(0)

// 初始化
if (props.result.questions) {
  questions.value = props.result.questions.map(q => ({
    questionId: q.questionId!,
    type: q.type || 1,
    content: q.content || '',
    difficulty: q.difficulty || 3,
    score: q.score || 10,
    compositeScore: q.compositeScore || 0,
  }))
}
totalScore.value = questions.value.reduce((s, q) => s + q.score, 0)

function recalcTotal() {
  totalScore.value = questions.value.reduce((s, q) => s + q.score, 0)
}

// ========== 展开详情 ==========
const expandedKeys = ref<number[]>([])

function getTypeName(t: number) {
  return { 1: '单选', 2: '多选', 3: '填空', 4: '简答' }[t] || '未知'
}
function getDifficultyName(d: number) {
  return { 1: '简单', 2: '中等', 3: '困难' }[d] || '未知'
}

// ========== 自动校验 ==========
const validation = computed(() => {
  const msgs: string[] = []
  const typeCount: Record<number, number> = {}
  const diffCount: Record<number, number> = {}
  for (const q of questions.value) {
    typeCount[q.type] = (typeCount[q.type] || 0) + 1
    diffCount[q.difficulty] = (diffCount[q.difficulty] || 0) + 1
  }
  // 题型分布检查
  if (Object.keys(typeCount).length < 2) {
    msgs.push('题型过于单一，建议至少包含2种题型')
  }
  // 难度分布检查
  const n = questions.value.length
  if (n > 0) {
    const easyPct = (diffCount[1] || 0) / n * 100
    const hardPct = (diffCount[3] || 0) / n * 100
    if (easyPct > 60) msgs.push(`简单题占比 ${easyPct.toFixed(0)}%，过高`)
    if (hardPct > 60) msgs.push(`困难题占比 ${hardPct.toFixed(0)}%，过高`)
  }
  return msgs
})

// ========== 移除题目 ==========
function handleRemove(index: number) {
  questions.value.splice(index, 1)
  recalcTotal()
}

// ========== 调整分值 ==========
function handleScoreChange() {
  recalcTotal()
}

// ========== 添加题目弹窗 ==========
const addVisible = ref(false)
const availableQuestions = ref<any[]>([])
const addLoading = ref(false)
const selectedAddKeys = ref<number[]>([])
const addPagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function loadAvailable() {
  addLoading.value = true
  try {
    const res = await listQuestionByPage({
      pageNum: addPagination.current,
      pageSize: addPagination.pageSize,
      subject: props.subject,
    })
    if (res.data.code === 0 && res.data.data) {
      availableQuestions.value = res.data.data.records || []
      addPagination.total = res.data.data.totalRow || 0
    }
  } catch { /* ignore */ } finally {
    addLoading.value = false
  }
}

function handleOpenAdd() {
  addVisible.value = true
  selectedAddKeys.value = []
  loadAvailable()
}

async function handleAddConfirm() {
  const existingIds = new Set(questions.value.map(q => q.questionId))
  const toAdd = availableQuestions.value.filter(q => selectedAddKeys.value.includes(q.id) && !existingIds.has(q.id))
  for (const q of toAdd) {
    questions.value.push({
      questionId: q.id!,
      type: q.type || 1,
      content: q.content || '',
      difficulty: q.difficulty || 3,
      score: 10,
      compositeScore: 0,
    })
  }
  recalcTotal()
  addVisible.value = false
  message.success(`已添加 ${toAdd.length} 道题目`)
}

// ========== 保存试卷 ==========
const saving = ref(false)

async function handleSavePaper() {
  if (questions.value.length === 0) { message.warning('试卷至少需要一道题'); return }
  saving.value = true
  try {
    const res = await saveAssembleResult({
      paperName: props.paperName,
      subject: props.subject,
      status: props.paperStatus,
      strategyId: props.strategyId,
      questions: questions.value.map((q, i) => ({
        questionId: q.questionId,
        questionScore: q.score,
        sort: i + 1,
      })),
    })
    if (res.data.code === 0) {
      message.success('试卷保存成功！')
      emit('done')
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch { message.error('保存请求失败') } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="assembly-preview">
    <a-card>
      <template #title>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>组卷结果预览 — {{ paperName }}</span>
          <a-space>
            <a-button @click="handleOpenAdd" type="dashed">添加题目</a-button>
            <a-button @click="emit('back')">返回修改</a-button>
          </a-space>
        </div>
      </template>

      <!-- 摘要 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="6">
          <a-statistic title="题目总数" :value="questions.length" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="试卷总分" :value="totalScore" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="算法类型" :value="result.algorithmType || '未知'" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="适应度" :value="result.fitness != null ? (result.fitness * 100).toFixed(1) + '%' : 'N/A'" />
        </a-col>
      </a-row>

      <!-- 校验提示 -->
      <a-alert
        v-for="(msg, i) in validation"
        :key="i"
        :message="msg"
        type="warning"
        show-icon
        style="margin-bottom:8px"
      />

      <!-- 降级提示 -->
      <a-alert
        v-if="result.degradeHints && result.degradeHints.length > 0"
        type="info"
        show-icon
        style="margin-bottom:12px"
      >
        <template #message>
          <div v-for="(h, i) in result.degradeHints" :key="i">
            {{ h.degradedIndicator }}: {{ h.originalConstraint }} → {{ h.degradedConstraint }}
            <span style="color:#999">({{ h.reason }})</span>
          </div>
        </template>
      </a-alert>

      <!-- 题目表格 -->
      <a-table
        :columns="[
          { title: '#', key: 'idx', width: 50 },
          { title: '题型', dataIndex: 'type', key: 'type', width: 80 },
          { title: '题目内容', dataIndex: 'content', key: 'content', ellipsis: true },
          { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 80 },
          { title: '分值', key: 'score', width: 100 },
          { title: '操作', key: 'action', width: 80 },
        ]"
        :data-source="questions"
        :pagination="false"
        size="small"
        row-key="questionId"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'idx'">{{ index + 1 }}</template>
          <template v-if="column.key === 'type'">
            <a-tag :color="['','blue','green','orange','purple'][record.type]">
              {{ getTypeName(record.type) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'difficulty'">
            <a-tag :color="['','green','blue','red'][record.difficulty]">
              {{ getDifficultyName(record.difficulty) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'score'">
            <a-input-number
              v-model:value="record.score"
              :min="1" :max="50"
              size="small"
              style="width:80px"
              @change="handleScoreChange"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" danger @click="handleRemove(index)">移除</a-button>
          </template>
        </template>
      </a-table>

      <!-- 保存按钮 -->
      <div style="margin-top:24px;text-align:center">
        <a-space size="large">
          <a-button type="primary" size="large" @click="handleSavePaper" :loading="saving">
            保存为试卷
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 添加题目弹窗 -->
    <a-modal
      v-model:open="addVisible"
      title="从题库添加题目"
      width="70%"
      @ok="handleAddConfirm"
      @cancel="addVisible = false"
    >
      <a-table
        :columns="[
          { title: 'ID', dataIndex: 'id', width: 60 },
          { title: '题型', dataIndex: 'type', key: 'type', width: 80 },
          { title: '科目', dataIndex: 'subject', width: 80 },
          { title: '题目内容', dataIndex: 'content', ellipsis: true },
          { title: '难度', dataIndex: 'difficulty', width: 60 },
        ]"
        :data-source="availableQuestions"
        :loading="addLoading"
        :pagination="addPagination"
        :row-selection="{ selectedRowKeys: selectedAddKeys, onChange: (keys: number[]) => selectedAddKeys = keys }"
        row-key="id"
        size="small"
        @change="(pag: any) => { addPagination.current = pag.current; loadAvailable() }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag size="small" :color="['','blue','green','orange','purple'][record.type]">
              {{ getTypeName(record.type) }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<style scoped>
.assembly-preview {
  padding: 0;
}
</style>

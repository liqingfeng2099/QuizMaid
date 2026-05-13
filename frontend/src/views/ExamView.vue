<template>
  <div class="exam-page">
    <!-- 考试中 -->
    <template v-if="examState === 'taking'">
      <div class="exam-header">
        <h2>{{ exam?.paperName || '在线考试' }}</h2>
        <div class="exam-info">
          <a-tag color="orange">{{ exam?.totalQuestions }} 题</a-tag>
          <a-tag color="blue">总分 {{ exam?.totalScore }} 分</a-tag>
          <a-tag v-if="exam?.durationText" color="purple">{{ exam.durationText }}</a-tag>
          <span class="timer" :class="{ 'timer-danger': remaining <= 300 }">
            <ClockCircleOutlined />
            {{ formatTime(remaining) }}
          </span>
        </div>
      </div>

      <a-row :gutter="16" style="flex: 1; overflow: hidden;">
        <!-- 题目导航侧边栏 -->
        <a-col :span="4">
          <div class="question-nav">
            <div class="nav-title">答题卡</div>
            <div class="nav-grid">
              <div
                v-for="(q, idx) in questions"
                :key="q.questionId"
                class="nav-item"
                :class="{
                  'nav-current': currentIndex === idx,
                  'nav-answered': answers[q.questionId!],
                }"
                @click="currentIndex = idx"
              >
                {{ idx + 1 }}
              </div>
            </div>
            <a-divider style="margin: 8px 0;" />
            <div style="font-size: 12px; color: #999;">
              已答 {{ answeredCount }} / {{ questions.length }}
            </div>
          </div>
        </a-col>

        <!-- 题目展示区 -->
        <a-col :span="20">
          <div class="question-area" v-if="currentQuestion">
            <div class="question-header">
              <span class="q-num">第 {{ currentIndex + 1 }} 题</span>
              <a-tag :color="getTypeColor(currentQuestion.type)">
                {{ getTypeName(currentQuestion.type) }}
              </a-tag>
              <span style="color: #999;">（{{ currentQuestion.score }} 分）</span>
            </div>

            <div class="q-content" v-html="renderContent(currentQuestion.content || '')"></div>

            <!-- 单选题 -->
            <a-radio-group
              v-if="currentQuestion.type === 1"
              v-model:value="answers[currentQuestion.questionId!]"
              class="options-group"
            >
              <a-radio
                v-for="opt in parseOptions(currentQuestion.options)"
                :key="opt.key"
                :value="opt.key"
                class="option-item"
              >
                {{ opt.key }}. {{ opt.text }}
              </a-radio>
            </a-radio-group>

            <!-- 多选题 -->
            <a-checkbox-group
              v-if="currentQuestion.type === 2"
              v-model:value="multiAnswers"
              class="options-group"
              @change="onMultiChange"
            >
              <a-checkbox
                v-for="opt in parseOptions(currentQuestion.options)"
                :key="opt.key"
                :value="opt.key"
                class="option-item"
              >
                {{ opt.key }}. {{ opt.text }}
              </a-checkbox>
            </a-checkbox-group>

            <!-- 填空题 -->
            <a-input
              v-if="currentQuestion.type === 3"
              v-model:value="answers[currentQuestion.questionId!]"
              placeholder="请输入答案"
              style="max-width: 400px;"
            />

            <!-- 简答题 -->
            <a-textarea
              v-if="currentQuestion.type === 4"
              v-model:value="answers[currentQuestion.questionId!]"
              placeholder="请输入答案..."
              :rows="6"
              style="max-width: 100%;"
            />

            <div class="nav-buttons">
              <a-button @click="currentIndex--" :disabled="currentIndex === 0">上一题</a-button>
              <a-button type="primary" @click="currentIndex++" :disabled="currentIndex >= questions.length - 1">下一题</a-button>
              <a-button
                type="primary"
                danger
                @click="showSubmitConfirm"
                style="margin-left: 24px;"
              >
                交卷
              </a-button>
            </div>
          </div>
          <a-empty v-else description="加载题目中..." />
        </a-col>
      </a-row>
    </template>

    <!-- 试卷选择列表 -->
    <template v-else-if="examState === 'select'">
      <a-card title="考试中心">
        <a-tabs v-model:activeKey="listTab">
          <a-tab-pane key="available" tab="可考试卷">
            <a-table :columns="paperColumns" :data-source="availablePapers" :loading="loading"
              row-key="id" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                  <a-button type="primary" size="small" @click="handleStartExam(record)">开始考试</a-button>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="history" tab="考试记录">
            <a-table :columns="recordColumns" :data-source="examRecords" :loading="loading"
              row-key="recordId" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag v-if="record.status === 0" color="orange">进行中</a-tag>
                  <a-tag v-else-if="record.status === 1" color="blue">已完成</a-tag>
                  <a-tag v-else-if="record.status === 2" color="green">已批改</a-tag>
                </template>
                <template v-if="column.key === 'action'">
                  <a-button v-if="record.status === 0" type="primary" size="small"
                    @click="handleResumeExam(record)">继续考试</a-button>
                  <a-button v-else type="link" size="small"
                    @click="handleViewResult(record)">查看成绩</a-button>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>

    <!-- 考试结果 -->
    <template v-else-if="examState === 'result'">
      <div class="result-page">
        <a-card>
          <a-result
            :status="(result?.userScore || 0) >= (result?.totalScore || 1) * 0.6 ? 'success' : 'info'"
            :title="'考试成绩：' + (result?.userScore || 0) + ' / ' + (result?.totalScore || 0) + ' 分'"
            :sub-title="'正确 ' + (result?.correctCount || 0) + ' 题，错误 ' + (result?.wrongCount || 0) + ' 题' + (result?.pendingCount ? '，待批改 ' + result.pendingCount + ' 题' : '')"
          >
            <template #extra>
              <a-space>
                <a-button @click="examState = 'select'; result = null">返回列表</a-button>
                <a-button type="primary" @click="handleViewResultDetail">查看详情</a-button>
              </a-space>
            </template>
          </a-result>

          <a-divider>答题详情</a-divider>
          <div v-for="(q, idx) in (result?.questions || [])" :key="q.questionId" class="result-item"
            :class="{ 'result-correct': q.correctStatus === 1, 'result-wrong': q.correctStatus === 2 }">
            <div class="result-q-header">
              <span class="q-num">第 {{ idx + 1 }} 题</span>
              <a-tag :color="getTypeColor(q.type)">{{ getTypeName(q.type) }}</a-tag>
              <span>{{ q.score }} 分</span>
              <a-tag v-if="q.correctStatus === 1" color="success">✓ 正确</a-tag>
              <a-tag v-else-if="q.correctStatus === 2" color="error">✗ 错误</a-tag>
              <a-tag v-else color="default">待批改</a-tag>
              <span v-if="q.correctStatus !== 0">得分：{{ q.actualScore || 0 }}</span>
            </div>
            <div class="result-q-content" v-html="renderContent(q.content || '')"></div>
            <div v-if="q.userAnswer" class="result-answer">
              <span class="label">你的答案：</span>{{ q.userAnswer }}
              <span v-if="q.correctStatus === 2" class="wrong-mark">✗</span>
            </div>
            <div v-if="q.correctStatus === 2 && q.correctAnswer" class="result-answer correct">
              <span class="label">正确答案：</span>{{ q.correctAnswer }}
            </div>
            <div v-if="q.analysis" class="result-analysis">
              <span class="label">解析：</span>{{ q.analysis }}
            </div>
          </div>
        </a-card>
      </div>
    </template>

    <!-- 交卷确认弹窗 -->
    <a-modal v-model:open="submitVisible" title="确认交卷"
      @ok="handleSubmit" :ok-text="'确认交卷'" :cancel-text="'继续检查'">
      <p>确定要提交试卷吗？</p>
      <p style="color: #ff4d4f;">
        已答 {{ answeredCount }} / {{ questions.length }} 题，
        未答 {{ questions.length - answeredCount }} 题将被视为放弃。
      </p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined } from '@ant-design/icons-vue'
import { startExam, submitExam, autoSubmitExam, getExamResult, getOngoingExam, getExamRecords } from '@/api/kaoshi'
import { listExamPaperByPage } from '@/api/shijuanguanli'

// ========== State ==========
const examState = ref<'select' | 'taking' | 'result'>('select')
const exam = ref<API.ExamRecordVO | null>(null)
const result = ref<API.ExamResultVO | null>(null)
const questions = ref<API.ExamQuestionItem[]>([])
const answers = ref<Record<string, string>>({})
const multiAnswers = ref<string[]>([])
const currentIndex = ref(0)
const remaining = ref(0)
const submitVisible = ref(false)
const loading = ref(false)

const listTab = ref('available')
const availablePapers = ref<any[]>([])
const examRecords = ref<API.ExamRecordVO[]>([])

let timerInterval: any = null

// ========== Computed ==========
const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const answeredCount = computed(() => Object.values(answers.value).filter(v => v && v.trim()).length)

// ========== Methods ==========
const formatTime = (secs: number) => {
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const getTypeName = (t?: number) => {
  const map: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题' }
  return map[t || 1] || '未知'
}
const getTypeColor = (t?: number) => {
  const map: Record<number, string> = { 1: 'blue', 2: 'purple', 3: 'orange', 4: 'green' }
  return map[t || 1] || 'default'
}

const renderContent = (c: string) => c?.replace(/\n/g, '<br/>') || ''

const parseOptions = (opts?: string): { key: string; text: string }[] => {
  if (!opts) return []
  try {
    const arr = JSON.parse(opts)
    return arr.map((o: any) => ({
      key: String(o.key || o.label || o),
      text: String(o.value || o.text || o)
    }))
  } catch {
    // Fallback: parse "A. xxx" format
    const lines = opts.split(/[\n;]/)
    return lines.map(l => {
      const m = l.trim().match(/^([A-D])[.\s、]*(.+)/)
      return m ? { key: m[1], text: m[2] } : { key: '', text: l }
    }).filter(o => o.key)
  }
}

const onMultiChange = (vals: string[]) => {
  if (currentQuestion.value) {
    answers.value[currentQuestion.value.questionId!] = vals.sort().join('')
  }
}

// Watch multiAnswers to sync with answers
watch(multiAnswers, (vals) => {
  if (currentQuestion.value) {
    answers.value[currentQuestion.value.questionId!] = vals.sort().join('')
  }
})

// ========== Timer ==========
const hiddenTimestamp = ref<number>(0)

const startTimer = () => {
  timerInterval = setInterval(() => {
    if (remaining.value <= 0) {
      clearInterval(timerInterval)
      handleAutoSubmit()
      return
    }
    remaining.value--
    // 每10秒自动保存到localStorage
    if (remaining.value % 10 === 0) {
      localStorage.setItem(`exam_answers_${exam.value?.recordId}`, JSON.stringify(answers.value))
    }
  }, 1000)
}

// B2: 页面切后台/关闭处理
const handleVisibilityChange = () => {
  if (examState.value !== 'taking') return
  if (document.hidden) {
    // 切后台：记录时间戳
    hiddenTimestamp.value = Date.now()
    localStorage.setItem(`exam_answers_${exam.value?.recordId}`, JSON.stringify(answers.value))
  } else {
    // 回前台：校准倒计时
    if (hiddenTimestamp.value > 0) {
      const elapsed = Math.floor((Date.now() - hiddenTimestamp.value) / 1000)
      remaining.value = Math.max(0, remaining.value - elapsed)
      hiddenTimestamp.value = 0
    }
  }
}

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (examState.value === 'taking') {
    e.preventDefault()
    e.returnValue = '考试进行中，确定离开吗？'
    return e.returnValue
  }
}

document.addEventListener('visibilitychange', handleVisibilityChange)
window.addEventListener('beforeunload', handleBeforeUnload)

const handleAutoSubmit = async () => {
  message.warning('考试时间到，系统将自动交卷')
  if (!exam.value?.recordId) return
  try {
    const res = await autoSubmitExam(exam.value.recordId)
    if (res.data.code === 0) {
      result.value = res.data.data || null
      examState.value = 'result'
      clearInterval(timerInterval)
      localStorage.removeItem(`exam_answers_${exam.value?.recordId}`)
    }
  } catch (e) {
    message.error('自动交卷失败')
  }
}

// ========== Actions ==========
const loadPapers = async () => {
  loading.value = true
  try {
    const res = await listExamPaperByPage({ pageNum: 1, pageSize: 100, status: 1 } as any)
    if (res.data.code === 0) {
      availablePapers.value = res.data.data?.records || []
    }
    const res2 = await getExamRecords()
    if (res2.data.code === 0) {
      examRecords.value = res2.data.data || []
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

const handleStartExam = async (paper: any) => {
  try {
    // Check for ongoing exam first
    const ongoing = await getOngoingExam(paper.id)
    if (ongoing.data.code === 0 && ongoing.data.data) {
      startExamSession(ongoing.data.data)
      return
    }

    const res = await startExam(paper.id)
    if (res.data.code === 0 && res.data.data) {
      startExamSession(res.data.data)
    } else {
      message.error(res.data.message || '开始考试失败')
    }
  } catch (e) {
    message.error('开始考试失败')
  }
}

const handleResumeExam = (record: any) => {
  // Resume ongoing exam
  getOngoingExam(record.paperId).then(res => {
    if (res.data.code === 0 && res.data.data) {
      startExamSession(res.data.data)
    }
  })
}

const startExamSession = (data: API.ExamRecordVO) => {
  exam.value = data
  questions.value = data.questions || []
  remaining.value = data.remainingSeconds || 0

  // Restore saved answers
  const saved = localStorage.getItem(`exam_answers_${data.recordId}`)
  if (saved) {
    try { answers.value = JSON.parse(saved) } catch { answers.value = {} }
  }
  // Restore from existing questions
  (data.questions || []).forEach(q => {
    if (q.userAnswer && !answers.value[q.questionId!]) {
      answers.value[q.questionId!] = q.userAnswer
    }
  })
  // Init multiAnswers for current multi-select question
  if (currentQuestion.value?.type === 2) {
    multiAnswers.value = (answers.value[currentQuestion.value.questionId!] || '').split('')
  }

  examState.value = 'taking'
  startTimer()
}

const showSubmitConfirm = () => {
  submitVisible.value = true
}

const handleSubmit = async () => {
  if (!exam.value?.recordId) return
  try {
    const res = await submitExam({ recordId: exam.value.recordId, answers: answers.value })
    if (res.data.code === 0) {
      result.value = res.data.data || null
      examState.value = 'result'
      clearInterval(timerInterval)
      localStorage.removeItem(`exam_answers_${exam.value?.recordId}`)
      message.success('交卷成功')
    } else {
      message.error(res.data.message || '交卷失败')
    }
  } catch (e) {
    message.error('交卷失败')
  }
  submitVisible.value = false
}

const handleViewResult = async (record: any) => {
  try {
    const res = await getExamResult(record.recordId)
    if (res.data.code === 0) {
      result.value = res.data.data || null
      examState.value = 'result'
    }
  } catch (e) {
    message.error('加载成绩失败')
  }
}

const handleViewResultDetail = () => {
  // Already showing details below the result card
}

// ========== Table columns ==========
const paperColumns = [
  { title: '试卷名称', dataIndex: 'paperName', key: 'name' },
  { title: '科目', dataIndex: 'subject', key: 'subject', width: 100 },
  { title: '总分', dataIndex: 'totalScore', key: 'score', width: 80 },
  { title: '操作', key: 'action', width: 120 }
]

const recordColumns = [
  { title: '试卷名称', dataIndex: 'paperName', key: 'name' },
  { title: '总分', dataIndex: 'totalScore', key: 'score', width: 80 },
  { title: '得分', dataIndex: 'userScore', key: 'userScore', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '时间', dataIndex: 'startTime', key: 'time', width: 180 },
  { title: '操作', key: 'action', width: 120 }
]

// Watch currentIndex to sync multiAnswers
watch(currentIndex, () => {
  if (currentQuestion.value?.type === 2) {
    multiAnswers.value = (answers.value[currentQuestion.value.questionId!] || '').split('').filter(Boolean)
  }
})

onMounted(() => {
  loadPapers()
  // 如果从试卷管理页传入paperId，直接开始考试
  const qPaperId = new URLSearchParams(window.location.search).get('paperId')
  if (qPaperId) {
    handleStartExam({ id: Number(qPaperId) })
  }
})

onUnmounted(() => {
  clearInterval(timerInterval)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.exam-page { display: flex; flex-direction: column; height: calc(100vh - 160px); }
.exam-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0f0f0; margin-bottom: 12px; }
.exam-header h2 { margin: 0; font-size: 18px; }
.exam-info { display: flex; align-items: center; gap: 12px; }
.timer { font-size: 20px; font-weight: bold; color: #1890ff; display: flex; align-items: center; gap: 4px; }
.timer-danger { color: #ff4d4f; animation: pulse 1s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

.question-nav { background: #fafafa; border-radius: 8px; padding: 12px; height: 100%; overflow-y: auto; }
.nav-title { font-weight: 600; margin-bottom: 8px; font-size: 14px; color: #333; }
.nav-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
.nav-item { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 50%; border: 1px solid #d9d9d9; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.nav-item:hover { border-color: #1890ff; }
.nav-current { background: #1890ff; color: white; border-color: #1890ff; }
.nav-answered { background: #e6f7ff; border-color: #91d5ff; }

.question-area { padding: 16px; background: white; border-radius: 8px; }
.question-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.q-num { font-size: 16px; font-weight: 600; color: #333; }
.q-content { font-size: 15px; line-height: 1.8; margin-bottom: 20px; padding: 12px; background: #fafafa; border-radius: 4px; }

.options-group { display: flex; flex-direction: column; gap: 8px; }
.option-item { padding: 8px 12px; border: 1px solid #f0f0f0; border-radius: 4px; margin: 0 !important; transition: background 0.2s; }
.option-item:hover { background: #f5f5f5; }

.nav-buttons { display: flex; gap: 12px; margin-top: 24px; justify-content: center; }

.result-item { padding: 12px; margin-bottom: 8px; border-radius: 8px; border: 1px solid #f0f0f0; }
.result-correct { border-left: 4px solid #52c41a; }
.result-wrong { border-left: 4px solid #ff4d4f; }
.result-q-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.result-q-content { font-size: 14px; padding: 8px; background: #fafafa; border-radius: 4px; margin-bottom: 8px; }
.result-answer { font-size: 14px; padding: 4px 0; }
.result-answer .label { color: #666; font-weight: 500; }
.result-answer .wrong-mark { color: #ff4d4f; font-weight: bold; margin-left: 8px; }
.result-answer.correct { color: #52c41a; }
.result-analysis { font-size: 13px; padding: 8px; background: #fffbe6; border-radius: 4px; margin-top: 4px; }
.result-analysis .label { color: #faad14; font-weight: 500; }
.result-page { max-width: 900px; margin: 0 auto; }
</style>

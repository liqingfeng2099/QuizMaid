<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { matchQuestionCount, greedyAssemble, geneticAssemble, addPaperStrategy, listPaperStrategyByPage } from '@/api/shijuanguanli'
import { useLoginUserStore } from '@/stores/loginUser'
import AssemblyPreview from './AssemblyPreview.vue'

const loginUserStore = useLoginUserStore()
const emit = defineEmits(['done'])

// ========== 试卷基础信息 ==========
const paperName = ref('')
const subject = ref<string>()
const paperStatus = ref(0)
const totalScore = ref(100)
const duration = ref(90)
const difficultyAvg = ref(3)

const subjectOptions = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治']

// ========== 策略选择 ==========
const strategyList = ref<API.PaperStrategyVO[]>([])
const selectedStrategyId = ref<number>()
const saveAsTemplate = ref(false)

async function loadStrategies() {
  try {
    const res = await listPaperStrategyByPage({ pageNum: 1, pageSize: 50 })
    if (res.data.code === 0 && res.data.data?.records) {
      strategyList.value = res.data.data.records
    }
  } catch { /* ignore */ }
}

// ========== 题型配置 ==========
interface TypeConfig {
  type: number; label: string; count: number; scorePerQ: number
}
const typeConfigs = reactive<TypeConfig[]>([
  { type: 1, label: '单选题', count: 10, scorePerQ: 5 },
  { type: 2, label: '多选题', count: 5, scorePerQ: 6 },
  { type: 3, label: '填空题', count: 5, scorePerQ: 8 },
  { type: 4, label: '简答题', count: 3, scorePerQ: 10 },
])

const computedTotalScore = computed(() =>
  typeConfigs.reduce((sum, t) => sum + t.count * t.scorePerQ, 0)
)

// ========== 难度占比 ==========
const easyRatio = ref(20)
const midRatio = ref(50)
const hardRatio = ref(30)
const ratioSum = computed(() => easyRatio.value + midRatio.value + hardRatio.value)
const ratioValid = computed(() => ratioSum.value === 100)

// ========== 知识点筛选 ==========
const knowledgeInput = ref('')

// ========== 实时匹配计数 ==========
const matchCount = ref<API.MatchCountVO>({})
const matchLoading = ref(false)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

async function fetchMatchCount() {
  matchLoading.value = true
  try {
    const types = typeConfigs.filter(t => t.count > 0).map(t => t.type)
    const res = await matchQuestionCount({
      subject: subject.value,
      difficulty: undefined,
      types,
      knowledgePoints: knowledgeInput.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      matchCount.value = res.data.data
    }
  } catch { /* ignore */ } finally {
    matchLoading.value = false
  }
}

function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchMatchCount, 300)
}

watch([subject, knowledgeInput], debouncedFetch)
watch(typeConfigs, debouncedFetch, { deep: true })

// ========== 组卷 ==========
const assembling = ref(false)
const assemblyResult = ref<API.AssemblyResultVO | null>(null)
const assemblyError = ref('')

async function handleGreedyAssemble() {
  if (!paperName.value) { message.warning('请输入试卷名称'); return }
  if (!ratioValid.value) { message.warning('难度占比之和必须为100%'); return }

  // 如果没有选策略，自动创建一个临时策略
  let sid = selectedStrategyId.value
  if (!sid) {
    try {
      const diffConfig = JSON.stringify([
        { level: 1, ratio: easyRatio.value / 100 },
        { level: 2, ratio: midRatio.value / 100 },
        { level: 3, ratio: hardRatio.value / 100 },
      ])
      const typeConfig = JSON.stringify(typeConfigs.filter(t => t.count > 0).map(t => ({
        type: t.type, count: t.count, score: t.scorePerQ,
      })))
      const res = await addPaperStrategy({
        strategyName: paperName.value + '-临时策略',
        totalScore: totalScore.value,
        difficultyAvg: difficultyAvg.value,
        duration: duration.value,
        questionTypeConfig: typeConfig,
        difficultyConfig: diffConfig,
        weights: [
          { weightType: 'difficulty', weightValue: 30 },
          { weightType: 'accuracy', weightValue: 15 },
          { weightType: 'discrimination', weightValue: 20 },
          { weightType: 'calcLevel', weightValue: 10 },
          { weightType: 'examFrequency', weightValue: 10 },
          { weightType: 'knowledgeCount', weightValue: 15 },
        ],
      })
      if (res.data.code === 0 && res.data.data?.id) {
        sid = res.data.data.id
        selectedStrategyId.value = sid
      }
    } catch { /* ignore */ }
  }
  if (!sid) { message.warning('请选择或创建组卷策略'); return }

  assemblyError.value = ''
  assembling.value = true
  try {
    const res = await greedyAssemble({ strategyId: sid, subject: subject.value, paperName: paperName.value })
    if (res.data.code === 0 && res.data.data) {
      assemblyResult.value = res.data.data
    } else {
      assemblyError.value = res.data.message || '组卷失败'
    }
  } catch (e) {
    console.error(e)
    assemblyError.value = '组卷请求失败'
  } finally {
    assembling.value = false
  }
}

async function handleGeneticAssemble() {
  if (!paperName.value) { message.warning('请输入试卷名称'); return }
  const sid = selectedStrategyId.value
  if (!sid) {
    message.warning('遗传组卷需要先选择策略')
    return
  }

  assemblyError.value = ''
  assembling.value = true
  try {
    const res = await geneticAssemble({ strategyId: sid, subject: subject.value, paperName: paperName.value })
    if (res.data.code === 0 && res.data.data) {
      assemblyResult.value = res.data.data
    } else {
      assemblyError.value = res.data.message || '组卷失败'
    }
  } catch (e) {
    console.error(e)
    assemblyError.value = '组卷请求失败'
  } finally {
    assembling.value = false
  }
}

function handleReset() {
  assemblyResult.value = null
  assemblyError.value = ''
}

onMounted(() => {
  loginUserStore.fetchLoginUser()
  loadStrategies()
  fetchMatchCount()
})
</script>

<template>
  <div class="manual-assembly">
    <!-- ======== 表单区 ======== -->
    <a-card v-if="!assemblyResult" title="手动组卷配置" class="config-card">
      <!-- 基础信息 -->
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="试卷名称" required>
            <a-input v-model:value="paperName" placeholder="如：高一数学期中测试" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="所属科目">
            <a-select v-model:value="subject" placeholder="选择科目" allow-clear>
              <a-select-option v-for="s in subjectOptions" :key="s" :value="s">{{ s }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="试卷状态">
            <a-select v-model:value="paperStatus">
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">已发布</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="目标总分">
            <a-input-number v-model:value="totalScore" :min="1" :max="500" style="width:100%" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="答题时长(分钟)">
            <a-input-number v-model:value="duration" :min="10" :max="300" style="width:100%" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="整体难度">
            <a-slider v-model:value="difficultyAvg" :min="1" :max="5" :marks="{1:'易',2:'较易',3:'中',4:'较难',5:'难'}" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="保存为模板">
            <a-switch v-model:checked="saveAsTemplate" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>题型配置</a-divider>
      <a-row :gutter="12">
        <a-col :span="6" v-for="tc in typeConfigs" :key="tc.type">
          <a-card size="small" :title="tc.label">
            <a-form-item label="数量">
              <a-input-number v-model:value="tc.count" :min="0" :max="50" size="small" style="width:100%" />
            </a-form-item>
            <a-form-item label="单题分值">
              <a-input-number v-model:value="tc.scorePerQ" :min="1" :max="50" size="small" style="width:100%" />
            </a-form-item>
            <div style="color:#999;font-size:12px">
              小计: {{ tc.count * tc.scorePerQ }} 分
            </div>
          </a-card>
        </a-col>
      </a-row>
      <div style="margin-top:8px;text-align:right;font-weight:bold">
        题型计算总分：{{ computedTotalScore }}
        <a-tag v-if="computedTotalScore !== totalScore" color="warning">与目标总分不一致</a-tag>
        <a-tag v-else color="success">匹配</a-tag>
      </div>

      <a-divider>难度占比配置</a-divider>
      <a-row :gutter="16">
        <a-col :span="8">
          <span>简单(1级): {{ easyRatio }}%</span>
          <a-slider v-model:value="easyRatio" :min="0" :max="100" />
        </a-col>
        <a-col :span="8">
          <span>中等(2级): {{ midRatio }}%</span>
          <a-slider v-model:value="midRatio" :min="0" :max="100" />
        </a-col>
        <a-col :span="8">
          <span>困难(3级): {{ hardRatio }}%</span>
          <a-slider v-model:value="hardRatio" :min="0" :max="100" />
        </a-col>
      </a-row>
      <div style="text-align:center">
        <a-tag v-if="!ratioValid" color="error">难度占比之和必须为100%（当前{{ ratioSum }}%）</a-tag>
        <a-tag v-else color="success">难度占比已平衡</a-tag>
      </div>

      <a-divider>知识点筛选（可选）</a-divider>
      <a-input v-model:value="knowledgeInput" placeholder="输入知识点关键词，多个用逗号分隔" />

      <a-divider>组卷策略</a-divider>
      <a-select
        v-model:value="selectedStrategyId"
        placeholder="选择已保存的策略（可选，不选则自动生成）"
        allow-clear
        style="width:100%"
      >
        <a-select-option v-for="s in strategyList" :key="s.id" :value="s.id">
          {{ s.strategyName }} (总分:{{ s.totalScore }} 难度:{{ s.difficultyAvg }})
        </a-select-option>
      </a-select>

      <!-- 实时匹配数 -->
      <a-divider />
      <a-space>
        <a-spin v-if="matchLoading" size="small" />
        <span>当前条件匹配题目：
          <a-tag :color="(matchCount.totalCount || 0) < computedTotalScore / 5 ? 'red' : 'green'">
            {{ matchCount.totalCount || 0 }} 道
          </a-tag>
        </span>
        <span v-if="matchCount.byType">
          按题型：
          <template v-for="tc in typeConfigs.filter(t => t.count > 0)" :key="tc.type">
            {{ tc.label }}
            <a-tag :color="(matchCount.byType?.[String(tc.type)] || 0) < tc.count ? 'orange' : 'blue'" size="small">
              {{ matchCount.byType?.[String(tc.type)] || 0 }}
            </a-tag>
          </template>
        </span>
      </a-space>

      <!-- 操作按钮 -->
      <div style="margin-top:24px;text-align:center">
        <a-space size="large">
          <a-button type="primary" size="large" @click="handleGreedyAssemble" :loading="assembling">
            贪心算法组卷 (快速)
          </a-button>
          <a-button size="large" @click="handleGeneticAssemble" :loading="assembling" :disabled="!selectedStrategyId">
            遗传算法组卷 (高精度)
          </a-button>
          <a-button size="large" @click="handleReset">重置</a-button>
        </a-space>
        <div v-if="assemblyError" style="margin-top:12px;color:red">{{ assemblyError }}</div>
      </div>
    </a-card>

    <!-- ======== 预览区 ======== -->
    <AssemblyPreview
      v-if="assemblyResult"
      :result="assemblyResult"
      :paper-name="paperName"
      :subject="subject || '综合'"
      :paper-status="paperStatus"
      :strategy-id="selectedStrategyId"
      :save-as-template="saveAsTemplate"
      @back="handleReset"
      @done="emit('done')"
    />
  </div>
</template>

<style scoped>
.manual-assembly {
  padding: 0;
}
.config-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { aiAssemblePaperV2, confirmAIAssembly, getAIProfile, getAIChatHistory } from '@/api/shijuanguanli'

const router = useRouter()
const subjectOptions = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治']

// ========== 表单 ==========
const formState = reactive({
  paperName: '',
  subject: undefined as string | undefined,
  difficulty: undefined as number | undefined,
  status: 0,
  userRequirement: '',
})
const usePersonalization = ref(true)
const includeWeak = ref(false)
const aiProfile = ref<API.AIProfileVO | null>(null)

// ========== 阶段 ==========
const stage = ref<'input' | 'review' | 'history'>('input')
const loading = ref(false)
const errorMsg = ref('')
const strategy = ref<API.AIAssemblyStrategyVO | null>(null)
const retryCount = ref(0)
const history = ref<API.AIChatVO[]>([])

async function loadProfile() {
  try {
    const res = await getAIProfile()
    if (res.data.code === 0 && res.data.data) aiProfile.value = res.data.data
  } catch { /* ignore */ }
}

async function loadHistory() {
  try {
    const res = await getAIChatHistory({ limit: 20 })
    if (res.data.code === 0 && res.data.data) history.value = res.data.data
  } catch { /* ignore */ }
}

async function handleGenerate() {
  if (!formState.paperName) { message.warning('请输入试卷名称'); return }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await aiAssemblePaperV2({
      paperName: formState.paperName,
      subject: formState.subject,
      difficulty: formState.difficulty,
      status: formState.status,
      userRequirement: formState.userRequirement,
      usePersonalization: usePersonalization.value,
      includeWeakAreas: includeWeak.value,
    })
    if (res.data.code === 0 && res.data.data) {
      strategy.value = res.data.data
      stage.value = 'review'
    } else {
      errorMsg.value = res.data.message || 'AI组卷失败，建议切换手动组卷'
    }
  } catch {
    errorMsg.value = 'AI组卷请求失败，建议切换手动组卷'
  }
  loading.value = false
}

async function handleConfirm() {
  if (!strategy.value?.questionIds?.length) { message.warning('没有题目可供保存'); return }
  loading.value = true
  try {
    const res = await confirmAIAssembly({
      paperName: formState.paperName,
      subject: formState.subject,
      status: formState.status,
      strategy: strategy.value,
    })
    if (res.data.code === 0) {
      message.success('AI组卷成功！试卷已创建')
      router.push('/paper')
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch { message.error('保存请求失败') }
  loading.value = false
}

function handleRegenerate() {
  strategy.value = null
  stage.value = 'input'
  handleGenerate()
}

function getDifficultyText(d?: number) {
  return { 1: '简单', 2: '中等', 3: '困难' }[d || 0] || '未知'
}
function getTypeName(t?: number) {
  return { 1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题' }[t || 0] || '?'
}

onMounted(() => { loadProfile(); loadHistory() })
</script>

<template>
  <div class="ai-assembly">
    <h2>AI 智能组卷</h2>

    <a-tabs v-model:activeKey="stage" @change="(k: string) => { if (k === 'history') loadHistory() }">
      <!-- ====== 输入需求 ====== -->
      <a-tab-pane key="input" tab="组卷需求">
        <a-card size="small">
          <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
            <a-form-item label="试卷名称" required>
              <a-input v-model:value="formState.paperName" placeholder="如：高一数学期中测试" />
            </a-form-item>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="所属科目">
                  <a-select v-model:value="formState.subject" placeholder="选择科目（可选）" allow-clear>
                    <a-select-option v-for="s in subjectOptions" :key="s" :value="s">{{ s }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="难度">
                  <a-select v-model:value="formState.difficulty" placeholder="选择难度（可选）" allow-clear>
                    <a-select-option :value="1">简单</a-select-option>
                    <a-select-option :value="2">中等</a-select-option>
                    <a-select-option :value="3">困难</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="状态">
              <a-select v-model:value="formState.status">
                <a-select-option :value="0">草稿</a-select-option>
                <a-select-option :value="1">已发布</a-select-option>
              </a-select>
            </a-form-item>

            <a-divider>个性化配置</a-divider>
            <a-form-item label="融入学习画像">
              <a-switch v-model:checked="usePersonalization" />
              <span style="margin-left:8px;color:#888;font-size:12px">答题统计和薄弱知识点预填为AI提示词</span>
            </a-form-item>
            <a-form-item v-if="usePersonalization" label="聚焦薄弱点">
              <a-switch v-model:checked="includeWeak" />
              <span style="margin-left:8px;color:#888;font-size:12px">优先选择薄弱知识点相关题目</span>
            </a-form-item>

            <a-alert v-if="usePersonalization && aiProfile" type="info" style="margin-bottom:12px">
              <template #message>
                总答题 {{ aiProfile.answerNum }} 题，正确率 {{ aiProfile.accuracy?.toFixed(1) || 'N/A' }}%
                <span v-if="aiProfile.weakPoints?.length">
                  | 薄弱：<a-tag v-for="wp in aiProfile.weakPoints.slice(0,3)" :key="wp.knowledgePoint" color="orange" size="small">{{ wp.knowledgePoint }}({{ wp.accuracy }}%)</a-tag>
                </span>
              </template>
            </a-alert>

            <a-form-item label="组卷需求">
              <a-textarea v-model:value="formState.userRequirement"
                placeholder="用自然语言描述需求，如：侧重二次函数、难度适中、题型均衡"
                :rows="3" />
            </a-form-item>
          </a-form>
          <div style="text-align:center;margin-top:12px">
            <a-button type="primary" size="large" @click="handleGenerate" :loading="loading">生成组卷方案</a-button>
          </div>
          <div v-if="errorMsg" style="text-align:center;margin-top:8px;color:red">{{ errorMsg }}</div>
        </a-card>
      </a-tab-pane>

      <!-- ====== 确认方案 ====== -->
      <a-tab-pane key="review" tab="确认方案" v-if="strategy">
        <a-card size="small">
          <a-alert v-if="strategy.stageDetail" :message="strategy.stageDetail" type="info" show-icon style="margin-bottom:12px" />

          <a-descriptions bordered size="small" :column="2" style="margin-bottom:12px">
            <a-descriptions-item label="选中题目数">{{ strategy.totalQuestions }} 题</a-descriptions-item>
            <a-descriptions-item label="实际总分">{{ strategy.actualTotalScore }} 分</a-descriptions-item>
            <a-descriptions-item label="目标难度">{{ strategy.difficultyAvg || '未指定' }}/5</a-descriptions-item>
            <a-descriptions-item label="组卷模式"><a-tag color="purple">A+C 混合</a-tag></a-descriptions-item>
          </a-descriptions>

          <!-- 推理策略 -->
          <a-card v-if="strategy.questionTypeConfig?.length" title="AI推断策略" size="small" style="margin-bottom:12px">
            <template #extra><a-tag v-if="strategy.strategyDescription" color="blue">{{ strategy.strategyDescription }}</a-tag></template>
            <div style="margin-bottom:8px">
              <b>题型：</b>
              <a-tag v-for="tc in strategy.questionTypeConfig" :key="tc.type" color="blue" style="margin:2px">{{ getTypeName(tc.type) }} {{ tc.count }}题×{{ tc.score }}分</a-tag>
            </div>
            <div>
              <b>难度分布：</b>
              <a-tag v-for="dc in strategy.difficultyConfig" :key="dc.level" :color="dc.level===1?'green':dc.level===2?'orange':'red'" style="margin:2px">{{ getDifficultyText(dc.level) }} {{ Math.round((dc.ratio||0)*100) }}%</a-tag>
            </div>
          </a-card>

          <a-divider>选中题目</a-divider>
          <a-table
            :columns="[{ title: '#', width: 50, customRender: ({ index }: any) => index + 1 }, { title: '题目ID', dataIndex: 'questionId', width: 80 }]"
            :data-source="strategy.questionIds?.map((id, i) => ({ key: i, questionId: id })) || []"
            :pagination="false" size="small" style="margin-bottom:12px"
          />

          <div style="text-align:center;margin-top:12px">
            <a-space size="large">
              <a-button type="primary" size="large" @click="handleConfirm" :loading="loading">确认组卷</a-button>
              <a-button size="large" @click="handleRegenerate" :loading="loading">重新生成</a-button>
            </a-space>
          </div>
        </a-card>
      </a-tab-pane>

      <!-- ====== 历史记录 ====== -->
      <a-tab-pane key="history" tab="历史记录">
        <a-table
          :columns="[
            { title: '时间', dataIndex: 'createTime', width: 160 },
            { title: '需求', dataIndex: 'chatContent', ellipsis: true },
            { title: '状态', dataIndex: 'status', width: 80 },
          ]"
          :data-source="history"
          :pagination="false" size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '成功' : '失败' }}</a-tag>
            </template>
          </template>
        </a-table>
        <a-empty v-if="!history.length" description="暂无历史记录" style="padding:24px" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.ai-assembly { padding: 0; }
h2 { margin-bottom: 16px; }
</style>

<template>
  <div class="paper-manager">
    <a-card>
      <template #title>
        <div class="card-title">试卷管理</div>
      </template>

      <!-- 筛选查询区 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="filters.paperName"
            placeholder="试卷名称"
            style="width: 200px"
          />
          <a-select
            v-model:value="filters.subject"
            placeholder="所属科目"
            style="width: 150px"
            allow-clear
          >
            <a-select-option v-for="subject in subjectOptions" :key="subject" :value="subject">
              {{ subject }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filters.status"
            placeholder="试卷状态"
            style="width: 120px"
            allow-clear
          >
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">已发布</a-select-option>
            <a-select-option :value="2">已归档</a-select-option>
            <a-select-option :value="3">已停用</a-select-option>
          </a-select>
          <a-button type="primary" @click="handleSearch">搜索</a-button>
          <a-button @click="handleReset">重置</a-button>
        </a-space>
      </div>

      <!-- 操作按钮区 -->
      <div class="table-operations">
        <a-button type="primary" @click="handleAdd">
          新增试卷
        </a-button>
        <a-button type="primary" @click="handleAIPaperAssembly">
          AI智能组卷
        </a-button>
        <a-button
          danger
          @click="handleBatchDelete"
          :disabled="selectedRowKeys.length === 0"
        >
          批量删除
        </a-button>
      </div>

      <!-- 试卷列表表格区 -->
      <a-table
        :columns="columns"
        :data-source="paperList"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{
          selectedRowKeys: selectedRowKeys,
          onChange: handleSelectionChange
        }"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">
                删除
              </a-button>
              <a-button type="link" size="small" @click="handleManageQuestions(record)">
                管理试题
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 试卷新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      :confirmLoading="modalLoading"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="试卷名称" name="paperName" :rules="[{ required: true, message: '请输入试卷名称' }]">
          <a-input v-model:value="formState.paperName" />
        </a-form-item>
        <a-form-item label="所属科目" name="subject" :rules="[{ required: true, message: '请选择所属科目' }]">
          <a-select v-model:value="formState.subject">
            <a-select-option v-for="subject in subjectOptions" :key="subject" :value="subject">
              {{ subject }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="总分" name="totalScore" :rules="[{ required: true, message: '请输入总分' }]">
          <a-input-number v-model:value="formState.totalScore" :min="0" :precision="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态" name="status" :rules="[{ required: true, message: '请选择状态' }]">
          <a-select v-model:value="formState.status">
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">已发布</a-select-option>
            <a-select-option :value="2">已归档</a-select-option>
            <a-select-option :value="3">已停用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 试卷试题管理弹窗 -->
    <a-modal
      v-model:open="questionManageVisible"
      :title="questionManageTitle"
      width="80%"
      @ok="handleQuestionManageOk"
      @cancel="handleQuestionManageCancel"
      :footer="null"
    >
      <div class="question-manage-content">
        <div class="question-manage-operations">
          <a-button type="primary" @click="handleAddQuestions">
            添加试题
          </a-button>
        </div>

        <a-table
          :columns="questionColumns"
          :data-source="currentPaperQuestions"
          :loading="questionLoading"
          :pagination="false"
          row-key="id"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'sort'">
              <a-input-number
                v-model:value="record.sort"
                :min="1"
                :precision="0"
                @change="handleSortChange(record)"
              />
            </template>
            <template v-if="column.key === 'type'">
              <a-tag :color="getQuestionTypeColor(record.type)">
                {{ getQuestionTypeText(record.type) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'questionScore'">
              <a-input-number
                v-model:value="record.questionScore"
                :min="0"
                :precision="0"
                @change="handleScoreChange(record)"
              />
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" danger @click="handleRemoveQuestion(record)">
                移除
              </a-button>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

    <!-- 选择试题弹窗 -->
    <a-modal
      v-model:open="selectQuestionVisible"
      title="选择试题"
      width="80%"
      @ok="handleSelectQuestionOk"
      @cancel="handleSelectQuestionCancel"
      :confirmLoading="selectQuestionLoading"
    >
      <div class="select-question-content">
        <div class="select-question-filters">
          <a-space>
            <a-input
              v-model:value="questionFilters.content"
              placeholder="题目内容"
              style="width: 200px"
            />
            <a-select
              v-model:value="questionFilters.subject"
              placeholder="科目"
              style="width: 120px"
              allow-clear
            >
              <a-select-option v-for="subject in subjectOptions" :key="subject" :value="subject">
                {{ subject }}
              </a-select-option>
            </a-select>
            <a-select
              v-model:value="questionFilters.type"
              placeholder="题目类型"
              style="width: 120px"
              allow-clear
            >
              <a-select-option :value="1">单选题</a-select-option>
              <a-select-option :value="2">多选题</a-select-option>
              <a-select-option :value="3">判断题</a-select-option>
              <a-select-option :value="4">填空题</a-select-option>
              <a-select-option :value="5">简答题</a-select-option>
            </a-select>
            <a-button type="primary" @click="handleSearchQuestions">搜索</a-button>
          </a-space>
        </div>

        <a-table
          :columns="selectQuestionColumns"
          :data-source="availableQuestions"
          :loading="availableQuestionsLoading"
          :pagination="availableQuestionsPagination"
          :row-selection="{
            selectedRowKeys: selectedQuestionKeys,
            onChange: handleQuestionSelectionChange
          }"
          @change="handleSelectQuestionTableChange"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <a-tag :color="getQuestionTypeColor(record.type)">
                {{ getQuestionTypeText(record.type) }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

    <!-- AI智能组卷弹窗 -->
    <a-modal
      v-model:open="aiAssemblyVisible"
      title="AI智能组卷"
      width="60%"
      @ok="handleAIAssemblyOk"
      @cancel="handleAIAssemblyCancel"
      :confirmLoading="aiAssemblyLoading"
    >
      <a-form
        ref="aiFormRef"
        :model="aiFormState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="试卷名称" name="paperName" :rules="[{ required: true, message: '请输入试卷名称' }]">
          <a-input v-model:value="aiFormState.paperName" placeholder="请输入试卷名称" />
        </a-form-item>
        <a-form-item label="所属科目" name="subject">
          <a-select v-model:value="aiFormState.subject" placeholder="请选择科目（可选）" allow-clear>
            <a-select-option v-for="subject in subjectOptions" :key="subject" :value="subject">
              {{ subject }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="章节" name="chapter">
          <a-input v-model:value="aiFormState.chapter" placeholder="请输入章节（可选）" />
        </a-form-item>
        <a-form-item label="难度" name="difficulty">
          <a-select v-model:value="aiFormState.difficulty" placeholder="请选择难度（可选）" allow-clear>
            <a-select-option :value="1">简单</a-select-option>
            <a-select-option :value="2">中等</a-select-option>
            <a-select-option :value="3">困难</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="总分" name="totalScore">
          <a-input-number v-model:value="aiFormState.totalScore" :min="0" :precision="0" style="width: 100%" placeholder="请输入总分（可选）" />
        </a-form-item>
        <a-form-item label="状态" name="status" :rules="[{ required: true, message: '请选择状态' }]">
          <a-select v-model:value="aiFormState.status">
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">已发布</a-select-option>
            <a-select-option :value="2">已归档</a-select-option>
            <a-select-option :value="3">已停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="组卷需求" name="userRequirement">
          <a-textarea
            v-model:value="aiFormState.userRequirement"
            placeholder="请描述您的组卷需求，例如：侧重基础知识、题型分布均匀、难度适中等"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  addExamPaper,
  deleteExamPaper,
  listExamPaperByPage,
  updateExamPaper,
  getExamPaperById,
  aiAssemblePaper
} from '@/api/shijuanguanli'
import {
  addQuestionToPaper,
  removeQuestionFromPaper,
  updatePaperQuestion
} from '@/api/shijuanshitiguanlianguanli'
import { listQuestionByPage, listAllQuestionByPage } from '@/api/shitiguanli'
import { useLoginUserStore } from '@/stores/loginUser'
import type { FormInstance } from 'ant-design-vue'

interface PaperRecord {
  id?: number
  paperName?: string
  subject?: string
  totalScore?: number
  creatorId?: number
  status?: number
  createTime?: string
  updateTime?: string
  questions?: PaperQuestionVO[]
}

interface PaperQuestionVO {
  id?: number
  questionId?: number
  questionContent?: string
  questionScore?: number
  sort?: number
  type?: number
}

interface QuestionRecord {
  id?: number
  type?: number
  subject?: string
  content?: string
  difficulty?: number
  knowledgePoints?: string
  tags?: string
  answer?: string
  analysis?: string
  creatorId?: number
  status?: number
  createTime?: string
  updateTime?: string
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '试卷名称', dataIndex: 'paperName', key: 'paperName' },
  { title: '所属科目', dataIndex: 'subject', key: 'subject' },
  { title: '总分', dataIndex: 'totalScore', key: 'totalScore' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 200 }
]

const questionColumns = [
  { title: '序号', key: 'sort', width: 100 },
  { title: '题目类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '题目内容', dataIndex: 'questionContent', key: 'questionContent' },
  { title: '分值', key: 'questionScore', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

const selectQuestionColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '题目类型', dataIndex: 'type', key: 'type' },
  { title: '科目', dataIndex: 'subject', key: 'subject' },
  { title: '题目内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '难度', dataIndex: 'difficulty', key: 'difficulty' }
]

const loading = ref(false)
const paperList = ref<PaperRecord[]>([])
const selectedRowKeys = ref<number[]>([])
const loginUserStore = useLoginUserStore()

const subjectOptions = ref<string[]>(['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治'])

const filters = reactive({
  paperName: '',
  subject: undefined as string | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const modalVisible = ref(false)
const modalTitle = ref('')
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  id: undefined as number | undefined,
  paperName: '',
  subject: '',
  totalScore: 100,
  status: 0
})

const questionManageVisible = ref(false)
const questionManageTitle = ref('')
const questionLoading = ref(false)
const currentPaperQuestions = ref<PaperQuestionVO[]>([])
const currentPaperId = ref<number>()

const selectQuestionVisible = ref(false)
const selectQuestionLoading = ref(false)
const availableQuestions = ref<QuestionRecord[]>([])
const availableQuestionsLoading = ref(false)
const selectedQuestionKeys = ref<number[]>([])

const questionFilters = reactive({
  content: '',
  subject: undefined as string | undefined,
  type: undefined as number | undefined
})

const availableQuestionsPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// AI组卷相关状态
const aiAssemblyVisible = ref(false)
const aiAssemblyLoading = ref(false)
const aiFormRef = ref<FormInstance>()

const aiFormState = reactive({
  paperName: '',
  subject: undefined as string | undefined,
  chapter: undefined as string | undefined,
  difficulty: undefined as number | undefined,
  totalScore: undefined as number | undefined,
  status: 0,
  userRequirement: ''
})

const getCurrentUser = () => {
  return loginUserStore.loginUser
}

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
  loadPaperList()
})

const loadPaperList = async () => {
  loading.value = true
  try {
    const query: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }

    if (filters.paperName) {
      query.paperName = filters.paperName
    }
    if (filters.subject) {
      query.subject = filters.subject
    }
    if (filters.status !== undefined) {
      query.status = filters.status
    }

    const currentUser = getCurrentUser()
    if (currentUser?.role !== 'admin') {
      query.creatorId = currentUser?.id
    }

    const res = await listExamPaperByPage(query)
    if (res.data.code === 0) {
      paperList.value = res.data.data?.records || []
      pagination.total = res.data.data?.totalRow || 0
    } else {
      message.error('加载试卷列表失败：' + res.data.message)
    }
  } catch (error) {
    message.error('加载试卷列表请求失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadPaperList()
}

const handleSelectionChange = (keys: number[]) => {
  selectedRowKeys.value = keys
}

const handleSearch = () => {
  pagination.current = 1
  loadPaperList()
}

const handleReset = () => {
  filters.paperName = ''
  filters.subject = undefined
  filters.status = undefined
  pagination.current = 1
  loadPaperList()
}

const handleAdd = () => {
  isEdit.value = false
  modalTitle.value = '新增试卷'
  resetForm()
  modalVisible.value = true
}

const handleEdit = (record: PaperRecord) => {
  isEdit.value = true
  modalTitle.value = '编辑试卷'
  formState.id = record.id
  formState.paperName = record.paperName || ''
  formState.subject = record.subject || ''
  formState.totalScore = record.totalScore || 100
  formState.status = record.status || 0
  modalVisible.value = true
}

const handleDelete = (record: PaperRecord) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除试卷「${record.paperName}」吗？`,
    onOk: async () => {
      if (!record.id) return
      try {
        const res = await deleteExamPaper({ id: record.id })
        if (res.data.code === 0) {
          message.success('删除成功')
          loadPaperList()
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('删除请求失败')
      }
    }
  })
}

const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个试卷吗？`,
    onOk: async () => {
      try {
        for (const id of selectedRowKeys.value) {
          const res = await deleteExamPaper({ id })
          if (res.data.code !== 0) {
            message.error(`删除ID为${id}的试卷失败：${res.data.message}`)
            return
          }
        }
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadPaperList()
      } catch (error) {
        message.error('批量删除请求失败')
      }
    }
  })
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      const res = await updateExamPaper({
        id: formState.id,
        paperName: formState.paperName,
        subject: formState.subject,
        totalScore: formState.totalScore
      })
      if (res.data.code === 0) {
        message.success('更新成功')
        modalVisible.value = false
        loadPaperList()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      const res = await addExamPaper({
        paperName: formState.paperName,
        subject: formState.subject,
        totalScore: formState.totalScore,
        status: formState.status
      })
      if (res.data.code === 0) {
        message.success('添加成功')
        modalVisible.value = false
        loadPaperList()
      } else {
        message.error('添加失败：' + res.data.message)
      }
    }
  } catch (error) {
    console.error(error)
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  resetForm()
}

const resetForm = () => {
  formState.id = undefined
  formState.paperName = ''
  formState.subject = ''
  formState.totalScore = 100
  formState.status = 0
}

const handleManageQuestions = async (record: PaperRecord) => {
  if (!record.id) return

  currentPaperId.value = record.id
  questionManageTitle.value = `管理试题 - ${record.paperName}`
  questionManageVisible.value = true

  await loadPaperQuestions(record.id)
}

const loadPaperQuestions = async (paperId: number) => {
  questionLoading.value = true
  try {
    const res = await getExamPaperById({ id: paperId })
    if (res.data.code === 0) {
      currentPaperQuestions.value = res.data.data?.questions || []
    } else {
      message.error('加载试卷试题失败：' + res.data.message)
    }
  } catch (error) {
    message.error('加载试卷试题请求失败')
  } finally {
    questionLoading.value = false
  }
}

const handleQuestionManageOk = () => {
  questionManageVisible.value = false
  loadPaperList()
}

const handleQuestionManageCancel = () => {
  questionManageVisible.value = false
  currentPaperQuestions.value = []
  currentPaperId.value = undefined
}

const handleAddQuestions = () => {
  selectQuestionVisible.value = true
  selectedQuestionKeys.value = []
  loadAvailableQuestions()
}

const loadAvailableQuestions = async () => {
  availableQuestionsLoading.value = true
  try {
    const query: any = {
      pageNum: availableQuestionsPagination.current,
      pageSize: availableQuestionsPagination.pageSize
    }

    if (questionFilters.content) {
      query.content = questionFilters.content
    }
    if (questionFilters.subject) {
      query.subject = questionFilters.subject
    }
    if (questionFilters.type !== undefined) {
      query.type = questionFilters.type
    }

    const currentUser = getCurrentUser()
    if (currentUser?.role === 'admin') {
      const res = await listAllQuestionByPage(query)
      if (res.data.code === 0) {
        availableQuestions.value = res.data.data?.records || []
        availableQuestionsPagination.total = res.data.data?.totalRow || 0
      } else {
        message.error('加载试题列表失败：' + res.data.message)
      }
    } else {
      query.creatorId = currentUser?.id
      const res = await listQuestionByPage(query)
      if (res.data.code === 0) {
        availableQuestions.value = res.data.data?.records || []
        availableQuestionsPagination.total = res.data.data?.totalRow || 0
      } else {
        message.error('加载试题列表失败：' + res.data.message)
      }
    }
  } catch (error) {
    message.error('加载试题列表请求失败')
  } finally {
    availableQuestionsLoading.value = false
  }
}

const handleSearchQuestions = () => {
  availableQuestionsPagination.current = 1
  loadAvailableQuestions()
}

const handleSelectQuestionTableChange = (pag: any) => {
  availableQuestionsPagination.current = pag.current
  availableQuestionsPagination.pageSize = pag.pageSize
  loadAvailableQuestions()
}

const handleQuestionSelectionChange = (keys: number[]) => {
  selectedQuestionKeys.value = keys
}

const handleSelectQuestionOk = async () => {
  if (selectedQuestionKeys.value.length === 0) {
    message.warning('请选择要添加的试题')
    return
  }

  if (!currentPaperId.value) return

  selectQuestionLoading.value = true
  try {
    const currentSort = currentPaperQuestions.value.length + 1
    for (const questionId of selectedQuestionKeys.value) {
      const question = availableQuestions.value.find(q => q.id === questionId)
      const res = await addQuestionToPaper({
        paperId: currentPaperId.value,
        questionId: questionId,
        questionScore: 10,
        sort: currentSort
      })

      if (res.data.code !== 0) {
        message.error(`添加试题失败：${res.data.message}`)
        return
      }
    }

    message.success('添加试题成功')
    selectQuestionVisible.value = false
    await loadPaperQuestions(currentPaperId.value)
  } catch (error) {
    message.error('添加试题请求失败')
  } finally {
    selectQuestionLoading.value = false
  }
}

const handleSelectQuestionCancel = () => {
  selectQuestionVisible.value = false
  selectedQuestionKeys.value = []
  availableQuestions.value = []
}

const handleRemoveQuestion = (record: PaperQuestionVO) => {
  Modal.confirm({
    title: '确认移除',
    content: '确定要移除这道试题吗？',
    onOk: async () => {
      if (!record.id || !currentPaperId.value) return

      try {
        const res = await removeQuestionFromPaper({ id: record.id })
        if (res.data.code === 0) {
          message.success('移除成功')
          await loadPaperQuestions(currentPaperId.value)
        } else {
          message.error('移除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('移除请求失败')
      }
    }
  })
}

const handleSortChange = async (record: PaperQuestionVO) => {
  if (!record.id) return

  try {
    const res = await updatePaperQuestion({
      id: record.id,
      sort: record.sort
    })
    if (res.data.code !== 0) {
      message.error('更新排序失败：' + res.data.message)
      await loadPaperQuestions(currentPaperId.value!)
    }
  } catch (error) {
    message.error('更新排序请求失败')
    await loadPaperQuestions(currentPaperId.value!)
  }
}

const handleScoreChange = async (record: PaperQuestionVO) => {
  if (!record.id) return

  try {
    const res = await updatePaperQuestion({
      id: record.id,
      questionScore: record.questionScore
    })
    if (res.data.code !== 0) {
      message.error('更新分值失败：' + res.data.message)
      await loadPaperQuestions(currentPaperId.value!)
    }
  } catch (error) {
    message.error('更新分值请求失败')
    await loadPaperQuestions(currentPaperId.value!)
  }
}

const getStatusColor = (status?: number) => {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'success',
    2: 'warning',
    3: 'error'
  }
  return colorMap[status || 0] || 'default'
}

const getStatusText = (status?: number) => {
  const textMap: Record<number, string> = {
    0: '草稿',
    1: '已发布',
    2: '已归档',
    3: '已停用'
  }
  return textMap[status || 0] || '未知'
}

const getQuestionTypeColor = (type?: number) => {
  const colorMap: Record<number, string> = {
    1: 'blue',
    2: 'green',
    3: 'orange',
    4: 'purple',
    5: 'red'
  }
  return colorMap[type || 0] || 'default'
}

const getQuestionTypeText = (type?: number) => {
  const textMap: Record<number, string> = {
    1: '单选题',
    2: '多选题',
    3: '判断题',
    4: '填空题',
    5: '简答题'
  }
  return textMap[type || 0] || '未知'
}

// AI组卷相关方法
const handleAIPaperAssembly = () => {
  aiAssemblyVisible.value = true
  resetAIForm()
}

const handleAIAssemblyOk = async () => {
  try {
    await aiFormRef.value?.validate()
    aiAssemblyLoading.value = true

    const res = await aiAssemblePaper({
      paperName: aiFormState.paperName,
      subject: aiFormState.subject,
      chapter: aiFormState.chapter,
      difficulty: aiFormState.difficulty,
      totalScore: aiFormState.totalScore,
      status: aiFormState.status,
      userRequirement: aiFormState.userRequirement
    })

    if (res.data.code === 0) {
      message.success(`AI组卷成功！共选中${res.data.data?.totalQuestions}道题，总分${res.data.data?.actualTotalScore}分`)
      aiAssemblyVisible.value = false
      loadPaperList()
      
      // 自动打开试卷详情
      if (res.data.data?.paperId) {
        setTimeout(() => {
          const paper = paperList.value.find(p => p.id === res.data.data?.paperId)
          if (paper) {
            handleManageQuestions(paper)
          }
        }, 500)
      }
    } else {
      message.error('AI组卷失败：' + res.data.message)
    }
  } catch (error) {
    console.error(error)
  } finally {
    aiAssemblyLoading.value = false
  }
}

const handleAIAssemblyCancel = () => {
  aiAssemblyVisible.value = false
  resetAIForm()
}

const resetAIForm = () => {
  aiFormState.paperName = ''
  aiFormState.subject = undefined
  aiFormState.chapter = undefined
  aiFormState.difficulty = undefined
  aiFormState.totalScore = undefined
  aiFormState.status = 0
  aiFormState.userRequirement = ''
}
</script>

<style scoped>
.paper-manager {
  padding: 0;
}

.card-title {
  font-size: 18px;
  font-weight: 500;
}

.filter-section {
  margin-bottom: 16px;
}

.table-operations {
  margin-bottom: 16px;
}

.question-manage-content {
  min-height: 400px;
}

.question-manage-operations {
  margin-bottom: 16px;
}

.select-question-content {
  min-height: 500px;
}

.select-question-filters {
  margin-bottom: 16px;
}
</style>

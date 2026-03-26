<template>
  <div class="question-manager">
    <a-card>
      <template #title>
        <div class="card-title">题目管理</div>
      </template>

      <div class="table-operations">
        <a-button type="primary" @click="handleAdd">
          新增题目
        </a-button>
        <a-button @click="handleImport">
          批量导入
        </a-button>
        <a-input-search
          placeholder="请输入题目内容搜索"
          style="width: 300px; margin-left: 16px"
          v-model:value="searchText"
          @search="handleSearch"
        />
      </div>

      <a-table
        :columns="columns"
        :data-source="questionList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'difficulty'">
            <a-tag :color="getDifficultyColor(record.difficulty)">
              {{ getDifficultyText(record.difficulty) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">
                详情
              </a-button>
              <a-button type="link" size="small" @click="handleEdit(record)" :disabled="!canEdit(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)" :disabled="!canDelete(record)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

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
        <a-form-item label="题目类型" name="type" :rules="[{ required: true, message: '请选择题目类型' }]">
          <a-select v-model:value="formState.type">
            <a-select-option value="1">单选</a-select-option>
            <a-select-option value="2">多选</a-select-option>
            <a-select-option value="3">填空</a-select-option>
            <a-select-option value="4">简答</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="科目" name="subject" :rules="[{ required: true, message: '请输入科目' }]">
          <a-input v-model:value="formState.subject" />
        </a-form-item>
        <a-form-item label="章节" name="chapter">
          <a-input v-model:value="formState.chapter" />
        </a-form-item>
        <a-form-item label="难度" name="difficulty" :rules="[{ required: true, message: '请选择难度' }]">
          <a-select v-model:value="formState.difficulty">
            <a-select-option value="1">简单</a-select-option>
            <a-select-option value="2">中等</a-select-option>
            <a-select-option value="3">困难</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="知识点" name="knowledgePoints">
          <a-input v-model:value="formState.knowledgePoints" />
        </a-form-item>
        <a-form-item label="标签" name="tags">
          <a-input v-model:value="formState.tags" placeholder="请输入标签，多个标签用逗号分隔" />
        </a-form-item>
        <a-form-item label="题目内容" name="content" :rules="[{ required: true, message: '请输入题目内容' }]">
          <a-textarea v-model:value="formState.content" :rows="4" />
        </a-form-item>
        <a-form-item label="选项" name="options">
          <a-textarea v-model:value="formState.options" :rows="4" placeholder="请输入选项，JSON格式" />
        </a-form-item>
        <a-form-item label="答案" name="answer" :rules="[{ required: true, message: '请输入答案' }]">
          <a-input v-model:value="formState.answer" />
        </a-form-item>
        <a-form-item label="解析" name="analysis">
          <a-textarea v-model:value="formState.analysis" :rows="4" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="formState.status">
            <a-select-option value="1">草稿</a-select-option>
            <a-select-option value="2">已发布</a-select-option>
            <a-select-option value="3">停用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情对话框 -->
    <a-modal
      v-model:open="detailVisible"
      title="题目详情"
      @cancel="handleDetailCancel"
    >
      <div v-if="currentQuestion">
        <a-descriptions bordered column="1">
          <a-descriptions-item label="题目类型">{{ getTypeText(currentQuestion.type) }}</a-descriptions-item>
          <a-descriptions-item label="科目">{{ currentQuestion.subject }}</a-descriptions-item>
          <a-descriptions-item label="章节">{{ currentQuestion.chapter }}</a-descriptions-item>
          <a-descriptions-item label="难度">{{ getDifficultyText(currentQuestion.difficulty) }}</a-descriptions-item>
          <a-descriptions-item label="知识点">{{ currentQuestion.knowledgePoints }}</a-descriptions-item>
          <a-descriptions-item label="标签">
            <a-tag v-for="(tag, index) in parseTags(currentQuestion.tags)" :key="index" style="margin-right: 8px;">
              {{ tag }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="题目内容">{{ currentQuestion.content }}</a-descriptions-item>
          <a-descriptions-item label="选项">{{ currentQuestion.options }}</a-descriptions-item>
          <a-descriptions-item label="答案">{{ currentQuestion.answer }}</a-descriptions-item>
          <a-descriptions-item label="解析">{{ currentQuestion.analysis }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentQuestion.createTime }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ currentQuestion.updateTime }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>

    <!-- 批量导入对话框 -->
    <a-modal
      v-model:open="importVisible"
      title="批量导入题目"
      @ok="handleImportOk"
      @cancel="handleImportCancel"
      :confirmLoading="importLoading"
    >
      <div>
        <a-upload
          v-model:file-list="fileList"
          :multiple="false"
          accept=".xlsx,.xls"
          :show-upload-list="true"
          :before-upload="beforeUpload"
          @change="handleFileChange"
        >
          <a-button>
            <UploadOutlined />
            选择文件
          </a-button>
        </a-upload>
        <div style="margin-top: 16px; color: #666;">
          请上传 Excel 文件，支持 .xlsx 和 .xls 格式
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { addQuestion, deleteQuestion, updateQuestion, listQuestionByPage, listAllQuestionByPage } from '@/api/shitiguanli'
import { uploadExcel, getImportStatus } from '@/api/questionImportController'
import { useLoginUserStore } from '@/stores/loginUser'
import type { FormInstance } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'

interface QuestionRecord {
  id?: number
  type?: number
  subject?: string
  chapter?: string
  difficulty?: number
  knowledgePoints?: string
  tags?: string
  content?: string
  options?: string
  answer?: string
  analysis?: string
  creatorId?: number
  status?: number
  correctCount?: number
  totalCount?: number
  accuracy?: number
  createTime?: string
  updateTime?: string
}

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '题目类型',
    dataIndex: 'type',
    key: 'type',
  },
  {
    title: '科目',
    dataIndex: 'subject',
    key: 'subject',
  },
  {
    title: '章节',
    dataIndex: 'chapter',
    key: 'chapter',
  },
  {
    title: '难度',
    dataIndex: 'difficulty',
    key: 'difficulty',
  },
  {
    title: '知识点',
    dataIndex: 'knowledgePoints',
    key: 'knowledgePoints',
  },
  {
    title: '题目内容',
    dataIndex: 'content',
    key: 'content',
    ellipsis: true,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
  },
]

const loading = ref(false)
const questionList = ref<QuestionRecord[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const modalVisible = ref(false)
const modalTitle = ref('')
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 详情对话框相关
const detailVisible = ref(false)
const currentQuestion = ref<QuestionRecord | null>(null)

// 批量导入相关
const importVisible = ref(false)
const fileList = ref<UploadFile[]>([])
const importLoading = ref(false)

const formState = reactive({
  id: undefined as number | undefined,
  type: 1,
  subject: '',
  chapter: '',
  difficulty: 1,
  knowledgePoints: '',
  tags: '',
  content: '',
  options: '',
  answer: '',
  analysis: '',
  status: 1,
})

// 搜索相关
const searchText = ref('')

// 使用登录用户 store
const loginUserStore = useLoginUserStore()

// 获取当前用户信息
const getCurrentUser = () => {
  return loginUserStore.loginUser
}

// 确保在组件挂载时获取用户信息
onMounted(async () => {
  await loginUserStore.fetchLoginUser()
  loadQuestionList()
})

const loadQuestionList = async () => {
  loading.value = true
  try {
    const query = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }

    // 添加搜索条件
    if (searchText.value) {
      Object.assign(query, { content: searchText.value })
    }

    let res
    const currentUser = getCurrentUser()
    // 管理员使用 listAllQuestionByPage 接口，普通用户使用 listQuestionByPage 接口
    if (currentUser?.role === 'admin') {
      res = await listAllQuestionByPage(query)
    } else {
      // 普通用户只能查看自己的题目
      Object.assign(query, { creatorId: currentUser?.id })
      res = await listQuestionByPage(query)
    }

    if (res.data.code === 0) {
      questionList.value = res.data.data?.records || []
      pagination.total = res.data.data?.totalRow || 0
    } else {
      message.error('加载题目列表失败：' + res.data.message)
    }
  } catch (error) {
    message.error('加载题目列表请求失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadQuestionList()
}

const handleSearch = () => {
  pagination.current = 1
  loadQuestionList()
}

const handleView = (record: QuestionRecord) => {
  currentQuestion.value = record
  detailVisible.value = true
}

const handleDetailCancel = () => {
  detailVisible.value = false
  currentQuestion.value = null
}

// 批量导入相关方法
const handleImport = () => {
  importVisible.value = true
  fileList.value = []
}

const handleImportCancel = () => {
  importVisible.value = false
  fileList.value = []
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isExcel = file.type === 'application/vnd.ms-excel' || file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  if (!isExcel) {
    message.error('只能上传 Excel 文件！')
  }
  return isExcel
}

const handleFileChange = (info: any) => {
  fileList.value = info.fileList
}

const handleImportOk = async () => {
  if (fileList.value.length === 0) {
    message.error('请选择要上传的文件')
    return
  }

  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0].originFileObj!)

    const res = await uploadExcel(formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    if (res.data.code === 0) {
      message.success('上传成功')
      importVisible.value = false
      fileList.value = []
      // 可以根据返回的任务ID查询导入状态
      // const taskId = res.data.data
      // checkImportStatus(taskId)
    } else {
      message.error('上传失败：' + res.data.message)
    }
  } catch (error) {
    message.error('上传请求失败')
  } finally {
    importLoading.value = false
  }
}

// 检查导入状态
const checkImportStatus = async (taskId: string) => {
  try {
    const res = await getImportStatus({ taskId })
    if (res.data.code === 0) {
      message.success('导入成功')
      loadQuestionList()
    } else {
      message.error('导入失败：' + res.data.message)
    }
  } catch (error) {
    message.error('查询导入状态失败')
  }
}

const handleAdd = () => {
  isEdit.value = false
  modalTitle.value = '新增题目'
  resetForm()
  modalVisible.value = true
}

const handleEdit = (record: QuestionRecord) => {
  isEdit.value = true
  modalTitle.value = '编辑题目'
  formState.id = record.id
  formState.type = record.type || 1
  formState.subject = record.subject || ''
  formState.chapter = record.chapter || ''
  formState.difficulty = record.difficulty || 1
  formState.knowledgePoints = record.knowledgePoints || ''
  // 将标签 JSON 字符串转换为逗号分隔的字符串
  try {
    if (record.tags) {
      // 尝试解析标签字符串
      let tagsArray
      if (typeof record.tags === 'string') {
        tagsArray = JSON.parse(record.tags)
      } else {
        tagsArray = record.tags
      }
      formState.tags = Array.isArray(tagsArray) ? tagsArray.join(',') : ''
    } else {
      formState.tags = ''
    }
  } catch (error) {
    formState.tags = ''
  }
  formState.content = record.content || ''
  formState.options = record.options || ''
  formState.answer = record.answer || ''
  formState.analysis = record.analysis || ''
  formState.status = record.status || 1
  modalVisible.value = true
}

const handleDelete = async (record: QuestionRecord) => {
  if (!record.id) return
  try {
    const res = await deleteQuestion({ id: record.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadQuestionList()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除请求失败')
  }
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      const res = await updateQuestion({
        id: formState.id,
        type: formState.type,
        subject: formState.subject,
        chapter: formState.chapter,
        difficulty: formState.difficulty,
        knowledgePoints: formState.knowledgePoints,
        tags: formState.tags ? JSON.stringify(formState.tags.split(',').map(tag => tag.trim()).filter(tag => tag !== '')) : '[]',
        content: formState.content,
        options: formState.options ? formState.options : '[]',
        answer: formState.answer,
        analysis: formState.analysis,
        status: formState.status,
      })
      if (res.data.code === 0) {
        message.success('更新成功')
        modalVisible.value = false
        loadQuestionList()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      const res = await addQuestion({
        type: formState.type,
        subject: formState.subject,
        chapter: formState.chapter,
        difficulty: formState.difficulty,
        knowledgePoints: formState.knowledgePoints,
        tags: formState.tags ? JSON.stringify(formState.tags.split(',').map(tag => tag.trim()).filter(tag => tag !== '')) : '[]',
        content: formState.content,
        options: formState.options ? formState.options : '[]',
        answer: formState.answer,
        analysis: formState.analysis,
        status: formState.status,
      })
      if (res.data.code === 0) {
        message.success('添加成功')
        modalVisible.value = false
        loadQuestionList()
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
  formState.type = 1
  formState.subject = ''
  formState.chapter = ''
  formState.difficulty = 1
  formState.knowledgePoints = ''
  formState.tags = ''
  formState.content = ''
  formState.options = ''
  formState.answer = ''
  formState.analysis = ''
  formState.status = 1
}

const canEdit = (record: QuestionRecord) => {
  const currentUser = getCurrentUser()
  if (!currentUser) return false

  // 确保 creatorId 和 currentUser.id 都是数字类型再比较
  const recordCreatorId = Number(record.creatorId)
  const currentUserId = Number(currentUser.id)
  return currentUser.role === 'admin' || recordCreatorId === currentUserId
}

const canDelete = (record: QuestionRecord) => {
  const currentUser = getCurrentUser()
  if (!currentUser) return false

  // 确保 creatorId 和 currentUser.id 都是数字类型再比较
  const recordCreatorId = Number(record.creatorId)
  const currentUserId = Number(currentUser.id)
  return currentUser.role === 'admin' || recordCreatorId === currentUserId
}

const getTypeColor = (type?: number) => {
  const colorMap: Record<number, string> = {
    1: 'blue',
    2: 'green',
    3: 'orange',
    4: 'purple',
  }
  return colorMap[type || 0] || 'default'
}

const getTypeText = (type?: number) => {
  const textMap: Record<number, string> = {
    1: '单选',
    2: '多选',
    3: '填空',
    4: '简答',
  }
  return textMap[type || 0] || '未知'
}

const getDifficultyColor = (difficulty?: number) => {
  const colorMap: Record<number, string> = {
    1: 'green',
    2: 'blue',
    3: 'red',
  }
  return colorMap[difficulty || 0] || 'default'
}

const getDifficultyText = (difficulty?: number) => {
  const textMap: Record<number, string> = {
    1: '简单',
    2: '中等',
    3: '困难',
  }
  return textMap[difficulty || 0] || '未知'
}

const getStatusColor = (status?: number) => {
  const colorMap: Record<number, string> = {
    1: 'default',
    2: 'success',
    3: 'error',
  }
  return colorMap[status || 0] || 'default'
}

const getStatusText = (status?: number) => {
  const textMap: Record<number, string> = {
    1: '草稿',
    2: '已发布',
    3: '停用',
  }
  return textMap[status || 0] || '未知'
}

// 解析标签字符串为数组
const parseTags = (tags?: string) => {
  if (!tags) return []
  try {
    // 尝试解析 JSON 字符串
    const parsedTags = JSON.parse(tags)
    return Array.isArray(parsedTags) ? parsedTags : []
  } catch (error) {
    // 如果解析失败，返回空数组
    return []
  }
}
</script>

<style scoped>
.question-manager {
  padding: 0;
}

.card-title {
  font-size: 18px;
  font-weight: 500;
}

.table-operations {
  margin-bottom: 16px;
}
</style>

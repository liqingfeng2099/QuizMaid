<template>
  <div class="user-manager">
    <a-card>
      <template #title>
        <div class="card-title">用户管理</div>
      </template>

      <div class="table-operations">
        <a-button type="primary" @click="handleAdd">
          添加用户
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="userList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <a-tag :color="getRoleColor(record.role)">
              {{ getRoleText(record.role) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'emailVerified'">
            <a-tag :color="record.emailVerified ? 'success' : 'warning'">
              {{ record.emailVerified ? '已验证' : '未验证' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 1 ? '正常' : '禁用' }}
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
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="formState.username" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="formState.nickname" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" />
        </a-form-item>
        <a-form-item label="角色" name="role" :rules="[{ required: true, message: '请选择角色' }]">
          <a-select v-model:value="formState.role">
            <a-select-option value="admin">管理员</a-select-option>
          <a-select-option value="user">用户</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listUserByPage, addUser, deleteUser, updateUserByAdmin } from '@/api/userController'
import type { FormInstance } from 'ant-design-vue'

interface UserRecord {
  id?: number
  username?: string
  nickname?: string
  email?: string
  role?: string
  emailVerified?: number
  status?: number
  answerNum?: number
  correctNum?: number
}

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username',
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    key: 'nickname',
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    key: 'email',
  },
  {
    title: '角色',
    dataIndex: 'role',
    key: 'role',
  },
  {
    title: '邮箱状态',
    dataIndex: 'emailVerified',
    key: 'emailVerified',
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
const userList = ref<UserRecord[]>([])
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

const formState = reactive({
  id: undefined as number | undefined,
  username: '',
  nickname: '',
  email: '',
  role: 'user',
  status: 0,
  emailVerified: 0,
  answerNum: 0,
  correctNum: 0,
})

onMounted(() => {
  loadUserList()
})

const loadUserList = async () => {
  loading.value = true
  try {
    const res = await listUserByPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (res.data.code === 0) {
      userList.value = res.data.data?.records || []
      pagination.total = res.data.data?.totalRow || 0
    } else {
      message.error('加载用户列表失败：' + res.data.message)
    }
  } catch (error) {
    console.error(error)
    message.error('加载用户列表请求失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadUserList()
}

const handleAdd = () => {
  isEdit.value = false
  modalTitle.value = '添加用户'
  resetForm()
  modalVisible.value = true
}

const handleEdit = (record: UserRecord) => {
  isEdit.value = true
  modalTitle.value = '编辑用户'
  formState.id = record.id
  formState.username = record.username || ''
  formState.nickname = record.nickname || ''
  formState.email = record.email || ''
  formState.role = record.role || 'user'
  formState.status = record.status || 0
  formState.emailVerified = record.emailVerified || 0
  formState.answerNum = record.answerNum || 0
  formState.correctNum = record.correctNum || 0
  modalVisible.value = true
}

const handleDelete = async (record: UserRecord) => {
  if (!record.id) return
  try {
    const res = await deleteUser({ id: record.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadUserList()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error(error)
    message.error('删除请求失败')
  }
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      const res = await updateUserByAdmin({
        id: formState.id,
        username: formState.username,
        nickname: formState.nickname,
        email: formState.email,
        role: formState.role,
        status: formState.status,
        emailVerified: formState.emailVerified,
        answerNum: formState.answerNum,
        correctNum: formState.correctNum,
      })
      if (res.data.code === 0) {
        message.success('更新成功')
        modalVisible.value = false
        loadUserList()
      } else {
        message.error('更新失败：' + res.data.message)
      }
    } else {
      const res = await addUser({
        username: formState.username,
        nickname: formState.nickname,
        role: formState.role,
      } as API.UserAddDTO)
      if (res.data.code === 0) {
        message.success('添加成功')
        modalVisible.value = false
        loadUserList()
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
  formState.username = ''
  formState.nickname = ''
  formState.email = ''
  formState.role = 'user'
  formState.status = 0
  formState.emailVerified = 0
  formState.answerNum = 0
  formState.correctNum = 0
}

const getRoleColor = (role?: string) => {
  const colorMap: Record<string, string> = {
    admin: 'red',
    user: 'blue',
  }
  return colorMap[role || ''] || 'default'
}

const getRoleText = (role?: string) => {
  const textMap: Record<string, string> = {
    admin: '管理员',
    user: '用户',
  }
  return textMap[role || ''] || role
}
</script>

<style scoped>
.user-manager {
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

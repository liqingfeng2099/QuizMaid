<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { listAllPapers, batchDeletePapers, forceDeletePaper, batchUpdatePaperStatus } from '@/api/admin'

const loading = ref(false)
const paperList = ref<any[]>([])
const selectedIds = ref<number[]>([])
const selectedStatus = ref(3)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '试卷名称', dataIndex: 'paperName' },
  { title: '科目', dataIndex: 'subject' },
  { title: '总分', dataIndex: 'totalScore', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建者ID', dataIndex: 'creatorId', width: 80 },
  { title: '删除', key: 'isDeleted', width: 60 },
]

function getStatusText(s: number) {
  return ({ 0: '草稿', 1: '已发布', 2: '已归档', 3: '已停用' } as any)[s] || '未知'
}

async function load() {
  loading.value = true
  try {
    const res = await listAllPapers({ pageNum: pagination.current, pageSize: pagination.pageSize })
    if (res.data.code === 0 && res.data.data) {
      paperList.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    }
  } catch { message.error('加载失败') }
  loading.value = false
}

function handleTableChange(pag: any) {
  pagination.current = pag.current; pagination.pageSize = pag.pageSize; load()
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) { message.warning('请选择试卷'); return }
  Modal.confirm({
    title: '确认批量删除', content: `确定删除 ${selectedIds.value.length} 个试卷吗？`,
    onOk: async () => {
      const res = await batchDeletePapers(selectedIds.value)
      if (res.data.code === 0) { message.success(`已删除 ${res.data.data} 个`); selectedIds.value = []; load() }
      else message.error(res.data.message)
    }
  })
}

async function handleForceDelete(id: number) {
  Modal.confirm({
    title: '确认强制删除', content: '此操作将物理删除试卷，不可恢复！',
    onOk: async () => {
      const res = await forceDeletePaper(id)
      if (res.data.code === 0) { message.success('已强制删除'); load() }
      else message.error(res.data.message)
    }
  })
}

async function handleBatchStatus() {
  if (!selectedIds.value.length) { message.warning('请选择试卷'); return }
  const res = await batchUpdatePaperStatus(selectedStatus.value, selectedIds.value)
  if (res.data.code === 0) { message.success(`已更新 ${res.data.data} 份试卷`); selectedIds.value = []; load() }
  else message.error(res.data.message)
}

onMounted(load)
</script>

<template>
  <div>
    <a-space style="margin-bottom:12px">
      <a-button danger :disabled="selectedIds.length===0" @click="handleBatchDelete">批量删除</a-button>
      <a-select v-model:value="selectedStatus" style="width:120px" size="small">
        <a-select-option :value="0">草稿</a-select-option>
        <a-select-option :value="1">已发布</a-select-option>
        <a-select-option :value="2">已归档</a-select-option>
        <a-select-option :value="3">已停用</a-select-option>
      </a-select>
      <a-button :disabled="selectedIds.length===0" @click="handleBatchStatus">批量修改状态</a-button>
    </a-space>
    <a-table :columns="columns" :data-source="paperList" :loading="loading"
      :pagination="pagination" @change="handleTableChange" row-key="id" size="small"
      :row-selection="{ selectedRowKeys: selectedIds, onChange: (keys: any) => selectedIds = keys }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status===1?'green':record.status===2?'orange':'default'">{{ getStatusText(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'isDeleted'">
          <a-tag v-if="record.isDeleted===1" color="red">已删除</a-tag>
          <span v-else>正常</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" danger @click="handleForceDelete(record.id)">强制删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

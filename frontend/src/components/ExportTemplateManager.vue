<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  createExportTemplate, updateExportTemplate, deleteExportTemplate,
  setDefaultExportTemplate, listExportTemplates
} from '@/api/exportTemplate'

const emit = defineEmits(['select'])
const props = defineProps<{ selectedId?: number }>()

const templates = ref<API.ExportTemplate[]>([])
const loading = ref(false)
const editVisible = ref(false)
const editTitle = ref('')
const editId = ref<number | undefined>()
const formName = ref('')
const formType = ref('Word')
const formConfig = ref('{"titleAlign":"center","showAnswer":true,"showAnalysis":false,"scorePosition":"right","fontSize":"16px"}')

const columns = [
  { title: '模板名', dataIndex: 'templateName' },
  { title: '类型', dataIndex: 'exportType', width: 80 },
  { title: '默认', key: 'isDefault', width: 80 },
  { title: '操作', key: 'action', width: 200 },
]

async function load() {
  loading.value = true
  try {
    const res = await listExportTemplates()
    if (res.data.code === 0) templates.value = res.data.data || []
  } catch { /* ignore */ }
  loading.value = false
}

function handleAdd() {
  editId.value = undefined; editTitle.value = '新建导出模板'
  formName.value = ''; formType.value = 'Word'
  formConfig.value = '{"titleAlign":"center","showAnswer":true,"showAnalysis":false,"scorePosition":"right","fontSize":"16px"}'
  editVisible.value = true
}

function handleEdit(record: API.ExportTemplate) {
  editId.value = record.id; editTitle.value = '编辑 - ' + record.templateName
  formName.value = record.templateName || ''; formType.value = record.exportType || 'Word'
  formConfig.value = record.config || '{}'
  editVisible.value = true
}

async function handleSave() {
  if (!formName.value) { message.warning('请输入模板名称'); return }
  try {
    const body = { name: formName.value, exportType: formType.value, config: formConfig.value }
    if (editId.value) {
      const res = await updateExportTemplate(editId.value, body)
      if (res.data.code === 0) { message.success('已更新'); editVisible.value = false; load() }
      else message.error(res.data.message)
    } else {
      const res = await createExportTemplate(body)
      if (res.data.code === 0) { message.success('已创建'); editVisible.value = false; load() }
      else message.error(res.data.message)
    }
  } catch { message.error('请求失败') }
}

async function handleDelete(id: number) {
  Modal.confirm({
    title: '确认删除', content: '确定删除此模板？',
    onOk: async () => {
      const res = await deleteExportTemplate(id)
      if (res.data.code === 0) { message.success('已删除'); load() }
      else message.error(res.data.message)
    }
  })
}

async function handleSetDefault(id: number) {
  const res = await setDefaultExportTemplate(id)
  if (res.data.code === 0) { message.success('已设为默认'); load() }
  else message.error(res.data.message)
}

function handleSelect(record: API.ExportTemplate) {
  emit('select', record)
}

onMounted(load)
</script>

<template>
  <a-card size="small" title="导出模板管理">
    <template #extra>
      <a-button type="primary" size="small" @click="handleAdd">新建模板</a-button>
    </template>
    <a-table :columns="columns" :data-source="templates" :loading="loading"
      row-key="id" size="small" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'isDefault'">
          <a-tag v-if="record.isDefault === 1" color="gold">默认</a-tag>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleSelect(record)">选择</a-button>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button v-if="record.isDefault !== 1" type="link" size="small" @click="handleSetDefault(record.id)">设默认</a-button>
            <a-button type="link" size="small" danger @click="handleDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <!-- 编辑弹窗 -->
  <a-modal v-model:open="editVisible" :title="editTitle" @ok="handleSave">
    <a-form :label-col="{ span: 6 }">
      <a-form-item label="模板名称"><a-input v-model:value="formName" /></a-form-item>
      <a-form-item label="导出类型">
        <a-select v-model:value="formType">
          <a-select-option value="Word">Word</a-select-option>
          <a-select-option value="PDF">PDF</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="配置JSON">
        <a-textarea v-model:value="formConfig" :rows="4" placeholder='{"titleAlign":"center","showAnswer":true,"showAnalysis":false,"scorePosition":"right"}' />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<template>
  <div>
    <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
      <span>共 {{ wrongQuestions.length }} 道高频错题</span>
      <a-button type="primary" size="small" @click="handleExportWrongExcel">
        导出错题Excel
      </a-button>
    </div>
    <a-table
      :columns="columns"
      :data-source="wrongQuestions"
      :pagination="{ pageSize: 10, showSizeChanger: false }"
      :row-key="(r: any) => r.questionId"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'difficulty'">
          <a-tag :color="getDiffColor(record.difficulty)">
            {{ record.difficultyName }}
          </a-tag>
        </template>
        <template v-if="column.key === 'questionType'">
          <a-tag color="blue">{{ record.questionTypeName }}</a-tag>
        </template>
      </template>
    </a-table>
    <a-empty v-if="wrongQuestions.length === 0" description="暂无高频错题数据" />
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import { exportWrongExcel } from '@/api/tongji'

const props = defineProps<{
  wrongQuestions: API.HighFreqWrongQuestionVO[]
  paperId: number | undefined
}>()

const columns = [
  { title: '题号', dataIndex: 'questionId', key: 'questionId', width: 80 },
  { title: '题干', dataIndex: 'questionContent', key: 'content', ellipsis: true },
  { title: '题型', dataIndex: 'questionTypeName', key: 'questionType', width: 80 },
  { title: '难度', dataIndex: 'difficultyName', key: 'difficulty', width: 80 },
  { title: '知识点', dataIndex: 'knowledgePoints', key: 'kp', width: 150, ellipsis: true },
  { title: '错误次数', dataIndex: 'wrongCount', key: 'wrongCount', width: 100 },
  { title: '失分合计', dataIndex: 'totalScoreLost', key: 'lost', width: 100 }
]

const getDiffColor = (d: number | undefined) => {
  if (d === 1) return 'green'
  if (d === 2) return 'orange'
  if (d === 3) return 'red'
  return 'default'
}

const handleExportWrongExcel = async () => {
  if (!props.paperId) {
    message.warning('请先选择试卷')
    return
  }
  try {
    const res = await exportWrongExcel(props.paperId)
    const blob = new Blob([res.data as unknown as BlobPart], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `高频错题.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}
</script>

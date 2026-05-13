<template>
  <a-card size="small">
    <a-row :gutter="16" align="middle">
      <a-col :span="8">
        <span style="margin-right: 8px;">选择试卷：</span>
        <a-select
          v-model:value="selectedId"
          style="width: 260px;"
          placeholder="请选择有考试记录的试卷"
          show-search
          :filter-option="filterOption"
          @change="handleSelect"
        >
          <a-select-option
            v-for="p in papers"
            :key="p.paperId"
            :value="p.paperId"
          >
            {{ p.paperName }} ({{ p.subject }})
          </a-select-option>
        </a-select>
      </a-col>
      <a-col :span="4">
        <a-tag v-if="papers.length > 0" color="blue">
          共 {{ papers.length }} 份可统计试卷
        </a-tag>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  papers: API.PaperStatisticsVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  query: [paperId: number]
}>()

const selectedId = ref<number | undefined>(undefined)

const filterOption = (input: string, option: any) => {
  const text = option.children?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

const handleSelect = (val: number) => {
  emit('query', val)
}
</script>

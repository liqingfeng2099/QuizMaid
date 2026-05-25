<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()

const route = useRoute()
const showLayout = computed(() => {
  return !route.path.startsWith('/user/') && !route.path.startsWith('/oauth/')
})
</script>

<template>
  <BasicLayout v-if="showLayout" />
  <router-view v-else />
</template>

<style>
/* 全局响应式基础样式 */
html { font-size: 16px; }

@media (max-width: 768px) {
  html { font-size: 14px; }
  .ant-table { font-size: 12px; }
  .ant-statistic-content { font-size: 20px !important; }
}

@media (max-width: 480px) {
  html { font-size: 13px; }
}

/* 导出格式兼容声明（Word/PDF/WPS打开无乱码） */
* { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
</style>

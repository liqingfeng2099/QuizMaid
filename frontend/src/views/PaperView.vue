<script setup lang="ts">
import { onMounted } from 'vue'
import { useLoginUserStore } from '@/stores/loginUser'
import PaperManager from '@/components/PaperManager.vue'

const loginUserStore = useLoginUserStore()

// 确保在组件挂载时获取用户信息
onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})
</script>

<template>
  <div class="page-content">
    <PaperManager />
  </div>
</template>

<style scoped>
.page-content {
  padding: 0;
}
</style>

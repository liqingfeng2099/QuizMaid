<script setup lang="ts">
import { onMounted } from 'vue'
import { useLoginUserStore } from '@/stores/loginUser'
import UserManager from '@/components/UserManager.vue'
import QuestionManager from '@/components/QuestionManager.vue'

const loginUserStore = useLoginUserStore()

// 确保在组件挂载时获取用户信息
onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})
</script>

<template>
  <div class="page-content">
    <a-tabs default-active-key="users">
      <a-tab-pane key="users" tab="用户管理">
        <UserManager />
      </a-tab-pane>
      <a-tab-pane key="questions" tab="题目管理">
        <QuestionManager />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.page-content {
  padding: 0;
}
</style>

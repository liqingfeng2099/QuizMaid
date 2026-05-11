<script setup lang="ts">
import { onMounted } from 'vue'
import { useLoginUserStore } from '@/stores/loginUser'
import ManualAssembly from '@/components/ManualAssembly.vue'
import { useRouter } from 'vue-router'

const loginUserStore = useLoginUserStore()
const router = useRouter()

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})

function handleDone() {
  router.push('/paper')
}
</script>

<template>
  <div class="page-content">
    <ManualAssembly @done="handleDone" />
  </div>
</template>

<style scoped>
.page-content {
  padding: 0;
}
</style>

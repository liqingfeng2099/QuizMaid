<template>
  <div class="github-callback-page">
    <a-spin size="large" tip="GitHub登录中..." />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { callback } from '@/api/githubOauthController'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

onMounted(async () => {
  const code = route.query.code as string

  if (!code) {
    message.error('GitHub授权失败：未获取到授权码')
    router.push('/user/login')
    return
  }

  try {
    const res = await callback({ code })
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('GitHub登录成功')
      router.push('/')
    } else {
      message.error('GitHub登录失败：' + res.data.message)
      router.push('/user/login')
    }
  } catch (error) {
    console.error(error)
    message.error('GitHub登录请求失败')
    router.push('/user/login')
  }
})
</script>

<style scoped>
.github-callback-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
</style>

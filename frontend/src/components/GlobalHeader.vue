<template>
  <a-layout-header class="global-header">
    <div class="header-left">
      <div class="logo">
        <img src="../assets/logo.png" alt="logo" />
        <span class="title">AI 智能题库管理系统</span>
      </div>
      <a-menu
        mode="horizontal"
        :selectedKeys="[currentRoute]"
        @click="handleMenuClick"
        style="border-bottom: none;"
      >
        <a-menu-item key="home">首页</a-menu-item>
        <a-menu-item key="question">试题管理</a-menu-item>
        <a-menu-item key="question-stats">题型统计</a-menu-item> <!-- 题型统计导航菜单 .hml -->
        <a-menu-item key="paper">试卷管理</a-menu-item>
        <a-sub-menu key="assembly" title="试卷组卷">
          <a-menu-item key="paper/assembly">手动组卷</a-menu-item>
          <a-menu-item key="paper/ai-assembly">AI组卷</a-menu-item>
        </a-sub-menu>
        <a-menu-item key="paper/strategy">组卷策略</a-menu-item>
        <a-menu-item key="exam">考试中心</a-menu-item>
        <a-menu-item key="error-book">错题本</a-menu-item>
        <a-menu-item key="profile">个人中心</a-menu-item>
        <a-menu-item v-if="loginUser.loginUser?.role === 'admin'" key="system">系统管理</a-menu-item>
      </a-menu>
    </div>
    <div class="header-right">
      <a-badge :count="unreadCount" :overflow-count="99" style="margin-right: 24px;">
        <BellOutlined style="font-size: 20px; cursor: pointer; color: #555;" @click="handleOpenNotifications" />
      </a-badge>
      <a-dropdown>
        <div class="user-info">
          <a-avatar :size="32">{{ loginUser.loginUser?.nickname?.charAt(0) || 'U' }}</a-avatar>
          <span class="nickname">{{ loginUser.loginUser?.nickname || '用户' }}</span>
        </div>
        <template #overlay>
          <a-menu>
            <a-menu-item key="notifications" @click="handleOpenNotifications">
              系统通知
            </a-menu-item>
            <a-menu-item key="logout">
              <a-button type="link" @click="handleLogout">退出登录</a-button>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
    <a-modal
      v-model:open="notificationVisible"
      title="系统通知"
      :footer="null"
      width="500px"
    >
      <a-list :data-source="notifications" :loading="notifLoading" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a-badge :dot="item.isRead === 0" :offset-x="0" :offset-y="4">
                  {{ item.title }}
                </a-badge>
              </template>
              <template #description>
                {{ item.content }}
                <br/><small>{{ item.createTime }}</small>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-if="notifications.length === 0 && !notifLoading" description="暂无通知" />
    </a-modal>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { logout } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getUnreadNotificationCount, getUnreadNotifications } from '@/api/shijuanguanli'

const router = useRouter()
const route = useRoute()
const loginUser = useLoginUserStore()

const notificationVisible = ref(false)
const unreadCount = ref(0)
const notifications = ref<any[]>([])
const notifLoading = ref(false)

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadNotificationCount()
    if (res.data.code === 0) {
      unreadCount.value = res.data.data || 0
    }
  } catch (e) { /* ignore */ }
}

const handleOpenNotifications = async () => {
  notificationVisible.value = true
  notifLoading.value = true
  try {
    const res = await getUnreadNotifications()
    if (res.data.code === 0) {
      notifications.value = res.data.data || []
    }
  } catch (e) { /* ignore */ }
  notifLoading.value = false
}

onMounted(() => {
  loginUser.fetchLoginUser()
  loadUnreadCount()
  setInterval(loadUnreadCount, 30000) // 每30秒轮询
})

const currentRoute = computed(() => {
  const path = route.path.startsWith('/') ? route.path.slice(1) : route.path
  return path || 'home'
})

const handleMenuClick = ({ key }: { key: string }) => {
  router.push(`/${key}`)
}

const handleLogout = async () => {
  try {
    const res = await logout()
    if (res.data.code === 0) {
      loginUser.setLoginUser({ username: '未登录' })
      message.success('已退出登录')
      router.push('/user/login')
    } else {
      message.error('退出失败：' + res.data.message)
    }
  } catch (error) {
    console.error(error)
    message.error('退出请求失败')
  }
}
</script>

<style scoped>
.global-header {
  background: #ffffff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo img {
  width: auto;
  height: 48px;
}

.logo .title {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.nickname {
  color: #666;
  font-size: 14px;
}

/* 响应式：手机端隐藏部分菜单项，缩小间距 */
@media (max-width: 768px) {
  .global-header { padding: 0 8px; }
  .header-left { gap: 8px; }
  .logo .title { font-size: 14px; }
  .logo img { height: 32px; }
}

@media (max-width: 480px) {
  .logo .title { display: none; }
  .header-right { gap: 4px; }
}
</style>

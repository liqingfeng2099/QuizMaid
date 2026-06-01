<template>
  <a-layout-sider width="220" class="global-sider">
    <a-menu 
      mode="inline" 
      class="side-menu" 
      :selectedKeys="[currentRoute]"
      @click="handleMenuClick"
    >
      <a-menu-item key="home">
        <HomeOutlined />
        <span>首页</span>
      </a-menu-item>
      <a-menu-item key="question">
        <FileTextOutlined />
        <span>试题管理</span>
      </a-menu-item>
      <a-menu-item key="paper">
        <ReadOutlined />
        <span>试卷管理</span>
      </a-menu-item>
      <a-menu-item key="paper/assembly">
        <FormOutlined />
        <span>试卷组卷</span>
      </a-menu-item>
      <a-menu-item key="paper/strategy">
        <ApartmentOutlined />
        <span>组卷策略</span>
      </a-menu-item>
      <a-menu-item key="exam">
        <AuditOutlined />
        <span>考试中心</span>
      </a-menu-item>
      <a-menu-item key="statistics">
        <BarChartOutlined />
        <span>成绩统计</span>
      </a-menu-item>
      <a-menu-item key="error-book">
        <CloseCircleOutlined />
        <span>错题本</span>
      </a-menu-item>
      <a-menu-item key="profile">
        <UserOutlined />
        <span>个人中心</span>
      </a-menu-item>
      <a-menu-item v-if="loginUserStore.loginUser?.role === 'admin'" key="system">
        <SettingOutlined />
        <span>系统管理</span>
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  HomeOutlined,
  FileTextOutlined,
  ReadOutlined,
  FormOutlined,
  ApartmentOutlined,
  AuditOutlined,
  CloseCircleOutlined,
  BarChartOutlined,
  UserOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const currentRoute = computed(() => {
  // 去掉前导 /，保留完整子路径 (如 paper/assembly)
  const path = route.path.startsWith('/') ? route.path.slice(1) : route.path
  return path || 'home'
})

const handleMenuClick = ({ key }: { key: string }) => {
  router.push(`/${key}`)
}

// 确保在组件挂载时获取用户信息
onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})
</script>

<style scoped>
.global-sider {
  background: #ffffff;
  border-right: 1px solid #f0f0f0;
  position: fixed;
  left: 0;
  top: 64px;
  bottom: 52px;
  overflow-y: auto;
}

.side-menu {
  height: 100%;
  border-right: none;
}

.side-menu :deep(.ant-menu-item) {
  margin: 4px 8px;
  padding-left: 16px !important;
  border-radius: 8px;
}

.side-menu :deep(.ant-menu-item-selected) {
  background: #e6f7ff;
  color: #1890ff;
}
</style>

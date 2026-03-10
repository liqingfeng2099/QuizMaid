<template>
  <a-layout-sider width="220" class="global-sider">
    <div class="sider-menu-wrapper">
      <a-menu mode="inline" class="side-menu">
        <a-menu-item key="dashboard">
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
        <a-menu-item key="exam">
          <AuditOutlined />
          <span>考试中心</span>
        </a-menu-item>
        <a-menu-item key="error">
          <CloseCircleOutlined />
          <span>错题本</span>
        </a-menu-item>
        <a-menu-item key="profile">
          <UserOutlined />
          <span>个人中心</span>
        </a-menu-item>
        <a-menu-item key="system">
          <SettingOutlined />
          <span>系统管理</span>
        </a-menu-item>
      </a-menu>
    </div>
  </a-layout-sider>
</template>

<script setup lang="ts">
import {
  HomeOutlined,
  FileTextOutlined,
  ReadOutlined,
  AuditOutlined,
  CloseCircleOutlined,
  UserOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'
</script>

<style scoped>
.global-sider {
  background: #ffffff;
  border-right: 1px solid #f0f0f0;
  min-height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  overflow: hidden;
  z-index: 100;
}

.sider-menu-wrapper {
  height: 100vh;
  padding-top: 64px;
  box-sizing: border-box;
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

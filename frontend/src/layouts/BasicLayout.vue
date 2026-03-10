<template>
  <a-layout class="basic-layout">
    <a-layout-header class="global-header">
      <div class="header-left">
        <div class="logo">
          <img src="../assets/logo.svg" alt="logo" />
          <span class="title">AI 智能题库管理系统</span>
        </div>
      </div>
      <div class="header-right">
        <div class="user-placeholder">
          <a-avatar :size="32">U</a-avatar>
          <span class="nickname">用户</span>
        </div>
      </div>
    </a-layout-header>
    <a-layout class="main-layout">
      <a-layout-sider width="220" class="global-sider">
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
      </a-layout-sider>
      <a-layout class="content-layout">
        <a-layout-content class="main-content">
          <router-view />
        </a-layout-content>
        <a-layout-footer class="global-footer">
          AI 智能题库管理系统 © 2026 公共教育学习平台
        </a-layout-footer>
      </a-layout>
    </a-layout>
  </a-layout>
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
.basic-layout {
  min-height: 100vh;
}

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
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo img {
  width: 36px;
  height: 36px;
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

.user-placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nickname {
  color: #666;
  font-size: 14px;
}

.main-layout {
  margin-top: 64px;
  min-height: calc(100vh - 64px);
}

.global-sider {
  background: #ffffff;
  border-right: 1px solid #f0f0f0;
  position: fixed;
  left: 0;
  top: 64px;
  bottom: 0;
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

.content-layout {
  margin-left: 220px;
  min-height: calc(100vh - 64px);
  background: #f5f7fa;
}

.main-content {
  padding: 24px;
  min-height: calc(100vh - 64px - 52px);
}

.global-footer {
  background: #ffffff;
  text-align: center;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  color: #666;
  font-size: 14px;
}
</style>

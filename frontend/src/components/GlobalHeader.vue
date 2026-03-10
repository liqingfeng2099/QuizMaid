<template>
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
</template>

<script setup lang="ts">
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
</style>

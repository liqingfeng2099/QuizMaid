<template>
  <a-layout class="layout">
    <GlobalHeader />
    <a-layout-content class="content">
      <a-breadcrumb style="margin: 16px 0">
        <a-breadcrumb-item>Home</a-breadcrumb-item>
        <a-breadcrumb-item>List</a-breadcrumb-item>
        <a-breadcrumb-item>App</a-breadcrumb-item>
      </a-breadcrumb>
      <div class="site-layout-content">
        <router-view />
      </div>
    </a-layout-content>
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.content {
  margin: 64px 50px 52px;
  padding: 24px;
  min-height: calc(100vh - 64px - 52px);
}

.site-layout-content {
  background: #fff;
  padding: 24px;
  min-height: 280px;
}

/* 平板端适配 */
@media (max-width: 992px) {
  .content {
    margin: 64px 16px 52px;
    padding: 16px;
  }
  .site-layout-content {
    padding: 16px;
  }
}

/* 手机端适配 */
@media (max-width: 768px) {
  .content {
    margin: 56px 8px 48px;
    padding: 12px;
  }
  .site-layout-content {
    padding: 12px;
  }
}
</style>

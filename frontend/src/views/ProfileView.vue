<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  updateSelf,
  bindEmail,
  resetPassword,
  sendEmail,
} from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getPersonalStats, exportPersonalStats } from '@/api/gerentongji'

const loginUserStore = useLoginUserStore()

const activeTab = ref('profile')
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

const profileForm = reactive({
  nickname: '',
  email: '',
})

const emailForm = reactive({
  email: '',
  code: '',
})

const passwordForm = reactive({
  email: '',
  code: '',
  password: '',
  checkPassword: '',
})

// ===== 学习统计 =====
const statsLoading = ref(false)
const statsPeriod = ref('30d')
const statsSubject = ref<string | undefined>(undefined)
const statSubjects = ['数学','语文','英语','物理','化学','生物','历史','地理','政治']
const personalStats = ref<API.PersonalStatsVO | null>(null)
const typeChartRef = ref<HTMLElement | null>(null)
const diffChartRef = ref<HTMLElement | null>(null)
const kpChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let charts: echarts.ECharts[] = []

const loadPersonalStats = async () => {
  statsLoading.value = true
  try {
    const res = await getPersonalStats({ subject: statsSubject.value, period: statsPeriod.value })
    if (res.data.code === 0 && res.data.data) {
      personalStats.value = res.data.data
      nextTick(() => renderCharts())
    }
  } catch (e) { /* ignore */ }
  statsLoading.value = false
}

const handleExportPersonalStats = async () => {
  try {
    const res = await exportPersonalStats({ subject: statsSubject.value, period: statsPeriod.value })
    const blob = new Blob([res.data as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = '个人统计.xlsx'; a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) { message.error('导出失败') }
}

const renderCharts = () => {
  charts.forEach(c => c.dispose()); charts = []
  const s = personalStats.value
  if (!s) return

  // 题型柱状图
  if (typeChartRef.value && s.byType) {
    const c = echarts.init(typeChartRef.value); charts.push(c)
    c.setOption({
      title: { text: '各题型正确率', textStyle: { fontSize: 14 } },
      tooltip: {}, toolbox: { feature: { saveAsImage: { title: '保存' } } },
      xAxis: { type: 'category', data: s.byType.map(t => t.dimensionKey) },
      yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
      series: [{ type: 'bar', data: s.byType.map(t => t.correctRate || 0), itemStyle: { color: '#1890ff' } }]
    })
  }
  // 难度饼图
  if (diffChartRef.value && s.byDifficulty) {
    const c = echarts.init(diffChartRef.value); charts.push(c)
    c.setOption({
      title: { text: '难度分布', textStyle: { fontSize: 14 } },
      tooltip: {}, toolbox: { feature: { saveAsImage: { title: '保存' } } },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: s.byDifficulty.map(d => ({ name: d.dimensionKey, value: d.totalCount })),
        label: { formatter: '{b}: {c}题' }
      }]
    })
  }
  // 知识点横向条形图
  if (kpChartRef.value && s.byKnowledge) {
    const c = echarts.init(kpChartRef.value); charts.push(c)
    const top = (s.byKnowledge || []).slice(0, 15)
    c.setOption({
      title: { text: '知识点正确率(Top15)', textStyle: { fontSize: 14 } },
      tooltip: {}, toolbox: { feature: { saveAsImage: { title: '保存' } } },
      grid: { left: '3%', right: '8%', containLabel: true },
      xAxis: { type: 'value', max: 100 },
      yAxis: { type: 'category', data: top.map(k => k.dimensionKey).reverse() },
      series: [{ type: 'bar', data: top.map(k => k.correctRate || 0).reverse(), itemStyle: { color: '#722ed1' } }]
    })
  }
  // 趋势折线图
  if (trendChartRef.value && s.trend) {
    const c = echarts.init(trendChartRef.value); charts.push(c)
    c.setOption({
      title: { text: '正确率趋势', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      toolbox: { feature: { saveAsImage: { title: '保存' }, dataZoom: {} } },
      xAxis: { type: 'category', data: s.trend.map(t => t.period), axisLabel: { rotate: 45 } },
      yAxis: { type: 'value', max: 100 },
      series: [{
        type: 'line', data: s.trend.map(t => t.accuracy || 0), smooth: true,
        areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[
          {offset:0,color:'rgba(24,144,255,0.3)'},{offset:1,color:'rgba(24,144,255,0.05)'}]) }
      }]
    })
  }
}

onMounted(() => {
  initProfile()
})

// 切换到学习统计tab时自动加载
watch(activeTab, (tab) => {
  if (tab === 'stats') loadPersonalStats()
})

onUnmounted(() => {
  charts.forEach(c => c.dispose())
})

const initProfile = () => {
  const user = loginUserStore.loginUser
  profileForm.nickname = user.nickname || ''
  profileForm.email = user.email || ''
}

const handleUpdateProfile = async () => {
  loading.value = true
  try {
    const res = await updateSelf({
      id: loginUserStore.loginUser.id,
      nickname: profileForm.nickname,
      email: profileForm.email,
    })
    if (res.data.code === 0) {
      message.success('更新成功')
      await loginUserStore.fetchLoginUser()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch {
    message.error('更新请求失败')
  } finally {
    loading.value = false
  }
}

const handleSendCode = async () => {
  if (!emailForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(emailForm.email)) {
    message.warning('请输入有效的邮箱地址')
    return
  }
  sendingCode.value = true
  try {
    const res = await sendEmail({ email: emailForm.email })
    if (res.data.code === 0) {
      message.success('验证码已发送')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      message.error('发送验证码失败：' + res.data.message)
    }
  } catch {
    message.error('发送验证码请求失败')
  } finally {
    sendingCode.value = false
  }
}

const handleBindEmail = async () => {
  loading.value = true
  try {
    const res = await bindEmail({
      email: emailForm.email,
      code: emailForm.code,
    })
    if (res.data.code === 0) {
      message.success('绑定成功')
      emailForm.email = ''
      emailForm.code = ''
      await loginUserStore.fetchLoginUser()
    } else {
      message.error('绑定失败：' + res.data.message)
    }
  } catch {
    message.error('绑定请求失败')
  } finally {
    loading.value = false
  }
}

const handlePasswordSendCode = async () => {
  if (!passwordForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(passwordForm.email)) {
    message.warning('请输入有效的邮箱地址')
    return
  }
  sendingCode.value = true
  try {
    const res = await sendEmail({ email: passwordForm.email })
    if (res.data.code === 0) {
      message.success('验证码已发送')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      message.error('发送验证码失败：' + res.data.message)
    }
  } catch {
    message.error('发送验证码请求失败')
  } finally {
    sendingCode.value = false
  }
}

const handleResetPassword = async () => {
  if (!passwordForm.email) {
    message.warning('请先验证邮箱')
    return
  }
  if (!passwordForm.code) {
    message.warning('请输入验证码')
    return
  }
  if (passwordForm.password !== passwordForm.checkPassword) {
    message.warning('两次输入的密码不一致')
    return
  }
  if (passwordForm.password.length < 6) {
    message.warning('密码不能小于 6 位')
    return
  }
  loading.value = true
  try {
    const res = await resetPassword({
      password: passwordForm.password,
      email: passwordForm.email,
      code: passwordForm.code,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功')
      passwordForm.password = ''
      passwordForm.checkPassword = ''
    } else {
      message.error('密码修改失败：' + res.data.message)
    }
  } catch {
    message.error('密码修改请求失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="profile-container">
    <a-card class="profile-card">
      <template #title>
        <div class="card-title">个人中心</div>
      </template>
      
      <a-tabs v-model:activeKey="activeTab" class="profile-tabs">
        <a-tab-pane key="profile" tab="基本信息">
          <a-form layout="vertical" :model="profileForm" class="profile-form">
            <a-form-item label="用户名">
              <a-input v-model:value="loginUserStore.loginUser.username" disabled />
            </a-form-item>
            <a-form-item label="昵称">
              <a-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
            </a-form-item>
            <a-form-item label="邮箱">
              <a-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
            </a-form-item>
            <a-form-item label="邮箱状态">
              <a-tag :color="loginUserStore.loginUser.emailVerified ? 'success' : 'warning'">
                {{ loginUserStore.loginUser.emailVerified ? '已验证' : '未验证' }}
              </a-tag>
            </a-form-item>
            <a-form-item label="角色">
              <a-tag color="blue">{{ loginUserStore.loginUser.role }}</a-tag>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="loading" @click="handleUpdateProfile">
                保存修改
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
        
        <a-tab-pane key="bind-email" tab="绑定邮箱">
          <a-form layout="vertical" :model="emailForm" class="profile-form">
            <a-form-item label="邮箱">
              <a-input v-model:value="emailForm.email" placeholder="请输入邮箱" />
            </a-form-item>
            <a-form-item label="验证码">
              <div class="code-input-row">
                <a-input v-model:value="emailForm.code" placeholder="请输入验证码" />
                <a-button
                  type="primary"
                  :disabled="countdown > 0"
                  :loading="sendingCode"
                  @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
                </a-button>
              </div>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="loading" @click="handleBindEmail">
                绑定邮箱
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
        
        <a-tab-pane key="password" tab="修改密码">
          <a-form layout="vertical" :model="passwordForm" class="profile-form">
            <a-form-item label="邮箱">
              <div class="code-input-row">
                <a-input v-model:value="passwordForm.email" placeholder="请输入邮箱" />
                <a-button
                  type="primary"
                  :disabled="countdown > 0"
                  :loading="sendingCode"
                  @click="handlePasswordSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
                </a-button>
              </div>
            </a-form-item>
            <a-form-item label="验证码">
              <a-input v-model:value="passwordForm.code" placeholder="请输入验证码" />
            </a-form-item>
            <a-form-item label="新密码">
              <a-input-password v-model:value="passwordForm.password" placeholder="请输入新密码" />
            </a-form-item>
            <a-form-item label="确认密码">
              <a-input-password v-model:value="passwordForm.checkPassword" placeholder="请确认新密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="loading" @click="handleResetPassword">
                修改密码
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="stats" tab="学习统计">
          <div style="margin-bottom: 12px;">
            <a-space>
              <a-select v-model:value="statsPeriod" style="width: 120px;" @change="loadPersonalStats">
                <a-select-option value="7d">近7天</a-select-option>
                <a-select-option value="30d">近30天</a-select-option>
                <a-select-option value="90d">近90天</a-select-option>
                <a-select-option value="all">全部</a-select-option>
              </a-select>
              <a-select v-model:value="statsSubject" style="width: 120px;" allow-clear placeholder="全部学科" @change="loadPersonalStats">
                <a-select-option v-for="s in statSubjects" :key="s" :value="s">{{ s }}</a-select-option>
              </a-select>
              <a-button type="primary" size="small" @click="loadPersonalStats">刷新</a-button>
              <a-button size="small" @click="handleExportPersonalStats">导出Excel</a-button>
            </a-space>
          </div>
          <a-spin :spinning="statsLoading">
            <a-row :gutter="16" style="margin-bottom: 16px;">
              <a-col :span="8"><a-statistic title="总答题数" :value="personalStats?.totalAnswers || 0" /></a-col>
              <a-col :span="8"><a-statistic title="正确数" :value="personalStats?.totalCorrect || 0" /></a-col>
              <a-col :span="8"><a-statistic title="总正确率" :value="personalStats?.totalAccuracy || 0" suffix="%" :precision="1" /></a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12"><div ref="typeChartRef" style="height: 280px;"></div></a-col>
              <a-col :span="12"><div ref="diffChartRef" style="height: 280px;"></div></a-col>
            </a-row>
            <a-row :gutter="16" style="margin-top: 16px;">
              <a-col :span="12"><div ref="kpChartRef" style="height: 300px;"></div></a-col>
              <a-col :span="12"><div ref="trendChartRef" style="height: 300px;"></div></a-col>
            </a-row>
          </a-spin>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<style scoped>
.profile-container {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 18px;
  font-weight: 500;
}

.profile-tabs {
  margin-top: 16px;
}

.profile-form {
  max-width: 500px;
}

.code-input-row {
  display: flex;
  gap: 8px;
}

.code-input-row .ant-input {
  flex: 1;
}
</style>

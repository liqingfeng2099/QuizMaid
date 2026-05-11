<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  updateSelf,
  bindEmail,
  resetPassword,
  sendEmail,
} from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

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

onMounted(() => {
  initProfile()
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

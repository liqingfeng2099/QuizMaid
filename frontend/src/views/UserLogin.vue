<template>
  <div id="userLoginPage">
    <h2 class="title">AI 智能题库管理系统 - 用户登录</h2>
    <div class="desc">高效管理题库，轻松考试</div>
    <div class="login-tabs">
      <span
        :class="['tab-item', { active: loginType === 'account' }]"
        @click="loginType = 'account'"
      >账号登录</span>
      <span
        :class="['tab-item', { active: loginType === 'email' }]"
        @click="loginType = 'email'"
      >邮箱登录</span>
    </div>

    <a-form
      v-if="loginType === 'account'"
      :model="accountForm"
      name="accountForm"
      autocomplete="off"
      @finish="handleAccountLogin"
    >
      <a-form-item name="username" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="accountForm.username" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码不能小于 6 位' },
        ]"
      >
        <a-input-password v-model:value="accountForm.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <div class="extra-row">
        <div class="github-login" @click="handleGithubLogin">
          <github-outlined class="github-icon" />
          <span>GitHub登录</span>
        </div>
        <div class="tips">
          没有账号？
          <a href="/user/register">去注册</a>
        </div>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>

    <a-form
      v-else
      :model="emailForm"
      name="emailForm"
      autocomplete="off"
      @finish="handleEmailLogin"
    >
      <a-form-item
        name="email"
        :rules="[
          { required: true, message: '请输入邮箱' },
          { type: 'email', message: '请输入有效的邮箱地址' },
        ]"
      >
        <a-input v-model:value="emailForm.email" placeholder="请输入邮箱" />
      </a-form-item>
      <a-form-item
        name="code"
        :rules="[{ required: true, message: '请输入验证码' }]"
      >
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
      <div class="extra-row">
        <div class="github-login" @click="handleGithubLogin">
          <github-outlined class="github-icon" />
          <span>GitHub登录</span>
        </div>
        <div class="tips">
          没有账号？
          <a href="/user/register">去注册</a>
        </div>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { GithubOutlined } from '@ant-design/icons-vue'
import { login, emailLogin, sendEmail } from '@/api/userController'
import { githubLogin } from '@/api/githubOauthController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const loginType = ref<'account' | 'email'>('account')

const accountForm = reactive({
  username: '',
  userPassword: '',
})

const emailForm = reactive({
  email: '',
  code: '',
})

const countdown = ref(0)
const sendingCode = ref(false)

const handleAccountLogin = async (values: { username: string; userPassword: string }) => {
  try {
    const res = await login(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.push('/')
    } else {
      message.error('登录失败：' + res.data.message)
    }
  } catch {
    message.error('登录请求失败')
  }
}

const handleEmailLogin = async (values: { email: string; code: string }) => {
  try {
    const res = await emailLogin(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.push('/')
    } else {
      message.error('登录失败：' + res.data.message)
    }
  } catch {
    message.error('登录请求失败')
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

const handleGithubLogin = async () => {
  try {
    const res = await githubLogin()
    if (res.data.code === 0 && res.data.data) {
      window.location.href = res.data.data
    } else {
      message.error('GitHub登录失败：' + res.data.message)
    }
  } catch {
    message.error('GitHub登录请求失败')
  }
}
</script>

<style scoped>
#userLoginPage {
  max-width: 360px;
  margin: 100px auto;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  font-size: 24px;
  color: #1890ff;
  margin-bottom: 8px;
}

.desc {
  text-align: center;
  color: #999;
  margin-bottom: 24px;
}

.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
  border-bottom: 1px solid #e8e8e8;
}

.tab-item {
  padding: 8px 24px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.tab-item:hover {
  color: #1890ff;
}

.tab-item.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.code-input-row {
  display: flex;
  gap: 8px;
}

.code-input-row .ant-input {
  flex: 1;
}

.extra-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.github-login {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: color 0.3s;
}

.github-login:hover {
  color: #1890ff;
}

.github-icon {
  font-size: 18px;
}

.tips {
  font-size: 14px;
  color: #666;
}

.tips a {
  color: #1890ff;
}
</style>

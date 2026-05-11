<template>
  <div id="userRegisterPage">
    <h2 class="title">AI 智能题库管理系统 - 用户注册</h2>
    <div class="desc">高效管理题库，轻松考试</div>
    <div class="login-tabs">
      <span
        :class="['tab-item', { active: registerType === 'account' }]"
        @click="registerType = 'account'"
      >账号注册</span>
      <span
        :class="['tab-item', { active: registerType === 'email' }]"
        @click="registerType = 'email'"
      >邮箱注册</span>
    </div>

    <a-form
      v-if="registerType === 'account'"
      :model="accountForm"
      name="accountForm"
      autocomplete="off"
      @finish="handleAccountRegister"
    >
      <a-form-item
        name="userName"
        :rules="[{ required: true, message: '请输入账号' }]"
      >
        <a-input v-model:value="accountForm.userName" placeholder="请输入账号" />
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
      <a-form-item
        name="checkUserPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { validator: validateAccountPasswordMatch, trigger: 'change' },
        ]"
      >
        <a-input-password v-model:value="accountForm.checkUserPassword" placeholder="请确认密码" />
      </a-form-item>
      <div class="tips">
        已有账号？
        <a href="/user/login">去登录</a>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>

    <a-form
      v-else
      :model="emailForm"
      name="emailForm"
      autocomplete="off"
      @finish="handleEmailRegister"
    >
      <a-form-item
        name="userName"
        :rules="[{ required: true, message: '请输入账号' }]"
      >
        <a-input v-model:value="emailForm.userName" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码不能小于 6 位' },
        ]"
      >
        <a-input-password v-model:value="emailForm.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkUserPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { validator: validateEmailPasswordMatch, trigger: 'change' },
        ]"
      >
        <a-input-password v-model:value="emailForm.checkUserPassword" placeholder="请确认密码" />
      </a-form-item>
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
      <div class="tips">
        已有账号？
        <a href="/user/login">去登录</a>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { register, emailRegister, sendEmail } from '@/api/userController'

const router = useRouter()

const registerType = ref<'account' | 'email'>('account')

const accountForm = reactive({
  userName: '',
  userPassword: '',
  checkUserPassword: '',
})

const emailForm = reactive({
  userName: '',
  userPassword: '',
  checkUserPassword: '',
  email: '',
  code: '',
})

const countdown = ref(0)
const sendingCode = ref(false)

const validateAccountPasswordMatch = (_: unknown, value: string) => {
  if (value !== accountForm.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const validateEmailPasswordMatch = (_: unknown, value: string) => {
  if (value !== emailForm.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const handleAccountRegister = async (values: { userName: string; userPassword: string; checkUserPassword: string }) => {
  try {
    const res = await register(values)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功')
      router.push('/user/login')
    } else {
      message.error('注册失败：' + res.data.message)
    }
  } catch {
    message.error('注册请求失败')
  }
}

const handleEmailRegister = async (values: { userName: string; userPassword: string; checkUserPassword: string; email: string; code: string }) => {
  try {
    const res = await emailRegister(values)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功')
      router.push('/user/login')
    } else {
      message.error('注册失败：' + res.data.message)
    }
  } catch {
    message.error('注册请求失败')
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
</script>

<style scoped>
#userRegisterPage {
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

.tips {
  text-align: right;
  margin-bottom: 24px;
  font-size: 14px;
  color: #666;
}

.tips a {
  color: #1890ff;
}
</style>

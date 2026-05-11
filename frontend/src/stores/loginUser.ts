import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLoginUserInfo } from '@/api/userController.ts'


export const useLoginUserStore = defineStore('loginUser', () => {
  // 默认值
  const loginUser = ref<API.UserVO>({
    username: '未登录',
  })

  // 获取登录用户信息
  async function fetchLoginUser() {
    const res = await getLoginUserInfo()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    }
  }
  // 更新登录用户信息
  function setLoginUser(newLoginUser: API.UserVO) {
    loginUser.value = newLoginUser
  }

  return { loginUser, setLoginUser, fetchLoginUser }
})

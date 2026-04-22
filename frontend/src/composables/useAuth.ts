import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login as loginApi, logout as logoutApi, type LoginRequest } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { parseAccessTokenUserInfo } from '@/utils/jwt'

export function useAuth() {
  const userStore = useUserStore()
  const router = useRouter()

  const isLoggedIn = computed(() => userStore.isLoggedIn)

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    userStore.setTokens(res.accessToken, res.refreshToken)
    userStore.setUserInfo(parseAccessTokenUserInfo(res.accessToken))
    ElMessage.success('登录成功')
    const redirect = (router.currentRoute.value.query.redirect as string) || '/'
    await router.replace(redirect)
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      userStore.clearAuth()
      await router.replace({ name: 'login' })
      ElMessage.success('已退出登录')
    }
  }

  return { isLoggedIn, login, logout }
}

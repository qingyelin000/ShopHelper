import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login as loginApi, logout as logoutApi, type LoginRequest } from '@/api/auth'
import { ElMessage } from 'element-plus'

export function useAuth() {
  const userStore = useUserStore()
  const router = useRouter()

  const isLoggedIn = computed(() => userStore.isLoggedIn)

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    userStore.setTokens(res.accessToken, res.refreshToken)
    userStore.setUserInfo(extractUserInfo(res.accessToken))
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

function extractUserInfo(accessToken: string): { userId: string; nickname: string } {
  const payloadSegment = accessToken.split('.')[1]
  if (!payloadSegment) {
    throw new Error('accessToken 格式不正确')
  }

  const normalizedBase64 = payloadSegment.replace(/-/g, '+').replace(/_/g, '/')
  const paddedBase64 = normalizedBase64.padEnd(Math.ceil(normalizedBase64.length / 4) * 4, '=')
  const payload = JSON.parse(atob(paddedBase64)) as { sub?: string; username?: string }

  if (!payload.sub || !payload.username) {
    throw new Error('accessToken 缺少用户信息')
  }

  return {
    userId: String(payload.sub),
    nickname: String(payload.username),
  }
}

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { parseAccessTokenUserInfo } from '@/utils/jwt'
import type { ParsedAccessTokenUserInfo } from '@/utils/jwt'
import type { UserRole } from '@/types/user.d'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(sessionStorage.getItem('accessToken') || '')
  const refreshToken = ref(sessionStorage.getItem('refreshToken') || '')
  const parsedUserInfo = resolveStoredUserInfo(accessToken.value)
  const userId = ref(sessionStorage.getItem('userId') || parsedUserInfo?.userId || '')
  const nickname = ref(sessionStorage.getItem('nickname') || parsedUserInfo?.nickname || '')
  const role = ref<UserRole | ''>((sessionStorage.getItem('role') as UserRole | null) || parsedUserInfo?.role || '')

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    sessionStorage.setItem('accessToken', access)
    sessionStorage.setItem('refreshToken', refresh)
  }

  function setUserInfo(info: { userId: string; nickname: string; role: UserRole }) {
    userId.value = info.userId
    nickname.value = info.nickname
    role.value = info.role
    sessionStorage.setItem('userId', info.userId)
    sessionStorage.setItem('nickname', info.nickname)
    sessionStorage.setItem('role', info.role)
  }

  function clearAuth() {
    accessToken.value = ''
    refreshToken.value = ''
    userId.value = ''
    nickname.value = ''
    role.value = ''
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('nickname')
    sessionStorage.removeItem('role')
  }

  return { accessToken, refreshToken, userId, nickname, role, isLoggedIn, isAdmin, setTokens, setUserInfo, clearAuth }
})

function resolveStoredUserInfo(accessToken: string): ParsedAccessTokenUserInfo | null {
  if (!accessToken) {
    return null
  }
  try {
    return parseAccessTokenUserInfo(accessToken)
  } catch {
    return null
  }
}

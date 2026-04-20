import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(sessionStorage.getItem('accessToken') || '')
  const refreshToken = ref(sessionStorage.getItem('refreshToken') || '')
  const userId = ref(sessionStorage.getItem('userId') || '')
  const nickname = ref(sessionStorage.getItem('nickname') || '')

  const isLoggedIn = computed(() => !!accessToken.value)

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    sessionStorage.setItem('accessToken', access)
    sessionStorage.setItem('refreshToken', refresh)
  }

  function setUserInfo(info: { userId: string; nickname: string }) {
    userId.value = info.userId
    nickname.value = info.nickname
    sessionStorage.setItem('userId', info.userId)
    sessionStorage.setItem('nickname', info.nickname)
  }

  function clearAuth() {
    accessToken.value = ''
    refreshToken.value = ''
    userId.value = ''
    nickname.value = ''
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('nickname')
  }

  return { accessToken, refreshToken, userId, nickname, isLoggedIn, setTokens, setUserInfo, clearAuth }
})

import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import type { Result, ApiError } from '@/types/api.d'
import { ErrorCode } from '@/types/api.d'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/** 生成请求 ID */
function generateRequestId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

/** 专用于刷新 Token 的独立 Axios 实例（避免拦截器递归） */
const refreshClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 10_000,
})

/** 单飞刷新 Promise：防止多个 401 同时触发多次刷新 */
let refreshPromise: Promise<string> | null = null

async function doRefreshToken(): Promise<string> {
  const userStore = useUserStore()
  const { data } = await refreshClient.post<Result<{ accessToken: string; refreshToken: string }>>(
    '/v1/auth/refresh',
    { refreshToken: userStore.refreshToken },
  )
  if (data.code === ErrorCode.SUCCESS && data.data) {
    const { accessToken, refreshToken } = data.data
    userStore.setTokens(accessToken, refreshToken)
    return accessToken
  }
  throw new Error(data.message || 'Token 刷新失败')
}

/** 主 Axios 实例 */
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

/* ---- 请求拦截器 ---- */
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // 注入 X-Request-Id
  config.headers.set('X-Request-Id', generateRequestId())

  // 注入 JWT
  const userStore = useUserStore()
  if (userStore.accessToken) {
    config.headers.set('Authorization', `Bearer ${userStore.accessToken}`)
  }
  return config
})

/* ---- 响应拦截器 ---- */
request.interceptors.response.use(
  async (response: AxiosResponse<Result>) => {
    const res = response.data

    // 成功：解包返回 data
    if (res.code === ErrorCode.SUCCESS) {
      return res.data as any
    }

    // 未授权：尝试刷新 Token
    if (res.code === ErrorCode.UNAUTHORIZED) {
      const originalConfig = response.config as InternalAxiosRequestConfig & { _retry?: boolean }

      // 避免对登录/刷新接口本身重试
      if (originalConfig.url?.includes('/auth/refresh') || originalConfig.url?.includes('/auth/login')) {
        return Promise.reject(toApiError(res))
      }

      // 已重试过则放弃
      if (originalConfig._retry) {
        forceLogout()
        return Promise.reject(toApiError(res))
      }

      originalConfig._retry = true

      try {
        // 单飞：复用同一个刷新 Promise
        if (!refreshPromise) {
          refreshPromise = doRefreshToken().finally(() => {
            refreshPromise = null
          })
        }
        const newToken = await refreshPromise
        originalConfig.headers.set('Authorization', `Bearer ${newToken}`)
        return request(originalConfig)
      } catch {
        forceLogout()
        return Promise.reject(toApiError(res))
      }
    }

    // 其他业务错误
    return Promise.reject(toApiError(res))
  },
  (error) => {
    // 网络错误 / 超时
    const apiError: ApiError = {
      code: error.response?.status ?? -1,
      message: error.message || '网络异常，请稍后重试',
      requestId: '',
    }
    return Promise.reject(apiError)
  },
)

function toApiError(res: Result): ApiError {
  return { code: res.code, message: res.message, requestId: res.requestId }
}

function forceLogout() {
  const userStore = useUserStore()
  userStore.clearAuth()
  router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
}

export default request

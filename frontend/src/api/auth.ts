import request from './request'

export interface LoginRequest {
  phone: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: string
  nickname: string
}

export interface RegisterRequest {
  phone: string
  password: string
  nickname: string
}

/** 登录 */
export function login(data: LoginRequest) {
  return request.post<any, LoginResponse>('/v1/auth/login', data)
}

/** 注册 */
export function register(data: RegisterRequest) {
  return request.post<any, void>('/v1/auth/register', data)
}

/** 登出 */
export function logout() {
  return request.post<any, void>('/v1/auth/logout')
}

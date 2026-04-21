import request from './request'

export interface LoginRequest {
  phone: string
  password: string
}

interface LoginApiRequest {
  loginType: 'phone'
  principal: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export interface RegisterRequest {
  phone: string
  password: string
  nickname: string
}

/** 登录 */
export function login(data: LoginRequest) {
  const payload: LoginApiRequest = {
    loginType: 'phone',
    principal: data.phone.trim(),
    password: data.password,
  }
  return request.post<any, LoginResponse>('/v1/auth/login', payload)
}

/** 注册 */
export function register(data: RegisterRequest) {
  return request.post<any, void>('/v1/users/register', data)
}

/** 登出 */
export function logout() {
  return request.post<any, void>('/v1/auth/logout')
}

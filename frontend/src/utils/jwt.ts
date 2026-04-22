import type { UserRole } from '@/types/user.d'

export interface ParsedAccessTokenUserInfo {
  userId: string
  nickname: string
  role: UserRole
}

export function parseAccessTokenUserInfo(accessToken: string): ParsedAccessTokenUserInfo {
  const payloadSegment = accessToken.split('.')[1]
  if (!payloadSegment) {
    throw new Error('accessToken 格式不正确')
  }

  const normalizedBase64 = payloadSegment.replace(/-/g, '+').replace(/_/g, '/')
  const paddedBase64 = normalizedBase64.padEnd(Math.ceil(normalizedBase64.length / 4) * 4, '=')
  const payload = JSON.parse(atob(paddedBase64)) as { sub?: string; username?: string; role?: string }

  if (!payload.sub || !payload.username || !payload.role) {
    throw new Error('accessToken 缺少用户信息')
  }

  const normalizedRole = String(payload.role).toUpperCase()
  if (normalizedRole !== 'USER' && normalizedRole !== 'ADMIN') {
    throw new Error('accessToken 角色信息非法')
  }
  const role: UserRole = normalizedRole

  return {
    userId: String(payload.sub),
    nickname: String(payload.username),
    role,
  }
}

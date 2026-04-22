import request from './request'
import type {
  AdminBootstrapRequest,
  AdminUser,
  AdminUserPage,
  AdminUserQuery,
  CreateUserAddressRequest,
  UpdateAdminUserRoleRequest,
  UpdateUserAddressRequest,
  UserAddress,
  UserProfile,
} from '@/types/user.d'

/** 获取当前用户画像 */
export function getMyProfile() {
  return request.get<any, UserProfile>('/v1/users/me/profile')
}

/** 获取当前用户地址列表 */
export function listMyAddresses() {
  return request.get<any, UserAddress[]>('/v1/users/me/addresses')
}

/** 新增地址 */
export function createMyAddress(data: CreateUserAddressRequest) {
  return request.post<any, UserAddress>('/v1/users/me/addresses', data)
}

/** 更新地址 */
export function updateMyAddress(addressId: string, data: UpdateUserAddressRequest) {
  return request.put<any, UserAddress>(`/v1/users/me/addresses/${addressId}`, data)
}

/** 删除地址 */
export function deleteMyAddress(addressId: string) {
  return request.delete<any, void>(`/v1/users/me/addresses/${addressId}`)
}

/** 设为默认地址 */
export function setDefaultMyAddress(addressId: string) {
  return request.put<any, void>(`/v1/users/me/addresses/${addressId}/default`)
}

/** 初始化首个管理员 */
export function bootstrapAdmin(data: AdminBootstrapRequest, bootstrapToken: string) {
  return request.post<any, AdminUser>('/v1/users/bootstrap/admin', data, {
    headers: {
      'X-Bootstrap-Token': bootstrapToken,
    },
  })
}

/** 管理员查询用户列表 */
export function listAdminUsers(params: AdminUserQuery) {
  return request.get<any, AdminUserPage>('/v1/users/admin', { params })
}

/** 管理员更新用户角色 */
export function updateAdminUserRole(userId: string, data: UpdateAdminUserRoleRequest) {
  return request.put<any, AdminUser>(`/v1/users/admin/${userId}/role`, data)
}

import request from './request'
import type {
  CreateUserAddressRequest,
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

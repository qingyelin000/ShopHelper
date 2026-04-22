export type UserRole = 'USER' | 'ADMIN'

export interface PreferredCategory {
  categoryId: string
  categoryName: string
  weight: number
}

export interface PriceBandPreference {
  min: number
  max: number
  typical: number
}

export interface UserProfile {
  userId: string
  preferredCategories: PreferredCategory[]
  priceBandPreference: PriceBandPreference
  recentBrowseCount: number
  purchaseCount: number
}

export interface UserAddress {
  id: string
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode?: string | null
  isDefault: boolean
}

export interface CreateUserAddressRequest {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode?: string
  isDefault?: boolean
}

export interface UpdateUserAddressRequest {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode?: string
}

export interface AdminBootstrapRequest {
  username: string
}

export interface AdminUser {
  userId: string
  username: string
  role: UserRole
  status: number
  createTime: string
  updateTime: string
}

export interface AdminUserQuery {
  keyword?: string
  role?: UserRole
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface AdminUserPage {
  list: AdminUser[]
  total: number
  pageNum: number
  pageSize: number
  hasNext: boolean
}

export interface UpdateAdminUserRoleRequest {
  role: UserRole
}

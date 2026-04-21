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

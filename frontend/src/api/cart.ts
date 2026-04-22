import request from './request'

export interface CartItem {
  itemId: string
  productId: string
  skuId: string
  productName: string
  productImage: string
  skuSpec: Record<string, string | number | boolean | null>
  unitPrice: number
  quantity: number
  totalPrice: number
  stock: number
  selected: boolean
  isAvailable: boolean
}

export interface CartSnapshot {
  items: CartItem[]
  selectedCount: number
  totalQuantity: number
  estimatedTotal: number
}

/** 获取购物车列表 */
export function getCart() {
  return request.get<any, CartSnapshot>('/v1/cart')
}

/** 添加购物车 */
export function addToCart(data: { skuId: string; quantity: number }) {
  return request.post<any, CartSnapshot>('/v1/cart/items', data)
}

/** 更新购物车项 */
export function updateCartItem(itemId: string, data: { quantity?: number; selected?: boolean }) {
  return request.put<any, CartSnapshot>(`/v1/cart/items/${itemId}`, data)
}

/** 删除购物车项 */
export function removeCartItem(itemId: string) {
  return request.delete<any, CartSnapshot>(`/v1/cart/items/${itemId}`)
}

/** 清空购物车 */
export function clearCart() {
  return request.delete<any, CartSnapshot>('/v1/cart')
}

import request from './request'

export interface CartItem {
  id: string
  productId: string
  skuId: string
  productName: string
  productImage: string
  specJson: string
  price: number
  quantity: number
  checked: boolean
}

/** 获取购物车列表 */
export function getCart() {
  return request.get<any, CartItem[]>('/v1/cart')
}

/** 添加购物车 */
export function addToCart(data: { skuId: string; quantity: number }) {
  return request.post<any, void>('/v1/cart', data)
}

/** 更新购物车数量 */
export function updateCartItem(skuId: string, quantity: number) {
  return request.put<any, void>(`/v1/cart/${skuId}`, { quantity })
}

/** 删除购物车项 */
export function removeCartItem(skuId: string) {
  return request.delete<any, void>(`/v1/cart/${skuId}`)
}

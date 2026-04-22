import request from './request'
import type { ProductDetail, ProductSummary, SearchProductPage } from '@/types/product.d'
import type { PageResult } from '@/types/api.d'

/** 商品详情 */
export function getProduct(id: string) {
  return request.get<any, ProductDetail>(`/v1/products/${id}`)
}

/** 商品列表 */
export function listProducts(params: { categoryId?: string; pageNum?: number; pageSize?: number }) {
  return request.get<any, PageResult<ProductSummary>>('/v1/products', { params })
}

/** 搜索商品 */
export function searchProducts(params: {
  keyword?: string
  categoryId?: string
  priceMin?: number
  priceMax?: number
  pageNum?: number
  pageSize?: number
  sortBy?: 'default' | 'price' | 'sales' | 'new'
  sortOrder?: 'asc' | 'desc'
}) {
  return request.get<any, SearchProductPage>('/v1/search/products', { params })
}

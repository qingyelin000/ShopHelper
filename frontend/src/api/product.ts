import request from './request'
import type { Product, SearchProduct } from '@/types/product.d'
import type { PageResult } from '@/types/api.d'

/** 商品详情 */
export function getProduct(id: string) {
  return request.get<any, Product>(`/v1/products/${id}`)
}

/** 分类商品列表 */
export function listProducts(params: { categoryId?: string; pageNum?: number; pageSize?: number }) {
  return request.get<any, PageResult<Product>>('/v1/products', { params })
}

/** 搜索商品 */
export function searchProducts(params: { keyword: string; pageNum?: number; pageSize?: number }) {
  return request.get<any, PageResult<SearchProduct>>('/v1/search/products', { params })
}

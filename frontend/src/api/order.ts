import request from './request'
import type { Order, CreateOrderRequest } from '@/types/order.d'
import type { PageResult } from '@/types/api.d'

/** 创建订单 */
export function createOrder(data: CreateOrderRequest) {
  return request.post<any, { orderId: string }>('/v1/orders', data)
}

/** 订单列表 */
export function listOrders(params: { status?: number; pageNum?: number; pageSize?: number }) {
  return request.get<any, PageResult<Order>>('/v1/orders', { params })
}

/** 订单详情 */
export function getOrder(id: string) {
  return request.get<any, Order>(`/v1/orders/${id}`)
}

/** 取消订单 */
export function cancelOrder(id: string) {
  return request.put<any, void>(`/v1/orders/${id}/cancel`)
}

/** 模拟支付 */
export function payOrder(id: string) {
  return request.put<any, void>(`/v1/orders/${id}/pay`)
}

/** 确认收货 */
export function confirmOrder(id: string) {
  return request.put<any, void>(`/v1/orders/${id}/confirm`)
}

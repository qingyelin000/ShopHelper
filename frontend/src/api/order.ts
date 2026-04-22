import request from './request'
import type { PageResult } from '@/types/api.d'
import type {
  CancelOrderRequest,
  CancelOrderResponse,
  CreateOrderRequest,
  CreateOrderResponse,
  OrderDetail,
  OrderStatus,
  OrderSummary,
  PayOrderRequest,
  PayOrderResponse,
} from '@/types/order.d'

/** 创建订单 */
export function createOrder(data: CreateOrderRequest) {
  return request.post<any, CreateOrderResponse>('/v1/orders', data, {
    headers: {
      'Idempotency-Key': createIdempotencyKey('create'),
    },
  })
}

/** 订单列表 */
export function listOrders(params: { status?: OrderStatus; pageNum?: number; pageSize?: number }) {
  return request.get<any, PageResult<OrderSummary>>('/v1/orders', { params })
}

/** 订单详情 */
export function getOrder(orderId: string) {
  return request.get<any, OrderDetail>(`/v1/orders/${orderId}`)
}

/** 取消订单 */
export function cancelOrder(orderId: string, data: CancelOrderRequest) {
  return request.post<any, CancelOrderResponse>(`/v1/orders/${orderId}/cancel`, data)
}

/** 模拟支付 */
export function payOrder(orderId: string, data: PayOrderRequest) {
  return request.post<any, PayOrderResponse>(`/v1/orders/${orderId}/pay`, data, {
    headers: {
      'Idempotency-Key': createIdempotencyKey('pay'),
    },
  })
}

/** 确认收货 */
export function confirmOrder(orderId: string) {
  return request.post<any, void>(`/v1/orders/${orderId}/confirm`)
}

function createIdempotencyKey(action: 'create' | 'pay') {
  return `idem_${action}_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDING'
  | 'REFUNDED'

export const OrderStatusLabel: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}

export type OrderPaymentMethod = 'ALIPAY' | 'WECHAT' | 'MOCK'
export type OrderSource = 'app' | 'web' | 'mini'

export interface OrderSummary {
  orderId: string
  orderNo: string
  orderStatus: OrderStatus
  payAmount: number
  itemCount: number
  coverImage: string | null
  createTime: string
  expireTime: string | null
}

export interface OrderAddressSnapshot {
  receiverName: string
  receiverPhone: string
  fullAddress: string
}

export interface OrderItem {
  itemId: string
  productId: string
  productName: string
  productImage: string | null
  skuSpec: Record<string, string | number | boolean | null>
  unitPrice: number
  quantity: number
  totalPrice: number
}

export interface OrderDetail {
  orderId: string
  orderNo: string
  orderStatus: OrderStatus
  totalAmount: number
  freightAmount: number
  payAmount: number
  payType: OrderPaymentMethod | null
  source: OrderSource
  remark: string | null
  address: OrderAddressSnapshot
  items: OrderItem[]
  logistics: unknown | null
  createTime: string
  payTime: string | null
}

export interface CreateOrderRequest {
  addressId: string
  items: Array<{
    skuId: string
    quantity: number
  }>
  remark?: string
  source: OrderSource
}

export interface CreateOrderResponse {
  orderId: string
  orderNo: string
  orderStatus: OrderStatus
  totalAmount: number
  payAmount: number
  freightAmount: number
  expireTime: string | null
  createTime: string
}

export interface CancelOrderRequest {
  reason: string
}

export interface CancelOrderResponse {
  orderId: string
  orderNo: string
  orderStatus: OrderStatus
  cancelTime: string
  refundNote?: string | null
}

export interface PayOrderRequest {
  paymentMethod: OrderPaymentMethod
}

export interface PayOrderResponse {
  orderId: string
  orderNo: string
  paymentMethod: OrderPaymentMethod
  paymentSessionId: string
  payUrl: string | null
  qrCode: string | null
  note: string | null
}

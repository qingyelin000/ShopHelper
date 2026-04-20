/** 订单状态 */
export enum OrderStatus {
  PENDING_PAY = 0,
  PAID = 1,
  SHIPPED = 2,
  COMPLETED = 3,
  CANCELLED = 4,
}

/** 订单状态标签映射 */
export const OrderStatusLabel: Record<OrderStatus, string> = {
  [OrderStatus.PENDING_PAY]: '待付款',
  [OrderStatus.PAID]: '已付款',
  [OrderStatus.SHIPPED]: '已发货',
  [OrderStatus.COMPLETED]: '已完成',
  [OrderStatus.CANCELLED]: '已取消',
}

/** 订单项 */
export interface OrderItem {
  id: string
  orderId: string
  productId: string
  skuId: string
  productName: string
  productImage: string
  specJson: string
  price: number
  quantity: number
}

/** 订单 */
export interface Order {
  id: string
  userId: string
  totalAmount: number
  payAmount: number
  status: OrderStatus
  addressSnapshot: string
  remark: string
  payTime: string | null
  shipTime: string | null
  completeTime: string | null
  items: OrderItem[]
  createdAt: string
}

/** 下单请求 */
export interface CreateOrderRequest {
  addressId: string
  cartItemIds: string[]
  remark?: string
}

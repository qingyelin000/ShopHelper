import request from './request'

export interface SeckillActivity {
  id: string
  productId: string
  productName: string
  productImage: string
  originalPrice: number
  seckillPrice: number
  totalStock: number
  availableStock: number
  startTime: string
  endTime: string
  status: number
}

/** 秒杀活动列表 */
export function listSeckillActivities() {
  return request.get<any, SeckillActivity[]>('/v1/seckill/activities')
}

/** 秒杀下单 */
export function seckillOrder(activityId: string, addressId: string) {
  return request.post<any, { orderId: string }>(`/v1/seckill/${activityId}/order`, { addressId })
}

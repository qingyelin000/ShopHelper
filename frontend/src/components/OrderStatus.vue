<script setup lang="ts">
import { OrderStatus, OrderStatusLabel } from '@/types/order.d'

const props = defineProps<{
  status: OrderStatus
}>()

const typeMap: Record<OrderStatus, 'warning' | 'success' | 'primary' | 'info' | 'danger'> = {
  [OrderStatus.PENDING_PAY]: 'warning',
  [OrderStatus.PAID]: 'primary',
  [OrderStatus.SHIPPED]: 'primary',
  [OrderStatus.COMPLETED]: 'success',
  [OrderStatus.CANCELLED]: 'info',
}
</script>

<template>
  <el-tag :type="typeMap[props.status]" size="small">
    {{ OrderStatusLabel[props.status] }}
  </el-tag>
</template>

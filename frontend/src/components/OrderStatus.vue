<script setup lang="ts">
import type { OrderStatus } from '@/types/order.d'
import { OrderStatusLabel } from '@/types/order.d'

const props = defineProps<{
  status: OrderStatus
}>()

const typeMap: Record<OrderStatus, 'warning' | 'success' | 'primary' | 'info' | 'danger'> = {
  PENDING_PAYMENT: 'warning',
  PAID: 'primary',
  SHIPPED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
  REFUNDING: 'danger',
  REFUNDED: 'success',
}
</script>

<template>
  <el-tag :type="typeMap[props.status]" size="small">
    {{ OrderStatusLabel[props.status] }}
  </el-tag>
</template>

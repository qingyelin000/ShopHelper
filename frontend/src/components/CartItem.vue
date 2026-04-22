<script setup lang="ts">
import type { CartItem } from '@/api/cart'
import { formatPrice } from '@/utils'

defineProps<{
  item: CartItem
}>()

defineEmits<{
  updateQuantity: [itemId: string, quantity: number]
  remove: [itemId: string]
  toggleCheck: [itemId: string, selected: boolean]
}>()

function formatSkuSpec(spec: CartItem['skuSpec']) {
  const labels = Object.entries(spec ?? {}).map(([key, value]) => `${key}: ${value}`)
  return labels.length > 0 ? labels.join(' / ') : '默认规格'
}
</script>

<template>
  <div class="flex items-center gap-4 py-3 border-b border-gray-100" :class="{ 'opacity-60': !item.isAvailable }">
    <el-checkbox
      :model-value="item.selected"
      :disabled="!item.isAvailable"
      @change="(val: unknown) => $emit('toggleCheck', item.itemId, !!val)"
    />
    <img :src="item.productImage" :alt="item.productName" class="w-20 h-20 object-cover rounded" />
    <div class="flex-1">
      <router-link :to="`/product/${item.productId}`" class="text-sm text-gray-700 no-underline hover:text-primary">
        {{ item.productName }}
      </router-link>
      <p class="text-xs text-gray-400 mt-1">{{ formatSkuSpec(item.skuSpec) }}</p>
      <p v-if="!item.isAvailable" class="text-xs text-danger mt-1">商品已下架或库存不足</p>
    </div>
    <span class="text-primary font-bold w-24 text-right">{{ formatPrice(item.unitPrice) }}</span>
    <el-input-number
      :model-value="item.quantity"
      :min="1"
      :max="Math.max(item.stock, 1)"
      size="small"
      :disabled="!item.isAvailable"
      @change="(val: number | undefined) => $emit('updateQuantity', item.itemId, val ?? 1)"
    />
    <el-button type="danger" text size="small" @click="$emit('remove', item.itemId)">删除</el-button>
  </div>
</template>

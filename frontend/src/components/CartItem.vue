<script setup lang="ts">
import type { CartItem } from '@/api/cart'
import { formatPrice } from '@/utils'

defineProps<{
  item: CartItem
}>()

defineEmits<{
  updateQuantity: [skuId: string, quantity: number]
  remove: [skuId: string]
  toggleCheck: [skuId: string]
}>()
</script>

<template>
  <div class="flex items-center gap-4 py-3 border-b border-gray-100">
    <el-checkbox :model-value="item.checked" @change="$emit('toggleCheck', item.skuId)" />
    <img :src="item.productImage" :alt="item.productName" class="w-20 h-20 object-cover rounded" />
    <div class="flex-1">
      <router-link :to="`/product/${item.productId}`" class="text-sm text-gray-700 no-underline hover:text-primary">
        {{ item.productName }}
      </router-link>
      <p class="text-xs text-gray-400 mt-1">{{ item.specJson }}</p>
    </div>
    <span class="text-primary font-bold w-24 text-right">{{ formatPrice(item.price) }}</span>
    <el-input-number
      :model-value="item.quantity"
      :min="1"
      :max="99"
      size="small"
      @change="(val: number | undefined) => $emit('updateQuantity', item.skuId, val ?? 1)"
    />
    <el-button type="danger" text size="small" @click="$emit('remove', item.skuId)">删除</el-button>
  </div>
</template>

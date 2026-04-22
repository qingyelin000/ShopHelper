<script setup lang="ts">
import type { ProductSummary } from '@/types/product.d'
import { formatPrice } from '@/utils'

defineProps<{
  product: ProductSummary
}>()
</script>

<template>
  <router-link :to="`/product/${product.id}`" class="product-card no-underline">
    <el-card shadow="hover" :body-style="{ padding: '0' }">
      <img :src="product.mainImage" :alt="product.name" class="w-full h-48 object-cover" />
      <div class="p-3">
        <p class="text-sm text-gray-700 line-clamp-2 mb-2">{{ product.name }}</p>
        <div class="flex-between">
          <span class="text-primary font-bold text-lg">{{ formatPrice(product.price) }}</span>
          <span class="text-gray-400 text-xs">{{ product.salesCount }}人付款</span>
        </div>
      </div>
    </el-card>
  </router-link>
</template>

<style scoped>
.product-card {
  display: block;
  transition: transform 0.2s;
}
.product-card:hover {
  transform: translateY(-2px);
}
</style>

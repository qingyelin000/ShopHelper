<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { createOrder } from '@/api/order'
import { formatPrice } from '@/utils'
import { ElMessage } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()

const checkedItems = computed(() => cartStore.checkedItems)
const totalAmount = computed(() => cartStore.checkedAmount)

async function handleSubmit() {
  if (checkedItems.value.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  try {
    const { orderId } = await createOrder({
      addressId: '1', // TODO: 接入地址选择
      cartItemIds: checkedItems.value.map((i) => i.id),
    })
    ElMessage.success('下单成功')
    router.replace(`/order/${orderId}`)
  } catch (err: any) {
    ElMessage.error(err.message || '下单失败')
  }
}
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-4">确认订单</h2>

    <!-- 收货地址 -->
    <el-card shadow="never" class="mb-4">
      <template #header><span class="font-medium">收货地址</span></template>
      <p class="text-gray-500 text-sm">请选择收货地址（功能开发中）</p>
    </el-card>

    <!-- 商品清单 -->
    <el-card shadow="never" class="mb-4">
      <template #header><span class="font-medium">商品清单</span></template>
      <div v-for="item in checkedItems" :key="item.skuId" class="flex items-center gap-4 py-2 border-b border-gray-50 last:border-0">
        <img :src="item.productImage" class="w-16 h-16 object-cover rounded" />
        <div class="flex-1">
          <p class="text-sm">{{ item.productName }}</p>
          <p class="text-xs text-gray-400">{{ item.specJson }} × {{ item.quantity }}</p>
        </div>
        <span class="text-primary">{{ formatPrice(item.price * item.quantity) }}</span>
      </div>
    </el-card>

    <!-- 提交 -->
    <div class="flex justify-end items-center gap-4">
      <span class="text-lg">
        合计：<span class="text-primary font-bold text-xl">{{ formatPrice(totalAmount) }}</span>
      </span>
      <el-button type="danger" size="large" @click="handleSubmit">提交订单</el-button>
    </div>
  </div>
</template>

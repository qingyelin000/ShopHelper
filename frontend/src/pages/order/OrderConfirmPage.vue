<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { createOrder } from '@/api/order'
import { listMyAddresses } from '@/api/user'
import type { UserAddress } from '@/types/user.d'
import { formatPrice } from '@/utils'
import { ElMessage } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()

const checkedItems = computed(() => cartStore.checkedItems)
const totalAmount = computed(() => cartStore.checkedAmount)
const addressLoading = ref(false)
const addresses = ref<UserAddress[]>([])
const selectedAddressId = ref('')

async function loadAddresses() {
  addressLoading.value = true
  try {
    addresses.value = await listMyAddresses()
    const defaultAddress = addresses.value.find((item) => item.isDefault)
    selectedAddressId.value = defaultAddress?.id || addresses.value[0]?.id || ''
  } catch (err: any) {
    ElMessage.error(err.message || '加载收货地址失败')
  } finally {
    addressLoading.value = false
  }
}

async function handleSubmit() {
  if (checkedItems.value.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  if (!selectedAddressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  try {
    const { orderId } = await createOrder({
      addressId: selectedAddressId.value,
      cartItemIds: checkedItems.value.map((i) => i.id),
    })
    ElMessage.success('下单成功')
    router.replace(`/order/${orderId}`)
  } catch (err: any) {
    ElMessage.error(err.message || '下单失败')
  }
}

onMounted(() => {
  void loadAddresses()
})
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-4">确认订单</h2>

    <!-- 收货地址 -->
    <el-card shadow="never" class="mb-4" v-loading="addressLoading">
      <template #header><span class="font-medium">收货地址</span></template>
      <div v-if="addresses.length" class="flex flex-col gap-3">
        <el-radio-group v-model="selectedAddressId" class="w-full">
          <div v-for="address in addresses" :key="address.id" class="mb-3 last:mb-0">
            <el-radio :value="address.id" border class="w-full !items-start !h-auto !m-0 !py-3">
              <div class="ml-2">
                <div class="flex items-center gap-2">
                  <span class="font-medium">{{ address.receiverName }}</span>
                  <span class="text-gray-500">{{ address.receiverPhone }}</span>
                  <el-tag v-if="address.isDefault" size="small" type="danger">默认</el-tag>
                </div>
                <p class="mt-1 text-sm text-gray-500">
                  {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}
                </p>
              </div>
            </el-radio>
          </div>
        </el-radio-group>
      </div>
      <p v-else class="text-gray-500 text-sm">暂无收货地址，请先通过用户中心接口创建地址</p>
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

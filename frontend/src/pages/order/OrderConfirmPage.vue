<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { getCart } from '@/api/cart'
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
const submitting = ref(false)
const addresses = ref<UserAddress[]>([])
const selectedAddressId = ref('')

function formatSkuSpec(spec: Record<string, string | number | boolean | null>) {
  return Object.entries(spec)
    .map(([key, value]) => `${key}: ${value}`)
    .join(' / ')
}

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

  submitting.value = true
  try {
    const order = await createOrder({
      addressId: selectedAddressId.value,
      items: checkedItems.value.map((item) => ({
        skuId: item.skuId,
        quantity: item.quantity,
      })),
      source: 'web',
    })
    try {
      cartStore.setSnapshot(await getCart())
    } catch {
      // 订单已创建成功，这里不阻断跳转
    }
    ElMessage.success('下单成功')
    await router.replace(`/order/${order.orderId}`)
  } catch (err: any) {
    ElMessage.error(err.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadAddresses()
})
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-4">确认订单</h2>

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
      <p v-else class="text-gray-500 text-sm">暂无收货地址，请先通过个人中心创建地址</p>
    </el-card>

    <el-card shadow="never" class="mb-4">
      <template #header><span class="font-medium">商品清单</span></template>
      <div
        v-for="item in checkedItems"
        :key="item.itemId"
        class="flex items-center gap-4 py-2 border-b border-gray-50 last:border-0"
      >
        <img :src="item.productImage" class="w-16 h-16 object-cover rounded" />
        <div class="flex-1">
          <p class="text-sm">{{ item.productName }}</p>
          <p class="text-xs text-gray-400">{{ formatSkuSpec(item.skuSpec) }} × {{ item.quantity }}</p>
        </div>
        <span class="text-primary">{{ formatPrice(item.totalPrice) }}</span>
      </div>
    </el-card>

    <div class="flex justify-end items-center gap-4">
      <span class="text-lg">
        合计：<span class="text-primary font-bold text-xl">{{ formatPrice(totalAmount) }}</span>
      </span>
      <el-button type="danger" size="large" :loading="submitting" @click="handleSubmit">提交订单</el-button>
    </div>
  </div>
</template>

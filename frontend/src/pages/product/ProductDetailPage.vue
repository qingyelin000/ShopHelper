<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProduct } from '@/api/product'
import { addToCart } from '@/api/cart'
import type { Product, ProductSku } from '@/types/product.d'
import { formatPrice } from '@/utils'
import { ElMessage } from 'element-plus'

const route = useRoute()
const product = ref<Product | null>(null)
const selectedSku = ref<ProductSku | null>(null)
const quantity = ref(1)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    product.value = await getProduct(route.params.id as string)
    if (product.value?.skuList?.length) {
      selectedSku.value = product.value.skuList[0]!
    }
  } catch {
    // 后端未就绪
  } finally {
    loading.value = false
  }
})

async function handleAddToCart() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  try {
    await addToCart({ skuId: selectedSku.value.id, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch (err: any) {
    ElMessage.error(err.message || '加入购物车失败')
  }
}
</script>

<template>
  <el-skeleton :rows="6" animated :loading="loading">
    <template #default>
      <div v-if="product" class="flex gap-8">
        <!-- 商品图片 -->
        <div class="w-400px">
          <el-image :src="product.mainImage" fit="contain" class="w-full rounded-lg" />
        </div>

        <!-- 商品信息 -->
        <div class="flex-1">
          <h1 class="text-xl font-bold mb-4">{{ product.name }}</h1>
          <div class="bg-primary/5 p-4 rounded-lg mb-4">
            <span class="text-primary text-2xl font-bold">
              {{ formatPrice(selectedSku?.price ?? product.minPrice) }}
            </span>
            <span class="text-gray-400 text-sm ml-4">累计销量 {{ product.totalSales }}</span>
          </div>

          <!-- SKU 选择 -->
          <div v-if="product.skuList?.length" class="mb-4">
            <p class="text-sm text-gray-600 mb-2">规格</p>
            <div class="flex gap-2 flex-wrap">
              <el-tag
                v-for="sku in product.skuList"
                :key="sku.id"
                :effect="selectedSku?.id === sku.id ? 'dark' : 'plain'"
                class="cursor-pointer"
                @click="selectedSku = sku"
              >
                {{ sku.specJson }}
              </el-tag>
            </div>
          </div>

          <!-- 数量 -->
          <div class="flex items-center gap-4 mb-6">
            <span class="text-sm text-gray-600">数量</span>
            <el-input-number v-model="quantity" :min="1" :max="selectedSku?.stock ?? 99" />
            <span class="text-xs text-gray-400">库存 {{ selectedSku?.stock ?? 0 }} 件</span>
          </div>

          <!-- 操作 -->
          <div class="flex gap-4">
            <el-button type="primary" size="large" @click="handleAddToCart">加入购物车</el-button>
            <el-button type="danger" size="large">立即购买</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="商品不存在" />
    </template>
  </el-skeleton>
</template>

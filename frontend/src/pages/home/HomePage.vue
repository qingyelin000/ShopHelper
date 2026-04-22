<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { listProducts } from '@/api/product'
import { getMyRecommendations } from '@/api/recommend'
import type { ProductSummary } from '@/types/product.d'
import ProductCard from '@/components/ProductCard.vue'
import type { RecommendationItem } from '@/types/recommend.d'

const products = ref<ProductSummary[]>([])
const loading = ref(false)
const recommendationAlgorithm = ref('')
const userStore = useUserStore()

onMounted(async () => {
  loading.value = true
  try {
    if (userStore.isLoggedIn) {
      const recommendation = await getMyRecommendations('homepage', 10)
      recommendationAlgorithm.value = recommendation.algorithm
      if (recommendation.list.length > 0) {
        products.value = recommendation.list.map(toProductCardModel)
        return
      }
    }

    const res = await listProducts({ pageNum: 1, pageSize: 20 })
    products.value = res.list
  } catch {
    // 后端未就绪时静默失败
  } finally {
    loading.value = false
  }
})

function toProductCardModel(item: RecommendationItem): ProductSummary {
  return {
    id: item.productId,
    categoryId: '',
    name: item.name,
    mainImage: item.mainImage,
    price: item.price,
    salesCount: item.salesCount,
  }
}
</script>

<template>
  <div>
    <!-- Banner -->
    <div class="bg-primary/5 rounded-lg p-8 mb-8 text-center">
      <h1 class="text-2xl font-bold text-gray-800 mb-2">欢迎来到 ShopHelper</h1>
      <p class="text-gray-500">仿淘宝电商学习平台 —— 发现好物，尽在此处</p>
    </div>

    <!-- 商品列表 -->
    <h2 class="text-lg font-bold mb-4">为你推荐</h2>
    <p v-if="recommendationAlgorithm" class="text-xs text-gray-400 mb-3">
      推荐策略：{{ recommendationAlgorithm }}
    </p>
    <el-skeleton :rows="4" animated :loading="loading">
      <template #default>
        <div v-if="products.length" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
          <ProductCard v-for="p in products" :key="p.id" :product="p" />
        </div>
        <el-empty v-else description="暂无商品" />
      </template>
    </el-skeleton>
  </div>
</template>

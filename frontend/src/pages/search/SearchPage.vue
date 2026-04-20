<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchProducts } from '@/api/product'
import type { SearchProduct } from '@/types/product.d'
import { formatPrice } from '@/utils'

const route = useRoute()
const router = useRouter()
const products = ref<SearchProduct[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)

const keyword = ref((route.query.keyword as string) || '')

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const res = await searchProducts({ keyword: keyword.value, pageNum: pageNum.value, pageSize: 20 })
    products.value = res.list
    total.value = res.total
  } catch {
    // 后端未就绪
  } finally {
    loading.value = false
  }
}

watch(() => route.query.keyword, (val) => {
  keyword.value = (val as string) || ''
  pageNum.value = 1
  doSearch()
})

onMounted(doSearch)

function handlePageChange(page: number) {
  pageNum.value = page
  doSearch()
}
</script>

<template>
  <div>
    <div class="flex-between mb-6">
      <h2 class="text-lg font-bold">
        搜索 "{{ keyword }}" <span class="text-gray-400 text-sm font-normal">共 {{ total }} 件商品</span>
      </h2>
    </div>

    <el-skeleton :rows="4" animated :loading="loading">
      <template #default>
        <div v-if="products.length" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
          <router-link
            v-for="p in products"
            :key="p.id"
            :to="`/product/${p.id}`"
            class="no-underline"
          >
            <el-card shadow="hover" :body-style="{ padding: '0' }">
              <img :src="p.mainImage" :alt="p.name" class="w-full h-48 object-cover" />
              <div class="p-3">
                <p class="text-sm text-gray-700 line-clamp-2 mb-2" v-html="p.highlightName || p.name" />
                <div class="flex-between">
                  <span class="text-primary font-bold">{{ formatPrice(p.price) }}</span>
                  <span class="text-gray-400 text-xs">{{ p.salesCount }}人付款</span>
                </div>
              </div>
            </el-card>
          </router-link>
        </div>
        <el-empty v-else description="未找到相关商品" />
      </template>
    </el-skeleton>

    <div v-if="total > 20" class="flex justify-center mt-8">
      <el-pagination
        :current-page="pageNum"
        :page-size="20"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

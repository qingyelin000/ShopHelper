<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listOrders } from '@/api/order'
import type { Order } from '@/types/order.d'
import OrderStatusTag from '@/components/OrderStatus.vue'
import { formatPrice, formatDateTime } from '@/utils'

const orders = ref<Order[]>([])
const total = ref(0)
const pageNum = ref(1)
const activeTab = ref<string>('all')
const loading = ref(false)

const tabs = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: '0' },
  { label: '已付款', value: '1' },
  { label: '已发货', value: '2' },
  { label: '已完成', value: '3' },
]

async function fetchOrders() {
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize: 10 }
    if (activeTab.value !== 'all') params.status = Number(activeTab.value)
    const res = await listOrders(params)
    orders.value = res.list
    total.value = res.total
  } catch {
    // 后端未就绪
  } finally {
    loading.value = false
  }
}

onMounted(fetchOrders)

function handleTabChange() {
  pageNum.value = 1
  fetchOrders()
}
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-4">我的订单</h2>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <el-skeleton :rows="3" animated :loading="loading">
      <template #default>
        <div v-if="orders.length" class="space-y-4">
          <el-card v-for="order in orders" :key="order.id" shadow="never">
            <div class="flex-between mb-3">
              <span class="text-sm text-gray-400">{{ formatDateTime(order.createdAt) }}</span>
              <OrderStatusTag :status="order.status" />
            </div>
            <div v-for="item in order.items" :key="item.id" class="flex items-center gap-3 py-2">
              <img :src="item.productImage" class="w-16 h-16 object-cover rounded" />
              <div class="flex-1">
                <p class="text-sm">{{ item.productName }}</p>
                <p class="text-xs text-gray-400">{{ item.specJson }} × {{ item.quantity }}</p>
              </div>
              <span class="text-primary">{{ formatPrice(item.price) }}</span>
            </div>
            <div class="text-right mt-3 pt-3 border-t border-gray-100">
              <span>合计：<span class="text-primary font-bold text-lg">{{ formatPrice(order.totalAmount) }}</span></span>
              <router-link :to="`/order/${order.id}`" class="ml-4">
                <el-button size="small">查看详情</el-button>
              </router-link>
            </div>
          </el-card>
        </div>
        <el-empty v-else description="暂无订单" />
      </template>
    </el-skeleton>

    <div v-if="total > 10" class="flex justify-center mt-6">
      <el-pagination
        :current-page="pageNum"
        :page-size="10"
        :total="total"
        layout="prev, pager, next"
        @current-change="(p: number) => { pageNum = p; fetchOrders() }"
      />
    </div>
  </div>
</template>

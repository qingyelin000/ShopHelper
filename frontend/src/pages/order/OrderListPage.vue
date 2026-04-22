<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listOrders } from '@/api/order'
import type { OrderStatus, OrderSummary } from '@/types/order.d'
import OrderStatusTag from '@/components/OrderStatus.vue'
import { formatPrice, formatDateTime } from '@/utils'
import { ElMessage } from 'element-plus'

const orders = ref<OrderSummary[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const activeTab = ref<'all' | OrderStatus>('all')

const tabs: Array<{ label: string; value: 'all' | OrderStatus }> = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: 'PENDING_PAYMENT' },
  { label: '已付款', value: 'PAID' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '退款中', value: 'REFUNDING' },
]

async function fetchOrders() {
  loading.value = true
  try {
    const res = await listOrders({
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      pageNum: pageNum.value,
      pageSize: 10,
    })
    orders.value = res.list
    total.value = res.total
  } catch (err: any) {
    ElMessage.error(err.message || '加载订单列表失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  pageNum.value = 1
  void fetchOrders()
}

function handlePageChange(page: number) {
  pageNum.value = page
  void fetchOrders()
}

onMounted(() => {
  void fetchOrders()
})
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
          <el-card v-for="order in orders" :key="order.orderId" shadow="never">
            <div class="flex-between mb-4">
              <div>
                <div class="font-medium">{{ order.orderNo }}</div>
                <div class="text-sm text-gray-400 mt-1">{{ formatDateTime(order.createTime) }}</div>
              </div>
              <OrderStatusTag :status="order.orderStatus" />
            </div>

            <div class="flex items-center gap-4">
              <img
                :src="order.coverImage || 'https://via.placeholder.com/80x80?text=Order'"
                class="w-20 h-20 object-cover rounded"
              />
              <div class="flex-1">
                <p class="text-sm">共 {{ order.itemCount }} 件商品</p>
                <p v-if="order.expireTime" class="text-xs text-gray-400 mt-1">
                  支付截止：{{ formatDateTime(order.expireTime) }}
                </p>
              </div>
              <div class="text-right">
                <p class="text-sm text-gray-400">实付金额</p>
                <p class="text-primary font-bold text-lg">{{ formatPrice(order.payAmount) }}</p>
              </div>
            </div>

            <div class="text-right mt-4 pt-4 border-t border-gray-100">
              <router-link :to="`/order/${order.orderId}`">
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
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

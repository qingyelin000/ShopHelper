<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrder, cancelOrder, payOrder, confirmOrder } from '@/api/order'
import type { Order } from '@/types/order.d'
import { OrderStatus } from '@/types/order.d'
import OrderStatusTag from '@/components/OrderStatus.vue'
import { formatPrice, formatDateTime } from '@/utils'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const order = ref<Order | null>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    order.value = await getOrder(route.params.id as string)
  } catch {
    // 后端未就绪
  } finally {
    loading.value = false
  }
})

async function handleCancel() {
  await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
  try {
    await cancelOrder(order.value!.id)
    order.value!.status = OrderStatus.CANCELLED
    ElMessage.success('订单已取消')
  } catch (err: any) {
    ElMessage.error(err.message || '取消失败')
  }
}

async function handlePay() {
  try {
    await payOrder(order.value!.id)
    order.value!.status = OrderStatus.PAID
    ElMessage.success('支付成功')
  } catch (err: any) {
    ElMessage.error(err.message || '支付失败')
  }
}

async function handleConfirm() {
  try {
    await confirmOrder(order.value!.id)
    order.value!.status = OrderStatus.COMPLETED
    ElMessage.success('已确认收货')
  } catch (err: any) {
    ElMessage.error(err.message || '确认失败')
  }
}
</script>

<template>
  <el-skeleton :rows="5" animated :loading="loading">
    <template #default>
      <div v-if="order">
        <div class="flex-between mb-6">
          <h2 class="text-lg font-bold">订单详情</h2>
          <OrderStatusTag :status="order.status" />
        </div>

        <!-- 商品列表 -->
        <el-card shadow="never" class="mb-4">
          <div v-for="item in order.items" :key="item.id" class="flex items-center gap-4 py-3 border-b border-gray-50 last:border-0">
            <img :src="item.productImage" class="w-20 h-20 object-cover rounded" />
            <div class="flex-1">
              <p class="text-sm font-medium">{{ item.productName }}</p>
              <p class="text-xs text-gray-400 mt-1">{{ item.specJson }}</p>
            </div>
            <div class="text-right">
              <p class="text-primary">{{ formatPrice(item.price) }}</p>
              <p class="text-xs text-gray-400">× {{ item.quantity }}</p>
            </div>
          </div>
        </el-card>

        <!-- 金额 & 信息 -->
        <el-card shadow="never" class="mb-4">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单编号">{{ order.id }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="商品总额">{{ formatPrice(order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span class="text-primary font-bold">{{ formatPrice(order.payAmount) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 操作按钮 -->
        <div class="text-right">
          <el-button v-if="order.status === OrderStatus.PENDING_PAY" @click="handleCancel">取消订单</el-button>
          <el-button v-if="order.status === OrderStatus.PENDING_PAY" type="danger" @click="handlePay">去支付</el-button>
          <el-button v-if="order.status === OrderStatus.SHIPPED" type="primary" @click="handleConfirm">确认收货</el-button>
        </div>
      </div>
      <el-empty v-else description="订单不存在" />
    </template>
  </el-skeleton>
</template>

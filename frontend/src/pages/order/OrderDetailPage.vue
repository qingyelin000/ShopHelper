<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmOrder, getOrder, payOrder } from '@/api/order'
import type { OrderDetail } from '@/types/order.d'
import OrderStatusTag from '@/components/OrderStatus.vue'
import { formatDateTime, formatPrice } from '@/utils'

const route = useRoute()
const order = ref<OrderDetail | null>(null)
const loading = ref(false)

const canCancel = computed(
  () => order.value?.orderStatus === 'PENDING_PAYMENT' || order.value?.orderStatus === 'PAID',
)
const canPay = computed(() => order.value?.orderStatus === 'PENDING_PAYMENT')
const canConfirm = computed(() => order.value?.orderStatus === 'SHIPPED')

function formatSkuSpec(spec: Record<string, string | number | boolean | null>) {
  return Object.entries(spec)
    .map(([key, value]) => `${key}: ${value}`)
    .join(' / ')
}

async function loadOrder() {
  loading.value = true
  try {
    order.value = await getOrder(route.params.id as string)
  } catch (err: any) {
    ElMessage.error(err.message || '加载订单详情失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  if (!order.value) {
    return
  }
  await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
  try {
    const result = await cancelOrder(order.value.orderId, { reason: '用户主动取消' })
    order.value.orderStatus = result.orderStatus
    ElMessage.success(result.refundNote || '订单已取消')
  } catch (err: any) {
    ElMessage.error(err.message || '取消失败')
  }
}

async function handlePay() {
  if (!order.value) {
    return
  }
  try {
    await payOrder(order.value.orderId, { paymentMethod: 'MOCK' })
    await loadOrder()
    ElMessage.success('支付成功')
  } catch (err: any) {
    ElMessage.error(err.message || '支付失败')
  }
}

async function handleConfirm() {
  if (!order.value) {
    return
  }
  try {
    await confirmOrder(order.value.orderId)
    await loadOrder()
    ElMessage.success('已确认收货')
  } catch (err: any) {
    ElMessage.error(err.message || '确认失败')
  }
}

onMounted(() => {
  void loadOrder()
})
</script>

<template>
  <el-skeleton :rows="5" animated :loading="loading">
    <template #default>
      <div v-if="order">
        <div class="flex-between mb-6">
          <div>
            <h2 class="text-lg font-bold">订单详情</h2>
            <p class="text-sm text-gray-400 mt-1">{{ order.orderNo }}</p>
          </div>
          <OrderStatusTag :status="order.orderStatus" />
        </div>

        <el-card shadow="never" class="mb-4">
          <template #header><span class="font-medium">收货信息</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="收货人">{{ order.address.receiverName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ order.address.receiverPhone }}</el-descriptions-item>
            <el-descriptions-item label="收货地址">{{ order.address.fullAddress }}</el-descriptions-item>
            <el-descriptions-item label="下单来源">{{ order.source }}</el-descriptions-item>
            <el-descriptions-item label="订单备注">{{ order.remark || '--' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="mb-4">
          <template #header><span class="font-medium">商品列表</span></template>
          <div
            v-for="item in order.items"
            :key="item.itemId"
            class="flex items-center gap-4 py-3 border-b border-gray-50 last:border-0"
          >
            <img :src="item.productImage || 'https://via.placeholder.com/80x80?text=Item'" class="w-20 h-20 object-cover rounded" />
            <div class="flex-1">
              <p class="text-sm font-medium">{{ item.productName }}</p>
              <p class="text-xs text-gray-400 mt-1">{{ formatSkuSpec(item.skuSpec) }}</p>
            </div>
            <div class="text-right">
              <p class="text-primary">{{ formatPrice(item.unitPrice) }}</p>
              <p class="text-xs text-gray-400">× {{ item.quantity }}</p>
              <p class="text-xs text-gray-500 mt-1">小计：{{ formatPrice(item.totalPrice) }}</p>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="mb-4">
          <template #header><span class="font-medium">订单金额</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(order.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="商品总额">{{ formatPrice(order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="运费">{{ formatPrice(order.freightAmount) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span class="text-primary font-bold">{{ formatPrice(order.payAmount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ order.payType || '--' }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ order.payTime ? formatDateTime(order.payTime) : '--' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <div class="text-right">
          <el-button v-if="canCancel" @click="handleCancel">取消订单</el-button>
          <el-button v-if="canPay" type="danger" @click="handlePay">MOCK 支付</el-button>
          <el-button v-if="canConfirm" type="primary" @click="handleConfirm">确认收货</el-button>
        </div>
      </div>
      <el-empty v-else description="订单不存在" />
    </template>
  </el-skeleton>
</template>

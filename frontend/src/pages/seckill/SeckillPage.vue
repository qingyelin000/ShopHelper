<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listSeckillActivities, seckillOrder } from '@/api/seckill'
import type { SeckillActivity } from '@/api/seckill'
import { formatPrice, formatDateTime } from '@/utils'
import { ElMessage } from 'element-plus'

const activities = ref<SeckillActivity[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    activities.value = await listSeckillActivities()
  } catch {
    // 后端未就绪
  } finally {
    loading.value = false
  }
})

async function handleSeckill(activityId: string) {
  try {
    const { orderId } = await seckillOrder(activityId, '1') // TODO: 地址选择
    ElMessage.success('秒杀成功！')
  } catch (err: any) {
    ElMessage.error(err.message || '秒杀失败')
  }
}
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-4">🔥 限时秒杀</h2>

    <el-skeleton :rows="3" animated :loading="loading">
      <template #default>
        <div v-if="activities.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <el-card v-for="act in activities" :key="act.id" shadow="hover">
            <div class="flex gap-4">
              <img :src="act.productImage" class="w-32 h-32 object-cover rounded" />
              <div class="flex-1">
                <p class="font-medium mb-2">{{ act.productName }}</p>
                <div class="mb-2">
                  <span class="text-primary text-xl font-bold">{{ formatPrice(act.seckillPrice) }}</span>
                  <span class="text-gray-400 text-sm line-through ml-2">{{ formatPrice(act.originalPrice) }}</span>
                </div>
                <el-progress
                  :percentage="Math.round(((act.totalStock - act.availableStock) / act.totalStock) * 100)"
                  :stroke-width="10"
                  color="#ff5000"
                  class="mb-2"
                />
                <p class="text-xs text-gray-400 mb-2">
                  {{ formatDateTime(act.startTime) }} ~ {{ formatDateTime(act.endTime) }}
                </p>
                <el-button type="danger" size="small" :disabled="act.availableStock <= 0" @click="handleSeckill(act.id)">
                  {{ act.availableStock > 0 ? '立即秒杀' : '已抢光' }}
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
        <el-empty v-else description="暂无秒杀活动" />
      </template>
    </el-skeleton>
  </div>
</template>

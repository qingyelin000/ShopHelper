<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCart, updateCartItem, removeCartItem } from '@/api/cart'
import { useCartStore } from '@/stores/cart'
import CartItemComp from '@/components/CartItem.vue'
import { formatPrice } from '@/utils'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const snapshot = await getCart()
    cartStore.setSnapshot(snapshot)
  } catch {
    // 后端未就绪
  } finally {
    loading.value = false
  }
})

const isAllChecked = computed(() =>
  cartStore.items.length > 0 && cartStore.items.every((i) => i.selected),
)

async function handleUpdateQuantity(itemId: string, quantity: number) {
  try {
    const snapshot = await updateCartItem(itemId, { quantity })
    cartStore.setSnapshot(snapshot)
  } catch (err: any) {
    ElMessage.error(err.message || '更新失败')
  }
}

async function handleRemove(itemId: string) {
  await ElMessageBox.confirm('确定删除该商品吗？', '提示', { type: 'warning' })
  try {
    const snapshot = await removeCartItem(itemId)
    cartStore.setSnapshot(snapshot)
    ElMessage.success('已删除')
  } catch (err: any) {
    ElMessage.error(err.message || '删除失败')
  }
}

async function handleToggleCheck(itemId: string, selected: boolean) {
  try {
    const snapshot = await updateCartItem(itemId, { selected })
    cartStore.setSnapshot(snapshot)
  } catch (err: any) {
    ElMessage.error(err.message || '更新失败')
  }
}

async function handleToggleCheckAll(selected: boolean) {
  const targets = cartStore.items.filter((item) => item.isAvailable && item.selected !== selected)
  if (targets.length === 0) return
  loading.value = true
  try {
    for (const item of targets) {
      const snapshot = await updateCartItem(item.itemId, { selected })
      cartStore.setSnapshot(snapshot)
    }
  } catch (err: any) {
    ElMessage.error(err.message || '更新失败')
  } finally {
    loading.value = false
  }
}

function goCheckout() {
  if (cartStore.checkedItems.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  router.push({ name: 'orderConfirm' })
}
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-4">购物车</h2>

    <el-skeleton :rows="3" animated :loading="loading">
      <template #default>
        <div v-if="cartStore.items.length">
          <!-- 全选 -->
          <div class="flex items-center gap-2 mb-2 text-sm text-gray-500">
            <el-checkbox :model-value="isAllChecked" @change="(val: unknown) => handleToggleCheckAll(!!val)">
              全选
            </el-checkbox>
          </div>

          <CartItemComp
            v-for="item in cartStore.items"
            :key="item.itemId"
            :item="item"
            @update-quantity="handleUpdateQuantity"
            @remove="handleRemove"
            @toggle-check="handleToggleCheck"
          />

          <!-- 结算栏 -->
          <div class="flex-between mt-6 p-4 bg-gray-50 rounded-lg">
            <span class="text-sm text-gray-600">
              已选 {{ cartStore.checkedItems.length }} 件商品
            </span>
            <div class="flex items-center gap-4">
              <span class="text-lg">
                合计：<span class="text-primary font-bold text-xl">{{ formatPrice(cartStore.checkedAmount) }}</span>
              </span>
              <el-button type="danger" size="large" @click="goCheckout">去结算</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="购物车是空的，去逛逛吧">
          <el-button type="primary" @click="router.push('/')">去购物</el-button>
        </el-empty>
      </template>
    </el-skeleton>
  </div>
</template>

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
    const items = await getCart()
    cartStore.setItems(items)
  } catch {
    // 后端未就绪
  } finally {
    loading.value = false
  }
})

const isAllChecked = computed(() =>
  cartStore.items.length > 0 && cartStore.items.every((i) => i.checked),
)

async function handleUpdateQuantity(skuId: string, quantity: number) {
  try {
    await updateCartItem(skuId, quantity)
    cartStore.updateQuantity(skuId, quantity)
  } catch (err: any) {
    ElMessage.error(err.message || '更新失败')
  }
}

async function handleRemove(skuId: string) {
  await ElMessageBox.confirm('确定删除该商品吗？', '提示', { type: 'warning' })
  try {
    await removeCartItem(skuId)
    cartStore.removeItem(skuId)
    ElMessage.success('已删除')
  } catch (err: any) {
    ElMessage.error(err.message || '删除失败')
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
            <el-checkbox :model-value="isAllChecked" @change="(val: boolean) => cartStore.toggleCheckAll(val)">
              全选
            </el-checkbox>
          </div>

          <CartItemComp
            v-for="item in cartStore.items"
            :key="item.skuId"
            :item="item"
            @update-quantity="handleUpdateQuantity"
            @remove="handleRemove"
            @toggle-check="cartStore.toggleCheck"
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

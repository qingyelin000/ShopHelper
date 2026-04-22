import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem, CartSnapshot } from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  const snapshot = ref<CartSnapshot>({
    items: [],
    selectedCount: 0,
    totalQuantity: 0,
    estimatedTotal: 0,
  })

  const items = computed(() => snapshot.value.items)

  const totalCount = computed(() => snapshot.value.totalQuantity)

  const checkedItems = computed(() => snapshot.value.items.filter((item) => item.selected))

  const checkedAmount = computed(() => snapshot.value.estimatedTotal)

  function setSnapshot(data: CartSnapshot) {
    snapshot.value = data
  }

  function clear() {
    snapshot.value = {
      items: [],
      selectedCount: 0,
      totalQuantity: 0,
      estimatedTotal: 0,
    }
  }

  return { items, totalCount, checkedItems, checkedAmount, setSnapshot, clear }
})

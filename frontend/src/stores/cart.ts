import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem } from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))

  const checkedItems = computed(() => items.value.filter((item) => item.checked))

  const checkedAmount = computed(() =>
    checkedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
  )

  function setItems(list: CartItem[]) {
    items.value = list
  }

  function updateQuantity(skuId: string, quantity: number) {
    const item = items.value.find((i) => i.skuId === skuId)
    if (item) item.quantity = quantity
  }

  function removeItem(skuId: string) {
    items.value = items.value.filter((i) => i.skuId !== skuId)
  }

  function toggleCheck(skuId: string) {
    const item = items.value.find((i) => i.skuId === skuId)
    if (item) item.checked = !item.checked
  }

  function toggleCheckAll(checked: boolean) {
    items.value.forEach((i) => (i.checked = checked))
  }

  function clear() {
    items.value = []
  }

  return { items, totalCount, checkedItems, checkedAmount, setItems, updateQuantity, removeItem, toggleCheck, toggleCheckAll, clear }
})

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const { isLoggedIn, logout } = useAuth()

const cartCount = computed(() => cartStore.totalCount)

function goSearch(keyword: string) {
  if (keyword.trim()) {
    router.push({ name: 'search', query: { keyword } })
  }
}
</script>

<template>
  <el-container class="min-h-screen">
    <!-- 顶部导航 -->
    <el-header class="border-b border-gray-200 flex-between">
      <div class="flex items-center gap-4">
        <router-link to="/" class="text-xl font-bold text-primary no-underline">
          🛒 ShopHelper
        </router-link>
        <el-input
          placeholder="搜索商品"
          style="width: 320px"
          @keyup.enter="goSearch(($event.target as HTMLInputElement).value)"
        >
          <template #prefix>
            <el-icon><i class="i-ep-search" /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="flex items-center gap-4">
        <router-link to="/seckill" class="nav-link">秒杀</router-link>
        <router-link to="/chat" class="nav-link">AI 导购</router-link>

        <router-link to="/cart" class="nav-link">
          <el-badge :value="cartCount" :hidden="cartCount === 0">
            🛒 购物车
          </el-badge>
        </router-link>

        <template v-if="isLoggedIn">
          <el-dropdown>
            <span class="nav-link cursor-pointer">{{ userStore.nickname || '我的' }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/order')">我的订单</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link">登录</router-link>
        </template>
      </div>
    </el-header>

    <!-- 内容区 -->
    <el-main class="page-container py-6">
      <router-view />
    </el-main>

    <!-- 底部 -->
    <el-footer class="text-center text-gray-400 text-sm border-t border-gray-200">
      © 2026 ShopHelper — 仿淘宝电商学习项目
    </el-footer>
  </el-container>
</template>

<style scoped>
.nav-link {
  color: var(--el-text-color-regular);
  text-decoration: none;
  font-size: 14px;
}
.nav-link:hover {
  color: var(--el-color-primary);
}
</style>

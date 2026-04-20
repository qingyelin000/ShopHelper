<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'

const userStore = useUserStore()
const { logout } = useAuth()
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-6">个人中心</h2>

    <el-card shadow="never">
      <el-descriptions title="基本信息" :column="1" border>
        <el-descriptions-item label="用户 ID">{{ userStore.userId }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.nickname }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-6 flex gap-4">
        <router-link to="/order">
          <el-button>我的订单</el-button>
        </router-link>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

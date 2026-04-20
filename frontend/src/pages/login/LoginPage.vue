<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import type { LoginRequest } from '@/api/auth'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const { login } = useAuth()

const isRegister = ref(false)
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref({
  phone: '',
  password: '',
  nickname: '',
})

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (isRegister.value) {
      await register(form.value)
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
    } else {
      await login(form.value as LoginRequest)
    }
  } catch (err: any) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="w-400px">
    <el-card>
      <template #header>
        <div class="text-center">
          <h2 class="text-xl font-bold text-primary mb-1">🛒 ShopHelper</h2>
          <p class="text-gray-400 text-sm">{{ isRegister ? '注册新账号' : '登录' }}</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item v-if="isRegister" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="w-full" :loading="loading" @click="handleSubmit">
            {{ isRegister ? '注册' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="text-center text-sm">
        <el-button link type="primary" @click="isRegister = !isRegister">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

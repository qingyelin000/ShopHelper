<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { refreshTokens } from '@/api/auth'
import {
  bootstrapAdmin,
  createMyAddress,
  deleteMyAddress,
  getMyProfile,
  listAdminUsers,
  listMyAddresses,
  setDefaultMyAddress,
  updateAdminUserRole,
  updateMyAddress,
} from '@/api/user'
import type {
  AdminUser,
  AdminUserPage,
  CreateUserAddressRequest,
  UserAddress,
  UserProfile,
  UserRole,
} from '@/types/user.d'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { formatDateTime } from '@/utils'
import { parseAccessTokenUserInfo } from '@/utils/jwt'

const userStore = useUserStore()
const { logout } = useAuth()

const profileLoading = ref(false)
const addressLoading = ref(false)
const adminLoading = ref(false)
const addressSubmitting = ref(false)
const bootstrapSubmitting = ref(false)
const adminSubmitting = ref(false)

const profile = ref<UserProfile | null>(null)
const addresses = ref<UserAddress[]>([])
const adminUsers = ref<AdminUser[]>([])

const addressDialogVisible = ref(false)
const editingAddressId = ref<string | null>(null)
const addressFormRef = ref<FormInstance>()

const addressForm = ref<CreateUserAddressRequest>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  postalCode: '',
  isDefault: false,
})

const adminPage = reactive<AdminUserPage>({
  list: [],
  total: 0,
  pageNum: 1,
  pageSize: 10,
  hasNext: false,
})

const bootstrapForm = reactive({
  username: '',
  bootstrapToken: '',
})

const adminFilters = reactive<{
  keyword: string
  role: '' | UserRole
  status: '' | number
}>({
  keyword: '',
  role: '',
  status: '',
})

const addressRules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入收货手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的收货手机号', trigger: 'blur' },
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

const isAdmin = computed(() => userStore.isAdmin)
const currentRoleText = computed(() => (userStore.role === 'ADMIN' ? '管理员' : '普通用户'))

async function loadProfile() {
  profileLoading.value = true
  try {
    profile.value = await getMyProfile()
  } catch (err: any) {
    ElMessage.error(err.message || '加载个人信息失败')
  } finally {
    profileLoading.value = false
  }
}

async function loadAddresses() {
  addressLoading.value = true
  try {
    addresses.value = await listMyAddresses()
  } catch (err: any) {
    ElMessage.error(err.message || '加载收货地址失败')
  } finally {
    addressLoading.value = false
  }
}

async function loadAdminUsers(pageNum = 1) {
  if (!isAdmin.value) {
    adminUsers.value = []
    adminPage.list = []
    adminPage.total = 0
    adminPage.pageNum = 1
    adminPage.hasNext = false
    return
  }

  adminLoading.value = true
  try {
    const data = await listAdminUsers({
      keyword: adminFilters.keyword.trim() || undefined,
      role: adminFilters.role || undefined,
      status: adminFilters.status === '' ? undefined : Number(adminFilters.status),
      pageNum,
      pageSize: adminPage.pageSize,
    })
    adminUsers.value = data.list
    adminPage.list = data.list
    adminPage.total = data.total
    adminPage.pageNum = data.pageNum
    adminPage.pageSize = data.pageSize
    adminPage.hasNext = data.hasNext
  } catch (err: any) {
    ElMessage.error(err.message || '加载管理员用户列表失败')
  } finally {
    adminLoading.value = false
  }
}

function openCreateAddressDialog() {
  editingAddressId.value = null
  addressForm.value = {
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    postalCode: '',
    isDefault: addresses.value.length === 0,
  }
  addressDialogVisible.value = true
}

function openEditAddressDialog(address: UserAddress) {
  editingAddressId.value = address.id
  addressForm.value = {
    receiverName: address.receiverName,
    receiverPhone: '',
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detailAddress,
    postalCode: address.postalCode || '',
    isDefault: false,
  }
  addressDialogVisible.value = true
}

async function handleBootstrapAdmin() {
  if (!bootstrapForm.username.trim()) {
    ElMessage.warning('请输入要初始化的用户名')
    return
  }
  if (!bootstrapForm.bootstrapToken.trim()) {
    ElMessage.warning('请输入 Bootstrap Token')
    return
  }

  bootstrapSubmitting.value = true
  try {
    await bootstrapAdmin({ username: bootstrapForm.username.trim() }, bootstrapForm.bootstrapToken.trim())
    const refreshedTokens = await refreshTokens(userStore.refreshToken)
    userStore.setTokens(refreshedTokens.accessToken, refreshedTokens.refreshToken)
    userStore.setUserInfo(parseAccessTokenUserInfo(refreshedTokens.accessToken))
    bootstrapForm.bootstrapToken = ''
    ElMessage.success('管理员初始化成功，当前登录态已刷新')
    await loadAdminUsers(1)
  } catch (err: any) {
    ElMessage.error(err.message || '管理员初始化失败')
  } finally {
    bootstrapSubmitting.value = false
  }
}

async function submitAddressForm() {
  const valid = await addressFormRef.value?.validate().catch(() => false)
  if (!valid) return

  addressSubmitting.value = true
  try {
    if (editingAddressId.value) {
      await updateMyAddress(editingAddressId.value, {
        receiverName: addressForm.value.receiverName,
        receiverPhone: addressForm.value.receiverPhone,
        province: addressForm.value.province,
        city: addressForm.value.city,
        district: addressForm.value.district,
        detailAddress: addressForm.value.detailAddress,
        postalCode: addressForm.value.postalCode,
      })
      ElMessage.success('地址已更新')
    } else {
      await createMyAddress(addressForm.value)
      ElMessage.success('地址已新增')
    }

    addressDialogVisible.value = false
    await loadAddresses()
  } catch (err: any) {
    ElMessage.error(err.message || '保存地址失败')
  } finally {
    addressSubmitting.value = false
  }
}

async function handleUpdateUserRole(user: AdminUser, role: UserRole) {
  if (user.role === role) {
    return
  }

  adminSubmitting.value = true
  try {
    await updateAdminUserRole(user.userId, { role })
    if (user.userId === userStore.userId) {
      const refreshedTokens = await refreshTokens(userStore.refreshToken)
      userStore.setTokens(refreshedTokens.accessToken, refreshedTokens.refreshToken)
      userStore.setUserInfo(parseAccessTokenUserInfo(refreshedTokens.accessToken))
    }
    ElMessage.success(`已将 ${user.username} 设为 ${role}`)
    await loadAdminUsers(adminPage.pageNum)
  } catch (err: any) {
    ElMessage.error(err.message || '更新用户角色失败')
  } finally {
    adminSubmitting.value = false
  }
}

function resetAdminFilters() {
  adminFilters.keyword = ''
  adminFilters.role = ''
  adminFilters.status = ''
  void loadAdminUsers(1)
}

function handleAdminPageChange(page: number) {
  void loadAdminUsers(page)
}

async function handleDeleteAddress(address: UserAddress) {
  await ElMessageBox.confirm(`确定删除地址“${address.receiverName}”吗？`, '提示', { type: 'warning' })
  try {
    await deleteMyAddress(address.id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch (err: any) {
    ElMessage.error(err.message || '删除地址失败')
  }
}

async function handleSetDefaultAddress(address: UserAddress) {
  if (address.isDefault) return
  try {
    await setDefaultMyAddress(address.id)
    ElMessage.success('已设为默认地址')
    await loadAddresses()
  } catch (err: any) {
    ElMessage.error(err.message || '设置默认地址失败')
  }
}

onMounted(() => {
  bootstrapForm.username = userStore.nickname
  void loadProfile()
  void loadAddresses()
  if (isAdmin.value) {
    void loadAdminUsers(1)
  }
})
</script>

<template>
  <div>
    <h2 class="text-lg font-bold mb-6">个人中心</h2>

    <el-card shadow="never" v-loading="profileLoading">
      <el-descriptions title="基本信息" :column="1" border>
        <el-descriptions-item label="用户 ID">{{ profile?.userId || userStore.userId }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.nickname }}</el-descriptions-item>
        <el-descriptions-item label="当前角色">{{ currentRoleText }}</el-descriptions-item>
        <el-descriptions-item label="最近浏览数">{{ profile?.recentBrowseCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="历史购买数">{{ profile?.purchaseCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="价格偏好">
          <span v-if="profile">
            {{ profile.priceBandPreference.min }} - {{ profile.priceBandPreference.max }}
            （典型价位 {{ profile.priceBandPreference.typical }}）
          </span>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="偏好类目">
          <div class="flex flex-wrap gap-2">
            <el-tag
              v-for="category in profile?.preferredCategories || []"
              :key="category.categoryId"
              size="small"
            >
              {{ category.categoryName }}（{{ Math.round(category.weight * 100) }}%）
            </el-tag>
            <span v-if="!profile?.preferredCategories?.length" class="text-gray-400">暂无画像数据</span>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <div class="mt-6 flex gap-4">
        <router-link to="/order">
          <el-button>我的订单</el-button>
        </router-link>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="mt-6">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">管理员工具</span>
          <el-tag :type="isAdmin ? 'danger' : 'info'">{{ currentRoleText }}</el-tag>
        </div>
      </template>

      <template v-if="isAdmin">
        <div class="mb-4 grid gap-3 md:grid-cols-4">
          <el-input v-model="adminFilters.keyword" placeholder="按昵称搜索" clearable />
          <el-select v-model="adminFilters.role" placeholder="角色" clearable>
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
          <el-select v-model="adminFilters.status" placeholder="状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <div class="flex gap-2">
            <el-button type="primary" @click="loadAdminUsers(1)">查询</el-button>
            <el-button @click="resetAdminFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="adminLoading" :data="adminUsers" border>
          <el-table-column prop="username" label="昵称" min-width="160" />
          <el-table-column prop="userId" label="用户 ID" min-width="180" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="168">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="168">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180" fixed="right">
            <template #default="{ row }">
              <div class="flex gap-2">
                <el-button
                  size="small"
                  type="danger"
                  :disabled="row.role === 'ADMIN' || adminSubmitting"
                  @click="handleUpdateUserRole(row, 'ADMIN')"
                >
                  设为管理员
                </el-button>
                <el-button
                  size="small"
                  :disabled="row.role === 'USER' || adminSubmitting"
                  @click="handleUpdateUserRole(row, 'USER')"
                >
                  设为普通用户
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-4 flex justify-end">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :total="adminPage.total"
            :page-size="adminPage.pageSize"
            :current-page="adminPage.pageNum"
            @current-change="handleAdminPageChange"
          />
        </div>
      </template>

      <template v-else>
        <el-alert
          title="首个管理员初始化"
          type="warning"
          :closable="false"
          description="仅在系统还没有可用管理员时可成功。初始化成功后会自动刷新当前登录态。"
          class="mb-4"
        />
        <el-form label-width="120px" style="max-width: 640px">
          <el-form-item label="目标用户名">
            <el-input v-model="bootstrapForm.username" placeholder="默认当前昵称，也可填写其他已注册用户名" />
          </el-form-item>
          <el-form-item label="Bootstrap Token">
            <el-input
              v-model="bootstrapForm.bootstrapToken"
              type="password"
              show-password
              placeholder="请输入 SHOP_ADMIN_BOOTSTRAP_TOKEN"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="bootstrapSubmitting" @click="handleBootstrapAdmin">
              初始化首个管理员
            </el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>

    <el-card shadow="never" class="mt-6" v-loading="addressLoading">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">收货地址管理</span>
          <el-button type="primary" @click="openCreateAddressDialog">新增地址</el-button>
        </div>
      </template>

      <div v-if="addresses.length" class="flex flex-col gap-4">
        <div
          v-for="address in addresses"
          :key="address.id"
          class="rounded-lg border border-gray-100 p-4"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <div class="flex items-center gap-2">
                <span class="font-medium">{{ address.receiverName }}</span>
                <span class="text-gray-500">{{ address.receiverPhone }}</span>
                <el-tag v-if="address.isDefault" size="small" type="danger">默认</el-tag>
              </div>
              <p class="mt-2 text-sm text-gray-500">
                {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}
              </p>
              <p v-if="address.postalCode" class="mt-1 text-xs text-gray-400">邮编：{{ address.postalCode }}</p>
            </div>

            <div class="flex gap-2">
              <el-button v-if="!address.isDefault" size="small" @click="handleSetDefaultAddress(address)">
                设为默认
              </el-button>
              <el-button size="small" @click="openEditAddressDialog(address)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteAddress(address)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="还没有收货地址，先新增一个吧" />
    </el-card>

    <el-dialog
      v-model="addressDialogVisible"
      :title="editingAddressId ? '编辑收货地址' : '新增收货地址'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="88px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入收货手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="addressForm.province" placeholder="请输入省份" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="addressForm.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="addressForm.district" placeholder="请输入区县" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item label="邮编">
          <el-input v-model="addressForm.postalCode" placeholder="选填" />
        </el-form-item>
        <el-form-item v-if="!editingAddressId">
          <el-checkbox v-model="addressForm.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="addressDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="addressSubmitting" @click="submitAddressForm">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    // ---------- 默认布局（Header + Footer） ----------
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/pages/home/HomePage.vue') },
        { path: 'product/:id', name: 'productDetail', component: () => import('@/pages/product/ProductDetailPage.vue') },
        { path: 'search', name: 'search', component: () => import('@/pages/search/SearchPage.vue') },
        { path: 'seckill', name: 'seckill', component: () => import('@/pages/seckill/SeckillPage.vue') },
        { path: 'chat', name: 'chat', component: () => import('@/pages/chat/ChatPage.vue') },
        // 需要登录
        { path: 'cart', name: 'cart', meta: { requiresAuth: true }, component: () => import('@/pages/cart/CartPage.vue') },
        { path: 'order', name: 'orderList', meta: { requiresAuth: true }, component: () => import('@/pages/order/OrderListPage.vue') },
        { path: 'order/confirm', name: 'orderConfirm', meta: { requiresAuth: true }, component: () => import('@/pages/order/OrderConfirmPage.vue') },
        { path: 'order/:id', name: 'orderDetail', meta: { requiresAuth: true }, component: () => import('@/pages/order/OrderDetailPage.vue') },
        { path: 'user', name: 'userProfile', meta: { requiresAuth: true }, component: () => import('@/pages/user/UserProfilePage.vue') },
      ],
    },
    // ---------- 空白布局（登录页） ----------
    {
      path: '/login',
      component: () => import('@/layouts/BlankLayout.vue'),
      children: [
        { path: '', name: 'login', component: () => import('@/pages/login/LoginPage.vue') },
      ],
    },
    // ---------- 404 ----------
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 路由守卫：检查登录态
router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, absorbTokenFromUrl } from '@supermarket/utils'

const publicPaths = ['/login', '/register']

const routes = [
  {
    path: '/',
    component: () => import('../layouts/Default.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../pages/home/HomePage.vue') },
      { path: 'search', name: 'search', component: () => import('../pages/search/SearchPage.vue') },
      { path: 'product/:id', name: 'product', component: () => import('../pages/product/ProductPage.vue') },
      { path: 'cart', name: 'cart', meta: { requiresAuth: true }, component: () => import('../pages/cart/CartPage.vue') },
      { path: 'order/confirm', name: 'orderConfirm', meta: { requiresAuth: true }, component: () => import('../pages/order/ConfirmPage.vue') },
      { path: 'order/result', name: 'orderResult', meta: { requiresAuth: true }, component: () => import('../pages/order/ResultPage.vue') },
      { path: 'order/:no', name: 'orderDetail', meta: { requiresAuth: true }, component: () => import('../pages/order/DetailPage.vue') },
      { path: 'user/orders', name: 'userOrders', meta: { requiresAuth: true }, component: () => import('../pages/user/OrdersPage.vue') },
      { path: 'user/center', name: 'userCenter', meta: { requiresAuth: true }, component: () => import('../pages/user/CenterPage.vue') },
      { path: 'user/profile', name: 'userProfile', meta: { requiresAuth: true }, component: () => import('../pages/user/ProfileEditPage.vue') },
      { path: 'user/addresses', name: 'addresses', meta: { requiresAuth: true }, component: () => import('../pages/user/AddressPage.vue') },
      { path: 'user/coupons', name: 'coupons', meta: { requiresAuth: true }, component: () => import('../pages/user/CouponPage.vue') },
      { path: 'user/reviews', name: 'reviews', meta: { requiresAuth: true }, component: () => import('../pages/user/ReviewPage.vue') },
      { path: 'user/notifications', name: 'notifications', meta: { requiresAuth: true }, component: () => import('../pages/notify/NotifyPage.vue') },
      { path: 'seckill', name: 'seckill', component: () => import('../pages/seckill/SeckillPage.vue') },
    ],
  },
  { path: '/login', name: 'login', component: () => import('../pages/login/LoginPage.vue') },
  { path: '/register', name: 'register', component: () => import('../pages/login/RegisterPage.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to, _from, next) => {
  // Absorb SSO token from URL
  absorbTokenFromUrl()

  // Public paths always allowed
  if (publicPaths.includes(to.path)) return next()

  // Auth-required routes
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  next()
})

export default router

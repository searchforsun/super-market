import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, hasRole, absorbTokenFromUrl, ROLE_MERCHANT, ROLE_ADMIN } from '@supermarket/utils'

const publicPaths = ['/login', '/register']

const routes = [
  { path: '/', redirect: '/merchant' },
  { path: '/login', name: 'login', component: () => import('../pages/login/LoginPage.vue') },
  { path: '/register', name: 'register', component: () => import('../pages/login/RegisterPage.vue') },
  {
    path: '/merchant', component: () => import('../layouts/Default.vue'),
    meta: { requiresRole: true },
    children: [
      { path: '', name: 'dashboard', component: () => import('../pages/dashboard/DashboardPage.vue') },
      { path: 'products', name: 'products', component: () => import('../pages/products/ListPage.vue') },
      { path: 'products/create', name: 'productCreate', component: () => import('../pages/products/CreatePage.vue') },
      { path: 'inventory', name: 'inventory', component: () => import('../pages/inventory/InventoryPage.vue') },
      { path: 'orders', name: 'merchantOrders', component: () => import('../pages/orders/OrdersPage.vue') },
      { path: 'orders/:no', name: 'merchantOrderDetail', component: () => import('../pages/orders/DetailPage.vue') },
      { path: 'shop/settings', name: 'shopSettings', component: () => import('../pages/shop/ShopPage.vue') },
      { path: 'coupons', name: 'merchantCoupons', component: () => import('../pages/coupons/CouponsPage.vue') },
      { path: 'seckill', name: 'merchantSeckill', component: () => import('../pages/seckill/SeckillPage.vue') },
      { path: 'reviews', name: 'merchantReviews', component: () => import('../pages/reviews/ReviewsPage.vue') },
      { path: 'notifications', name: 'merchantNotify', component: () => import('../pages/notifications/NotificationsPage.vue') },
      { path: 'settlement', name: 'settlement', component: () => import('../pages/settlement/SettlementPage.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to, _from, next) => {
  absorbTokenFromUrl()

  if (publicPaths.includes(to.path)) return next()

  if (!isAuthenticated()) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (to.meta.requiresRole && !hasRole(ROLE_MERCHANT) && !hasRole(ROLE_ADMIN)) {
    return next({ path: '/login', query: { error: 'no_permission' } })
  }

  next()
})

export default router

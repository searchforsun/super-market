import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, hasRole, absorbTokenFromUrl, ROLE_ADMIN } from '@supermarket/utils'

const publicPaths = ['/login']

const routes = [
  { path: '/', redirect: '/admin' },
  { path: '/login', name: 'login', component: () => import('../pages/login/LoginPage.vue') },
  {
    path: '/admin', component: () => import('../layouts/Default.vue'),
    meta: { requiresRole: true },
    children: [
      { path: '', name: 'adminDashboard', component: () => import('../pages/dashboard/DashboardPage.vue') },
      { path: 'merchants', name: 'merchants', component: () => import('../pages/merchants/MerchantPage.vue') },
      { path: 'products/audit', name: 'productAudit', component: () => import('../pages/products/AuditPage.vue') },
      { path: 'categories', name: 'categories', component: () => import('../pages/categories/CategoryPage.vue') },
      { path: 'banners', name: 'banners', component: () => import('../pages/banners/BannerPage.vue') },
      { path: 'risk', name: 'risk', component: () => import('../pages/risk/RiskPage.vue') },
      { path: 'reports', name: 'reports', component: () => import('../pages/reports/ReportPage.vue') },
      { path: 'notify-templates', name: 'notifyTemplates', component: () => import('../pages/notify/TemplatePage.vue') },
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

  if (to.meta.requiresRole && !hasRole(ROLE_ADMIN)) {
    return next({ path: '/login', query: { error: 'no_permission' } })
  }

  next()
})

export default router

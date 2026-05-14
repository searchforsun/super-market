import { createRouter, createWebHistory } from 'vue-router'
export default createRouter({ history: createWebHistory(), routes: [
  { path: '/', redirect: '/admin' },
  { path: '/login', name: 'login', component: () => import('../pages/login/LoginPage.vue') },
  { path: '/admin', component: () => import('../layouts/Default.vue'), children: [
    { path: '', name:'adminDashboard', component: () => import('../pages/dashboard/DashboardPage.vue') },
    { path: 'merchants', name:'merchants', component: () => import('../pages/merchants/MerchantPage.vue') },
    { path: 'products/audit', name:'productAudit', component: () => import('../pages/products/AuditPage.vue') },
    { path: 'categories', name:'categories', component: () => import('../pages/categories/CategoryPage.vue') },
    { path: 'banners', name:'banners', component: () => import('../pages/banners/BannerPage.vue') },
    { path: 'risk', name:'risk', component: () => import('../pages/risk/RiskPage.vue') },
    { path: 'reports', name:'reports', component: () => import('../pages/reports/ReportPage.vue') },
    { path: 'notify-templates', name:'notifyTemplates', component: () => import('../pages/notify/TemplatePage.vue') },
  ]
}]})
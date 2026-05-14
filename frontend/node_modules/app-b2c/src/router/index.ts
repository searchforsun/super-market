import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../layouts/Default.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../pages/home/HomePage.vue') },
      { path: 'search', name: 'search', component: () => import('../pages/search/SearchPage.vue') },
      { path: 'product/:id', name: 'product', component: () => import('../pages/product/ProductPage.vue') },
      { path: 'cart', name: 'cart', component: () => import('../pages/cart/CartPage.vue') },
      { path: 'order/confirm', name: 'orderConfirm', component: () => import('../pages/order/ConfirmPage.vue') },
      { path: 'order/result', name: 'orderResult', component: () => import('../pages/order/ResultPage.vue') },
      { path: 'order/:no', name: 'orderDetail', component: () => import('../pages/order/DetailPage.vue') },
      { path: 'user/orders', name: 'userOrders', component: () => import('../pages/user/OrdersPage.vue') },
      { path: 'user/center', name: 'userCenter', component: () => import('../pages/user/CenterPage.vue') },
      { path: 'user/addresses', name: 'addresses', component: () => import('../pages/user/AddressPage.vue') },
      { path: 'user/coupons', name: 'coupons', component: () => import('../pages/user/CouponPage.vue') },
      { path: 'user/reviews', name: 'reviews', component: () => import('../pages/user/ReviewPage.vue') },
      { path: 'user/notifications', name: 'notifications', component: () => import('../pages/notify/NotifyPage.vue') },
      { path: 'seckill', name: 'seckill', component: () => import('../pages/seckill/SeckillPage.vue') },
    ],
  },
  { path: '/login', name: 'login', component: () => import('../pages/login/LoginPage.vue') },
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

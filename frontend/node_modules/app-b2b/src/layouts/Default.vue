<template>
  <div class="b2b-layout">
    <div class="b2b-header"><router-link to="/merchant" class="b2b-logo">Super Market 商家中心</router-link></div>
    <div style="display:flex">
      <div class="b2b-sidebar">
        <router-link to="/merchant">📊 首页看板</router-link>
        <router-link to="/merchant/products">📦 商品管理</router-link>
        <router-link to="/merchant/inventory">📋 库存管理</router-link>
        <router-link to="/merchant/orders">📝 订单处理</router-link>
        <router-link to="/merchant/shop/settings">🏪 店铺设置</router-link>
        <router-link to="/merchant/coupons">🎫 优惠券</router-link>
        <router-link to="/merchant/seckill">⚡ 秒杀活动</router-link>
        <router-link to="/merchant/reviews">⭐ 评价管理</router-link>
        <router-link to="/merchant/settlement">💰 结算查询</router-link>
      </div>
      <div class="b2b-content"><router-view /></div>
    </div>
  </div>
</template>
<style scoped>
.b2b-layout{min-height:100vh;background:#f5f7fa}
.b2b-header{background:#3b82f6;color:#fff;padding:12px 24px;font-size:18px;font-weight:700}
.b2b-logo{color:#fff;text-decoration:none}
.b2b-sidebar{width:180px;background:#fff;min-height:calc(100vh - 48px);padding:12px 0;border-right:1px solid #eee}
.b2b-sidebar a{display:block;padding:10px 20px;font-size:13px;color:#333;text-decoration:none}
.b2b-sidebar a:hover,.b2b-sidebar a.router-link-active{background:#eff6ff;color:#3b82f6}
.b2b-content{flex:1;padding:20px}
</style>

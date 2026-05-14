<template>
  <div class="admin-layout">
    <div class="admin-header">Super Market 运营管理平台</div>
    <div style="display:flex">
      <div class="admin-sidebar">
        <router-link to="/admin">📊 运营看板</router-link>
        <router-link to="/admin/merchants">🏪 商家审核</router-link>
        <router-link to="/admin/products/audit">📦 商品审核</router-link>
        <router-link to="/admin/categories">📂 类目管理</router-link>
        <router-link to="/admin/banners">🖼️ Banner/广告位</router-link>
        <router-link to="/admin/risk">🛡️ 风控管理</router-link>
        <router-link to="/admin/reports">📈 数据报表</router-link>
        <router-link to="/admin/notify-templates">📧 通知模板</router-link>
      </div>
      <div class="admin-content"><router-view /></div>
    </div>
  </div>
</template>
<style scoped>
.admin-layout{min-height:100vh;background:#f5f5f5}
.admin-header{background:#8b5cf6;color:#fff;padding:12px 24px;font-size:18px;font-weight:700}
.admin-sidebar{width:180px;background:#fff;min-height:calc(100vh - 48px);padding:12px 0;border-right:1px solid #eee}
.admin-sidebar a{display:block;padding:10px 20px;font-size:13px;color:#333;text-decoration:none}
.admin-sidebar a:hover,.admin-sidebar a.router-link-active{background:#f5f3ff;color:#8b5cf6}
.admin-content{flex:1;padding:20px}
</style>

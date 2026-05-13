# 微服务注册表

| 序号 | 模块目录 | 服务名 | 中文名称 | 端口 | 业务域 | 数据库 | 包名 |
|------|----------|--------|----------|------|--------|--------|------|
| 1 | service-user | user-service | 用户服务 | 9301 | 用户域 | db_user | com.supermarket.user |
| 2 | service-auth | auth-service | 认证授权服务 | 9302 | 用户域 | db_user | com.supermarket.auth |
| 3 | service-member | member-service | 会员服务 | 9303 | 用户域 | db_user | com.supermarket.member |
| 4 | service-address | address-service | 地址服务 | 9304 | 用户域 | db_user | com.supermarket.address |
| 5 | service-product | product-service | 商品基础服务 | 9311 | 商品域 | db_product | com.supermarket.product |
| 6 | service-category | category-service | 类目服务 | 9312 | 商品域 | db_product | com.supermarket.category |
| 7 | service-inventory | inventory-service | 库存服务 | 9313 | 商品域 | db_product | com.supermarket.inventory |
| 8 | service-review | review-service | 评价服务 | 9314 | 商品域 | db_product | com.supermarket.review |
| 9 | service-cart | cart-service | 购物车服务 | 9321 | 交易域 | db_order | com.supermarket.cart |
| 10 | service-order | order-service | 订单服务 | 9322 | 交易域 | db_order | com.supermarket.order |
| 11 | service-payment | payment-service | 支付服务 | 9331 | 支付域 | db_payment | com.supermarket.payment |
| 12 | service-coupon | coupon-service | 优惠券服务 | 9351 | 营销域 | db_marketing | com.supermarket.coupon |
| 13 | service-seckill | seckill-service | 秒杀服务 | 9352 | 营销域 | db_marketing | com.supermarket.seckill |
| 14 | service-search | search-service | 搜索服务 | 9361 | 搜索域 | db_product | com.supermarket.search |
| 15 | service-shop | shop-service | 商家店铺服务 | 9341 | 商家域 | db_shop | com.supermarket.shop |
| 16 | service-platform | platform-service | 运营平台服务 | 9381 | 平台域 | db_platform | com.supermarket.platform |
| 17 | service-file | file-service | 文件服务 | 9371 | 基础支撑域 | db_product | com.supermarket.file |
| 18 | service-notify | notify-service | 通知服务 | 9372 | 基础支撑域 | db_product | com.supermarket.notify |

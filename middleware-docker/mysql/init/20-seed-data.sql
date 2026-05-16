-- Seed Data for Super Market
-- Categories, Products, SKUs, Inventory, Shops

-- ============ 类目数据 ============
INSERT INTO db_product.categories (id, parent_id, name, level, sort_order, status) VALUES
(1,  0,  '手机通讯', 1, 1, 1),
(2,  1,  '智能手机', 2, 1, 1),
(3,  1,  '老人机',   2, 2, 1),
(4,  2,  '苹果手机', 3, 1, 1),
(5,  2,  '安卓手机', 3, 2, 1),
(6,  0,  '电脑办公', 1, 2, 1),
(7,  6,  '笔记本',   2, 1, 1),
(8,  7,  '轻薄本',   3, 1, 1),
(9,  7,  '游戏本',   3, 2, 1),
(10, 0,  '食品生鲜', 1, 3, 1),
(11, 10, '休闲零食', 2, 1, 1),
(12, 10, '粮油调味', 2, 2, 1);

-- ============ 品牌数据 ============
INSERT INTO db_product.brands (id, name, logo_url, status) VALUES
(1, 'Apple',  'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15-pro-max.jpg', 1),
(2, '华为',    'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-mate60-pro.jpg', 1),
(3, '小米',    'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/xiaomi-14-ultra.jpg', 1),
(4, '联想',    'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/thinkpad-x1-carbon.jpg', 1),
(5, '良品铺子','http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/mixed-nuts.jpg', 1),
(6, '金龙鱼',  'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/olive-oil.jpg', 1);

-- ============ 店铺数据 ============
INSERT INTO db_shop.shops (id, merchant_id, shop_name, shop_logo, shop_desc, status) VALUES
(1, 1, 'Apple 官方旗舰店', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15-pro-max.jpg', 'Apple 官方授权旗舰店', 1),
(2, 2, '华为官方旗舰店',   'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-mate60-pro.jpg', '华为官方授权旗舰店', 1),
(3, 3, '良品铺子旗舰店',   'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/mixed-nuts.jpg', '高端零食品牌', 1);

-- ============ SPU 商品数据 ============
INSERT INTO db_product.spu (id, spu_no, shop_id, category_id, brand_id, name, subtitle, main_image, description, audit_status, shelf_status, is_deleted) VALUES
(1001, 'SPU1001', 1, 4, 1, 'iPhone 15 Pro Max 256GB',   'A17 Pro芯片 | 钛金属设计 | 4800万像素', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15-pro-max.jpg',      '<p>6.7英寸超视网膜XDR显示屏</p>', 1, 1, 0),
(1002, 'SPU1002', 1, 4, 1, 'iPhone 15 128GB',           'A16芯片 | 灵动岛 | USB-C接口',             'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15.jpg',        '<p>6.1英寸超视网膜XDR显示屏</p>', 1, 1, 0),
(1003, 'SPU1003', 2, 5, 2, '华为 Mate 60 Pro 512GB',    '卫星通话 | 昆仑玻璃 | 5000万像素',          'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-mate60-pro.jpg',       '<p>6.82英寸OLED曲面屏</p>', 1, 1, 0),
(1004, 'SPU1004', 2, 5, 2, '华为 Pura 70 Ultra 256GB',  '伸缩摄像头 | 超聚光影像 | 100W快充',        'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-pura70-ultra.jpg',         '<p>6.8英寸LTPO OLED屏幕</p>', 1, 1, 0),
(1005, 'SPU1005', 2, 5, 3, '小米 14 Ultra 256GB',       '骁龙8Gen3 | 徕卡光学 | 5000万四摄',         'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/xiaomi-14-ultra.jpg',           '<p>6.73英寸2K AMOLED屏幕</p>', 1, 1, 0),
(1006, 'SPU1006', 1, 8, 4, '联想 ThinkPad X1 Carbon',   '第12代酷睿 | 14英寸2.8K | 轻薄商务',        'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/thinkpad-x1-carbon.jpg',      '<p>重量仅1.12kg</p>', 1, 1, 0),
(1007, 'SPU1007', 1, 9, 4, '联想 拯救者 Y9000P',        'i9-14900HX | RTX 4070 | 16英寸2.5K 240Hz', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/legion-y9000p.jpg',          '<p>顶级电竞笔记本</p>', 1, 1, 0),
(1008, 'SPU1008', 3, 11, 5, '良品铺子 每日坚果 750g',    '7种坚果果干 | 科学配比 | 每日一包',          'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/mixed-nuts.jpg',            '<p>30袋独立包装</p>', 1, 1, 0),
(1009, 'SPU1009', 3, 12, 6, '金龙鱼 食用油 5L',          '非转基因 | 物理压榨 | 炒菜专用',              'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/olive-oil.jpg',             '<p>一级压榨花生油</p>', 1, 1, 0),
(1010, 'SPU1010', 3, 12, 6, '金龙鱼 大米 10kg',          '东北五常 | 稻花香2号 | 真空包装',             'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/rice.jpg',            '<p>2025年新米</p>', 1, 1, 0);

-- ============ SKU 数据 (stock in inventory table) ============
INSERT INTO db_product.sku (id, sku_no, spu_id, spec_name, image, price, market_price, status) VALUES
(2001, 'SKU2001', 1001, '原色钛金属 256GB', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15-pro-max.jpg',  9999.00, 10999.00, 1),
(2002, 'SKU2002', 1001, '蓝色钛金属 256GB', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15-pro-max.jpg',     9999.00, 10999.00, 1),
(2003, 'SKU2003', 1001, '原色钛金属 512GB', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15-pro-max.jpg', 11999.00, 12999.00, 1),
(2004, 'SKU2004', 1002, '黑色 128GB',       'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15.jpg',      5999.00,  6999.00, 1),
(2005, 'SKU2005', 1002, '蓝色 128GB',       'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15.jpg',       5999.00,  6999.00, 1),
(2006, 'SKU2006', 1002, '粉色 256GB',       'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/iphone-15.jpg',       6999.00,  7999.00, 1),
(2007, 'SKU2007', 1003, '雅丹黑 512GB',      'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-mate60-pro.jpg',    6999.00,  7999.00, 1),
(2008, 'SKU2008', 1003, '白沙银 1TB',        'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-mate60-pro.jpg',   7999.00,  8999.00, 1),
(2009, 'SKU2009', 1004, '香颂绿 256GB',      'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-pura70-ultra.jpg',      9999.00, 10999.00, 1),
(2010, 'SKU2010', 1004, '摩卡棕 512GB',      'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/huawei-pura70-ultra.jpg',     10999.00, 11999.00, 1),
(2011, 'SKU2011', 1005, '黑色 256GB',        'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/xiaomi-14-ultra.jpg',        5999.00,  6499.00, 1),
(2012, 'SKU2012', 1005, '白色 512GB',        'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/xiaomi-14-ultra.jpg',        6499.00,  6999.00, 1),
(2013, 'SKU2013', 1006, 'i7-1365U 16G 512G', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/thinkpad-x1-carbon.jpg',        8999.00,  9999.00, 1),
(2014, 'SKU2014', 1006, 'i7-1370P 32G 1TB',  'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/thinkpad-x1-carbon.jpg',       11999.00, 12999.00, 1),
(2015, 'SKU2015', 1007, 'i9+4070 16G 1TB',   'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/legion-y9000p.jpg',            9999.00, 10999.00, 1),
(2016, 'SKU2016', 1007, 'i9+4070 32G 2TB',   'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/legion-y9000p.jpg',           11999.00, 12999.00, 1),
(2017, 'SKU2017', 1008, '混合装 750g (30袋)',  'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/mixed-nuts.jpg',              79.90,   99.90, 1),
(2018, 'SKU2018', 1009, '压榨一级花生油 5L',   'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/olive-oil.jpg',              89.90,  109.90, 1),
(2019, 'SKU2019', 1009, '压榨一级花生油 1.8L', 'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/olive-oil.jpg',              36.90,   45.90, 1),
(2020, 'SKU2020', 1010, '五常大米 10kg',       'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/rice.jpg',              59.90,   69.90, 1),
(2021, 'SKU2021', 1010, '五常大米 5kg',        'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/rice.jpg',              32.90,   39.90, 1);

-- ============ 库存初始数据 ============
INSERT INTO db_product.inventory (id, sku_id, total_stock, available_stock, locked_stock, safety_stock, version) VALUES
(1, 2001, 50,  50, 0, 5,  0),
(2, 2002, 35,  35, 0, 5,  0),
(3, 2003, 20,  20, 0, 3,  0),
(4, 2004, 80,  80, 0, 5,  0),
(5, 2005, 60,  60, 0, 5,  0),
(6, 2006, 40,  40, 0, 5,  0),
(7, 2007, 25,  25, 0, 3,  0),
(8, 2008, 15,  15, 0, 3,  0),
(9, 2009, 30,  30, 0, 3,  0),
(10, 2010, 18,  18, 0, 3,  0),
(11, 2011, 45,  45, 0, 5,  0),
(12, 2012, 30,  30, 0, 5,  0),
(13, 2013, 15,  15, 0, 2,  0),
(14, 2014, 8,   8,  0, 2,  0),
(15, 2015, 20,  20, 0, 3,  0),
(16, 2016, 10,  10, 0, 2,  0),
(17, 2017, 200, 200, 0, 20, 0),
(18, 2018, 150, 150, 0, 15, 0),
(19, 2019, 200, 200, 0, 20, 0),
(20, 2020, 300, 300, 0, 30, 0),
(21, 2021, 400, 400, 0, 40, 0);

-- ============ 追加示例商品（真实 MinIO 图片） ============
-- 图片由 docker-compose/minio/init/init-minio.sh 初始化到 MinIO
-- 图片源: GSMArena (手机), ZOL中关村在线 (笔记本), Unsplash (食品)

INSERT INTO db_product.spu (id, spu_no, shop_id, category_id, brand_id, name, subtitle, main_image, description, audit_status, shelf_status, is_deleted) VALUES
(1011, 'SPU1011', 3, 10, 5, '良品铺子 纯牛奶 250ml*16盒',   '源自优质牧场 | 高钙高蛋白 | 早餐必备',              'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/milk.jpg',              '<p>每100ml含3.2g优质乳蛋白，营养健康每一天</p>',                          1, 1, 0),
(1012, 'SPU1012', 3, 12, 6, '金龙鱼 特级初榨橄榄油 1L',     '西班牙进口 | 冷压初榨 | 适合中式烹饪',              'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/olive-oil.jpg',         '<p>精选西班牙优质橄榄，物理冷压初榨工艺，保留橄榄果香</p>',            1, 1, 0),
(1013, 'SPU1013', 3, 11, 5, '良品铺子 每日坚果 750g',       '7种坚果果干 | 科学配比 | 每日一包',                'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/mixed-nuts.jpg',        '<p>精选腰果、巴旦木、核桃、蔓越莓等7种坚果果干，30袋独立包装</p>',    1, 1, 0),
(1014, 'SPU1014', 3, 11, 5, '曼可顿 全麦面包 400g',         '高纤维全麦 | 低脂健康 | 早餐代餐',                  'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/whole-wheat-bread.jpg', '<p>选用优质全麦粉，高膳食纤维，低脂健康，切片即食</p>',              1, 1, 0),
(1015, 'SPU1015', 3, 12, 6, '金龙鱼 东北五常大米 10kg',     '五常稻花香2号 | 2025年新米 | 真空包装',             'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/rice.jpg',              '<p>源自东北五常核心产区，稻花香2号品种，颗粒饱满，饭香浓郁</p>',      1, 1, 0),
(1016, 'SPU1016', 3, 10, NULL, '佳沛 新西兰金奇异果 12粒装', '阳光金果 | 维C丰富 | 进口水果',                    'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/kiwifruit.jpg',         '<p>新西兰原箱进口，阳光充足，果肉金黄，香甜多汁</p>',              1, 1, 0),
(1017, 'SPU1017', 3, 11, NULL, '星巴克 进口咖啡豆 250g',     '中度烘焙 | 阿拉比卡 | 醇香浓郁',                    'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/coffee-beans.jpg',      '<p>100%阿拉比卡咖啡豆，中度烘焙，带有焦糖和坚果风味</p>',         1, 1, 0),
(1018, 'SPU1018', 3, 10, NULL, '德清源 鲜鸡蛋 30枚装',       '谷物喂养 | 无抗生素 | 安全可追溯',                  'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/eggs.jpg',              '<p>精选谷物喂养鸡蛋，蛋黄饱满，蛋清浓稠，喷码追溯更安心</p>',        1, 1, 0);

-- ============ SKU 数据 ============
INSERT INTO db_product.sku (id, sku_no, spu_id, spec_name, image, price, market_price, status) VALUES
(2022, 'SKU2022', 1011, '纯牛奶 250ml*16盒',         'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/milk.jpg',              59.90,  79.90, 1),
(2023, 'SKU2023', 1012, '特级初榨橄榄油 1L',         'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/olive-oil.jpg',         89.90, 119.00, 1),
(2024, 'SKU2024', 1013, '混合坚果 750g (30袋)',       'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/mixed-nuts.jpg',        79.90,  99.90, 1),
(2025, 'SKU2025', 1014, '全麦面包切片 400g',         'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/whole-wheat-bread.jpg', 15.90,  19.90, 1),
(2026, 'SKU2026', 1015, '五常大米 10kg',              'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/rice.jpg',              59.90,  69.90, 1),
(2027, 'SKU2027', 1016, '金奇异果 12粒 (单果100g+)',  'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/kiwifruit.jpg',         49.90,  69.90, 1),
(2028, 'SKU2028', 1017, '中度烘焙咖啡豆 250g',        'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/coffee-beans.jpg',      89.00, 108.00, 1),
(2029, 'SKU2029', 1018, '鲜鸡蛋 30枚',                'http://__DOCKER_HOST_IP__:__MINIO_API_PORT__/smt-product/eggs.jpg',              36.90,  45.90, 1);

-- ============ 库存初始数据 ============
INSERT INTO db_product.inventory (id, sku_id, total_stock, available_stock, locked_stock, safety_stock, version) VALUES
(22, 2022, 200, 200, 0, 20, 0),
(23, 2023, 150, 150, 0, 15, 0),
(24, 2024, 300, 300, 0, 30, 0),
(25, 2025, 180, 180, 0, 20, 0),
(26, 2026, 250, 250, 0, 25, 0),
(27, 2027, 120, 120, 0, 10, 0),
(28, 2028, 100, 100, 0, 10, 0),
(29, 2029, 350, 350, 0, 30, 0);

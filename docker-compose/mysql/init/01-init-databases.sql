-- 创建业务数据库（垂直分库）
CREATE DATABASE IF NOT EXISTS db_user       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_product    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_order      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_payment    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_marketing  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_shop       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_platform   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建基础设施数据库
CREATE DATABASE IF NOT EXISTS xxl_job       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS seata         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS canal_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建只读用户（读写分离预留）
GRANT SELECT, INSERT, UPDATE, DELETE ON db_user.*       TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_product.*    TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_order.*      TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_payment.*    TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_marketing.*  TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_shop.*       TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_platform.*   TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON xxl_job.*       TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON seata.*         TO 'app_user'@'%';
FLUSH PRIVILEGES;

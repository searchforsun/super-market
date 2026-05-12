#!/bin/bash
# 批量生成微服务脚手架脚本
# 基于 user-service 模板，生成其余 17 个服务

set -e

TEMPLATE_DIR="super-market-services/service-user"

if [ ! -d "$TEMPLATE_DIR" ]; then
    echo "Error: Template directory $TEMPLATE_DIR not found. Run Task C2 first."
    exit 1
fi

SERVICES=(
  "service-auth:9002:com.supermarket.auth:AuthApplication:db_user"
  "service-member:9003:com.supermarket.member:MemberApplication:db_user"
  "service-address:9004:com.supermarket.address:AddressApplication:db_user"
  "service-product:9011:com.supermarket.product:ProductApplication:db_product"
  "service-category:9012:com.supermarket.category:CategoryApplication:db_product"
  "service-inventory:9013:com.supermarket.inventory:InventoryApplication:db_product"
  "service-review:9014:com.supermarket.review:ReviewApplication:db_product"
  "service-cart:9021:com.supermarket.cart:CartApplication:db_order"
  "service-order:9022:com.supermarket.order:OrderApplication:db_order"
  "service-payment:9031:com.supermarket.payment:PaymentApplication:db_payment"
  "service-coupon:9051:com.supermarket.coupon:CouponApplication:db_marketing"
  "service-seckill:9052:com.supermarket.seckill:SeckillApplication:db_marketing"
  "service-search:9061:com.supermarket.search:SearchApplication:db_product"
  "service-shop:9041:com.supermarket.shop:ShopApplication:db_shop"
  "service-platform:9081:com.supermarket.platform:PlatformApplication:db_platform"
  "service-file:9071:com.supermarket.file:FileApplication:db_product"
  "service-notify:9072:com.supermarket.notify:NotifyApplication:db_product"
)

for svc in "${SERVICES[@]}"; do
  IFS=':' read -r dir port pkg class db <<< "$svc"
  echo "Generating $dir..."

  TARGET="super-market-services/$dir"
  mkdir -p "$TARGET/src/main/java/${pkg//.//}/config"
  mkdir -p "$TARGET/src/main/resources"
  mkdir -p "$TARGET/src/test/java/${pkg//.//}"

  # pom.xml - replace user-service specifics
  SVC_DESC="${dir#service-}"
  sed -e "s/service-user/$dir/g" \
      -e "s/user-service/${dir#service-}-service/g" \
      -e "s/UserApplication/$class/g" \
      -e "s/用户服务/${SVC_DESC}服务/g" \
      "$TEMPLATE_DIR/pom.xml" > "$TARGET/pom.xml"

  # Application.java
  PKG_PATH="${pkg//.//}"
  cat > "$TARGET/src/main/java/$PKG_PATH/${class}.java" << JEOF
package $pkg;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@SpringBootApplication(scanBasePackages = {
    "$pkg",
    "com.supermarket.common"
})
public class $class {
    public static void main(String[] args) {
        SpringApplication.run($class.class, args);
    }
}
JEOF

  # DubboProviderConfig.java
  cat > "$TARGET/src/main/java/$PKG_PATH/config/DubboProviderConfig.java" << JEOF
package $pkg.config;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@DubboComponentScan(basePackages = "$pkg")
public class DubboProviderConfig {
}
JEOF

  # application.yml
  SVC_NAME="${dir#service-}-service"
  sed -e "s/user-service/$SVC_NAME/g" \
      -e "s/9001/$port/g" \
      "$TEMPLATE_DIR/src/main/resources/application.yml" > "$TARGET/src/main/resources/application.yml"

  # application-dev.yml - replace database
  sed "s/db_user/$db/g" \
      "$TEMPLATE_DIR/src/main/resources/application-dev.yml" > "$TARGET/src/main/resources/application-dev.yml"

  # bootstrap.yml (copy as-is)
  cp "$TEMPLATE_DIR/src/main/resources/bootstrap.yml" "$TARGET/src/main/resources/bootstrap.yml"

  echo "  Done: $dir ($SVC_NAME :$port -> $db)"
done

echo ""
echo "All 17 services generated successfully."

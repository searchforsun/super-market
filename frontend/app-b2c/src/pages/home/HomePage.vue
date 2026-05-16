<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-content">
          <h1 class="hero-title">品质生活<br><span class="hero-accent">从这里开始</span></h1>
          <p class="hero-subtitle">精选全球好物，为你的每一天注入灵感与品质</p>
          <div class="hero-actions">
            <router-link to="/search?categoryId=4" class="hero-btn primary">探索新品</router-link>
            <router-link to="/seckill" class="hero-btn secondary">限时秒杀</router-link>
          </div>
        </div>
        <div class="hero-visual">
          <div class="hero-card card-1">
            <span class="hero-card-icon">&#x1F4F1;</span>
            <span class="hero-card-label">数码</span>
          </div>
          <div class="hero-card card-2">
            <span class="hero-card-icon">&#x1F4BB;</span>
            <span class="hero-card-label">电脑</span>
          </div>
          <div class="hero-card card-3">
            <span class="hero-card-icon">&#x1F96C;</span>
            <span class="hero-card-label">零食</span>
          </div>
        </div>
      </div>
      <div class="hero-dot-pattern"></div>
    </section>

    <!-- Category Pills -->
    <section class="categories">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">浏览分类</h2>
          <router-link to="/search" class="section-link">查看全部 &rarr;</router-link>
        </div>
        <div class="category-pills">
          <router-link v-for="c in categories" :key="c.id" :to="`/search?categoryId=${c.id}`"
            class="category-pill">
            <span class="pill-icon">{{ c.icon }}</span>
            <span class="pill-name">{{ c.name }}</span>
          </router-link>
        </div>
      </div>
    </section>

    <!-- Featured Products -->
    <section class="featured">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">为你推荐</h2>
          <router-link to="/search?sort=sales" class="section-link">更多热销 &rarr;</router-link>
        </div>
        <div v-loading="loading" class="product-grid">
          <ProductCard v-for="p in products" :key="p.spuId ?? p.id" :image="p.mainImage"
            :title="p.name" :price="p.minPrice" :original-price="p.maxPrice"
            :tag="tags[p.spuId ?? p.id]"
            @click="$router.push(`/product/${p.spuId ?? p.id}`)" />
        </div>
        <div v-if="!loading && products.length === 0" class="empty-section">
          暂无商品，请稍后访问
        </div>
      </div>
    </section>

    <!-- Seckill Teaser -->
    <section class="seckill-teaser">
      <div class="container">
        <div class="teaser-card">
          <div class="teaser-text">
            <h2 class="teaser-title">&#9889; 限时秒杀</h2>
            <p class="teaser-desc">每日精选好物，低至 5 折起</p>
            <router-link to="/seckill" class="teaser-btn">立即抢购</router-link>
          </div>
          <div class="teaser-visual">
            <span class="teaser-emoji">&#x23F0;</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCategoryTree, getProductList } from '@supermarket/api'
import { ProductCard } from '@supermarket/ui'

const categories = ref<any[]>([])
const products = ref<any[]>([])
const loading = ref(false)
const tags: Record<number, string> = {}

const icons: Record<number, string> = {
  1: '\u{1F4F1}', 4: '\u{1F4F1}', 5: '\u{1F4F1}',
  6: '\u{1F4BB}', 7: '\u{1F4BB}', 8: '\u{1F4BB}', 9: '\u{1F4BB}',
  10: '\u{1F96C}', 11: '\u{1F36A}', 12: '\u{1FADB}',
}

onMounted(async () => {
  try {
    const data: any = await getCategoryTree()
    categories.value = (data || []).slice(0, 8).map((c: any) => ({
      ...c, icon: icons[c.id] || '\u{1F6CD}',
    }))
  } catch {}

  loading.value = true
  try {
    const res: any = await getProductList({ auditStatus: 1, page: 1, size: 8 })
    const records = res.records || []
    if (records[0]) tags[records[0].spuId ?? records[0].id] = '热卖'
    if (records[1]) tags[records[1].spuId ?? records[1].id] = '新品'
    products.value = records
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
/* ===== Hero ===== */
.hero {
  background: linear-gradient(160deg, #faf8f5 0%, #f0ebe0 40%, #e8ded0 100%);
  position: relative; overflow: hidden;
  padding: var(--space-3xl) 0;
}
.hero-inner {
  display: flex; align-items: center;
  justify-content: space-between;
  position: relative; z-index: 1;
}
.hero-content { max-width: 520px; }
.hero-title {
  font-family: var(--font-display);
  font-size: clamp(36px, 5vw, 56px);
  line-height: 1.15;
  color: var(--color-text-primary);
  font-weight: 600;
  margin-bottom: var(--space-lg);
}
.hero-accent { color: var(--color-accent); font-style: italic; }
.hero-subtitle {
  font-size: 16px; color: var(--color-text-secondary);
  line-height: 1.6; margin-bottom: var(--space-xl);
  max-width: 400px;
}
.hero-actions { display: flex; gap: var(--space-md); }
.hero-btn {
  padding: 12px 28px; border-radius: var(--radius-full);
  font-size: 14px; font-weight: 600;
  transition: all var(--duration-base) var(--ease-out);
}
.hero-btn.primary {
  background: var(--color-text-primary);
  color: var(--color-text-inverse);
}
.hero-btn.primary:hover {
  background: #000; transform: translateY(-1px);
  box-shadow: var(--shadow-lg);
}
.hero-btn.secondary {
  border: 1.5px solid var(--color-border);
  color: var(--color-text-primary);
}
.hero-btn.secondary:hover { border-color: var(--color-text-primary); }
.hero-visual { display: flex; gap: var(--space-md); flex-shrink: 0; }
.hero-card {
  width: 120px; height: 140px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: var(--space-sm);
  box-shadow: var(--shadow-md);
  transition: transform var(--duration-base) var(--ease-out);
}
.hero-card:hover { transform: translateY(-4px); }
.hero-card.card-1 { margin-top: 20px; }
.hero-card.card-2 { margin-top: -10px; }
.hero-card.card-3 { margin-top: 40px; }
.hero-card-icon { font-size: 32px; }
.hero-card-label { font-size: 13px; color: var(--color-text-secondary); font-weight: 500; }
.hero-dot-pattern {
  position: absolute; right: -80px; top: -80px;
  width: 400px; height: 400px;
  background: radial-gradient(circle, var(--color-accent-soft) 2px, transparent 2px);
  background-size: 24px 24px;
  opacity: 0.5; pointer-events: none;
}

/* ===== Sections ===== */
.section-header {
  display: flex; align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
}
.section-title {
  font-family: var(--font-display);
  font-size: 24px; font-weight: 600;
  color: var(--color-text-primary);
}
.section-link {
  font-size: 13px; color: var(--color-text-muted);
  transition: color var(--duration-fast);
}
.section-link:hover { color: var(--color-text-primary); }

/* ===== Categories ===== */
.categories { padding: var(--space-2xl) 0; }
.category-pills { display: flex; gap: var(--space-md); flex-wrap: wrap; }
.category-pill {
  display: flex; align-items: center; gap: var(--space-sm);
  padding: 10px 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-full);
  font-size: 14px; color: var(--color-text-secondary);
  transition: all var(--duration-fast) var(--ease-out);
}
.category-pill:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: var(--color-accent-light);
}
.pill-icon { font-size: 18px; }
.pill-name { font-weight: 500; }

/* ===== Products ===== */
.featured { padding: var(--space-xl) 0 var(--space-3xl); }
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md);
}
.empty-section {
  text-align: center; padding: var(--space-3xl);
  color: var(--color-text-muted); font-size: 14px;
}

/* ===== Seckill Teaser ===== */
.seckill-teaser { padding-bottom: var(--space-2xl); }
.teaser-card {
  background: linear-gradient(135deg, #1a1816 0%, #2d2824 100%);
  border-radius: var(--radius-xl);
  padding: var(--space-2xl);
  display: flex; align-items: center;
  justify-content: space-between;
  overflow: hidden; position: relative;
}
.teaser-card::before {
  content: '';
  position: absolute; right: -40px; top: -40px;
  width: 200px; height: 200px;
  background: radial-gradient(circle, rgba(196,30,58,0.3) 0%, transparent 70%);
}
.teaser-title {
  font-family: var(--font-display);
  font-size: 28px; color: var(--color-text-inverse);
  margin-bottom: var(--space-sm);
}
.teaser-desc {
  font-size: 14px; color: rgba(255,255,255,0.6);
  margin-bottom: var(--space-lg);
}
.teaser-btn {
  display: inline-block;
  background: var(--color-accent);
  color: var(--color-text-inverse);
  padding: 10px 24px; border-radius: var(--radius-full);
  font-size: 14px; font-weight: 600;
  transition: background var(--duration-fast);
}
.teaser-btn:hover { background: var(--color-accent-hover); }
.teaser-visual { position: relative; z-index: 1; }
.teaser-emoji { font-size: 64px; }

@media (max-width: 768px) {
  .hero-visual { display: none; }
  .product-grid { grid-template-columns: repeat(2, 1fr); }
  .teaser-card { flex-direction: column; text-align: center; }
}
</style>

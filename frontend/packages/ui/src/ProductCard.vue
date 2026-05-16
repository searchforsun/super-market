<template>
  <article class="product-card" @click="$emit('click')">
    <div class="card-image">
      <img :src="image || placeholderImage" :alt="title" loading="lazy" />
      <span v-if="tag" class="card-tag">{{ tag }}</span>
      <div class="card-overlay">
        <span class="overlay-text">查看详情</span>
      </div>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ title }}</h3>
      <div class="card-price-row">
        <PriceDisplay :price="price" :original-price="originalPrice" />
      </div>
      <div class="card-meta" v-if="sales || rating">
        <span v-if="sales" class="meta-sales">{{ sales }}+ 已售</span>
        <RatingStars v-if="rating" :rating="rating" :size="12" />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import PriceDisplay from './PriceDisplay.vue'
import RatingStars from './RatingStars.vue'

defineProps<{
  image: string
  title: string
  price: number
  originalPrice?: number
  sales?: number
  rating?: number
  tag?: string
}>()

defineEmits<{ click: [] }>()

const placeholderImage = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300" fill="#e8e3db"><rect width="300" height="300"/><text x="150" y="155" text-anchor="middle" fill="#a09890" font-size="14">暂无图片</text></svg>'
)
</script>

<style scoped>
.product-card {
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  border: 1px solid var(--color-border-light, #f0ece6);
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(26, 24, 22, 0.10);
  border-color: transparent;
}

/* Image */
.card-image {
  position: relative;
  aspect-ratio: 1;
  background: var(--color-bg, #faf8f5);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.card-image img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.product-card:hover .card-image img {
  transform: scale(1.06);
}
.card-tag {
  position: absolute; top: 10px; left: 10px;
  background: var(--color-accent, #c41e3a);
  color: #fff;
  font-size: 11px; font-weight: 600;
  padding: 3px 10px; border-radius: var(--radius-sm, 4px);
  letter-spacing: 0.02em;
  z-index: 2;
}
.card-overlay {
  position: absolute; inset: 0;
  background: rgba(26, 24, 22, 0.03);
  display: flex; align-items: center; justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.product-card:hover .card-overlay { opacity: 1; }
.overlay-text {
  color: var(--color-text-primary, #1a1816);
  font-size: 13px; font-weight: 600;
  padding: 8px 18px;
  background: rgba(255,255,255,0.9);
  border-radius: var(--radius-full, 9999px);
  backdrop-filter: blur(4px);
}

/* Body */
.card-body { padding: 14px 16px 16px; }
.card-title {
  font-size: 14px; font-weight: 500; line-height: 1.4;
  color: var(--color-text-primary, #1a1816);
  overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  margin-bottom: 8px;
}
.card-price-row { margin-bottom: 6px; }
.card-meta {
  display: flex; justify-content: space-between;
  align-items: center; font-size: 11px;
  color: var(--color-text-muted, #a09890);
}
.meta-sales { white-space: nowrap; }
</style>

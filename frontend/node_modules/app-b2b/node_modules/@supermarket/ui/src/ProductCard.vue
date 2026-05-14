<template>
  <div class="product-card" @click="$emit('click')">
    <div class="card-img">
      <img :src="image" :alt="title" />
      <span v-if="tag" class="card-tag">{{ tag }}</span>
    </div>
    <div class="card-body">
      <p class="card-title">{{ title }}</p>
      <div class="card-price">
        <PriceDisplay :price="price" :originalPrice="originalPrice" />
      </div>
      <div class="card-meta">
        <span v-if="sales > 0">{{ sales }}+ 已售</span>
        <RatingStars v-if="rating" :rating="rating" :size="12" />
      </div>
    </div>
  </div>
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
</script>

<style scoped>
.product-card {
  background: #fff; border-radius: 4px; overflow: hidden; cursor: pointer;
  transition: box-shadow 0.2s; width: 220px;
}
.product-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,.1); }
.card-img { position: relative; height: 220px; display: flex; align-items: center; justify-content: center; }
.card-img img { max-width: 100%; max-height: 100%; }
.card-tag { position: absolute; top: 6px; left: 6px; background: #e1251b; color: #fff; font-size: 11px; padding: 1px 6px; border-radius: 2px; }
.card-body { padding: 10px; }
.card-title { font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; }
.card-price { margin-bottom: 4px; }
.card-meta { display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: #999; }
</style>

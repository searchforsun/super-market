<template>
  <div class="review-page page-enter"><h2>我的评价</h2>
    <div v-for="r in reviews" :key="r.id" class="review-item">
      <RatingStars :rating="r.rating" /><p>{{ r.content }}</p>
      <span class="date">{{ r.createdAt }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useUserStore } from '@supermarket/stores'
import { getReviewsByUser } from '@supermarket/api'; import { RatingStars } from '@supermarket/ui'
const userStore = useUserStore(); const reviews = ref<any[]>([])
onMounted(async () => { const res: any = await getReviewsByUser(userStore.userId); reviews.value = res.records || [] })
</script>
<style scoped>
.review-page{background:#fff;padding:20px;border-radius:4px}
.review-item{padding:10px 0;border-bottom:1px solid #eee}.date{font-size:11px;color:#999}
</style>

import { ref, onMounted, onUnmounted, type Ref } from 'vue'

export function useRevealOnScroll(
  targetRef: Ref<HTMLElement | null>,
  options?: { threshold?: number; rootMargin?: string }
) {
  const revealed = ref(false)
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (!targetRef.value) return
    observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          revealed.value = true
          observer?.unobserve(entry.target)
        }
      },
      { threshold: options?.threshold ?? 0.1, rootMargin: options?.rootMargin ?? '0px 0px -40px 0px' }
    )
    observer.observe(targetRef.value)
  })

  onUnmounted(() => observer?.disconnect())

  return { revealed }
}

import { ref, onMounted, onUnmounted, watch, type Ref } from 'vue'
import * as echarts from 'echarts'

function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function chartColors(): { text: string; accent: string; surface: string; border: string; success: string; warning: string; danger: string } {
  return {
    text: getCssVar('--color-text-secondary') || '#666',
    accent: getCssVar('--color-accent') || '#2563eb',
    surface: getCssVar('--color-surface') || '#f8fafc',
    border: getCssVar('--color-border') || '#e2e8f0',
    success: getCssVar('--color-success') || '#059669',
    warning: getCssVar('--color-warning') || '#d97706',
    danger: getCssVar('--color-danger') || '#dc2626',
  }
}

export function useECharts(
  containerRef: Ref<HTMLElement | null>,
  optionFn: () => echarts.EChartsOption
) {
  const instance = ref<echarts.ECharts | null>(null)

  function initChart() {
    if (!containerRef.value) return
    instance.value = echarts.init(containerRef.value)
    instance.value.setOption(optionFn())
  }

  function refresh() {
    if (instance.value) {
      instance.value.resize()
      instance.value.setOption(optionFn(), true)
    }
  }

  let observer: MutationObserver | null = null

  onMounted(() => {
    initChart()
    window.addEventListener('resize', refresh)

    // Watch theme changes
    observer = new MutationObserver(() => {
      if (instance.value) {
        instance.value.setOption(optionFn(), true)
      }
    })
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  })

  onUnmounted(() => {
    window.removeEventListener('resize', refresh)
    observer?.disconnect()
    instance.value?.dispose()
  })

  return { instance, refresh, chartColors }
}

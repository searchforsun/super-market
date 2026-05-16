import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../HomePage.vue'

vi.mock('@supermarket/api', () => ({
  getCategoryTree: vi.fn().mockResolvedValue([
    { id: 1, name: '手机' },
    { id: 2, name: '电脑' },
  ]),
  getProductList: vi.fn().mockResolvedValue({
    records: [
      { spuId: 101, name: '商品A', minPrice: 1999, mainImage: '/a.jpg' },
      { spuId: 102, name: '商品B', minPrice: 2999, mainImage: '/b.jpg' },
    ],
  }),
}))

describe('HomePage', () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/', component: HomePage }],
    })
  })

  it('mounts without errors', async () => {
    const wrapper = mount(HomePage, {
      global: {
        plugins: [router],
        stubs: {
          'router-link': true,
          'ProductCard': true,
        },
        directives: {
          loading: vi.fn(),
        },
      },
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('renders hero section title', async () => {
    const wrapper = mount(HomePage, {
      global: {
        plugins: [router],
        stubs: {
          'router-link': true,
          'ProductCard': true,
        },
        directives: {
          loading: vi.fn(),
        },
      },
    })
    expect(wrapper.text()).toContain('品质生活')
  })

  it('renders section headers', async () => {
    const wrapper = mount(HomePage, {
      global: {
        plugins: [router],
        stubs: {
          'router-link': true,
          'ProductCard': true,
        },
        directives: {
          loading: vi.fn(),
        },
      },
    })
    expect(wrapper.text()).toContain('浏览分类')
    expect(wrapper.text()).toContain('为你推荐')
    expect(wrapper.text()).toContain('限时秒杀')
  })
})

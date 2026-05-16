import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ProductCard from '../ProductCard.vue'

describe('ProductCard', () => {
  it('renders product title', () => {
    const wrapper = mount(ProductCard, {
      props: {
        image: '/test.jpg',
        title: 'Test Product',
        price: 99,
      },
    })
    expect(wrapper.text()).toContain('Test Product')
  })

  it('renders price', () => {
    const wrapper = mount(ProductCard, {
      props: {
        image: '/test.jpg',
        title: 'Test',
        price: 88.8,
      },
    })
    expect(wrapper.text()).toContain('88.8')
  })

  it('renders sales count', () => {
    const wrapper = mount(ProductCard, {
      props: {
        image: '/test.jpg',
        title: 'Test',
        price: 50,
        sales: 999,
      },
    })
    expect(wrapper.text()).toContain('999+')
  })

  it('renders tag when provided', () => {
    const wrapper = mount(ProductCard, {
      props: {
        image: '/test.jpg',
        title: 'Test',
        price: 50,
        tag: '热卖',
      },
    })
    expect(wrapper.text()).toContain('热卖')
  })

  it('emits click event when clicked', async () => {
    const wrapper = mount(ProductCard, {
      props: {
        image: '/test.jpg',
        title: 'Test',
        price: 50,
      },
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
    expect(wrapper.emitted('click')?.length).toBe(1)
  })
})

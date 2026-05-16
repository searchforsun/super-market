import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SkuSelector from '../SkuSelector.vue'

describe('SkuSelector', () => {
  const specs = [
    { name: '颜色', values: ['红色', '蓝色', '黑色'] },
    { name: '尺寸', values: ['S', 'M', 'L'] },
  ]

  it('renders spec names and values', () => {
    const wrapper = mount(SkuSelector, {
      props: { specs },
    })
    expect(wrapper.text()).toContain('颜色')
    expect(wrapper.text()).toContain('尺寸')
    expect(wrapper.text()).toContain('红色')
    expect(wrapper.text()).toContain('M')
    expect(wrapper.text()).toContain('L')
  })

  it('emits select event with spec name and value on click', async () => {
    const wrapper = mount(SkuSelector, {
      props: { specs },
    })
    const redButton = wrapper.findAll('.spec-val')[0]
    await redButton.trigger('click')
    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')?.[0]).toEqual(['颜色', '红色'])
  })

  it('adds active class to clicked value', async () => {
    const wrapper = mount(SkuSelector, {
      props: { specs },
    })
    const blueButton = wrapper.findAll('.spec-val')[1]
    await blueButton.trigger('click')
    expect(blueButton.classes()).toContain('active')
  })
})

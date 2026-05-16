import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchBar from '../SearchBar.vue'

describe('SearchBar', () => {
  it('renders default placeholder', () => {
    const wrapper = mount(SearchBar)
    const input = wrapper.find('input')
    expect(input.attributes('placeholder')).toBe('搜索你想要的商品...')
  })

  it('renders custom placeholder', () => {
    const wrapper = mount(SearchBar, {
      props: { placeholder: '搜索商品...' },
    })
    const input = wrapper.find('input')
    expect(input.attributes('placeholder')).toBe('搜索商品...')
  })

  it('emits search with trimmed keyword on Enter', async () => {
    const wrapper = mount(SearchBar)
    const input = wrapper.find('input')
    await input.setValue('  手机  ')
    await input.trigger('keyup.enter')
    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]).toEqual(['手机'])
  })

  it('emits search with trimmed keyword on button click', async () => {
    const wrapper = mount(SearchBar)
    const input = wrapper.find('input')
    await input.setValue('笔记本')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')?.[0]).toEqual(['笔记本'])
  })

  it('emits empty string when keyword is only whitespace', async () => {
    const wrapper = mount(SearchBar)
    const input = wrapper.find('input')
    await input.setValue('   ')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('search')?.[0]).toEqual([''])
  })
})

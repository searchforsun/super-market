import { defineConfig } from 'vitest/config'
import { resolve } from 'path'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@supermarket/ui': resolve(__dirname, 'packages/ui/src'),
      '@supermarket/api': resolve(__dirname, 'packages/api/src'),
      '@supermarket/stores': resolve(__dirname, 'packages/stores/src'),
      '@supermarket/utils': resolve(__dirname, 'packages/utils/src'),
    },
  },
  test: {
    environment: 'jsdom',
    include: [
      'packages/utils/src/**/*.test.ts',
      'packages/stores/src/**/*.test.ts',
      'packages/ui/src/**/*.test.ts',
      'app-b2c/src/**/*.test.ts',
    ],
  },
})

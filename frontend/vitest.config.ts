import { defineConfig } from 'vitest/config'
import { resolve } from 'path'

export default defineConfig({
  resolve: {
    alias: {
      '@supermarket/ui': resolve(__dirname, 'packages/ui/src'),
      '@supermarket/api': resolve(__dirname, 'packages/api/src'),
      '@supermarket/stores': resolve(__dirname, 'packages/stores/src'),
      '@supermarket/utils': resolve(__dirname, 'packages/utils/src'),
    },
  },
  test: {
    environment: 'node',
    include: ['packages/utils/src/**/*.test.ts', 'packages/stores/src/**/*.test.ts'],
  },
})

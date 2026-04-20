import { defineConfig, presetUno } from 'unocss'

export default defineConfig({
  presets: [presetUno()],
  shortcuts: {
    'flex-center': 'flex items-center justify-center',
    'flex-between': 'flex items-center justify-between',
    'page-container': 'max-w-1200px mx-auto px-4',
  },
  theme: {
    colors: {
      primary: '#ff5000',
      danger: '#f56c6c',
      success: '#67c23a',
    },
  },
})

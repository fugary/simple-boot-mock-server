import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv, splitVendorChunkPlugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'
import vueJsx from '@vitejs/plugin-vue-jsx'
import eslint from 'vite-plugin-eslint'
import { visualizer } from 'rollup-plugin-visualizer'
import packageJson from './package.json'

const optionalPlugins = [{
  plugin: visualizer({ open: true }),
  enabled: false
}, {
  plugin: viteMockServe({
    mockPath: './mock'
  }),
  enabled: true
}, {
  plugin: splitVendorChunkPlugin(),
  enabled: true
}].filter(p => p.enabled).map(p => p.plugin)

const JS_FILE_NAMES = 'js/[name]-[hash].js'
const CSS_FILE_NAMES = 'css/[name]-[hash].css'
const IMG_EXT_LIST = ['.png', '.jpg', '.gif', '.svg', '.bmp', '.webp']
const IMG_FILE_NAMES = 'images/[name]-[hash].[ext]'

// https://vitejs.dev/config/
export default ({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return defineConfig({
    base: env.VITE_APP_CONTEXT_PATH,
    define: {
      'import.meta.env.VITE_APP_VERSION': JSON.stringify(packageJson.version)
    },
    plugins: [vue(), vueJsx(), eslint(), ...optionalPlugins],
    esbuild: {
      drop: mode === 'production' ? ['console', 'debugger'] : []
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      sourcemap: true,
      rollupOptions: {
        output: {
          chunkFileNames: JS_FILE_NAMES, // 引入文件名的名称
          entryFileNames: JS_FILE_NAMES, // 包的入口文件名称
          assetFileNames (assetInfo) {
            if (assetInfo.name?.endsWith('.css')) { // CSS文件
              return CSS_FILE_NAMES
            } else if (IMG_EXT_LIST.some((ext) => assetInfo.name?.endsWith(ext))) { // 图片
              return IMG_FILE_NAMES
            }
            return 'assets/[name]-[hash].[ext]' // 其他资源
          },
          manualChunks (id) {
            if (id.includes('element-plus')) {
              return 'elp'
            }
          }
        }
      }
    },
    worker: {
      rollupOptions: {
        output: {
          assetFileNames: JS_FILE_NAMES
        }
      }
    }
  })
}

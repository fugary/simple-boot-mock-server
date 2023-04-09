// 模块占用大小分析插件
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
// moment过滤部分locale插件
const MomentLocalesPlugin = require('moment-locales-webpack-plugin')
const TerserPlugin = require('terser-webpack-plugin')
const port = process.env.port || process.env.npm_config_port || 8080 // port
module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  filenameHashing: true,
  productionSourceMap: false, // 生产环境是否产生sourceMap？
  configureWebpack: config => {
    const plugins = [
      // new BundleAnalyzerPlugin(),
      new MomentLocalesPlugin({
        localesToKeep: ['zh-cn']
      })]
    if (process.env.NODE_ENV === 'production') {
      // 为生产环境修改配置...
    } else {
      // 为开发环境修改配置...
    }
    return {
      plugins,
      optimization: {
        minimizer: [
          new TerserPlugin({
            terserOptions: {
              warnings: false,
              parse: {},
              compress: {
                drop_console: true,
                drop_debugger: true
              }
            }
          })
        ]
      }
    }
  },
  devServer: {
    host: '0.0.0.0',
    port: port,
    https: false
    // See https://github.com/vuejs/vue-cli/blob/dev/docs/cli-service.md#configuring-proxy
    // 服务器端配置cors之后，测试环境也不再使用proxy配置
    // proxy: {
    //   '/': {
    //     target: 'http://127.0.0.1:9086/', // 需要请求的地址
    //     changeOrigin: true // 是否跨域
    //   }
    // }
  }
}

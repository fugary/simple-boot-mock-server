import { config } from 'md-editor-v3'
import hljs from 'highlight.js/lib/common'
import mermaid from 'mermaid'
import katex from 'katex'
import 'katex/dist/katex.min.css'

export const MD_AUTO_FOLD_THRESHOLD = 200

export const initMarkdownTheme = isDark => {
  mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: isDark ? 'dark' : 'default' })
}

// 与参考项目一致，修正 ~~~ 围栏的闭合判断，避免未完成的流式图表提前渲染。
export const mermaidFencePlugin = md => {
  const renderAttrs = md.renderer.renderAttrs.bind(md.renderer)
  md.renderer.renderAttrs = token => {
    if (token.type === 'fence' && token.info === 'mermaid' && token.markup.startsWith('~') &&
      token.map && token.attrGet('data-closed') !== null) {
      const blockLines = token.map[1] - token.map[0]
      const contentLines = token.content ? token.content.replace(/\n$/, '').split('\n').length : 0
      token.attrSet('data-closed', String(blockLines === contentLines + 2))
    }
    return renderAttrs(token)
  }
}

// 仅在当前预览内定位，避免多个回答含同名标题时跳到其他区域。
export const handleMarkdownLink = event => {
  const link = event.target.closest('a')
  const href = link?.getAttribute('href')
  if (!href?.startsWith('#')) return
  event.preventDefault()
  try {
    const id = decodeURIComponent(href.slice(1))
    const target = Array.from(event.currentTarget.querySelectorAll('[id]'))
      .find(element => element.id === id || element.id === encodeURIComponent(id))
    target?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  } catch {
    // 忽略无效的锚点编码。
  }
}

config({
  editorExtensions: {
    highlight: { instance: hljs },
    mermaid: { instance: mermaid },
    katex: { instance: katex }
  },
  mermaidConfig: base => ({ ...base, startOnLoad: false, securityLevel: 'strict' }),
  markdownItConfig: md => md.use(mermaidFencePlugin)
})

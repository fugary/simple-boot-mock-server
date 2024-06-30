import { INSTALL_ICONS } from '@/icons'
import { chunk } from 'lodash-es'

/**
 * @param keywords {string}
 * @param colSize {number}
 * @returns {{id: number, icons: *}[]}
 */
export const filterIconsByKeywords = (keywords, colSize) => {
  let installIcons = INSTALL_ICONS
  if (keywords) {
    installIcons = installIcons.filter(icon => {
      keywords = keywords.trim()
      if (keywords.includes(' ')) {
        return keywords.split(/\s+/).every(k => icon.toLowerCase().includes(k.toLowerCase()))
      } else {
        return icon.toLowerCase().includes(keywords.toLowerCase())
      }
    })
  }
  return chunk(installIcons, colSize).map((arr, idx) => {
    return {
      id: idx,
      icons: arr
    }
  })
}

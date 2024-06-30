import { $httpPost } from '@/vendors/axios'
import { $i18nMsg } from '@/messages'

/**
 * @typedef {Object} CityDto
 * @property {string} code
 * @property {string} nameCn
 * @property {string} nameEn
 */
/**
 * 加载可选城市数据
 * @return {{success:boolean, message:string, resultData: {cityList:[CityDto]}}}
 */
export const loadSelectCities = (query, config) => {
  return $httpPost('/city/selectCities', query, config)
}
/**
 * 加载自动完成城市数据
 * @return {{success:boolean, message:string, resultData: {cityList:[CityDto]}}}
 */
export const loadAutoCities = (data, config) => {
  return $httpPost('/city/autoCities', data, config)
}
/**
 * 城市自动完成配置
 * @return {CommonAutocompleteOption}
 */
export const useCityAutocompleteConfig = () => {
  return {
    idProp: 'code',
    labelProp: $i18nMsg('nameCn', 'nameEn'),
    columns: [{
      label: $i18nMsg('代码', 'Code'),
      property: 'code'
    }, {
      label: $i18nMsg('中文名', 'CN Name'),
      property: 'nameCn'
    }, {
      label: $i18nMsg('英文名', 'EN Name'),
      property: 'nameEn'
    }],
    searchMethod ({ page }, cb) {
      loadAutoCities({ page }) // {query, page}
        .then(result => {
          const data = {
            page: result.resultData.page,
            items: result.resultData.cityList
          }
          cb(data)
        })
    }
  }
}
/**
 * 程序可选项配置
 */
export const useCitySelectPageConfig = () => {
  return {
    tabs: [{
      label: $i18nMsg('热门', 'Hot'),
      id: '0'
    }, {
      label: 'A-F',
      id: 'A-F'
    }, {
      label: 'G-J',
      id: 'G-J'
    }, {
      label: 'K-N',
      id: 'K-N'
    }, {
      label: 'P-W',
      id: 'P-W'
    }, {
      label: 'X-Z',
      id: 'X-Z'
    }],
    searchMethod (id, cb) {
      loadSelectCities({ id })
        .then(result => {
          console.info('================selectCities', result)
          cb(result.resultData.cityList)
        })
    }
  }
}

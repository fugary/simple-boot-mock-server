import { $i18nBundle, $i18nKey } from '@/messages'
import { formatDate, formatJsonStr } from '@/utils'
import { isFunction } from 'lodash-es'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { MOCK_LOAD_BALANCE_OPTIONS } from '@/consts/MockConstants'

export const getLoadBalancerLabel = (request) => {
  const loadBalancer = request.loadBalancer
  const labelKey = MOCK_LOAD_BALANCE_OPTIONS.find(opt => opt.value === loadBalancer)?.labelKey
  return labelKey ? $i18nBundle(labelKey) : loadBalancer
}

export const getRequestHistoryViewOptions = (request, history) => {
  return [
    { labelKey: 'mock.label.requestPath', prop: 'requestPath' },
    { labelKey: 'mock.label.version', prop: () => `${request.version ?? ''}${request.current ? ` <${$i18nBundle('mock.label.current')}>` : ''}` },
    { labelKey: 'common.label.modifyDate', prop: () => formatDate(request[history ? 'createDate' : 'modifyDate']) },
    { labelKey: 'common.label.modifier', prop: () => request[history ? 'creator' : 'modifier'] },
    { labelKey: 'mock.label.method', prop: 'method' },
    { labelKey: 'mock.label.proxyUrl', prop: 'proxyUrl' },
    { labelKey: 'common.label.status', prop: () => [$i18nBundle('common.label.statusDisabled'), $i18nBundle('common.label.statusEnabled')][request.status] },
    {
      labelKey: 'mock.label.disabledMock',
      prop: () => request.disableMock !== undefined ? $i18nBundle(request.disableMock ? 'common.label.yes' : 'common.label.no') : ''
    },
    { labelKey: 'common.label.delay', prop: 'delay' },
    { labelKey: 'mock.label.matchPattern', prop: 'matchPattern' },
    { labelKey: 'mock.label.loadBalanceType', prop: () => request.loadBalancer ? getLoadBalancerLabel(request) : '' },
    { labelKey: 'mock.label.requestName', prop: 'requestName' },
    { labelKey: 'common.label.description', prop: 'description' },
    { label: $i18nKey('common.label.commonTest', 'mock.label.queryParams'), prop: () => formatJsonStr(request.mockParams) }
  ]
}

export const getDataHistoryViewOptions = (data, history) => {
  return [
    { labelKey: 'mock.label.statusCode', prop: 'statusCode' },
    { labelKey: 'mock.label.version', prop: () => `${data.version ?? ''}${data.current ? ` <${$i18nBundle('mock.label.current')}>` : ''}` },
    { labelKey: 'common.label.modifyDate', prop: () => formatDate(data[history ? 'createDate' : 'modifyDate']) },
    { labelKey: 'common.label.modifier', prop: () => data[history ? 'creator' : 'modifier'] },
    { labelKey: 'mock.label.responseName', prop: 'dataName' },
    { labelKey: 'common.label.status', prop: () => [$i18nBundle('common.label.statusDisabled'), $i18nBundle('common.label.statusEnabled')][data.status] },
    {
      labelKey: 'mock.label.default',
      prop: () => data.defaultFlag !== undefined ? $i18nBundle(data.defaultFlag ? 'common.label.yes' : 'common.label.no') : ''
    },
    { labelKey: 'common.label.delay', prop: 'delay' },
    { labelKey: 'mock.label.matchPattern', prop: 'matchPattern' },
    { labelKey: 'mock.label.responseHeaders', prop: () => formatJsonStr(data.headers) },
    { label: 'Content Type', prop: 'contentType' },
    { label: 'Charset', prop: 'defaultCharset' },
    { labelKey: 'mock.label.dataFormat', prop: 'responseFormat' },
    { labelKey: 'mock.label.mockResponseBody', prop: 'responseBody' },
    { labelKey: 'common.label.description', prop: 'description' },
    { label: $i18nKey('common.label.commonTest', 'mock.label.queryParams'), prop: () => formatJsonStr(data.mockParams) }
  ]
}

export const calcHistoryContent = (historyOptionsMethod, doc, history) => {
  const options = historyOptionsMethod(doc, history).filter(item => item.enabled !== false)
  return options.map(option => {
    const propValue = isFunction(option.prop) ? option.prop(doc, history) : doc[option.prop]
    return `[${option.labelKey ? $i18nBundle(option.labelKey) : option.label}]
${propValue ?? ''}`
  }).join('\n\n')
}

export const showCompareWindowNew = ({ original, modified, historyOptionsMethod, ...config }) => {
  const originalContent = calcHistoryContent(historyOptionsMethod, original, true)
  const modifiedContent = calcHistoryContent(historyOptionsMethod, modified)
  return showCodeWindow({
    title: $i18nBundle('mock.label.compare'),
    language: 'markdown',
    diffEditor: true,
    readOnly: true,
    closeOnClickModal: false,
    originalContent,
    modifiedContent,
    ...config
  })
}

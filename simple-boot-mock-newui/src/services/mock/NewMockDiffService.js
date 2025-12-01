import { $i18nBundle, $i18nKey } from '@/messages'
import { formatDate, formatJsonStr } from '@/utils'

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

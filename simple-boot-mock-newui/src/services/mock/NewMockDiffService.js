import { $i18nBundle } from '@/messages'
import { formatDate, formatJsonStr } from '@/utils'

export const getRequestHistoryViewOptions = (request, history) => {
  return [
    { labelKey: 'mock.label.responseName', prop: 'dataName' },
    { labelKey: 'mock.label.version', prop: () => `${request.version ?? ''}${request.current ? ` <${$i18nBundle('mock.label.current')}>` : ''}` },
    { labelKey: 'common.label.modifyDate', prop: () => formatDate(request[history ? 'createDate' : 'modifyDate']) },
    { labelKey: 'common.label.modifier', prop: () => request[history ? 'creator' : 'modifier'] },
    { labelKey: 'common.label.status', prop: () => [$i18nBundle('common.label.statusDisabled'), $i18nBundle('common.label.statusEnabled')][request.status] },
    {
      labelKey: 'mock.label.default',
      prop: () => request.defaultFlag !== undefined ? $i18nBundle(request.defaultFlag ? 'common.label.yes' : 'common.label.no') : ''
    },
    { labelKey: 'mock.label.statusCode', prop: 'statusCode' },
    { label: 'Content Type', prop: 'contentType' },
    { label: 'Charset', prop: 'defaultCharset' },
    { labelKey: 'mock.label.matchPattern', prop: 'matchPattern' },
    { labelKey: 'mock.label.dataFormat', prop: 'responseFormat' },
    { labelKey: 'common.label.delay', prop: 'delay' },
    { labelKey: 'mock.label.responseHeaders', prop: () => formatJsonStr(request.headers) },
    { labelKey: 'common.label.description', prop: 'description' },
    { labelKey: 'mock.label.queryParams', prop: () => formatJsonStr(request.mockParams) },
    { labelKey: 'mock.label.mockResponseBody', prop: 'responseBody' }
  ]
}

export const getDataHistoryViewOptions = (data, history) => {
  return [
    { labelKey: 'mock.label.responseName', prop: 'dataName' },
    { labelKey: 'mock.label.version', prop: () => `${data.version ?? ''}${data.current ? ` <${$i18nBundle('mock.label.current')}>` : ''}` },
    { labelKey: 'common.label.modifyDate', prop: () => formatDate(data[history ? 'createDate' : 'modifyDate']) },
    { labelKey: 'common.label.modifier', prop: () => data[history ? 'creator' : 'modifier'] },
    { labelKey: 'common.label.status', prop: () => [$i18nBundle('common.label.statusDisabled'), $i18nBundle('common.label.statusEnabled')][data.status] },
    {
      labelKey: 'mock.label.default',
      prop: () => data.defaultFlag !== undefined ? $i18nBundle(data.defaultFlag ? 'common.label.yes' : 'common.label.no') : ''
    },
    { labelKey: 'mock.label.statusCode', prop: 'statusCode' },
    { label: 'Content Type', prop: 'contentType' },
    { label: 'Charset', prop: 'defaultCharset' },
    { labelKey: 'mock.label.matchPattern', prop: 'matchPattern' },
    { labelKey: 'mock.label.dataFormat', prop: 'responseFormat' },
    { labelKey: 'common.label.delay', prop: 'delay' },
    { labelKey: 'mock.label.responseHeaders', prop: () => formatJsonStr(data.headers) },
    { labelKey: 'common.label.description', prop: 'description' },
    { labelKey: 'mock.label.queryParams', prop: () => formatJsonStr(data.mockParams) },
    { labelKey: 'mock.label.mockResponseBody', prop: 'responseBody' }
  ]
}

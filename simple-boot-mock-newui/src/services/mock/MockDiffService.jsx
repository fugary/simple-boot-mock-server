import { isString } from 'lodash-es'
import { formatDate } from '@/utils'
import { limitStr } from '@/components/utils'
import { $i18nBundle, $i18nMsg } from '@/messages'
import { ElTag, ElText } from 'element-plus'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import CommonIcon from '@/components/common-icon/index.vue'
import { ALL_STATUS_CODES } from '@/api/mock/MockDataApi'

export const calcFormatter = ({ original, modified, key, modifiedKey, value }) => {
  return () => {
    modifiedKey = modifiedKey || key
    const type = modified[modifiedKey] !== original[key] ? 'warning' : ''
    return <ElText type={type}>
            {value}
        </ElText>
  }
}

export const getMockCompareItem = ({
  original,
  modified,
  label,
  labelKey,
  key,
  modifiedKey,
  originalAppend,
  modifiedAppend,
  date = false,
  copy = false,
  limit,
  ...config
}) => {
  modifiedKey = modifiedKey || key
  const enabled = config.enabled ?? (original[key] !== null || modified[modifiedKey] !== null)
  let originalValue = original[key]
  let modifiedValue = modified[modifiedKey]
  if (limit && limit > 0) {
    if (isString(originalValue) && originalValue && originalValue.length > limit) {
      originalValue = limitStr(originalValue, limit)
    }
    if (isString(modifiedValue) && modifiedValue && modifiedValue.length > limit) {
      modifiedValue = limitStr(modifiedValue, limit)
    }
  }
  return [{
    enabled,
    label,
    labelKey,
    formatter: config.originalFormatter || (() => {
      return <>
                {date ? formatDate(originalValue) : originalValue}
                {copy ? <MockUrlCopyLink class="margin-left1" showLink={!!original[key]} content={original[key]}/> : ''}
                {originalAppend}
            </>
    })
  }, {
    enabled,
    labelFormatter: calcFormatter({
      original,
      modified,
      key,
      value: label || $i18nBundle(labelKey)
    }),
    formatter: config.modifiedFormatter || calcFormatter({
      original,
      modified,
      key,
      value: <>
                {date ? formatDate(modifiedValue) : modifiedValue}
                {copy
                  ? <MockUrlCopyLink class="margin-left1" showLink={!!modified[modifiedKey]}
                                       content={modified[modifiedKey]}/>
                  : ''}
                {modifiedAppend}
            </>
    })
  }]
}

export const getStatusCode = (data) => {
  let type = 'danger'
  if (data.statusCode < 300) {
    type = 'success'
  } else if (data.statusCode < 400) {
    type = 'primary'
  } else if (data.statusCode < 500) {
    type = 'warning'
  }
  const status = ALL_STATUS_CODES.find(status => data.statusCode === status.code)
  const statusLabel = status ? $i18nMsg(`${status.labelCn} - ${(status.labelEn)}`, `${status.labelEn} - ${(status.labelCn)}`) : ''
  if (!status) {
    return ''
  }
  return <ElText type="success">
    {data.defaultFlag
      ? <CommonIcon type="success"
                      v-common-tooltip={$i18nBundle('mock.label.default')}
                      icon="Flag"/>
      : ''}
    <ElTag v-common-tooltip={statusLabel} type={type} class={data.defaultFlag ? 'margin-left1' : ''}>{data.statusCode}</ElTag>
  </ElText>
}

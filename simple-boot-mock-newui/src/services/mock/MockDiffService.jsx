import { isString } from 'lodash-es'
import { formatDate } from '@/utils'
import { $i18nBundle } from '@/messages'
import { ElText } from 'element-plus'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'

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
  const enabled = original[key] !== null || modified[modifiedKey] !== null
  let originalValue = original[key]
  let modifiedValue = modified[modifiedKey]
  if (limit && limit > 0) {
    if (isString(originalValue) && originalValue && originalValue.length > limit) {
      originalValue = originalValue.substring(0, limit) + '...'
    }
    if (isString(modifiedValue) && modifiedValue && modifiedValue.length > limit) {
      modifiedValue = modifiedValue.substring(0, limit) + '...'
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

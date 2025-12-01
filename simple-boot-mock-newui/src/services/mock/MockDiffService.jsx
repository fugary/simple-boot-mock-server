import { $i18nBundle, $i18nMsg } from '@/messages'
import { ElTag, ElText } from 'element-plus'
import CommonIcon from '@/components/common-icon/index.vue'
import { ALL_STATUS_CODES } from '@/api/mock/MockDataApi'

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

import { $coreConfirm } from '@/utils'
import { $i18nKey } from '@/messages'
import { sample } from 'openapi-sampler'
import { XMLBuilder } from 'fast-xml-parser'

export const generateSchemaSample = (schemaBody, type) => {
  return $coreConfirm($i18nKey('common.msg.commonConfirm', 'common.label.generateData'))
    .then(() => {
      const schema = JSON.parse(schemaBody)
      const json = sample(schema)
      let resStr
      if (type?.includes('xml')) {
        console.log('===========================schema', schema)
        const builder = new XMLBuilder({
          ignoreAttributes: false,
          format: true,
          indentBy: '\t'
        })
        resStr = builder.build(json)
      } else {
        resStr = JSON.stringify(json)
      }
      return resStr
    })
}

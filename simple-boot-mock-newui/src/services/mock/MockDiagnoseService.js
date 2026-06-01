import { $i18nBundle } from '@/messages'

const diagnoseLabelPrefixes = {
  stage: 'diagnoseStage',
  code: 'diagnoseCode',
  detail: 'diagnoseDetail'
}

const toUpperCamelCase = key => String(key || '')
  .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
  .split(/[^a-zA-Z0-9]+/)
  .filter(Boolean)
  .map(item => item.charAt(0).toUpperCase() + item.slice(1))
  .join('')

const toDiagnoseLabelKey = (type, key) => {
  const suffix = toUpperCamelCase(key)
  return suffix ? `mock.label.${diagnoseLabelPrefixes[type]}${suffix}` : ''
}

const getDiagnoseLabel = (type, key) => {
  const labelKey = toDiagnoseLabelKey(type, key)
  const label = $i18nBundle(labelKey)
  return label && label !== labelKey ? label : key
}

export const getDiagnoseStageLabel = stage => getDiagnoseLabel('stage', stage)

export const getDiagnoseCodeLabel = code => getDiagnoseLabel('code', code)

export const getDiagnoseDetailLabel = key => getDiagnoseLabel('detail', key)

export const shouldShowDiagnoseKey = (label, key) => !!key && !!label && label !== key

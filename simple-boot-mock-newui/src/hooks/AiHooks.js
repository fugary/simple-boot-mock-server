import { computed, onScopeDispose, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { loadAiModels } from '@/api/mock/AiApi'
import { $i18nBundle } from '@/messages'

export const useAiModels = (config, form, modelProp) => {
  const models = ref([])
  const loading = ref(false)
  let generation = 0
  onScopeDispose(() => { generation++ })
  watch(() => [config.value.id, config.value.provider, config.value.baseUrl, config.value.apiKey], () => {
    generation++
    models.value = []
    loading.value = false
  }, { flush: 'sync' })
  const fetchModels = async () => {
    if (loading.value) return
    const requestGeneration = generation
    loading.value = true
    try {
      const response = await loadAiModels(config.value)
      if (requestGeneration !== generation) return
      if (!response.success || !Array.isArray(response.resultData)) {
        ElMessage.error(response.message || $i18nBundle('common.msg.unknownError'))
        return
      }
      models.value = response.resultData
      if (!form.value[modelProp]) form.value[modelProp] = models.value[0] || ''
    } catch (error) {
      if (requestGeneration === generation) {
        ElMessage.error(error?.data?.message || error?.response?.data?.message || error?.message || $i18nBundle('common.msg.unknownError'))
      }
    } finally {
      if (requestGeneration === generation) loading.value = false
    }
  }
  const modelOption = computed(() => ({
    labelKey: 'mock.label.aiModelName',
    prop: modelProp,
    type: 'select',
    required: true,
    tooltip: $i18nBundle('mock.label.aiLoadModels'),
    tooltipIcon: 'Refresh',
    tooltipFunc: fetchModels,
    placeholder: $i18nBundle('mock.msg.aiSelectOrInputModel'),
    attrs: {
      filterable: true,
      allowCreate: true,
      defaultFirstOption: true,
      loading: loading.value
    },
    children: [...new Set([form.value[modelProp], ...models.value].filter(Boolean))].map(value => ({ label: value, value }))
  }))
  return { modelOption }
}

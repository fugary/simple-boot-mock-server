<script setup>
import { computed, h, onMounted, onScopeDispose, ref, watch } from 'vue'
import { useAiModels } from '@/hooks/AiHooks'
import { $i18nBundle } from '@/messages'
import { AiConfigApi, loadAiConfigOptions, testAiConfig } from '@/api/mock/AiApi'
import { AI_PROVIDERS } from '@/consts/AiConstants'
import AiAnswerViewer from '@/views/components/utils/AiAnswerViewer.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import AiConfigLabel from '@/views/components/utils/AiConfigLabel.vue'
const visible = defineModel({ type: Boolean, default: false })
const props = defineProps({
  config: { type: Object, default: () => ({}) }
})
const formData = ref({ configId: props.config.id, model: props.config.defaultModel, prompt: $i18nBundle('mock.msg.aiDefaultPrompt'), result: '' })
const configs = ref(props.config.id ? [props.config] : [])
const selectedConfig = ref(props.config)
const loadingConfig = ref(false)
const { modelOption } = useAiModels(selectedConfig, formData, 'model')
let configSequence = 0
const selectConfig = async id => {
  const sequence = ++configSequence
  stopTest()
  selectedConfig.value = configs.value.find(config => config.id === id) || {}
  formData.value.model = ''
  formData.value.result = ''
  metrics.value = null
  error.value = ''
  stopped.value = false
  loadingConfig.value = true
  try {
    const response = await AiConfigApi.getById(id)
    if (sequence !== configSequence) return
    if (response.success) {
      selectedConfig.value = response.resultData
      formData.value.model = response.resultData.defaultModel || ''
    } else {
      error.value = response.message || $i18nBundle('common.msg.unknownError')
    }
  } catch (err) {
    if (sequence === configSequence) error.value = err?.message || $i18nBundle('common.msg.unknownError')
  } finally {
    if (sequence === configSequence) loadingConfig.value = false
  }
}
onMounted(async () => {
  if (props.config.id) return
  loadingConfig.value = true
  try {
    const response = await loadAiConfigOptions()
    if (!visible.value) return
    if (!response.success) {
      error.value = response.message || $i18nBundle('common.msg.unknownError')
      return
    }
    configs.value = (response.resultData || []).filter(config => !config.deleted && config.status === 1)
    const initial = configs.value.find(config => config.isDefault === 1) || configs.value[0]
    if (initial) {
      formData.value.configId = initial.id
      await selectConfig(initial.id)
    }
  } catch (err) {
    if (visible.value) error.value = err?.message || $i18nBundle('common.msg.unknownError')
  } finally {
    loadingConfig.value = false
  }
})
const formOptions = computed(() => [{
  labelKey: 'mock.label.aiConfigSelect',
  prop: 'configId',
  type: 'select',
  required: true,
  disabled: !!props.config.id || loadingConfig.value || testing.value,
  attrs: { clearable: false, filterable: true, loading: loadingConfig.value },
  slots: {
    label: () => h(AiConfigLabel, { config: selectedConfig.value })
  },
  change: selectConfig,
  children: configs.value.map(config => {
    const provider = AI_PROVIDERS.find(provider => provider.value === config.provider)
    return {
      value: config.id,
      label: [provider ? $i18nBundle(provider.labelKey) : '', config.configName, config.baseUrl, config.isDefault === 1 ? $i18nBundle('mock.label.default') : ''].filter(Boolean).join(' · '),
      slots: { default: () => h(AiConfigLabel, { config }) }
    }
  })
}, { ...modelOption.value, disabled: testing.value || loadingConfig.value }, {
  labelKey: 'mock.label.aiPrompt',
  prop: 'prompt',
  required: true,
  disabled: testing.value || loadingConfig.value,
  attrs: { type: 'textarea', rows: 3, maxlength: 16000, showWordLimit: true }
}, {
  labelKey: 'mock.label.aiTestResult',
  prop: 'result',
  slot: 'result'
}])
const testing = ref(false)
const error = ref('')
const metrics = ref(null)
const stopped = ref(false)
const metricFields = computed(() => [
  ['mock.label.aiDuration', metrics.value?.durationMs],
  ['mock.label.aiPromptTokens', metrics.value?.promptTokens],
  ['mock.label.aiCompletionTokens', metrics.value?.completionTokens],
  ['mock.label.aiTotalTokens', metrics.value?.totalTokens]
].filter(([, value]) => value != null))
let controller
let generation = 0
const stopTest = () => {
  generation++
  controller?.abort()
  controller = undefined
  stopped.value = testing.value
  testing.value = false
}
watch(visible, value => {
  if (!value) {
    configSequence++
    stopTest()
  }
}, { flush: 'sync' })
watch(() => props.config, () => {
  stopTest()
  configSequence++
  loadingConfig.value = false
  selectedConfig.value = props.config
  configs.value = props.config.id ? [props.config] : []
  formData.value = { configId: props.config.id, model: props.config.defaultModel, prompt: $i18nBundle('mock.msg.aiDefaultPrompt'), result: '' }
  metrics.value = null
  error.value = ''
  stopped.value = false
})
onScopeDispose(() => {
  configSequence++
  stopTest()
})
const executeTest = async () => {
  const current = ++generation
  controller = new AbortController()
  testing.value = true
  formData.value.result = ''
  error.value = ''
  metrics.value = null
  stopped.value = false
  try {
    await testAiConfig(selectedConfig.value.id, { prompt: formData.value.prompt, model: formData.value.model }, {
      onDelta: data => {
        if (current === generation) formData.value.result += data.text || ''
      },
      onFinish: data => {
        if (current !== generation) return
        formData.value.result = data.resultData?.content || formData.value.result
        metrics.value = data.resultData
      },
      onError: data => {
        if (current === generation) error.value = data.message || $i18nBundle('common.msg.unknownError')
      }
    }, controller.signal)
  } catch (err) {
    if (current !== generation) return
    error.value = err?.data?.message || err?.response?.data?.message || err?.message || $i18nBundle('common.msg.unknownError')
  } finally {
    if (current === generation) {
      testing.value = false
      controller = undefined
    }
  }
}
const runTest = ({ form }) => {
  if (!testing.value && !loadingConfig.value) {
    testing.value = true
    const current = generation
    formData.value.prompt = formData.value.prompt.trim()
    formData.value.model = formData.value.model?.trim() || ''
    form.validate(valid => {
      if (current !== generation) return
      if (valid && visible.value) {
        executeTest()
      } else {
        testing.value = false
      }
    })
  }
  return false
}
</script>
<template>
  <common-window
    v-model="visible"
    :title="$t('common.label.test')"
    width="min(800px, calc(100vw - 32px))"
    show-fullscreen
    :cancel-label="$t('common.label.close')"
    :ok-label="$t('mock.label.aiSendTest')"
    :ok-click="runTest"
    :ok-loading="testing"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <el-container class="flex-column">
      <common-form
        class="form-edit-width-100"
        :model="formData"
        :show-buttons="false"
        :options="formOptions"
        label-width="140px"
      >
        <template #result="{ option }">
          <common-form-control
            :model="formData"
            :option="option"
          >
            <template #afterLabel>
              <mock-url-copy-link
                v-if="formData.result"
                :content="formData.result"
                :aria-label="$t('common.label.copy')"
              />
            </template>
            <ai-answer-viewer
              v-if="formData.result || testing"
              :key="generation"
              :content="formData.result"
              :loading="testing"
            >
              <template #actions>
                <el-button
                  v-if="testing"
                  size="small"
                  @click="stopTest"
                >
                  {{ $t('mock.label.aiStopWaiting') }}
                </el-button>
              </template>
            </ai-answer-viewer>
            <div
              v-else
              class="ai-test-placeholder"
              role="status"
            >
              <el-text type="info">
                {{ $t('mock.msg.aiTestResultPlaceholder') }}
              </el-text>
            </div>
            <el-space
              v-if="metricFields.length"
              wrap
              class="margin-top1"
            >
              <el-tag
                v-for="[label, value] in metricFields"
                :key="label"
                size="small"
              >
                {{ $t(label) }}: {{ value }}
              </el-tag>
            </el-space>
          </common-form-control>
        </template>
      </common-form>
      <el-alert
        v-if="stopped"
        :title="$t('mock.msg.aiTestStopped')"
        type="warning"
        :closable="false"
      />
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
      />
    </el-container>
  </common-window>
</template>
<style scoped>
.ai-test-placeholder { display: flex; align-items: center; gap: 8px; min-height: 120px; }
</style>

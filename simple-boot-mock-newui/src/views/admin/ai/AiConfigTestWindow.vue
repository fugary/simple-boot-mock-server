<script setup>
import { computed, onScopeDispose, ref, watch } from 'vue'
import { useAiModels } from '@/hooks/AiHooks'
import { $i18nBundle } from '@/messages'
import { testAiConfig } from '@/api/mock/AiApi'
import AiAnswerViewer from '@/views/components/utils/AiAnswerViewer.vue'
import AiConfigLabel from '@/views/components/utils/AiConfigLabel.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import AiResponseJsonAction from '@/views/components/utils/AiResponseJsonAction.vue'
const visible = defineModel({ type: Boolean, default: false })
const props = defineProps({
  config: { type: Object, required: true }
})
const formData = ref({ configName: props.config.configName, model: props.config.defaultModel, prompt: $i18nBundle('mock.msg.aiDefaultPrompt'), result: '' })
const { modelOption } = useAiModels(computed(() => props.config), formData, 'model')
const formOptions = computed(() => [{
  labelKey: 'mock.label.aiConfigName',
  prop: 'configName',
  slot: 'configName'
}, modelOption.value, {
  labelKey: 'mock.label.aiPrompt',
  prop: 'prompt',
  required: true,
  attrs: { type: 'textarea', rows: 4, maxlength: 16000, showWordLimit: true }
}, {
  labelKey: 'mock.label.aiResult',
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
watch(visible, value => { if (!value) stopTest() }, { flush: 'sync' })
watch(() => props.config, () => {
  stopTest()
  formData.value = { configName: props.config.configName, model: props.config.defaultModel, prompt: $i18nBundle('mock.msg.aiDefaultPrompt'), result: '' }
  metrics.value = null
  error.value = ''
  stopped.value = false
})
onScopeDispose(stopTest)
const executeTest = async () => {
  const current = ++generation
  controller = new AbortController()
  testing.value = true
  formData.value.result = ''
  error.value = ''
  metrics.value = null
  stopped.value = false
  try {
    const response = await testAiConfig(props.config.id, { prompt: formData.value.prompt, model: formData.value.model }, controller.signal)
    if (current !== generation) return
    if (response.success) {
      formData.value.result = response.resultData?.content || ''
      metrics.value = response.resultData
    } else {
      error.value = response.message || $i18nBundle('common.msg.unknownError')
    }
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
  if (!testing.value) {
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
    :title="$t('mock.label.aiTest')"
    width="min(900px, calc(100vw - 32px))"
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
        :disabled="testing"
        label-width="140px"
      >
        <template #configName="{ option }">
          <common-form-control
            :model="formData"
            :option="option"
          >
            <ai-config-label :config="config" />
          </common-form-control>
        </template>
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
              v-if="formData.result"
              :key="generation"
              :content="formData.result"
            >
              <template #actions>
                <ai-response-json-action :response="metrics?.rawResponse" />
              </template>
            </ai-answer-viewer>
            <div
              v-else
              class="ai-test-placeholder"
              role="status"
            >
              <common-icon
                v-if="testing"
                icon="Loading"
                class="is-loading"
              />
              <el-text type="info">
                {{ $t(testing ? 'mock.msg.aiTestWaiting' : 'mock.msg.aiTestResultPlaceholder') }}
              </el-text>
            </div>
          </common-form-control>
        </template>
      </common-form>
      <el-button
        v-if="testing"
        class="ai-stop"
        @click="stopTest"
      >
        {{ $t('mock.label.aiStopWaiting') }}
      </el-button>
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
.ai-stop { align-self: flex-start; margin-bottom: 12px; }
.ai-test-placeholder { display: flex; align-items: center; gap: 8px; min-height: 120px; }
</style>

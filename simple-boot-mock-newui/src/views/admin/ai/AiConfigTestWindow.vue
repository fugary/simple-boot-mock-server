<script setup>
import { computed, ref } from 'vue'
import { useAiModels } from '@/hooks/AiHooks'
import { $i18nBundle } from '@/messages'
import { testAiConfig } from '@/api/mock/AiApi'
const visible = defineModel({ type: Boolean, default: false })
const props = defineProps({
  config: { type: Object, required: true }
})
const formData = ref({ configName: props.config.configName, model: props.config.defaultModel, prompt: $i18nBundle('mock.msg.aiDefaultPrompt'), result: '' })
const { modelOption } = useAiModels(computed(() => props.config), formData, 'model')
const formOptions = computed(() => [{
  labelKey: 'mock.label.aiConfigName',
  prop: 'configName',
  disabled: true
}, modelOption.value, {
  labelKey: 'mock.label.aiPrompt',
  prop: 'prompt',
  required: true,
  attrs: { type: 'textarea', rows: 4, maxlength: 16000, showWordLimit: true }
}, {
  labelKey: 'mock.label.responseBody',
  prop: 'result',
  attrs: { type: 'textarea', rows: 10, readonly: true }
}])
const testing = ref(false)
const error = ref('')
const executeTest = async () => {
  testing.value = true
  formData.value.result = ''
  error.value = ''
  try {
    const response = await testAiConfig(props.config.id, { prompt: formData.value.prompt, model: formData.value.model })
    if (response.success) {
      formData.value.result = response.resultData?.content || ''
    } else {
      error.value = response.message
    }
  } catch (err) {
    error.value = err?.data?.message || err?.response?.data?.message || err?.message || $i18nBundle('common.msg.unknownError')
  } finally {
    testing.value = false
  }
}
const runTest = ({ form }) => {
  if (!testing.value) {
    testing.value = true
    formData.value.prompt = formData.value.prompt.trim()
    form.validate(valid => {
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
    :ok-label="$t('common.label.test')"
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

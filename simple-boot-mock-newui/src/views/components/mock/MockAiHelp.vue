<script setup>
import { computed, ref } from 'vue'
import { askAiHelp } from '@/api/mock/AiApi'
import { $i18nBundle } from '@/messages'

const loading = ref(false)
const error = ref('')
const formData = ref({ question: '', answer: '' })
const options = computed(() => [{
  labelKey: 'mock.label.aiHelpQuestion',
  prop: 'question',
  attrs: { type: 'textarea', rows: 3, maxlength: 2000, showWordLimit: true }
}, {
  labelKey: 'mock.label.aiHelpAnswer',
  prop: 'answer',
  attrs: { type: 'textarea', rows: 14, readonly: true }
}])
const errorMessage = err => err?.data?.message || err?.response?.data?.message || err?.message || $i18nBundle('common.msg.unknownError')
const ask = async () => {
  const question = formData.value.question.trim()
  if (!question || loading.value) return
  loading.value = true
  error.value = ''
  formData.value.answer = ''
  try {
    const response = await askAiHelp(question)
    if (response.success) formData.value.answer = response.resultData?.content || ''
    else error.value = response.message || $i18nBundle('common.msg.unknownError')
  } catch (err) {
    error.value = errorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="ai-help">
    <p>{{ $t('mock.msg.aiHelpDescription') }}</p>
    <common-form
      :model="formData"
      :options="options"
      :show-buttons="false"
      :disabled="loading"
      label-position="top"
      class="form-edit-width-100"
    />
    <el-button
      type="primary"
      :loading="loading"
      :disabled="!formData.question.trim()"
      @click="ask"
    >
      {{ $t('mock.label.aiHelpAsk') }}
    </el-button>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
      class="margin-top1"
    />
  </section>
</template>

<style scoped>
.ai-help {
  padding: 0 12px 16px;
  color: var(--el-text-color-regular);
}
</style>

<script setup>
import { computed, h } from 'vue'
import { $i18nBundle } from '@/messages'
import { formatDate } from '@/utils'
const visible = defineModel({ type: Boolean, default: false })
const props = defineProps({ record: { type: Object, required: true } })
const fields = computed(() => {
  const record = props.record
  return [
    ['mock.label.aiConfigName', record.configName],
    ['mock.label.aiModel', record.model],
    ['common.label.status', $i18nBundle(record.status === 1 ? 'mock.label.aiSuccess' : 'mock.label.aiFailed')],
    ['common.label.createDate', formatDate(record.createDate)],
    ['mock.label.aiDuration', record.durationMs],
    ['mock.label.aiPromptTokens', record.promptTokens],
    ['mock.label.aiCompletionTokens', record.completionTokens],
    ['mock.label.aiTotalTokens', record.totalTokens],
    ['mock.label.aiError', record.errorMessage]
  ].map(([labelKey, value]) => ({ labelKey, value: h('span', String(value ?? '-')) }))
})
</script>
<template>
  <common-window
    v-model="visible"
    :title="$t('mock.label.aiUsageLogs')"
    width="900px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    show-fullscreen
    append-to-body
    destroy-on-close
  >
    <el-container class="flex-column">
      <common-descriptions
        :items="fields"
        :column="2"
        border
      />
      <h4>{{ $t('mock.label.aiPrompt') }}</h4>
      <pre class="ai-record-text">{{ record.prompt || '-' }}</pre>
      <h4>{{ $t('mock.label.responseBody') }}</h4>
      <el-text type="info">
        {{ $t('mock.msg.aiLogResponseLimit') }}
      </el-text>
      <pre class="ai-record-text">{{ record.responseText || '-' }}</pre>
    </el-container>
  </common-window>
</template>
<style scoped>
.ai-record-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 360px;
  overflow: auto;
  padding: 12px;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  border-radius: var(--el-border-radius-base);
}
</style>

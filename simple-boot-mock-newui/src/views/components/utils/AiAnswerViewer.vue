<script setup>
import { computed, ref } from 'vue'
import { MdPreview } from 'md-editor-v3'
import DOMPurify from 'dompurify'
import { $i18nBundle } from '@/messages'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import 'md-editor-v3/lib/preview.css'

const props = defineProps({
  content: { type: String, default: '' },
  maxHeight: { type: Number, default: 460 },
  loading: { type: Boolean, default: false }
})
const mode = ref('preview')
const globalConfig = useGlobalConfigStore()
const modeOptions = computed(() => [
  { label: $i18nBundle('common.label.preview'), value: 'preview' },
  { label: $i18nBundle('mock.label.aiAnswerSource'), value: 'source' }
])
const sourceHeight = computed(() => `${Math.min(props.maxHeight, Math.max(160, props.content.split('\n').length * 20 + 12))}px`)
const sourceOptions = { readOnly: true, domReadOnly: true, automaticLayout: true, minimap: { enabled: false }, scrollBeyondLastLine: false, wordWrap: 'on' }
const sanitize = html => DOMPurify.sanitize(html)
const isJson = computed(() => {
  try {
    const value = JSON.parse(props.content)
    return value !== null && typeof value === 'object'
  } catch {
    return false
  }
})
const previewContent = computed(() => isJson.value ? `\`\`\`json\n${props.content}\n\`\`\`` : props.content)
</script>

<template>
  <div
    class="ai-answer"
    :style="{ '--ai-answer-max-height': `${maxHeight}px` }"
  >
    <div class="ai-answer-toolbar">
      <el-segmented
        v-model="mode"
        :options="modeOptions"
        size="small"
      />
      <slot name="actions" />
    </div>
    <div
      v-if="loading && !content"
      class="ai-answer-loading"
      role="status"
    >
      <common-icon
        icon="Loading"
        class="is-loading"
      />
      {{ $t('mock.msg.aiTestWaiting') }}
    </div>
    <vue-monaco-editor
      v-else-if="mode === 'source'"
      :value="content"
      :language="isJson ? 'json' : 'markdown'"
      :height="sourceHeight"
      :theme="globalConfig.monacoTheme"
      :options="sourceOptions"
    />
    <md-preview
      v-else
      :model-value="previewContent"
      :language="globalConfig.currentLocale"
      :theme="globalConfig.isDarkTheme ? 'dark' : 'light'"
      :sanitize="sanitize"
      no-mermaid
      no-katex
      no-highlight
      class="ai-answer-preview"
    />
  </div>
</template>

<style scoped>
.ai-answer { width: 100%; min-width: 0; }
.ai-answer-loading { display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 120px; border-radius: var(--el-border-radius-base); background: var(--el-fill-color-light); color: var(--el-text-color-placeholder); }
.ai-answer-toolbar { min-height: 32px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.ai-answer-toolbar :deep(.el-segmented) { margin-right: auto; }
.ai-answer-preview {
  --md-bk-color: var(--el-fill-color-blank);
  --md-color: var(--el-text-color-regular);
  border-radius: var(--el-border-radius-base);
  font-weight: normal;
  line-height: normal;
}
.ai-answer-preview :deep(.md-editor-preview-wrapper) { max-height: var(--ai-answer-max-height); overflow: auto; padding: 12px 16px; }
</style>

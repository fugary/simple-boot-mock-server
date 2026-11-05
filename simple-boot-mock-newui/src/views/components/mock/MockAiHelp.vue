<script setup>
import { computed, reactive, ref, nextTick, watch } from 'vue'
import { useResizeObserver } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { useAiConfigOption, useAiModels } from '@/hooks/AiHooks'
import { useAiHelpStore } from '@/stores/AiHelpStore'
import AiAnswerViewer from '@/views/components/utils/AiAnswerViewer.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { $i18nBundle } from '@/messages'

const props = defineProps({ active: { type: Boolean, default: true } })
const composer = ref()
const prefillQuestion = async question => {
  formData.value.question = question
  await nextTick()
  composer.value?.querySelector('textarea')?.focus()
}
defineExpose({ prefillQuestion })
const conversation = useAiHelpStore()
const { loading, error, formData, submittedQuestion, previousTurns, stopped, metrics, selection, configs, selectedConfig, loadingConfig } = storeToRefs(conversation)
const configOption = useAiConfigOption(configs, selectedConfig)
const { modelOption } = useAiModels(selectedConfig, selection, 'model', true)
const selectionDisabled = computed(() => loading.value || loadingConfig.value)
const canAsk = computed(() => !selectionDisabled.value && selectedConfig.value.id === selection.value.configId && !!selection.value.configId && !!selection.value.model?.trim())
watch(() => [props.active, loading.value], ([active, generating], previous = []) => {
  if (active && !generating && (previous[0] !== active || !selectedConfig.value.id)) conversation.loadConfigs()
}, { immediate: true })
const composerModel = reactive({ selection, formData })
const options = computed(() => [{
  ...configOption.value,
  prop: 'selection.configId',
  disabled: selectionDisabled.value,
  attrs: { ...configOption.value.attrs, loading: loadingConfig.value },
  change: id => conversation.selectConfig(id)
}, {
  ...modelOption.value,
  prop: 'selection.model',
  disabled: selectionDisabled.value || !selectedConfig.value.id,
  tooltipFunc: () => {
    if (!selectionDisabled.value && selectedConfig.value.id) return modelOption.value.tooltipFunc()
  }
}, {
  prop: 'formData.question',
  showLabel: false,
  disabled: loading.value,
  attrs: {
    type: 'textarea',
    autosize: { minRows: 1, maxRows: 3 },
    clearable: true,
    maxlength: 2000,
    showWordLimit: true,
    placeholder: $i18nBundle('mock.label.aiHelpQuestion'),
    'aria-label': $i18nBundle('mock.label.aiHelpQuestion'),
    onKeydown: handleQuestionKeydown
  }
}])
const answerViewport = ref()
const answerContent = ref()
const following = ref(true)
const scrollToLatest = () => {
  following.value = true
  const viewport = answerViewport.value
  if (viewport) viewport.scrollTop = viewport.scrollHeight
}
const trackScroll = () => {
  const viewport = answerViewport.value
  if (viewport) following.value = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight < 48
}
useResizeObserver([answerContent, answerViewport], () => {
  if (following.value) scrollToLatest()
})
const metricFields = computed(() => [
  ['mock.label.aiDuration', metrics.value?.durationMs],
  ['mock.label.aiPromptTokens', metrics.value?.promptTokens],
  ['mock.label.aiCompletionTokens', metrics.value?.completionTokens],
  ['mock.label.aiTotalTokens', metrics.value?.totalTokens]
].filter(([, value]) => value != null))
const stop = () => conversation.stop()
const resetComposer = () => {
  if (!submittedQuestion.value) {
    formData.value.question = ''
    return
  }
  conversation.clearConversation()
  nextTick(scrollToLatest)
}
const ask = () => {
  if (!formData.value.question.trim() || !canAsk.value || !props.active) return
  following.value = true
  nextTick(scrollToLatest)
  return conversation.ask()
}
const handleQuestionKeydown = event => {
  if (event.key !== 'Enter' || event.shiftKey || event.ctrlKey || event.altKey || event.metaKey || event.isComposing || event.keyCode === 229) return
  event.preventDefault()
  if (!event.repeat) return ask()
}
</script>

<template>
  <section class="ai-help">
    <div
      ref="answerViewport"
      class="ai-help-answer"
      @scroll="trackScroll"
    >
      <div ref="answerContent">
        <p class="ai-help-description">
          {{ $t('mock.msg.aiHelpDescription') }}
        </p>
        <div
          v-for="(turn, index) in previousTurns"
          :key="index"
          class="ai-help-turn"
        >
          <div class="ai-help-question">
            <strong>{{ $t('mock.label.aiHelpQuestion') }}</strong>
            <p>{{ turn.question }}</p>
          </div>
          <ai-answer-viewer
            :content="turn.answer"
            :max-height="0"
          />
        </div>
        <div
          v-if="submittedQuestion"
          class="ai-help-question"
        >
          <strong>{{ $t('mock.label.aiHelpQuestion') }}</strong>
          <p>{{ submittedQuestion }}</p>
        </div>
        <div class="ai-help-heading">
          {{ $t('mock.label.aiHelpAnswer') }}
          <mock-url-copy-link
            v-if="formData.answer"
            :content="formData.answer"
            :aria-label="$t('common.label.copy')"
          />
        </div>
        <ai-answer-viewer
          v-if="formData.answer || loading"
          :content="formData.answer"
          :loading="loading"
          :max-height="0"
        />
        <el-text
          v-else
          type="info"
        >
          {{ $t('mock.msg.aiHelpAnswerPlaceholder') }}
        </el-text>
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
          class="margin-top1"
        />
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          :closable="false"
          class="margin-top1"
        />
      </div>
    </div>
    <div
      ref="composer"
      class="ai-help-composer"
    >
      <el-button
        v-if="!following"
        class="ai-help-latest"
        size="small"
        @click="scrollToLatest"
      >
        {{ $t('mock.label.aiScrollToLatest') }}
      </el-button>
      <common-form
        :model="composerModel"
        :options="options"
        :show-buttons="false"
        label-position="left"
        label-width=""
        class="ai-help-form form-edit-width-100"
      />
      <div class="ai-help-actions">
        <el-text
          size="small"
          type="info"
        >
          {{ $t('mock.msg.aiHelpKeyboardHint') }}
        </el-text>
        <div class="ai-help-buttons">
          <el-button
            type="primary"
            plain
            :disabled="!submittedQuestion && !formData.question"
            @click="resetComposer"
          >
            {{ $t(submittedQuestion ? 'mock.label.aiNewConversation' : 'mock.label.aiClearInput') }}
          </el-button>
          <el-button
            v-if="loading"
            type="warning"
            plain
            @click="stop"
          >
            {{ $t('mock.label.aiStopWaiting') }}
          </el-button>
          <el-button
            v-else
            type="primary"
            :disabled="!formData.question.trim() || !canAsk"
            @click="ask"
          >
            {{ $t('mock.label.aiHelpAsk') }}
          </el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ai-help { display: flex; flex-direction: column; height: 100%; min-height: 0; color: var(--el-text-color-regular); }
.ai-help-answer { flex: 1; min-height: 0; overflow-y: auto; overflow-anchor: none; padding: 0 12px 16px; }
.ai-help-description { line-height: 1.6; color: var(--el-text-color-secondary); }
.ai-help-heading { display: flex; align-items: center; gap: 4px; margin-bottom: 12px; }
.ai-help-turn { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--el-border-color-lighter); }
.ai-help-question { margin-bottom: 16px; padding: 12px; border-radius: var(--el-border-radius-base); background: var(--el-fill-color-light); }
.ai-help-question p { margin: 8px 0 0; white-space: pre-wrap; overflow-wrap: anywhere; }
.ai-help-composer { position: relative; flex-shrink: 0; padding: 12px; border-top: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); }
.ai-help-composer :deep(.el-form-item) { margin-bottom: 8px; }
.ai-help-form { container-type: inline-size; }
.ai-help-form :deep(.el-form) { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 12px; }
.ai-help-form :deep(.el-form-item:last-child) { grid-column: 1 / -1; }
@container (max-width: 610px) { .ai-help-form :deep(.el-form) { grid-template-columns: minmax(0, 1fr); } }
.ai-help-actions { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 8px; }
.ai-help-buttons { display: flex; flex-shrink: 0; align-items: center; gap: 8px; margin-left: auto; }
.ai-help-buttons .el-button { margin-left: 0; }
.ai-help-latest { position: absolute; bottom: calc(100% + 8px); left: 50%; transform: translateX(-50%); }
</style>

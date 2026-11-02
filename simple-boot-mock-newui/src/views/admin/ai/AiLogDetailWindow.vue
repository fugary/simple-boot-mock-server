<script setup>
import { computed, ref, watch } from 'vue'
import { $i18nBundle } from '@/messages'
import { formatDate } from '@/utils'
import { showCodeWindow } from '@/utils/DynamicUtils'
import AiAnswerViewer from '@/views/components/utils/AiAnswerViewer.vue'
import AiConfigLabel from '@/views/components/utils/AiConfigLabel.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import AiResponseJsonAction from '@/views/components/utils/AiResponseJsonAction.vue'
const visible = defineModel({ type: Boolean, default: false })
const props = defineProps({ record: { type: Object, required: true }, config: { type: Object, default: () => ({}) } })
const activeTab = ref('result')
const execution = computed(() => {
  try { return JSON.parse(props.record.executionDetail || 'null') } catch { return null }
})
const statusLabel = status => ({ success: 'mock.label.aiSuccess', failed: 'mock.label.aiFailed', cancelled: 'mock.label.aiCancelled' })[status] || 'mock.msg.aiNoContent'
const statusType = status => ({ success: 'success', failed: 'danger', cancelled: 'info' })[status] || 'info'
const finishStatus = computed(() => execution.value?.finishReason || (props.record.status === 1 ? 'success' : 'failed'))
const steps = computed(() => Array.isArray(execution.value?.steps) ? execution.value.steps.filter(step => step && typeof step === 'object') : [])
const formatNumber = value => value == null ? '-' : value.toLocaleString()
const viewRequest = () => showCodeWindow(execution.value?.requestBody, { title: $i18nBundle('mock.label.aiRequestJson'), language: 'json', readOnly: true })
watch(() => props.record.id, () => { activeTab.value = 'result' })
const configIdentity = computed(() => ({ ...props.config, configName: props.record.configName }))
const metrics = computed(() => {
  const record = props.record
  return [
    ['mock.label.aiDuration', record.durationMs],
    ['mock.label.aiPromptTokens', record.promptTokens],
    ['mock.label.aiCompletionTokens', record.completionTokens],
    ['mock.label.aiTotalTokens', record.totalTokens]
  ].map(([labelKey, value]) => ({ labelKey, value: formatNumber(value) }))
})
</script>
<template>
  <common-window
    v-model="visible"
    :title="$t('mock.label.aiRecordDetail')"
    width="min(1100px, calc(100vw - 32px))"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    show-fullscreen
    append-to-body
    destroy-on-close
  >
    <div class="record-content">
      <div class="record-meta">
        <el-tag
          :type="statusType(finishStatus)"
          size="small"
        >
          {{ $t(statusLabel(finishStatus)) }}
        </el-tag>
        <ai-config-label :config="configIdentity" />
        <span>{{ $t('common.label.username') }}: {{ record.userName || '-' }}</span>
        <span>{{ record.model || '-' }}</span>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane
          :label="$t('mock.label.aiResult')"
          name="result"
        >
          <section class="record-section">
            <div class="record-section-header">
              <h4>
                {{ $t('mock.label.aiRecordInput') }}
                <mock-url-copy-link
                  v-if="record.prompt"
                  :content="record.prompt"
                  :aria-label="$t('common.label.copy')"
                />
              </h4>
              <el-tooltip
                :disabled="!!execution?.requestBody"
                :content="$t('mock.msg.aiNoRawRequest')"
              >
                <span>
                  <el-button
                    :disabled="!execution?.requestBody"
                    size="small"
                    @click="viewRequest"
                  >
                    {{ $t('common.label.json') }}
                  </el-button>
                </span>
              </el-tooltip>
            </div>
            <ai-answer-viewer
              v-if="record.prompt"
              :key="record.id"
              :content="record.prompt"
              :max-height="240"
            />
            <el-empty
              v-else
              :description="$t('mock.msg.aiNoContent')"
              :image-size="60"
            />
          </section>
          <section class="record-section">
            <div class="record-section-header">
              <h4>
                {{ $t('mock.label.responseBody') }}
                <mock-url-copy-link
                  v-if="record.responseText"
                  :content="record.responseText"
                  :aria-label="$t('common.label.copy')"
                />
              </h4>
              <ai-response-json-action
                :response="record.rawResponse"
              />
            </div>
            <el-alert
              v-if="execution?.truncated"
              :title="$t('mock.msg.aiRawTruncated')"
              type="warning"
              :closable="false"
              class="margin-bottom1"
            />
            <el-alert
              v-if="record.errorMessage"
              :title="record.errorMessage"
              type="error"
              :closable="false"
              class="margin-bottom1"
            />
            <ai-answer-viewer
              v-if="record.responseText"
              :key="record.id"
              :content="record.responseText"
            />
            <el-empty
              v-else
              :description="$t('mock.msg.aiNoContent')"
              :image-size="60"
            />
            <el-text
              v-if="record.responseText?.length >= 16000"
              type="info"
            >
              {{ $t('mock.msg.aiLogResponseLimit') }}
            </el-text>
          </section>
        </el-tab-pane>
        <el-tab-pane
          :label="$t('mock.label.aiExecution')"
          name="execution"
        >
          <div class="record-info-toolbar">
            <span>{{ $t('common.label.createDate') }}: {{ formatDate(record.createDate) || '-' }}</span>
            <span
              v-if="execution"
              class="execution-meta"
            >
              <span>{{ $t(execution.streaming ? 'mock.label.aiStreaming' : 'mock.label.aiNonStreaming') }}</span>
              <span v-if="execution.httpStatus != null">HTTP {{ execution.httpStatus }}</span>
            </span>
          </div>
          <dl class="record-metrics">
            <div
              v-for="metric in metrics"
              :key="metric.labelKey"
              class="record-metric"
            >
              <dt>{{ $t(metric.labelKey) }}</dt>
              <dd>{{ metric.value }}</dd>
            </div>
          </dl>
          <el-alert
            v-if="execution?.usageComplete === false"
            :title="$t('mock.msg.aiPartialUsage')"
            type="warning"
            :closable="false"
            class="margin-bottom1"
          />
          <section
            v-if="steps.length"
            class="execution-steps"
          >
            <div class="execution-heading">
              <span>{{ $t('mock.label.aiModelCall') }}</span>
              <el-tag
                size="small"
                type="info"
                round
              >
                {{ steps.length }}
              </el-tag>
            </div>
            <ol class="execution-list">
              <li
                v-for="(step, index) in steps"
                :key="index"
                class="execution-step"
              >
                <span class="step-index">{{ index + 1 }}</span>
                <span class="step-model">{{ record.model || $t('mock.label.aiModelCall') }}</span>
                <el-tag
                  :type="statusType(step.status)"
                  size="small"
                  effect="plain"
                >
                  {{ $t(statusLabel(step.status)) }}
                </el-tag>
                <span class="step-duration">{{ formatNumber(step.duration) }} ms</span>
              </li>
            </ol>
            <p
              v-if="record.errorMessage"
              class="execution-error"
            >
              {{ record.errorMessage }}
            </p>
          </section>
          <el-empty
            v-else
            :description="$t('mock.msg.aiNoExecution')"
            :image-size="60"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </common-window>
</template>
<style scoped>
.record-content { width: 100%; min-width: 0; }
.record-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.record-section + .record-section { margin-top: 24px; }
.record-section h4 { display: flex; align-items: center; gap: 8px; margin: 0 0 12px; font-size: 14px; color: var(--el-text-color-primary); }
.record-section-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px; }
.record-section-header h4 { margin-bottom: 0; }
.record-info-toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px 16px; margin: 4px 0 16px; color: var(--el-text-color-secondary); font-size: 13px; }
.execution-meta { display: inline-flex; flex-wrap: wrap; gap: 12px; }
.record-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin: 0 0 20px; padding: 16px 0; border-block: 1px solid var(--el-border-color-lighter); }
.record-metric { padding: 0 16px; border-inline-start: 1px solid var(--el-border-color-lighter); }
.record-metric:first-child { padding-inline-start: 0; border-inline-start: 0; }
.record-metric dt { font-size: 12px; color: var(--el-text-color-secondary); }
.record-metric dd { margin: 6px 0 0; color: var(--el-text-color-primary); font-size: 20px; font-weight: 600; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.execution-steps { padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-left: 3px solid var(--el-color-primary); border-radius: 6px; background: var(--el-fill-color-light); }
.execution-heading { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); }
.execution-list { list-style: none; padding: 0; margin: 12px 0 0; }
.execution-step { display: flex; align-items: center; flex-wrap: wrap; gap: 8px 12px; padding: 8px 0; }
.execution-step + .execution-step { border-top: 1px solid var(--el-border-color-lighter); }
.step-index { color: var(--el-text-color-secondary); font-size: 12px; font-variant-numeric: tabular-nums; }
.step-model { color: var(--el-text-color-regular); overflow-wrap: anywhere; min-width: 0; }
.step-duration { margin-inline-start: auto; color: var(--el-text-color-secondary); font-size: 12px; white-space: nowrap; font-variant-numeric: tabular-nums; }
.execution-error { margin: 8px 0 0; color: var(--el-color-danger); overflow-wrap: anywhere; }
@media (max-width: 600px) {
  .record-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); row-gap: 16px; }
  .record-metric:nth-child(odd) { padding-inline-start: 0; border-inline-start: 0; }
}
</style>

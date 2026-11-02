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
watch(() => props.record.id, () => { activeTab.value = 'result' })
const configIdentity = computed(() => ({ ...props.config, configName: props.record.configName }))
const viewJson = () => showCodeWindow(props.record, { title: $i18nBundle('mock.label.aiRecordJson'), language: 'json', readOnly: true })
const metrics = computed(() => {
  const record = props.record
  return [
    ['mock.label.aiDuration', record.durationMs],
    ['mock.label.aiPromptTokens', record.promptTokens],
    ['mock.label.aiCompletionTokens', record.completionTokens],
    ['mock.label.aiTotalTokens', record.totalTokens]
  ].map(([labelKey, value]) => ({ labelKey, value: value == null ? '-' : value.toLocaleString() }))
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
          :type="record.status === 1 ? 'success' : 'danger'"
          size="small"
        >
          {{ $t(record.status === 1 ? 'mock.label.aiSuccess' : 'mock.label.aiFailed') }}
        </el-tag>
        <ai-config-label :config="configIdentity" />
        <span>{{ record.model || '-' }}</span>
        <span>{{ $t('mock.label.aiDuration') }}: {{ record.durationMs ?? '-' }}</span>
        <span>{{ $t('mock.label.aiTotalTokens') }}: {{ record.totalTokens ?? '-' }}</span>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane
          :label="$t('mock.label.aiResult')"
          name="result"
        >
          <section class="record-section">
            <h4>
              {{ $t('mock.label.aiRecordInput') }}
              <mock-url-copy-link
                v-if="record.prompt"
                :content="record.prompt"
                :aria-label="$t('common.label.copy')"
              />
            </h4>
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
                v-if="!record.responseText"
                :response="record.rawResponse"
              />
            </div>
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
            >
              <template #actions>
                <ai-response-json-action :response="record.rawResponse" />
              </template>
            </ai-answer-viewer>
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
          :label="$t('mock.label.aiRecordMetrics')"
          name="metrics"
        >
          <div class="record-info-toolbar">
            <span>{{ $t('common.label.createDate') }}: {{ formatDate(record.createDate) || '-' }}</span>
            <el-button
              size="small"
              @click="viewJson"
            >
              {{ $t('mock.label.aiRecordJson') }}
            </el-button>
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
.record-info-toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin: 4px 0 20px; color: var(--el-text-color-secondary); font-size: 13px; }
.record-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin: 0 0 12px; }
.record-metric { padding: 20px; border: 1px solid var(--el-border-color-light); border-radius: 8px; background: var(--el-fill-color-light); }
.record-metric dt { font-size: 13px; color: var(--el-text-color-secondary); }
.record-metric dd { margin: 12px 0 0; color: var(--el-text-color-primary); font-size: 26px; font-weight: 600; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
@media (max-width: 760px) { .record-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 420px) { .record-metrics { grid-template-columns: 1fr; } }
</style>

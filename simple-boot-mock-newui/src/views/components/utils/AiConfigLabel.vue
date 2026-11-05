<script setup>
import { computed } from 'vue'
import { AI_PROVIDERS } from '@/consts/AiConstants'
const props = defineProps({ config: { type: Object, required: true } })
const provider = computed(() => AI_PROVIDERS.find(item => item.value === props.config.provider))
const connectionLabel = computed(() => (props.config.configName || '-') + (props.config.baseUrl ? ' (' + props.config.baseUrl + ')' : ''))
</script>

<template>
  <span class="ai-config-label">
    <el-tag
      v-if="provider"
      :type="provider.tagType"
      size="small"
    >
      {{ $t(provider.labelKey) }}
    </el-tag>
    <span
      class="ai-config-name"
      :title="connectionLabel"
    >{{ connectionLabel }}</span>
    <el-text
      v-if="config.isDefault === 1 && !config.deleted"
      type="success"
    >[{{ $t('mock.label.default') }}]</el-text>
    <el-text
      v-if="config.deleted"
      type="info"
    >[{{ $t('mock.label.aiDeletedConfig') }}]</el-text>
    <el-text
      v-else-if="config.status === 0"
      type="info"
    >[{{ $t('common.label.statusDisabled') }}]</el-text>
  </span>
</template>

<style scoped>
.ai-config-label { display: inline-flex; align-items: center; flex-wrap: wrap; gap: 6px; min-width: 0; overflow-wrap: anywhere; color: var(--el-text-color-regular); }
.ai-config-label--selected { display: flex; flex-wrap: nowrap; width: 100%; overflow: hidden; white-space: nowrap; }
.ai-config-label--selected .ai-config-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.ai-config-label--selected :is(.el-tag, .el-text) { flex-shrink: 0; }
</style>

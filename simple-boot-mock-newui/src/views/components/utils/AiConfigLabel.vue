<script setup>
import { computed } from 'vue'
import { AI_PROVIDERS } from '@/consts/AiConstants'
const props = defineProps({ config: { type: Object, required: true } })
const provider = computed(() => AI_PROVIDERS.find(item => item.value === props.config.provider))
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
    <span>{{ config.configName || '-' }}<template v-if="config.baseUrl"> ({{ config.baseUrl }})</template></span>
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
</style>

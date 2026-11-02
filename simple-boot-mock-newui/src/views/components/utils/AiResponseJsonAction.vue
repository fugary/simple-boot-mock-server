<script setup>
import { $i18nBundle } from '@/messages'
import { showCodeWindow } from '@/utils/DynamicUtils'
defineProps({ response: { type: String, default: '' } })
const view = response => showCodeWindow(response, { title: $i18nBundle('mock.label.aiResponseJson'), language: 'json', readOnly: true })
</script>

<template>
  <el-tooltip
    :disabled="!!response"
    :content="$t('mock.msg.aiNoRawResponse')"
  >
    <span>
      <el-button
        :disabled="!response"
        size="small"
        @click="view(response)"
      >
        {{ $t('common.label.json') }}
      </el-button>
    </span>
  </el-tooltip>
</template>

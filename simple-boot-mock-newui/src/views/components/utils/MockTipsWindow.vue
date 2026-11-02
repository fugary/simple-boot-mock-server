<script setup>
import { computed, ref, watch } from 'vue'
import { useAiHelpStore } from '@/stores/AiHelpStore'
import MockTips from '@/views/mock/MockTips.vue'
import { isString } from 'lodash-es'

const showWindow = ref(false)
watch(showWindow, visible => {
  if (!visible) useAiHelpStore().stop()
}, { flush: 'sync' })

const currentTab = ref()
const showMockTips = (tab) => {
  showWindow.value = true
  if (isString(tab)) {
    currentTab.value = tab
  }
}
defineExpose({
  showMockTips
})
const fullscreen = ref(false)
const tipsHeight = computed(() => {
  return fullscreen.value ? 'calc(100dvh - 100px)' : 'min(720px, calc(90dvh - 110px))'
})
</script>

<template>
  <common-window
    v-model="showWindow"
    v-model:fullscreen="fullscreen"
    width="min(1100px, calc(100vw - 32px))"
    top="5dvh"
    show-fullscreen
    :show-buttons="false"
    destroy-on-close
    :title="$t('mock.label.guideWindowTitle')"
    append-to-body
    v-bind="$attrs"
  >
    <mock-tips
      v-model="currentTab"
      :style="{height: tipsHeight, maxHeight:tipsHeight}"
    />
  </common-window>
</template>

<style scoped>

</style>

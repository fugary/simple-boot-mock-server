<script setup>
import { ref } from 'vue'
import MockTips from '@/views/mock/MockTips.vue'
import { isString } from 'lodash-es'

const showWindow = ref(false)

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
</script>

<template>
  <common-window
    v-model="showWindow"
    width="1000px"
    show-fullscreen
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="`${$t('mock.label.requestObject')} & ${$t('mock.label.buildInFunctions')}`"
    append-to-body
    v-bind="$attrs"
  >
    <mock-tips v-model="currentTab" />
  </common-window>
</template>

<style scoped>

</style>

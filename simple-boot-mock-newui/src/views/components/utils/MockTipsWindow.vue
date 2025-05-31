<script setup>
import { ref } from 'vue'
import MockTips from '@/views/mock/MockTips.vue'

const showWindow = ref(false)

const showMockTips = () => {
  showWindow.value = true
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
    <mock-tips />
  </common-window>
</template>

<style scoped>

</style>

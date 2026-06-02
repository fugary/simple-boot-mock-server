<script setup>
import { computed, ref, watch } from 'vue'
import MockDiagnoseInfo from '@/views/components/mock/MockDiagnoseInfo.vue'

const showWindow = ref(false)
const fullscreen = ref(false)
const currentDiagnoseInfo = ref()

const windowHeight = computed(() => {
  return fullscreen.value ? 'calc(100dvh - 150px)' : '65vh'
})

const showDiagnoseWindow = diagnoseInfo => {
  currentDiagnoseInfo.value = diagnoseInfo
  showWindow.value = true
}

watch(showWindow, value => {
  if (!value) {
    currentDiagnoseInfo.value = null
    fullscreen.value = false
  }
})

defineExpose({
  showDiagnoseWindow
})
</script>

<template>
  <common-window
    v-model="showWindow"
    v-model:fullscreen="fullscreen"
    width="1100px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="$t('mock.label.diagnose')"
    append-to-body
    show-fullscreen
    v-bind="$attrs"
  >
    <el-scrollbar
      class="mock-diagnose-window__body"
      :style="{ height: windowHeight, maxHeight: windowHeight }"
    >
      <mock-diagnose-info
        v-if="currentDiagnoseInfo"
        :diagnose-info="currentDiagnoseInfo"
      />
    </el-scrollbar>
  </common-window>
</template>

<style scoped>
.mock-diagnose-window__body {
  width: 100%;
}
</style>

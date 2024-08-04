<script setup>
import { ref, nextTick } from 'vue'
import MockRequestPreview from '@/views/components/mock/MockRequestPreview.vue'

const showWindow = ref(false)
const loading = ref(true)
const mockPreviewRef = ref()
const toPreviewRequest = async (...args) => {
  showWindow.value = true
  nextTick(() => {
    mockPreviewRef.value?.toPreviewRequest(...args)
      .finally(() => { loading.value = false })
  })
}

defineExpose({
  toPreviewRequest
})

</script>

<template>
  <common-window
    v-model="showWindow"
    width="1000px"
    :show-cancel="false"
    :close-on-click-modal="false"
    :ok-label="$t('common.label.close')"
    show-fullscreen
    destroy-on-close
  >
    <template #header>
      <span class="el-dialog__title">
        {{ $t('mock.msg.requestTest') }}
      </span>
    </template>
    <mock-request-preview
      ref="mockPreviewRef"
      v-loading="loading"
      style="min-height:200px;"
    />
  </common-window>
</template>

<style scoped>

</style>

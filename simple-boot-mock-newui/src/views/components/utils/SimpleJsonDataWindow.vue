<script setup lang="js">
import { ref } from 'vue'
import SimpleJsonDataTable from '@/views/components/utils/SimpleJsonDataTable.vue'
const showWindow = ref(false)
const jsonData = ref()
const config = ref({})
const showJsonDataWindow = (data, conf) => {
  jsonData.value = data
  Object.assign(config.value, conf)
  showWindow.value = true
}
defineExpose({
  showJsonDataWindow
})
</script>

<template>
  <common-window
    v-model="showWindow"
    width="1100px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="config.title||$t('common.label.info')"
    append-to-body
    show-fullscreen
    v-bind="$attrs"
  >
    <simple-json-data-table
      :data="jsonData"
      :columns="config.columns"
    />
  </common-window>
</template>

<style scoped>

</style>

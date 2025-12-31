<script setup lang="js">
import { ref } from 'vue'
import SimpleJsonDataTable from '@/views/components/utils/SimpleJsonDataTable.vue'
import { cloneDeep } from 'lodash-es'
const showWindow = ref(false)
defineProps({
  title: {
    type: String,
    default: ''
  }
})
const vModel = defineModel({ type: String, default: '' })
const tableConfig = defineModel('tableConfig', { type: Object })
defineEmits(['update:tableConfig'])

const formModel = ref({})
const showJsonDataWindow = (data) => {
  vModel.value = data
  formModel.value = cloneDeep(tableConfig.value)
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
    :title="title||$t('mock.label.viewAsTable')"
    append-to-body
    show-fullscreen
    v-bind="$attrs"
  >
    <simple-json-data-table
      v-model:table-config="formModel"
      v-model="vModel"
      @save-table-config="tableConfig=cloneDeep(formModel)"
    />
  </common-window>
</template>

<style scoped>

</style>

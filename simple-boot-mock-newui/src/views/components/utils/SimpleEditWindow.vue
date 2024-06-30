<script setup>
import { $i18nBundle, $i18nKey } from '@/messages'
import { ElMessage } from 'element-plus'

const props = defineProps({
  formOptions: {
    type: Array,
    default: () => []
  },
  name: {
    type: String,
    default: ''
  },
  saveCurrentItem: {
    type: Function,
    default: () => {}
  }
})

const showEditWindow = defineModel('showEditWindow', {
  type: Boolean,
  default: false
})

const currentItem = defineModel('modelValue', {
  type: Object,
  default: () => ({})
})

const internalSaveCurrentItem = ({ form }) => {
  form.validate(valid => {
    if (valid) {
      props.saveCurrentItem(currentItem.value).then(() => {
        ElMessage.success($i18nBundle('common.msg.saveSuccess'))
      })
    }
  })
}

</script>

<template>
  <common-window
    v-model="showEditWindow"
    :title="currentItem?.id?$i18nKey('common.label.commonEdit', name):$i18nKey('common.label.commonAdd', name)"
    :ok-click="internalSaveCurrentItem"
  >
    <common-form
      v-if="currentItem"
      class="form-edit-width-100"
      :model="currentItem"
      :options="formOptions"
      label-width="100px"
      :show-buttons="false"
    />
  </common-window>
</template>

<style scoped>

</style>

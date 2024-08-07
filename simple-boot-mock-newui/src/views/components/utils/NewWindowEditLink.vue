<script setup>
import { showCodeWindow } from '@/utils/DynamicUtils'
import { $i18nBundle } from '@/messages'

const props = defineProps({
  tooltip: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: 'EditPen'
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  closeOnClickModal: {
    type: Boolean,
    default: false
  },
  fullEditor: {
    type: Boolean,
    default: false
  },
  language: {
    type: String,
    default: undefined
  }
})

const vModel = defineModel('modelValue', {
  type: String, default: ''
})

const newWindowEdit = () => {
  showCodeWindow(vModel, {
    title: $i18nBundle('common.label.codeEdit'),
    readOnly: props.readOnly,
    closeOnClickModal: props.closeOnClickModal,
    fullEditor: props.fullEditor,
    language: props.language
  })
}

</script>

<template>
  <el-link
    v-common-tooltip="tooltip || $t('common.label.newWindowEdit')"
    type="primary"
    :underline="false"
  >
    <common-icon
      :size="18"
      :icon="icon"
      @click="newWindowEdit"
    />
  </el-link>
</template>

<style scoped>

</style>

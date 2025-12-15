<script setup>
import { $i18nBundle, $i18nKey } from '@/messages'
import { ElMessage } from 'element-plus'
import { computed } from 'vue'
import { getStyleGrow } from '@/utils'

const props = defineProps({
  formOptions: {
    type: Array,
    default: () => []
  },
  name: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '800px'
  },
  saveCurrentItem: {
    type: Function,
    default: () => {}
  },
  inlineAutoMode: {
    type: Boolean,
    default: false
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
        showEditWindow.value = false
      })
    }
  })
  return false
}

const calcOptions = computed(() => {
  if (props.inlineAutoMode) {
    return props.formOptions.map(option => {
      const style = { ...getStyleGrow(10), ...option.style || {} }
      return {
        ...option, style
      }
    })
  }
  return props.formOptions
})

</script>

<template>
  <common-window
    v-model="showEditWindow"
    :title="currentItem?.id?$i18nKey('common.label.commonEdit', name):$i18nKey('common.label.commonAdd', name)"
    :ok-click="internalSaveCurrentItem"
    append-to-body
    destroy-on-close
    show-fullscreen
    :close-on-click-modal="false"
    :width="width"
  >
    <common-form
      v-if="currentItem"
      class="form-edit-width-100"
      :model="currentItem"
      :options="calcOptions"
      :show-buttons="false"
      v-bind="$attrs"
      :class-name="inlineAutoMode?'common-form-auto':''"
    >
      <template
        v-for="(slot, slotKey) in $slots"
        :key="slotKey"
        #[slotKey]="scope"
      >
        <slot
          :name="slotKey"
          v-bind="scope"
        />
      </template>
    </common-form>
  </common-window>
</template>

<style scoped>

</style>

<script setup>
import { useVModel } from '@vueuse/core'
import { computed, ref, provide, unref, watch, onBeforeUnmount } from 'vue'
import { UPDATE_MODEL_EVENT } from 'element-plus'
import { proxyMethod } from '@/components/utils'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  header: {
    type: String,
    default: ''
  },
  height: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '800px'
  },
  defaultCls: {
    type: [String, Object],
    default: ''
  },
  buttons: {
    type: Array,
    default: () => []
  },
  showClose: {
    type: Boolean,
    default: true
  },
  showFullscreen: {
    type: Boolean,
    default: false
  },
  dblclickToFullscreen: {
    type: Boolean,
    default: true
  },
  beforeClose: {
    type: Function,
    default: null
  },
  showOk: {
    type: Boolean,
    default: true
  },
  showCancel: {
    type: Boolean,
    default: true
  },
  okLabel: {
    type: String,
    default: ''
  },
  cancelLabel: {
    type: String,
    default: ''
  },
  okClick: {
    type: Function,
    default: null
  },
  cancelClick: {
    type: Function,
    default: null
  },
  closeClick: {
    type: Function,
    default: null
  },
  showButtons: {
    type: Boolean,
    default: true
  },
  destroyOnClose: {
    type: Boolean,
    default: false
  },
  draggable: {
    type: Boolean,
    default: true
  },
  overflow: {
    type: Boolean,
    default: true
  },
  closeOnClickModal: {
    type: Boolean,
    default: true
  },
  closeOnPressEscape: {
    type: Boolean,
    default: false
  },
  appendToBody: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits([UPDATE_MODEL_EVENT])
const showDialog = useVModel(props, 'modelValue', emit) // 自动响应v-model
const windowForms = ref([]) // 如果common-window下面有common-form，注册到这里
const commonWindow = ref({
  showWindow: showDialog,
  addForm (form) {
    if (form && !windowForms.value.includes(form)) {
      windowForms.value.push(form)
    }
  },
  removeForm (form) {
    if (form && windowForms.value.includes(form)) {
      windowForms.value.splice(windowForms.value.indexOf(form), 1)
    }
  }
})
provide('commonWindow', commonWindow)
const methods = ['validate', 'validateField', 'resetFields', 'clearValidate']
const windowForm = computed(() => {
  let result = windowForms.value[0]
  if (windowForms.value.length > 1) {
    result = methods.reduce((res, methodName) => {
      res[methodName] = proxyMethod(windowForms.value, methodName)
      return res
    }, {})
  }
  return unref(result)
})

const okButtonClick = $event => {
  if (!props.okClick || props.okClick({ $event, form: windowForm.value }) !== false) {
    showDialog.value = false
  }
}
const cancelButtonClick = $event => {
  if (!props.cancelClick || props.cancelClick({ $event, form: windowForm.value }) !== false) {
    showDialog.value = false
  }
}

const calcBeforeClose = computed(() => {
  if (props.beforeClose) {
    return props.beforeClose
  } else if (props.closeClick) {
    return done => {
      if (props.closeClick({ form: windowForm.value }) !== false) {
        done()
      }
    }
  }
  return null
})

const isFullscreen = defineModel('fullscreen', { type: Boolean, default: false })

const fullscreenRef = ref()
if (props.showFullscreen && props.dblclickToFullscreen) {
  watch(fullscreenRef, (fullscreenRefVal) => {
    const headerElement = fullscreenRefVal?.parentElement
    if (headerElement) {
      const dblclickHandler = () => { isFullscreen.value = !isFullscreen.value }
      headerElement.addEventListener('dblclick', dblclickHandler)
      onBeforeUnmount(() => headerElement.removeEventListener('dblclick', dblclickHandler))
    }
  })
}

</script>

<template>
  <el-dialog
    v-model="showDialog"
    class="common-window"
    :header="header||title"
    :before-close="calcBeforeClose"
    :width="width"
    :draggable="draggable"
    :overflow="overflow"
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    :append-to-body="appendToBody"
    :show-close="showClose"
    :fullscreen="isFullscreen"
  >
    <template #header="{ titleId, titleClass}">
      <slot name="header">
        <span
          :id="titleId"
          class="el-dialog__title"
          :class="titleClass"
        >
          {{ header||title }}
        </span>
      </slot>
      <button
        v-if="showFullscreen"
        ref="fullscreenRef"
        class="el-dialog__headerbtn dialog-fullscreen-btn"
        style="right: 30px;"
        @click="isFullscreen = !isFullscreen"
      >
        <common-icon
          class="el-dialog__close"
          :icon="isFullscreen?'FullscreenExitFilled':'FullscreenFilled'"
        />
      </button>
    </template>
    <el-container
      :class="defaultCls"
      :style="{ height:height }"
    >
      <slot
        name="default"
      />
    </el-container>
    <template
      v-if="showButtons"
      #footer
    >
      <span class="dialog-footer container-center">
        <el-button
          v-if="showOk"
          type="primary"
          @click="okButtonClick($event)"
        >{{ okLabel||$t('common.label.confirm') }}</el-button>
        <el-button
          v-if="showCancel"
          type="default"
          @click="cancelButtonClick($event)"
        >
          {{ cancelLabel||$t('common.label.cancel') }}
        </el-button>
        <template v-for="(button, index) in buttons">
          <el-button
            v-if="button.enabled!==false&&(!button.buttonIf||button.buttonIf())"
            :key="index"
            :type="button.type"
            :icon="button.icon"
            :size="button.size"
            :disabled="button.disabled"
            :round="button.round"
            :circle="button.circle"
            @click="button.click&&button.click({$event, form:windowForm})"
          >
            {{ button.label || $t(button.labelKey) }}
          </el-button>
        </template>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>

</style>

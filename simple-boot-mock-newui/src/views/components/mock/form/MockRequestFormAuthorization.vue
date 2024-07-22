<script setup>

import { ref, computed, watch, nextTick } from 'vue'
import { AUTH_OPTION_CONFIG, AUTH_OPTIONS, AUTH_TYPE } from '@/consts/MockConstants'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { useFormItem } from 'element-plus'

const props = defineProps({
  formProp: {
    type: String,
    default: 'authContent'
  }
})

const vModel = defineModel('modelValue', {
  type: Object,
  required: true
})

const authValid = defineModel('authValid', {
  type: Boolean,
  default: true
})

const authTypeSelectOption = ref({
  labelKey: 'mock.label.authType',
  type: 'radio-group',
  prop: 'authType',
  children: AUTH_OPTIONS,
  attrs: {
    clearable: false
  }
})

const authOptions = computed(() => {
  let options = AUTH_OPTION_CONFIG[vModel.value.authType]?.options || []
  if (vModel.value.authType === AUTH_TYPE.JWT) {
    options = [...options, jwtPayloadOption]
  }
  return options
})

const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })

languageRef.value = 'json'
if (!vModel.value.payload) {
  vModel.value.payload = '{}'
}

const jwtPayloadOption = {
  label: 'Payload',
  type: 'vue-monaco-editor',
  prop: 'payload',
  required: true,
  attrs: {
    value: vModel.value.payload,
    'onUpdate:value': (value) => {
      vModel.value.payload = value
      contentRef.value = value
    },
    language: languageRef.value,
    height: '100px',
    options: monacoEditorOptions
  }
}

const { form } = useFormItem()
watch([vModel, authOptions], () => {
  nextTick(async () => {
    if (form?.validateField) {
      authValid.value = await form?.validateField(authOptions.value.map(option => `${props.formProp}.${option.prop}`))
        .then(() => true).catch(() => false)
    }
  })
}, { immediate: true, deep: true })

</script>

<template>
  <el-container class="flex-column">
    <common-form-control
      :model="vModel"
      label-width="130px"
      :option="authTypeSelectOption"
    />
    <common-form-control
      v-for="option in authOptions"
      :key="option.prop"
      :model="vModel"
      :option="option"
      label-width="130px"
      :prop="`${formProp}.${option.prop}`"
    />
  </el-container>
</template>

<style scoped>

</style>

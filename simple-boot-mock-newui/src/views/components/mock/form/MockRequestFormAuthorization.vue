<script setup>

import { ref, computed } from 'vue'
import { AUTH_OPTION_CONFIG, AUTH_OPTIONS } from '@/consts/MockConstants'

const vModel = defineModel('modelValue', {
  type: Object,
  required: true
})

const authTypeSelectOption = ref({
  label: '认证类型',
  type: 'radio-group',
  prop: 'authType',
  children: AUTH_OPTIONS,
  attrs: {
    clearable: false
  }
})

const authOptions = computed(() => {
  return AUTH_OPTION_CONFIG[vModel.value.authType]?.options || []
})

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
      :prop="`authContent.${option.prop}`"
    />
    {{ vModel }}
  </el-container>
</template>

<style scoped>

</style>

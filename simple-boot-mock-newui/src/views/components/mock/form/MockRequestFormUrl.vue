<script setup>

import { getSingleSelectOptions } from '@/utils'
import { defineFormOptions } from '@/components/utils'
import { ALL_METHODS } from '@/api/mock/MockRequestApi'

const paramTarget = defineModel('modelValue', {
  type: Object,
  required: true
})

const baseFormOptions = defineFormOptions([{
  type: 'select',
  prop: 'method',
  children: getSingleSelectOptions(...ALL_METHODS.map(method => method.method)),
  labelWidth: '10px',
  style: {
    flexGrow: 1
  },
  attrs: {
    clearable: false
  }
}, {
  prop: 'requestPath',
  labelWidth: '10px',
  style: {
    flexGrow: 5
  },
  disabled: true
}])

</script>

<template>
  <common-form
    class-name="common-form-auto padding-top1"
    inline
    :show-buttons="false"
    :model="paramTarget"
    :options="baseFormOptions"
  />
</template>

<style scoped>

</style>

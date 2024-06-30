<script setup>
defineProps({
  /**
   * @type {CommonFormOption}
   */
  option: {
    type: Object,
    required: true
  },
  model: {
    type: Object,
    required: true
  },
  labelWidth: {
    type: String,
    default: null
  },
  prop: {
    type: String,
    default: ''
  }
})

</script>

<template>
  <common-form-control
    :model="model"
    :label-width="labelWidth"
    :option="option"
    :prop="prop"
    v-bind="$attrs"
  />
</template>

<style scoped>

</style>

<script setup>
defineProps({
  data: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: 'RemoveRedEyeFilled'
  },
  tooltip: {
    type: String,
    default: ''
  }
})
defineEmits(['viewDataDetails'])
</script>

<template>
  <div>
    <el-text v-bind="$attrs">
      {{ data }}
    </el-text>&nbsp;
    <el-link
      v-common-tooltip="tooltip"
      type="primary"
      :underline="false"
      @click="$emit('viewDataDetails')"
    >
      <common-icon :icon="icon" />
    </el-link>
  </div>
</template>

<style scoped>

</style>

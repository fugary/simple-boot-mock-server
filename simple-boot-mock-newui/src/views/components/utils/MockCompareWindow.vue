<script setup>
defineProps({
  title: {
    type: String,
    default: 'Data Compare'
  },
  target: {
    type: Object, default: () => ({})
  },
  compare: {
    type: Object, default: () => ({})
  },
  columns: {
    type: Array, default: () => []
  }
})
const showWindow = defineModel({
  type: Boolean,
  default: false
})
</script>

<template>
  <common-window
    v-model="showWindow"
    width="1000px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    destroy-on-close
    :title="title"
    append-to-body
    show-fullscreen
    v-bind="$attrs"
  >
    <el-container class="flex-column">
      111
    </el-container>
  </common-window>
</template>

<style scoped>

</style>

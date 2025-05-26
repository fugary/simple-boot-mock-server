<script setup>
defineProps({
  data: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: 'PostAddOutlined'
  },
  iconSize: {
    type: String,
    default: '18px'
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
    <el-link
      v-if="data"
      v-common-tooltip="tooltip"
      type="primary"
      v-bind="$attrs"
      @click="$emit('viewDataDetails')"
    >
      {{ data }}
    </el-link>&nbsp;
    <el-link
      v-else
      v-common-tooltip="tooltip"
      type="primary"
      underline="never"
      @click="$emit('viewDataDetails')"
    >
      <common-icon
        :size="iconSize"
        :icon="icon"
      />
    </el-link>
  </div>
</template>

<style scoped>

</style>

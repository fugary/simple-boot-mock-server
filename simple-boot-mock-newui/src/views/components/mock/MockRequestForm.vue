<script setup>
defineProps({
  request: {
    type: Object,
    required: true
  },
  data: {
    type: Object,
    default: undefined
  }
})
</script>

<template>
  <el-container class="flex-column">
    <el-tabs
      type="border-card"
      class="form-edit-width-100"
    >
      <el-tab-pane label="路径参数">
        路径参数
      </el-tab-pane>
      <el-tab-pane label="请求参数">
        请求参数
      </el-tab-pane>
      <el-tab-pane label="请求头">
        请求头
      </el-tab-pane>
      <el-tab-pane label="请求体">
        请求体
      </el-tab-pane>
    </el-tabs>
    {{ request }}
  </el-container>
</template>

<style scoped>

</style>

<template>
  <div>
    <el-alert :closable="false" title="数据测试">
      <router-view />
    </el-alert>
  </div>
</template>

<script>
export default {
  name: 'MockDataPreview'
}
</script>

<style scoped>

</style>

<template>
  <div class="text-center">
    <h1>
      404
    </h1>
    <div class="padding">
      页面不存在！！！
    </div>
    <el-button
      icon="el-icon-back"
      @click="$router.replace('/')"
      round
      type="primary"
    >
      返回
    </el-button>
  </div>
</template>

<script>
export default {
  name: '404'
}
</script>

<style scoped></style>

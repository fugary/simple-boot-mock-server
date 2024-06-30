<script setup>
import UserInfo from '@/views/components/user/UserInfo.vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
</script>

<template>
  <el-container>
    <user-info
      :user="useLoginConfigStore().accountInfo"
    />
  </el-container>
</template>

<style scoped>

</style>

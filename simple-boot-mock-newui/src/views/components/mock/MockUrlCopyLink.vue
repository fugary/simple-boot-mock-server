<script setup>
import { $copyText } from '@/utils'
import { getMockUrl } from '@/api/mock/MockRequestApi'

defineProps({
  urlPath: {
    type: String,
    default: ''
  }
})

</script>

<template>
  <el-link
    v-common-tooltip="'复制请求地址'"
    type="primary"
    :underline="false"
  >
    <common-icon
      :size="18"
      icon="DocumentCopy"
      @click="$copyText(getMockUrl(urlPath))"
    />
  </el-link>
</template>

<style scoped>

</style>

<script setup>
import { $copyText } from '@/utils'
import { getMockUrl } from '@/api/mock/MockRequestApi'

const props = defineProps({
  urlPath: {
    type: String,
    default: ''
  },
  content: {
    type: String,
    default: ''
  },
  tooltip: {
    type: String,
    default: '复制链接地址'
  }
})

const copyInfo = () => {
  let info = props.content
  if (!info && props.urlPath) {
    info = getMockUrl(props.urlPath)
  }
  if (info) {
    $copyText(info)
  }
}

</script>

<template>
  <el-link
    v-common-tooltip="tooltip"
    type="primary"
    :underline="false"
    @click="copyInfo"
  >
    <slot>
      <common-icon
        :size="18"
        icon="DocumentCopy"
      />
    </slot>
  </el-link>
</template>

<style scoped>

</style>

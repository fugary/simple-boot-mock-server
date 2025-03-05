<script setup>
import { $copyText } from '@/utils'
import { getMockUrl } from '@/api/mock/MockRequestApi'
import { isExternalLink } from '@/components/utils'
import { computed } from 'vue'

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

const info = computed(() => {
  let info = props.content
  if (!info && props.urlPath) {
    info = getMockUrl(props.urlPath)
  }
  return info
})

const copyInfo = () => {
  if (info.value) {
    $copyText(info.value)
  }
}

const externalLink = computed(() => isExternalLink(info.value) ? info.value : '')

</script>

<template>
  <el-link
    v-common-tooltip="tooltip"
    v-open-new-window="externalLink"
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

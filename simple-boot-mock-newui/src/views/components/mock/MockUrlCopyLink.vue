<script setup>
import { $copyText } from '@/utils'
import { getMockUrl } from '@/api/mock/MockRequestApi'
import { isExternalLink } from '@/components/utils'
import { computed } from 'vue'
import { $i18nBundle } from '@/messages'

const props = defineProps({
  showLink: {
    type: Boolean,
    default: true
  },
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
    default: null
  },
  icon: {
    type: String,
    default: 'DocumentCopy'
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

const tooltip = computed(() => props.tooltip ?? $i18nBundle('common.msg.clickToCopy'))

</script>

<template>
  <el-link
    v-if="showLink"
    v-common-tooltip="tooltip"
    v-open-new-window="externalLink"
    type="primary"
    underline="never"
    @click="copyInfo"
  >
    <slot>
      <common-icon
        :size="18"
        :icon="icon"
      />
    </slot>
  </el-link>
</template>

<style scoped>

</style>

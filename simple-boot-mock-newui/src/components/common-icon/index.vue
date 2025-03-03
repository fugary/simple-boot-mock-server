<script setup>
import { computed } from 'vue'
import { ICON_PREFIX } from '@/icons'
import { kebabCase } from 'lodash-es'

const props = defineProps({
  icon: {
    type: String,
    default: ''
  }
})
const calcIcon = computed(() => {
  if (props.icon) {
    return `${ICON_PREFIX}${kebabCase(props.icon)}`
  }
  return props.icon
})

const customIcon = computed(() => {
  if (props.icon?.startsWith('custom')) {
    return props.icon
  }
  return ''
})

</script>

<template>
  <el-icon
    v-if="calcIcon"
    v-bind="$attrs"
  >
    <component
      :is="calcIcon"
      v-if="!customIcon"
    />
    <span
      v-else
      class="custom-icon"
      :class="customIcon"
    />
  </el-icon>
</template>

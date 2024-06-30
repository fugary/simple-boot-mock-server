<script setup>
import { computed } from 'vue'
import { $i18nBundle } from '@/messages'

const props = defineProps({
  typeConfig: {
    type: Object,
    default: null
  },
  valueConfig: {
    type: Object,
    default: null
  },
  modelValue: {
    type: [Number, String],
    default: ''
  },
  effect: {
    type: String,
    default: 'light'
  },
  reverse: {
    type: Boolean,
    default: true
  }
})

const typeConf = computed(() => {
  if (!props.typeConfig) {
    return props.reverse
      ? {
          1: 'success',
          0: 'danger'
        }
      : {
          0: 'success',
          1: 'danger'
        }
  }
  return props.typeConfig
})

const valueConf = computed(() => {
  if (!props.valueConfig) {
    return props.reverse
      ? {
          1: $i18nBundle('common.label.statusEnabled'),
          0: $i18nBundle('common.label.statusDisabled')
        }
      : {
          0: $i18nBundle('common.label.statusEnabled'),
          1: $i18nBundle('common.label.statusDisabled')
        }
  }
  return props.valueConfig
})

</script>

<template>
  <el-tag
    v-if="valueConf[modelValue]"
    :effect="effect"
    class="statusTag"
    :type="typeConf[modelValue]"
    v-bind="$attrs"
  >
    {{ valueConf[modelValue] }}
  </el-tag>
</template>

<style scoped>

</style>

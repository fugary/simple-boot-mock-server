<script setup>
import { computed } from 'vue'
import { $i18nBundle, $i18nKey } from '@/messages'
import { $coreConfirm } from '@/utils'

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
  },
  clickToToggle: {
    type: Boolean,
    default: false
  },
  confirmBeforeToggle: {
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

const reversedValue = computed(() => {
  return Object.keys(valueConf.value).find(key => key !== `${props.modelValue}`)
})

const emit = defineEmits(['toggleValue'])
const tooltip = computed(() => {
  let toValue = $i18nBundle('common.label.statusEnabled')
  if (toValue === valueConf.value[props.modelValue]) {
    toValue = $i18nBundle('common.label.statusDisabled')
  }
  return {
    disabled: !props.clickToToggle,
    content: $i18nKey('common.msg.clickTo', toValue)
  }
})

const handleClick = $event => {
  $event.stopPropagation()
  if (props.clickToToggle) {
    if (props.confirmBeforeToggle) {
      $coreConfirm($i18nBundle('common.msg.commonConfirm', [valueConf.value[reversedValue.value]]))
        .then(() => emit('toggleValue', reversedValue.value))
    } else {
      emit('toggleValue', reversedValue.value)
    }
  }
}

</script>

<template>
  <el-link
    v-common-tooltip="tooltip"
    underline="never"
    @click="handleClick"
  >
    <el-tag
      v-if="valueConf[modelValue]"
      :effect="effect"
      class="statusTag"
      :type="typeConf[modelValue]"
      v-bind="$attrs"
    >
      {{ valueConf[modelValue] }}
    </el-tag>
  </el-link>
</template>

<style scoped>

</style>

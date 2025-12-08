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
  },
  switchMode: {
    type: Boolean,
    default: true
  }
})

const modelValue = defineModel({
  type: [Number, String],
  default: ''
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
  return Object.keys(valueConf.value).find(key => key !== `${modelValue.value}`)
})

const emit = defineEmits(['toggleValue'])
const tooltip = computed(() => {
  let toValue = $i18nBundle('common.label.statusEnabled')
  if (toValue === valueConf.value[modelValue.value]) {
    toValue = $i18nBundle('common.label.statusDisabled')
  }
  return {
    disabled: !props.clickToToggle,
    content: $i18nKey('common.msg.clickTo', toValue)
  }
})

const handleClick = () => {
  if (props.clickToToggle) {
    if (props.confirmBeforeToggle) {
      return $coreConfirm($i18nBundle('common.msg.commonConfirm', [valueConf.value[reversedValue.value]]))
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
    @click.stop="!switchMode?handleClick($event):null"
  >
    <el-switch
      v-if="switchMode"
      v-model="modelValue"
      :disabled="!clickToToggle"
      style="--el-switch-on-color: #67c23a; --el-switch-off-color: #f56c6c"
      :active-value="Number(Object.keys(valueConf)[1])"
      :inactive-value="Number(Object.keys(valueConf)[0])"
      :before-change="handleClick"
      @click.stop
    />
    <el-tag
      v-else-if="valueConf[modelValue]"
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

<script setup>
import { computed } from 'vue'
import { toLabelByKey } from '@/components/utils'
import { isFunction, cloneDeep } from 'lodash-es'

const props = defineProps({
  buttonConfig: {
    type: Object,
    default: () => ({})
  },
  item: {
    type: Object,
    default: null
  },
  buttonSize: {
    type: String,
    default: 'small'
  },
  scope: {
    type: Object,
    default: () => ({})
  }
})

const button = computed(() => {
  let buttonConfig = cloneDeep(props.buttonConfig)
  if (isFunction(buttonConfig.buttonIf) && props.item && props.scope.$index > -1) {
    buttonConfig.enabled = !!buttonConfig.buttonIf(props.item)
  }
  if (isFunction(buttonConfig.dynamicButton) && props.item && props.scope.$index > -1) {
    buttonConfig = { ...buttonConfig, ...buttonConfig.dynamicButton(props.item) }
  }
  return buttonConfig
})

</script>

<template>
  <el-button
    v-if="button.enabled!==false"
    v-common-tooltip="button.tooltip"
    :type="button.type"
    :size="button.size||buttonSize"
    :disabled="button.disabled"
    :round="button.round"
    :circle="button.circle"
    v-bind="button.attrs"
    @click="button.click&&button.click(item, scope)"
  >
    {{ button.label || toLabelByKey(button.labelKey) }}
    <template
      v-if="!!button.icon"
      #icon
    >
      <common-icon
        :icon="button.icon"
        :size="button.iconSize"
      />
    </template>
  </el-button>
</template>

<style scoped>

</style>

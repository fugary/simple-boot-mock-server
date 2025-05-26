<script setup>
import { computed } from 'vue'
import { toLabelByKey, useInputType } from '@/components/utils'
import { isFunction } from 'lodash-es'

/**
 * @type {{option:CommonFormOption}}
 */
const props = defineProps({
  /**
   * @type {CommonFormOption}
   */
  option: {
    type: Object,
    required: true
  }
})

const inputType = computed(() => useInputType(props.option))

const label = computed(() => {
  const option = props.option
  if (option.labelKey) {
    return toLabelByKey(option.labelKey)
  }
  return option.label
})

const tooltipFunc = ($event) => {
  $event.preventDefault()
  $event.stopImmediatePropagation()
  if (isFunction(props.option.tooltipFunc)) {
    props.option.tooltipFunc($event)
  }
}

</script>

<template>
  <component
    :is="inputType"
    :value="option.value"
    :label="label"
    :disabled="option.disabled"
    :readonly="option.readonly"
    v-bind="option.attrs"
  >
    {{ label }}
    <el-tooltip
      v-if="option.tooltip||option.tooltipFunc"
      class="box-item common-el-tooltip"
      effect="dark"
      :disabled="!option.tooltip"
      :content="option.tooltip"
      placement="top-start"
      raw-content
    >
      <span>
        <el-link
          underline="never"
          @click="tooltipFunc($event)"
        >&nbsp;
          <common-icon
            icon="QuestionFilled"
          />
        </el-link>
      </span>
    </el-tooltip>
  </component>
</template>

<style scoped>

</style>

<script setup>
import { computed, isVNode, ref, watch } from 'vue'
import { $i18nBundle } from '@/messages'
import ControlChild from '@/components/common-form-control/control-child.vue'
import { toLabelByKey, useInputType } from '@/components/utils'
import { cloneDeep, get, isFunction, set, isArray, isString } from 'lodash-es'

import dayjs from 'dayjs'

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
  },
  model: {
    type: Object,
    required: true
  },
  labelWidth: {
    type: String,
    default: null
  },
  addInfo: {
    type: Object,
    default: () => ({})
  }
})

const needProcessValue = option => option.trim || option.upperCase || option.lowerCase
const processValue = (value, option) => {
  if (value && isString(value)) {
    value = option.trim ? value.trim() : value
    value = option.upperCase ? value.toUpperCase() : value
    value = option.lowerCase ? value.toLowerCase() : value
    modelValue.value = value
  }
  return value
}

const calcOption = computed(() => {
  let option = { ...props.option }
  if (isFunction(option.dynamicOption)) {
    option = { ...option, ...option.dynamicOption(props.model, option, props.addInfo) }
  } else if (isFunction(option.dynamicAttrs)) {
    option = { ...option, attrs: { ...option.attrs, ...option.dynamicAttrs(props.model, option, props.addInfo) } }
  }
  if (needProcessValue(option)) {
    const change = option.change
    option.change = value => {
      value = processValue(value, option)
      isFunction(change) && change(value)
    }
  }
  return option
})

const inputType = computed(() => useInputType(calcOption.value))

const modelAttrs = computed(() => {
  const option = calcOption.value
  const attrs = { ...option.attrs }
  if (['el-input', 'el-select', 'el-select-v2', 'common-autocomplete', 'el-autocomplete', 'el-cascader', 'el-tree-select'].includes(inputType.value)) {
    attrs.clearable = attrs.clearable ?? true
  }
  if (inputType.value === 'common-autocomplete' && option.getAutocompleteLabel) {
    attrs.defaultLabel = option.getAutocompleteLabel(props.model, option)
  }
  if (inputType.value === 'el-date-picker') {
    attrs.disabledDate = attrs.disabledDate || ((date) => {
      const option = calcOption.value
      let result = false
      if (option.minDate) {
        result = date.getTime() < dayjs(option.minDate).startOf('d').toDate().getTime()
      }
      if (!result && option.maxDate) {
        result = date.getTime() > dayjs(option.maxDate).startOf('d').toDate().getTime()
      }
      return result
    })
    const defaultValue = attrs.defaultValue || modelValue.value || option.minDate
    if (defaultValue && !isArray(defaultValue)) {
      attrs.defaultValue = dayjs(defaultValue).toDate()
    }
  }
  return attrs
})

watch([inputType, () => calcOption.value.minDate, () => calcOption.value.maxDate], ([type]) => {
  const option = calcOption.value
  const date = modelValue.value
  if (type === 'el-date-picker' && date && !option.disabled && option.clearInvalidDate !== false) {
    let invalid = false
    if (isFunction(modelAttrs.value.disabledDate)) {
      invalid = modelAttrs.value.disabledDate(dayjs(date).toDate())
    }
    if (invalid) {
      modelValue.value = undefined
    }
  }
})

const label = computed(() => {
  const option = calcOption.value
  if (option.labelKey) {
    return toLabelByKey(option.labelKey)
  }
  return option.label
})

const showLabel = computed(() => {
  return calcOption.value.showLabel !== false && props.labelWidth !== '0'
})

const formModel = computed(() => calcOption.value.model || props.model)

const modelValue = computed({
  get () {
    if (formModel.value && calcOption.value.prop) {
      return get(formModel.value, calcOption.value.prop)
    }
    return null
  },
  set (val) {
    if (formModel.value && calcOption.value.prop) {
      set(formModel.value, calcOption.value.prop, val)
    }
  }
})

const childTypeMapping = { // 自动映射子元素类型，配置的时候可以不写type
  'checkbox-group': 'checkbox',
  'radio-group': 'radio',
  select: 'option'
}

const children = computed(() => {
  const option = calcOption.value
  let result = option.children || [] // 初始化一些默认值
  result = result.filter(childItem => childItem.enabled !== false)
  result.forEach(childItem => {
    if (!childItem.type) {
      childItem.type = childTypeMapping[option.type]
    }
  })
  return result
})

const formItemRef = ref()

const rules = computed(() => {
  const option = calcOption.value
  let _rules = cloneDeep(option.rules || [])
  if (option.prop) {
    if (option.required) {
      const label = option.label || toLabelByKey(option.labelKey)
      _rules = [{
        trigger: option.trigger,
        required: option.required,
        message: $i18nBundle('common.msg.nonNull', [label])
      }, ..._rules]
    }
    if (option.pattern) {
      const label = option.label || toLabelByKey(option.labelKey)
      _rules = [{
        pattern: option.pattern,
        message: option.patternMsg || $i18nBundle('common.msg.patternInvalid', [label])
      }, ..._rules]
    }
  }
  formItemRef.value && formItemRef.value.clearValidate()
  return _rules
})

const initFormModel = () => {
  if (formModel.value) {
    const option = calcOption.value
    if (option.prop) {
      const defaultVal = get(formModel.value, option.prop)
      set(formModel.value, option.prop, defaultVal ?? option.value ?? undefined)
    }
  }
}

initFormModel()

watch(() => calcOption.value, initFormModel, { deep: true })

const hasModelText = computed(() => isFunction(calcOption.value.formatter))

const emit = defineEmits(['change'])

const controlChange = (...args) => {
  const option = calcOption.value
  if (option.change) {
    option.change(...args)
  }
  emit('change', ...args)
}

const formItemEnabled = computed(() => calcOption.value.enabled !== false)

const controlLabelWidth = computed(() => {
  const option = calcOption.value
  const labelWidth = props.labelWidth
  return option.labelWidth || modelAttrs.value.labelWidth || labelWidth
})

const formatResult = computed(() => {
  if (hasModelText.value) {
    const result = calcOption.value.formatter(modelValue.value, calcOption.value)
    return {
      modelText: result,
      vnode: isVNode(result)
    }
  }
  return null
})

</script>

<template>
  <el-form-item
    v-if="formItemEnabled"
    ref="formItemRef"
    :rules="rules"
    :prop="calcOption.prop"
    :style="calcOption.style"
    :label-width="controlLabelWidth"
    v-bind="$attrs"
  >
    <template
      v-if="showLabel"
      #label
    >
      <slot name="beforeLabel" />
      <span
        v-if="!$slots.label"
        :class="calcOption.labelCls"
      >{{ label }}</span>
      <slot
        v-else
        name="label"
        :option="calcOption"
        :model="formModel"
      />
      <slot name="afterLabel" />
      <el-tooltip
        v-if="calcOption.tooltip||calcOption.tooltipFunc"
        class="box-item common-el-tooltip"
        effect="dark"
        :disabled="!calcOption.tooltip"
        :content="calcOption.tooltip"
        placement="top-start"
        raw-content
        v-bind="calcOption.tooltipAttrs"
      >
        <span>
          <el-link
            v-bind="calcOption.tooltipLinkAttrs"
            underline="never"
            @click="calcOption.tooltipFunc"
          >&nbsp;
            <common-icon
              :icon="calcOption.tooltipIcon||'QuestionFilled'"
            />
          </el-link>
        </span>
      </el-tooltip>
    </template>
    <slot>
      <component
        :is="inputType"
        v-model="modelValue"
        v-bind="modelAttrs"
        :placeholder="calcOption.placeholder"
        :disabled="calcOption.disabled"
        :readonly="calcOption.readonly"
        @change="controlChange"
      >
        <template
          v-for="(slot, slotKey) in (calcOption.slots||{})"
          :key="slotKey"
          #[slotKey]="scope"
        >
          <component
            :is="scope[`__slotResult__${slotKey}`]"
            v-if="isVNode(scope[`__slotResult__${slotKey}`] = slot(scope, calcOption))"
          />
          <template v-else>
            {{ scope[`__slotResult__${slotKey}`] }}
          </template>
        </template>
        <template
          v-if="hasModelText&&formatResult"
          #default
        >
          <span
            v-if="formatResult.modelText&&!formatResult.vnode"
            class="common-form-label-text"
            v-html="formatResult.modelText"
          />
          <component
            :is="formatResult.modelText"
            v-if="formatResult.vnode"
            class="common-form-label-text"
          />
        </template>
        <slot name="childBefore" />
        <template v-if="children&&children.length">
          <control-child
            v-for="(childItem, index) in children"
            :key="index"
            :option="childItem"
          />
        </template>
        <slot name="childAfter" />
      </component>
    </slot>
    <slot name="after" />
  </el-form-item>
</template>

<style scoped>

</style>

<script setup>
import { useVModel } from '@vueuse/core'
import { onMounted } from 'vue'
import { toLabelByKey } from '@/components/utils'

/**
 * 同时只能有一个字段被排序
 * 排序的数据会被放到vModel中
 * 如 { "deptTime": "DESC", "price": "", "duration": "" }
 * @type {{modelValue:SortProps}} 可以传入默认排序值，但是最多只能有一个字段有值 如：{ "deptTime": "DESC", "price": "", "duration": "" }
 * @type {{options:SortOption[]}} 所有要排序的字段和要显示的值
 */
const props = defineProps({
  modelValue: {
    type: Object,
    default: () => { }
  },
  options: {
    type: Array,
    default () {
      return []
    }
  }
})
const emit = defineEmits(['update:modelValue'])
const vModel = useVModel(props, 'modelValue', emit)

onMounted(() => {
  // 根据传入的option和初始vmodel设置vModel
  const model = {}
  for (const option of props.options) {
    const prop = option.prop
    if (vModel.value[prop] !== undefined) {
      model[prop] = vModel.value[prop]
    } else {
      model[option.prop] = ''
    }
  }
  vModel.value = model
})

/**
 * 计算要显示的label
 * @param {SortOption} option
 */
const calLabel = (option) => {
  if (option.labelKey) {
    return toLabelByKey(option.labelKey)
  }
  if (option.label) {
    return option.label
  }
  return 'undefined'
}
const handleClick = (key, fixedValue) => {
  const newValue = { ...vModel.value }
  for (const k in newValue) {
    if (k === key) { continue }
    newValue[k] = ''
  }
  if (fixedValue) {
    newValue[key] = fixedValue
  } else {
    newValue[key] = newValue[key] === 'ASC' ? 'DESC' : 'ASC'
  }
  vModel.value = newValue
}

</script>

<template>
  <el-button-group
    class="ml-4"
  >
    <template
      v-for="option in props.options"
      :key="option.prop"
    >
      <el-button
        :type="vModel[option.prop]===''?'':'primary'"
        @click="handleClick(option.prop, option.fixedValue)"
      >
        {{ calLabel(option) }}
        <template v-if="option.showIcon!==false">
          <common-icon
            v-if="vModel[option.prop]==='ASC'"
            icon="SortUp"
          />
          <common-icon
            v-if="vModel[option.prop]==='DESC'"
            icon="SortDown"
          />
        </template>
      </el-button>
    </template>
  </el-button-group>
</template>

<style scoped>

</style>

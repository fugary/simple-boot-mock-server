<script setup>
import { calcSlotsResult, toLabelByKey } from '@/components/utils'
import { isFunction } from 'lodash-es'
import { isVNode, computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  width: {
    type: String,
    default: ''
  },
  minWidth: {
    type: String,
    default: ''
  }
})

const calcItems = computed(() => {
  return props.items.filter(item => item.enabled !== false).map(item => {
    let label = item.labelKey ? toLabelByKey(item.labelKey) : item.label
    label = isFunction(item.labelFormatter) ? item.labelFormatter(item) : label
    const value = isFunction(item.formatter) ? item.formatter(item) : item.value
    const labelResult = { label, vnode: isVNode(label) }
    const valueResult = { value, vnode: isVNode(value) }
    const slotsResult = calcSlotsResult(item)
    return {
      ...item,
      width: item.width || props.width,
      minWidth: item.minWidth || props.minWidth,
      valueResult,
      labelResult,
      slotsResult
    }
  })
})

</script>

<template>
  <el-descriptions
    v-bind="$attrs"
  >
    <el-descriptions-item
      v-for="(calcItem, index) in calcItems"
      :key="index"
      :min-width="calcItem.minWidth"
      :width="calcItem.width"
      :span="calcItem.span"
      :align="calcItem.align"
      v-bind="calcItem.attrs"
    >
      <template
        v-for="(slot, slotKey) in (calcItem.slots||{})"
        :key="slotKey"
        #[slotKey]
      >
        <component
          :is="calcItem.slotsResult[slotKey].result"
          v-if="calcItem.slotsResult[slotKey]?.vnode"
        />
        <template v-else>
          {{ calcItem.slotsResult[slotKey].result }}
        </template>
      </template>
      <template
        v-if="calcItem.labelResult.label"
        #label
      >
        <span
          v-if="calcItem.labelResult.label&&!calcItem.labelResult.vnode"
          v-html="calcItem.labelResult.label"
        />
        <component
          :is="calcItem.labelResult.label"
          v-if="calcItem.labelResult.vnode"
        />
      </template>
      <template v-if="calcItem.valueResult.value">
        <span
          v-if="calcItem.valueResult.value&&!calcItem.valueResult.vnode"
          v-html="calcItem.valueResult.value"
        />
        <component
          :is="calcItem.valueResult.value"
          v-if="calcItem.valueResult.vnode"
        />
      </template>
    </el-descriptions-item>
  </el-descriptions>
</template>

<style scoped>

</style>

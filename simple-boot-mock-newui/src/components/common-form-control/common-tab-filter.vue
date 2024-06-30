<script setup>
import { useVModel } from '@vueuse/core'
import ControlChild from '@/components/common-form-control/control-child.vue'
import { computed } from 'vue'
import { useInputType } from '@/components/utils'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => {}
  },
  tabs: {
    type: Array,
    default: () => []
  },
  type: {
    type: String,
    default: 'checkbox-group'
  },
  defaultIcon: {
    type: String,
    default: 'CaretBottom'
  },
  iconSize: {
    type: Number,
    default: undefined
  }
})
const emit = defineEmits(['update:modelValue', 'change'])
const vModel = useVModel(props, 'modelValue', emit)

const childTypeMapping = { // 自动映射子元素类型，配置的时候可以不写type
  'checkbox-group': 'checkbox',
  'radio-group': 'radio',
  select: 'option'
}

const calcTabs = computed(() => {
  return props.tabs.map(tab => {
    tab.children = (tab.children || []).map(child => {
      child.type = child.type || childTypeMapping[props.type]
      return child
    })
    if (!tab.icon) {
      tab.icon = props.defaultIcon
    }
    return tab
  })
})

const inputType = computed(() => useInputType({ type: props.type }))

</script>

<template>
  <el-tabs
    type="border-card"
    class="form-edit-width-100"
  >
    <el-tab-pane
      v-for="(tab, index) in calcTabs"
      :key="tab.label + index"
    >
      <template #label>
        {{ tab.label }}&nbsp;
        <common-icon
          :size="tab.iconSize||iconSize"
          :icon="tab.icon"
        />
      </template>
      <component
        :is="inputType"
        v-if="vModel"
        v-model="vModel[tab.prop]"
        @change="vModel.isTabFilter=true;$emit('change', vModel)"
      >
        <control-child
          v-for="(childItem, childIdx) in tab.children"
          :key="childIdx"
          :option="childItem"
        />
      </component>
    </el-tab-pane>
  </el-tabs>
</template>

<style scoped>

</style>

<script setup>
import { formatDate } from '@/utils'
import { computed, useSlots } from 'vue'
import { get } from 'lodash-es'
import { toLabelByKey } from '@/components/utils'
import TableDynamicButton from '@/components/common-table/table-dynamic-button.vue'

/**
 * 配置信息
 * @type {CommonTableColumnProps}
 */
const props = defineProps({
  /**
   * @type {CommonTableColumn}
   */
  column: {
    type: Object,
    required: true
  },
  /**
   * @type {''|'large'|'small'|'default'}
   */
  buttonSize: {
    type: String,
    default: 'small'
  }
})

const formatter = computed(() => {
  const column = props.column
  if (!column.formatter && column.dateFormat) { // 没有formatter但是有dateFormat
    return row => {
      const data = getPropertyData(row)
      return formatDate(data, column.dateFormat)
    }
  }
  return column.formatter
})

const getPropertyData = (row) => {
  return get(row, props.column.prop || props.column.property)
}

const slots = useSlots()

</script>

<template>
  <el-table-column
    v-if="!column.isOperation"
    :label="column.label || toLabelByKey(column.labelKey)"
    :prop="column.prop||column.property"
    :width="column.width"
    :min-width="column.minWidth"
    v-bind="column.attrs"
    :formatter="formatter"
  >
    <template
      v-if="column.click"
      #default="scope"
    >
      <el-link
        v-if="column.click"
        type="primary"
        v-bind="column.linkAttrs"
        @click="column.click(scope.row, scope)"
      >
        {{ formatter?formatter(scope.row, scope):getPropertyData(scope.row) }}
      </el-link>
    </template>
    <template
      v-for="(slot, slotKey) in slots"
      :key="slotKey"
      #[slotKey]="scope"
    >
      <slot
        :name="slotKey"
        v-bind="scope"
        :column-conf="column"
      />
    </template>
  </el-table-column>
  <el-table-column
    v-if="column.isOperation"
    :label="column.label || toLabelByKey(column.labelKey)"
    :width="column.width"
    v-bind="column.attrs"
  >
    <template
      #default="scope"
    >
      <template
        v-for="(button, index) in column.buttons"
        :key="index"
      >
        <table-dynamic-button
          :button-config="button"
          :item="scope.row"
          :button-size="buttonSize"
          :scope="{...scope,item:scope.row}"
        />
      </template>
      <slot
        name="default"
        v-bind="scope"
      />
    </template>
  </el-table-column>
</template>

<style scoped>

</style>

<script setup lang="jsx">
import { computed, useSlots, ref, onMounted } from 'vue'
import { toLabelByKey } from '@/components/utils'
import { get } from 'lodash-es'
import { $i18nBundle } from '@/messages'
import TableDynamicButton from '@/components/common-table/table-dynamic-button.vue'
import { formatDate } from '@/utils'

defineOptions({
  inheritAttrs: false
})

/**
 * @type CommonTableProps
 */
const props = defineProps({
  /**
   * @type {[CommonTableColumn]}
   */
  columns: {
    type: Array,
    default: () => []
  },
  /**
   * 显示数据
   */
  data: {
    type: Array,
    default () {
      return []
    }
  },
  highlightCurrentRow: {
    type: Boolean,
    default: true
  },
  stripe: {
    type: Boolean,
    default: true
  },
  border: {
    type: Boolean,
    default: false
  },
  rowHeight: {
    type: Number,
    default: 50
  },
  height: {
    type: String,
    default: '500px'
  },
  autoColWidth: {
    type: Boolean,
    default: true
  },
  defaultColWidth: {
    type: String,
    default: '150px'
  },
  expandColumnKey: {
    type: String,
    default: ''
  },
  rowKey: {
    type: String,
    default: 'id'
  },
  loading: {
    type: Boolean,
    default: false
  },
  loadingText: {
    type: String,
    default: ''
  },
  /**
   * el-button
   * @type [TableButtonProps]
   */
  buttons: {
    type: Array,
    default () {
      return []
    }
  },
  buttonsSlot: {
    type: String,
    default: ''
  },
  buttonSize: {
    type: String,
    default: 'small'
  },
  buttonsColumnAttrs: {
    type: Object,
    default: null
  }
})

const expandRowKeys = defineModel('expandRowKeys', {
  type: Array, default: undefined
})

const calcExpandColumnKey = computed(() => {
  if (!props.expandColumnKey && props.columns.length && props.expandRowKeys) {
    return getColumnKey(props.columns[0])
  }
  return props.expandColumnKey
})

const getColumnKey = (column) => {
  return column.prop || column.property || column.slot
}

const getPropertyData = (row, column) => {
  return get(row, column.prop || column.property)
}

/**
 * v1版本formatter转换成CellRenderer，方便统一配置
 * @param column
 * @return {function(*): *}
 */
const formatter2Render = (column) => {
  let formatter = column.formatter
  if (column.dateFormat && !formatter) {
    formatter = row => {
      const data = getPropertyData(row, column)
      return formatDate(data, column.dateFormat)
    }
  }
  if (formatter) {
    return (data) => {
      const value = getPropertyData(data.rowData, column)
      return formatter(data.rowData, data.cellData, value, data.rowIndex)
    }
  }
}

const slots = useSlots()

const data2Scope = (data) => {
  return {
    row: data.rowData,
    item: data.rowData,
    column: data.column,
    $index: data.rowIndex
  }
}

/**
 * v1版本slot信息转换成CellRenderer，方便统一配置
 * @param slotName 槽的名称
 * @return {function(*): VNode[]}
 */
const slot2Render = (slotName) => {
  if (slotName && slots[slotName]) {
    return (data) => {
      // row: any, column: any, $index: number
      console.log('===================================slot', data)
      return slots[slotName](data2Scope(data))
    }
  }
}

const defaultWidth = computed(() => {
  if (props.autoColWidth) {
    const columnCount = (props.buttons?.length ? props.columns.length + 1 : props.columns.length) || 1
    return 100 / columnCount + '%'
  }
  return defaultWidth
})

const calcColumns = computed(() => {
  const tmpColumns = props.columns.filter(column => column.enabled !== false)
    .map((column) => {
      return Object.assign({
        title: column.label || toLabelByKey(column.labelKey),
        headerCellRenderer: column.headerCellRenderer || slot2Render(column.headerSlot),
        cellRenderer: column.cellRenderer || formatter2Render(column) || slot2Render(column.slot),
        dataKey: column.prop || column.property,
        key: getColumnKey(column),
        width: column.width || defaultWidth.value
      }, column.attrs || {})
    })
  if (props.buttons?.length) {
    tmpColumns.push(Object.assign({
      title: $i18nBundle('common.label.operation'),
      cellRenderer: (data) => {
        return props.buttons
          .map((button) => {
            return <TableDynamicButton
                      buttonConfig={button}
                      buttonSize={props.buttonSize}
                      item={data.rowData}
                      scope={data2Scope(data)} />
          })
      }
    }, props.buttonsColumnAttrs || {}))
  }
  return tmpColumns
})

const calcStyle = computed(() => {
  return { height: props.height }
})

const table = ref()
const tableContainerRef = ref()

defineExpose({
  table
})

onMounted(() => {
  if (table.value && props.expandColumnKey) {
    table.value.toggleRowExpansion = (rowData) => {
      const rowKeyValue = get(rowData, props.rowKey)
      const $row = tableContainerRef.value?.$el?.querySelector(`div[rowkey="${rowKeyValue}"]`)
      const $expandIcon = $row?.querySelector('.el-table-v2__expand-icon')
      if ($expandIcon) {
        $expandIcon.click()
      }
      console.log('===================toggleRowExpansion', rowData, rowKeyValue, $row, $expandIcon)
    }
  }
})

</script>

<template>
  <el-container
    ref="tableContainerRef"
    v-loading="loading"
    class="common-table-v2"
    :element-loading-text="loadingText"
    :style="calcStyle"
  >
    <el-auto-resizer>
      <template #default="{ height: tableHeight, width }">
        <el-table-v2
          ref="table"
          v-model:expanded-row-keys="expandRowKeys"
          :row-height="rowHeight"
          :columns="calcColumns"
          :data="data"
          :width="width"
          :height="tableHeight"
          :row-key="rowKey"
          :expand-column-key="calcExpandColumnKey"
          v-bind="$attrs"
        />
      </template>
    </el-auto-resizer>
  </el-container>
</template>

<style scoped>

</style>

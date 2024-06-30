<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { debounce, isEmpty, isObject, cloneDeep, chunk } from 'lodash-es'
import { onClickOutside, onKeyStroke, useVModel } from '@vueuse/core'
import { UPDATE_MODEL_EVENT, CHANGE_EVENT, useFormItem } from 'element-plus'

/**
 * @type {CommonAutocompleteProps}
 */
const props = defineProps({
  modelValue: {
    type: [String, Object],
    default: null
  },
  useIdModel: {
    type: Boolean,
    default: true
  },
  title: {
    type: String,
    default: ''
  },
  autoPageShowTitle: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: ''
  },
  defaultLabel: {
    type: String,
    default: ''
  },
  idProp: {
    type: String,
    default: 'value'
  },
  labelProp: {
    type: String,
    default: 'label'
  },
  debounceTime: { // 防抖
    type: Number,
    default: 300
  },
  autocompleteWidth: {
    type: String,
    default: '500px'
  },
  inputWidth: {
    type: String,
    default: ''
  },
  autocompleteConfig: {
    type: Object,
    required: true
  },
  selectPageConfig: {
    type: Object,
    default: null
  },
  colSize: {
    type: Number,
    default: 4
  },
  rowSize: {
    type: Number,
    default: 6
  },
  tabStretch: {
    type: Boolean,
    default: true
  },
  clearable: {
    type: Boolean,
    default: true
  },
  inputAsValue: {
    type: Boolean,
    default: false
  },
  inputAttrs: {
    type: Object,
    default: null
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  emptySearchEnabled: {
    type: Boolean,
    default: false
  },
  loadingText: {
    type: String,
    default: ''
  },
  minHeight: {
    type: String,
    default: '100px'
  },
  validateEvent: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits([UPDATE_MODEL_EVENT, CHANGE_EVENT, 'update:defaultLabel'])
// 关键字搜索
const keywords = ref(props.defaultLabel)
// 上次搜索记录
const lastAutocompleteLabel = ref(props.defaultLabel)
// 分页条
/**
 * @type {PaginationProps}
 */
const pageAttrs = { layout: 'total, prev, pager, next', small: true, background: true }
/**
 * @type {PaginationProps}
 */
const selectPageAttrs = { layout: 'prev, pager, next', small: true, background: true }
// 自动完成数据
const dataList = ref([])
// 选项表数据
const selectPageData = ref({})
const selectPageTab = ref(null)
const popoverVisible = ref(false)
const autocompletePopover = ref()
const inputRef = ref()
const defaultAutoPage = {
  pageSize: props.autocompleteConfig?.pageSize || 8,
  pageNumber: 1
}
const autoPage = ref(props.autocompleteConfig?.frontendPaging
  ? null
  : cloneDeep(defaultAutoPage))
const loadingData = ref(false)

const idProp = computed(() => props.autocompleteConfig?.idProp || props.idProp)
const labelProp = computed(() => props.autocompleteConfig?.labelProp || props.labelProp)

const showSelectPage = computed(() => {
  return props.selectPageConfig && (!keywords.value || lastAutocompleteLabel.value === keywords.value)
})

const showPopover = () => {
  nextTick(() => {
    popoverVisible.value = true
  })
}

const loadAutoDataList = (val) => {
  if (val || props.emptySearchEnabled) {
    showPopover()
    loadingData.value = true
    props.autocompleteConfig.searchMethod({ query: val, page: autoPage.value }, (result) => {
      dataList.value = result.items || []
      if (result.page) {
        autoPage.value = { ...result.page }
      }
      loadingData.value = false
    })
  }
}

const loadSelectData = () => {
  const tabId = selectPageTab.value || props.selectPageConfig?.tabs?.[0]?.id
  if (tabId && !selectPageData.value[tabId]) {
    selectPageTab.value = tabId
    loadingData.value = true
    props.selectPageConfig?.searchMethod(tabId, (result) => {
      selectPageData.value[tabId] = result
      loadingData.value = false
    })
  }
}

/**
 * @type {function(boolean?)}
 */
const onInputKeywords = debounce((input) => {
  if (!props.disabled && !props.readonly) {
    const val = keywords.value
    if (showSelectPage.value) {
      loadSelectData()
      showPopover()
    } else {
      if (input && autoPage.value) {
        autoPage.value = { ...autoPage.value, pageNumber: 1 }
      }
      loadAutoDataList(val)
    }
    if (!val && input) {
      onSelectData()
    } else if (input && props.inputAsValue && props.useIdModel) {
      vModel.value = val
    }
  }
}, props.debounceTime)

const calcDefaultLabelFunc = () => {
  if (!props.useIdModel) {
    const value = props.modelValue
    return value && isObject(value) ? value[labelProp.value] : ''
  }
  return props.defaultLabel
}

const calcDefaultLabel = computed(calcDefaultLabelFunc)

onMounted(() => {
  onClickOutside(autocompletePopover.value?.popperRef?.contentRef, () => {
    popoverVisible.value = false
  })
  setAutocompleteLabel(calcDefaultLabel.value)
  // 向下按键移动元素
  onKeyStroke('ArrowDown', () => moveSelection(true), { target: inputRef.value })
  // 向上按键移动元素
  onKeyStroke('ArrowUp', () => moveSelection(false), { target: inputRef.value })
  // 选中回车
  onKeyStroke('Enter', (event) => {
    onSelectData(currentOnRow.value)
    event?.stopImmediatePropagation()
    event?.stopPropagation()
  }, { target: inputRef.value })
})

watch(() => popoverVisible.value, (val) => {
  if (!val) {
    nextTick(() => {
      if (lastAutocompleteLabel.value && keywords.value && keywords.value !== lastAutocompleteLabel.value) {
        keywords.value = lastAutocompleteLabel.value
      }
    })
  }
})

watch(() => props.modelValue, (value) => {
  if (!props.useIdModel) {
    setAutocompleteLabel(value && isObject(value) ? value[labelProp.value] : '')
    if (isEmpty(value)) {
      vModel.value = null
    }
  } else if (!value) {
    setAutocompleteLabel('')
    vModel.value = value
  }
})

watch(calcDefaultLabelFunc, (label) => {
  setAutocompleteLabel(label)
})

//* ********************数据选择*********************

const vModel = useVModel(props, 'modelValue', emit)
const vDefaultLabel = useVModel(props, 'defaultLabel', emit)

const setAutocompleteLabel = label => {
  keywords.value = label
  vDefaultLabel.value = label
  lastAutocompleteLabel.value = label
}

const { formItem } = useFormItem()

const onSelectData = (row) => {
  popoverVisible.value = false
  if (!vModel.value && !row) {
    return
  }
  let label = ''
  let value
  if (row) {
    label = row[labelProp.value]
    value = props.useIdModel ? row[idProp.value] : row
  }
  setAutocompleteLabel(label)
  vModel.value = value
  emit(CHANGE_EVENT, row)
  if (props.validateEvent) {
    formItem?.validate(CHANGE_EVENT)
  }
}

// =======================按键处理===================
const tableRef = ref()
const currentOnRow = ref()
const currentOnIndex = ref(-1)

const moveSelection = function (down) {
  if (dataList.value.length > 0) {
    if (down) {
      if (currentOnIndex.value < dataList.value.length - 1) {
        currentOnIndex.value++
      } else {
        currentOnIndex.value = 0
      }
    } else {
      if (currentOnIndex.value > 0) {
        currentOnIndex.value--
      } else {
        currentOnIndex.value = dataList.value.length - 1
      }
    }
    currentOnRow.value = dataList.value[currentOnIndex.value]
  } else {
    currentOnIndex.value = -1
    currentOnRow.value = null
  }
  tableRef.value?.table?.setCurrentRow(currentOnRow.value)
}

//= ===============selectPage处理=================//
const selectPagePageConfig = ref({})
const parsedSelectPageData = computed(() => {
  const result = {}
  if (selectPageData.value) {
    Object.entries(selectPageData.value).forEach(([key, value]) => {
      const chunkPages = chunk(value, props.colSize)
      const pager = selectPagePageConfig.value[key] = selectPagePageConfig.value[key] || getSelectPage(chunkPages.length)
      result[key] = chunkPages.slice((pager.pageNumber - 1) * pager.pageSize, pager.pageNumber * pager.pageSize)
    })
  }
  return result
})

const getSelectPage = (totalCount) => {
  const pageSize = props.rowSize
  const pageCount = Math.floor((totalCount + pageSize - 1) / pageSize)
  return {
    pageNumber: 1,
    pageSize,
    totalCount,
    pageCount
  }
}

const selectPagePagination = (tab) => {
  const pager = selectPagePageConfig.value?.[tab.id]
  return pager && pager.pageCount && pager.pageCount > 1
}

const selectPagePaginationChange = (tab, pageNumber) => {
  const pager = selectPagePageConfig.value?.[tab.id]
  pager.pageNumber = pageNumber
  selectPageData.value = { ...selectPageData.value }
}

const autoTitle = computed(() => {
  if (!showSelectPage.value && !props.autoPageShowTitle) {
    return undefined
  }
  return props.title || props.placeholder
})

watch(() => props.selectPageConfig, () => {
  selectPagePageConfig.value = {}
  selectPageData.value = {}
  selectPageTab.value = null
})

watch(() => props.autocompleteConfig, (autocompleteConfig) => {
  defaultAutoPage.pageSize = autocompleteConfig.pageSize || 8
  if (autocompleteConfig.frontendPaging) {
    autoPage.value = null
  } else {
    autoPage.value = cloneDeep(defaultAutoPage)
  }
})

</script>

<template>
  <el-popover
    ref="autocompletePopover"
    :visible="popoverVisible"
    popper-class="common-autocomplete"
    placement="bottom-start"
    :width="autocompleteWidth"
    :title="autoTitle"
  >
    <template #reference>
      <el-input
        ref="inputRef"
        v-model="keywords"
        :clearable="clearable"
        :placeholder="placeholder||title"
        :disabled="disabled"
        :readonly="readonly"
        :style="{width: inputWidth}"
        v-bind="inputAttrs"
        @input="onInputKeywords(true)"
        @click="onInputKeywords(false)"
      />
    </template>
    <template #default>
      <div v-if="showSelectPage">
        <el-tabs
          v-model="selectPageTab"
          class="common-select-page"
          type="border-card"
          :stretch="tabStretch"
          @tab-click="onInputKeywords(false)"
        >
          <el-tab-pane
            v-for="tab in selectPageConfig.tabs"
            :key="tab.id"
            :name="tab.id"
          >
            <template #label>
              <span>{{ tab.label || (tab.labelKey && $t(tab.labelKey)) || tab.id }}</span>
            </template>
            <template #default>
              <div
                v-loading="loadingData"
                :element-loading-text="loadingText"
                class="select-page-content"
                :style="{minHeight}"
              >
                <template
                  v-for="(rowData, index) in parsedSelectPageData[tab.id]"
                  :key="index"
                >
                  <el-row>
                    <el-col
                      v-for="(colData, idx) in rowData"
                      :key="idx"
                      :span="24/colSize"
                    >
                      <el-button
                        plain
                        class="common-select-page-btn is-text"
                        @click="onSelectData(colData)"
                      >
                        {{ colData[labelProp] }}
                      </el-button>
                    </el-col>
                  </el-row>
                </template>
                <el-pagination
                  v-if="selectPagePagination(tab)"
                  :style="{'justify-content':'center'}"
                  v-bind="selectPageAttrs"
                  :total="selectPagePageConfig[tab.id].totalCount"
                  :page-size="selectPagePageConfig[tab.id].pageSize"
                  :current-page="selectPagePageConfig[tab.id].pageNumber"
                  @current-change="selectPagePaginationChange(tab, $event)"
                />
              </div>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
      <!--自动完成内容-->
      <common-table
        v-else
        ref="tableRef"
        v-model:page="autoPage"
        :loading="loadingData"
        :loading-text="loadingText"
        class="autocomplete-table"
        :columns="autocompleteConfig.columns"
        :empty-text="autocompleteConfig.emptyMessage"
        :data="dataList"
        :page-attrs="pageAttrs"
        :frontend-paging="autocompleteConfig.frontendPaging"
        :frontend-page-size="defaultAutoPage.pageSize"
        @row-click="onSelectData($event)"
        @current-page-change="onInputKeywords(false)"
      >
        <template
          v-for="column in autocompleteConfig.columns"
          #[column.slot]="scope"
        >
          <slot
            v-if="column.slot"
            :item="scope.item"
            :column-conf="scope.columnConf"
            :name="column.slot"
          />
        </template>
      </common-table>
    </template>
  </el-popover>
</template>

<style scoped>

</style>

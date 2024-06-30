<script setup>
import CommonTableColumn from '@/components/common-table/common-table-column.vue'
import { computed, ref, onMounted, watch, onUnmounted } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'
import { getFrontendPage } from '@/components/utils'

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
  },
  /**
   * @type {CommonPage}
   */
  page: {
    type: Object,
    default: null
  },
  pageAlign: {
    type: String,
    default: 'center'
  },
  pageAttrs: {
    type: Object,
    default () {
      return {
        layout: 'total, sizes, prev, pager, next',
        pageSizes: [10, 20, 50],
        background: true
      }
    }
  },
  showPageSizes: {
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  loadingText: {
    type: String,
    default: ''
  },
  expandTable: {
    type: Boolean,
    default: false
  },
  hideExpandBtn: {
    type: Boolean,
    default: false
  },
  frontendPaging: {
    type: Boolean,
    default: false
  },
  frontendPageSize: {
    type: Number,
    default: 10
  },
  infinitePaging: {
    type: Boolean,
    default: false
  }
})
/**
 *
 * @type {ComputedRef<[CommonTableColumn]>}
 */
const calcColumns = computed(() => {
  let _columns = props.columns
  if (props.expandTable) {
    _columns = [{
      slot: 'expand',
      attrs: {
        type: 'expand',
        width: props.hideExpandBtn ? 1 : 0
      }
    }, ..._columns]
  }
  if (props.buttons.length || props.buttonsSlot) {
    const buttonColumn = {
      labelKey: 'common.label.operation',
      isOperation: true,
      slot: props.buttonsSlot,
      buttons: props.buttons,
      attrs: props.buttonsColumnAttrs
    }
    _columns = [..._columns, buttonColumn]
  }
  return _columns.filter(column => column.enabled !== false)
})

const emit = defineEmits(['pageSizeChange', 'currentPageChange', 'update:page'])

const pageSizeChange = (pageSize) => {
  emit('update:page', { ...props.page, pageSize })
  emit('pageSizeChange', pageSize)
}
const currentPageChange = (pageNumber) => {
  emit('update:page', { ...props.page, pageNumber })
  emit('currentPageChange', pageNumber)
}

const calcData = ref([])
const frontendPage = ref(getFrontendPage(0, props.frontendPageSize))
const infiniteRef = ref(null)
const isInfiniteEnd = ref(false)

function checkInfiniteEnd (pageVal) {
  if (props.infinitePaging) {
    isInfiniteEnd.value = pageVal ? pageVal.pageNumber >= (pageVal.pageCount || 0) : true
  }
}

function calcFrontEndPageData () {
  if (props.frontendPaging) {
    calcData.value = props.data?.slice((frontendPage.value.pageNumber - 1) * frontendPage.value.pageSize,
      frontendPage.value.pageNumber * frontendPage.value.pageSize) // 展示数据
    checkInfiniteEnd(frontendPage.value)
  }
}

function calcTableDataAndPage () {
  if (props.frontendPaging) { // 前端分页模式
    frontendPage.value = getFrontendPage(props.data?.length, frontendPage.value.pageSize, frontendPage.value.pageNumber) // 前端分页信息
    calcFrontEndPageData()
  } else { // 后端分页模式
    if (props.infinitePaging) { // 无限加载模式
      if (!calcData.value.length && props.page && props.page.pageNumber > 1) { // 如果进来就是后面的页码，重新查询
        emit('update:page', { ...props.page, pageNumber: 1 }) // 仅更新pageNumber
      } else {
        calcData.value = [...calcData.value, ...props.data]
      }
      checkInfiniteEnd(props.page)
    } else {
      calcData.value = props.data
    }
  }
}

watch(() => props.data, () => {
  calcTableDataAndPage()
}, { deep: true, immediate: true })

watch(() => frontendPage, () => {
  calcFrontEndPageData()
}, { deep: true, immediate: true })

onMounted(() => {
  if (props.infinitePaging) {
    const { stop } = useIntersectionObserver(infiniteRef, onInfiniteLoad, {
      threshold: 0.5
    })
    onUnmounted(stop)
  }
})

const onInfiniteLoad = (args) => {
  const isIntersecting = args[0].isIntersecting
  console.info('===========================infinite', isIntersecting, ...args)
  if (isIntersecting && calcData.value?.length) {
    if (props.frontendPaging) {
      frontendPage.value = getFrontendPage(props.data?.length,
        frontendPage.value.pageSize + props.frontendPageSize,
        frontendPage.value.pageNumber)
    } else if (props.page) {
      currentPageChange(props.page.pageNumber + 1)
    }
  }
}

const calcPageAttrs = computed(() => {
  let newPageAttrs = props.pageAttrs
  if (!props.showPageSizes && newPageAttrs.layout) {
    newPageAttrs = {
      ...newPageAttrs,
      layout: newPageAttrs.layout.split(/\s*,\s*/).filter(item => item !== 'sizes').join(',')
    }
  }
  return newPageAttrs
})

const calcBorder = computed(() => {
  if (props.infinitePaging) {
    return true // 这里有个bug，infinitePaging时border为false显示将会有问题
  }
  return props.border
})

const table = ref()

defineExpose({
  table
})

</script>

<template>
  <el-container
    v-loading="loading"
    class="flex-column"
    :element-loading-text="loadingText"
  >
    <el-table
      ref="table"
      v-bind="$attrs"
      :highlight-current-row="highlightCurrentRow"
      :stripe="stripe"
      :data="calcData"
      :class="{'common-hide-expand': hideExpandBtn}"
      :border="calcBorder"
    >
      <common-table-column
        v-for="(column, index) in calcColumns"
        :key="index"
        :column="column"
        :button-size="buttonSize"
      >
        <template
          v-if="column.headerSlot"
          #header="scope"
        >
          <slot
            v-bind="scope"
            :name="column.headerSlot"
          />
        </template>
        <!--用于自定义显示属性-->
        <template
          v-if="column.slot"
          #default="scope"
        >
          <slot
            v-bind="scope"
            :item="scope.row"
            :name="column.slot"
          />
        </template>
      </common-table-column>
      <template #append="scope">
        <slot
          name="append"
          v-bind="scope"
        />
        <el-container
          v-show="infinitePaging&&!isInfiniteEnd"
          ref="infiniteRef"
          v-loading="true"
          class="container-center"
        >
          Loading
        </el-container>
      </template>
      <template #empty="scope">
        <slot
          name="empty"
          v-bind="scope"
        />
      </template>
    </el-table>
    <el-pagination
      v-if="!infinitePaging&&!frontendPaging&&page&&page.pageCount"
      class="common-pagination"
      v-bind="calcPageAttrs"
      :total="page.totalCount"
      :page-size="page.pageSize"
      :current-page="page.pageNumber"
      @size-change="pageSizeChange($event)"
      @current-change="currentPageChange($event)"
    />
    <el-pagination
      v-if="!infinitePaging&&frontendPaging&&frontendPage&&frontendPage.pageCount"
      class="common-pagination"
      v-bind="calcPageAttrs"
      :total="frontendPage.totalCount"
      :page-size="frontendPage.pageSize"
      :current-page="frontendPage.pageNumber"
      @size-change="frontendPage.pageSize=$event"
      @current-change="frontendPage.pageNumber=$event"
    />
  </el-container>
</template>

<style scoped>
.common-pagination {
  margin-top: 15px;
  justify-content: v-bind(pageAlign);
}
</style>

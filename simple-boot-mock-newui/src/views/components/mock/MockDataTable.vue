<script setup lang="jsx">
import { onMounted, ref, computed, watch } from 'vue'
import { defineTableColumns, defineFormOptions, defineTableButtons, limitStr } from '@/components/utils'
import { $coreConfirm, checkShowColumn, getSingleSelectOptions, getStyleGrow } from '@/utils'
import MockDataApi, {
  ALL_STATUS_CODES,
  DEFAULT_CONTENT_TYPE,
  markDefault,
  copyMockData,
  searchHistories, loadHistoryDiff, recoverFromHistory
} from '@/api/mock/MockDataApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import CommonIcon from '@/components/common-icon/index.vue'
import CommonFormControl from '@/components/common-form-control/index.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useFormDelay, useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import {
  showMockTips,
  showHistoryListWindow,
  toTestMatchPattern,
  showCodeWindow
} from '@/utils/DynamicUtils'
import { $i18nBundle, $i18nKey, $i18nMsg } from '@/messages'
import { ElMessage, ElTag } from 'element-plus'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import MockDataResponseEdit from '@/views/components/mock/MockDataResponseEdit.vue'
import ViewDataLink from '@/views/components/utils/ViewDataLink.vue'
import MockRequestPreview from '@/views/components/mock/MockRequestPreview.vue'
import { calcContentType, DEFAULT_HEADERS } from '@/consts/MockConstants'
import { useContentTypeOption } from '@/services/mock/MockCommonService'
import { useDefaultPage } from '@/config'
import { getDataHistoryViewOptions, showCompareWindowNew } from '@/services/mock/NewMockDiffService'
import { getStatusCode } from '@/services/mock/MockDiffService'

const props = defineProps({
  groupItem: {
    type: Object,
    required: true
  },
  editable: {
    type: Boolean,
    default: false
  }
})
const requestItem = defineModel('requestItem', {
  type: Object,
  required: true
})
const selectedRows = ref([])
const batchMode = ref(false)
const columns = computed(() => {
  const hasMatchPattern = checkShowColumn(tableData.value, 'matchPattern')
  return defineTableColumns([{
    width: '50px',
    attrs: {
      type: 'selection'
    },
    enabled: batchMode.value
  }, {
    labelKey: 'mock.label.statusCode',
    property: 'statusCode',
    minWidth: '120px',
    formatter: getStatusCode,
    headerFormatter () {
      const statusCodeOption = {
        prop: 'statusCode',
        type: 'select',
        children: getSingleSelectOptions('2XX', '3XX', '4XX', '5XX'),
        attrs: {
          filterable: true
        },
        change: loadMockData,
        placeholder: $i18nBundle('mock.label.statusCode')
      }
      return <CommonFormControl class="no-form-label" option={statusCodeOption} model={searchParam.value}/>
    },
    attrs: {
      align: 'center'
    }
  }, {
    labelKey: 'mock.label.responseName',
    property: 'dataName',
    enabled: checkShowColumn(tableData.value, 'dataName')
  }, {
    label: 'Content Type',
    property: 'contentType',
    minWidth: '150px'
  }, {
    labelKey: 'common.label.delay',
    property: 'delay',
    enabled: checkShowColumn(tableData.value, 'delay')
  }, {
    labelKey: 'mock.label.matchPattern',
    minWidth: hasMatchPattern ? '150px' : '80px',
    formatter (data) {
      const showStr = limitStr(data.matchPattern, 60)
      return <ViewDataLink data={showStr} icon="RuleFilled" style="word-break: break-all;"
                             tooltip={$i18nKey('common.label.commonConfig', 'mock.label.matchPattern')}
                             onViewDataDetails={() => toTestMatchPattern(props.groupItem, requestItem.value, data, props.editable)
                               .then(() => loadMockData())}/>
    }
  }, {
    minWidth: '100px',
    formatter (data) {
      return <DelFlagTag v-model={data.status} clickToToggle={props.editable}
                         onToggleValue={(status) => saveMockData({ ...data, status })}/>
    },
    headerFormatter () {
      const statusOption = useSearchStatus({
        change: loadMockData,
        placeholder: $i18nBundle('common.label.status'),
        labelKey: ''
      })
      return <CommonFormControl class="no-form-label" option={statusOption} model={searchParam.value}/>
    }
  }, {
    labelKey: 'mock.label.mockResponseBody',
    property: 'responseBody',
    minWidth: '220px',
    formatter (data) {
      const showStr = limitStr(data.responseBody, 60)
      const status = data?.statusCode || 200
      const isRedirect = status >= 300 && status < 400 // redirect
      return <ViewDataLink data={showStr} style="word-break: break-all;"
                           tooltip={$i18nKey('common.label.commonConfig', isRedirect ? 'mock.label.redirectUrl' : 'mock.label.responseBody')}
                           onViewDataDetails={() => toEditDataResponse(data)}/>
    }
  }, {
    headerSlot: 'buttonHeader',
    slot: 'buttons',
    minWidth: '200px',
    attrs: {
      fixed: 'right'
    }
  }])
})
const { searchParam, tableData, loading, searchMethod: searchMockData } = useTableAndSearchForm({
  defaultParam: { requestId: requestItem.value.id, page: useDefaultPage(5) },
  searchMethod: MockDataApi.search,
  saveParam: false
})

const loadMockData = (...args) => {
  const lastId = selectDataItem.value?.id
  return searchMockData(...args)
    .then((result) => {
      requestItem.value.dataCount = tableData.value?.length || 0
      if (!tableData.value?.length) {
        mockPreviewRef.value?.clearParamsAndResponse()
      } else {
        const selectData = tableData.value.find(data => data.id === lastId) || tableData.value[0]
        dataTableRef.value?.table?.setCurrentRow(selectData, true)
        const historyMap = result.infos?.historyMap || {}
        tableData.value.forEach(data => {
          data.historyCount = historyMap[data.id] || 0
        })
      }
    })
}

onMounted(() => {
  loadMockData()
})
watch(requestItem, () => {
  searchParam.value.requestId = requestItem.value?.id
  loadMockData()
})
const buttons = computed(() => defineTableButtons([{
  labelKey: 'common.label.edit',
  type: 'primary',
  icon: 'Edit',
  click: item => {
    newOrEdit(item.id)
  }
}, {
  labelKey: 'common.label.copy',
  type: 'warning',
  icon: 'FileCopyFilled',
  enabled: props.editable,
  click: item => {
    $coreConfirm($i18nBundle('common.msg.confirmCopy'))
      .then(() => copyMockData(item.id))
      .then(() => loadMockData())
  }
}, {
  labelKey: 'mock.label.modifyHistory',
  type: 'info',
  icon: 'AccessTimeFilled',
  buttonIf (item) {
    return !!item.historyCount
  },
  click: item => {
    toShowHistoryWindow(item)
  }
}, {
  labelKey: 'mock.label.setDefault',
  type: 'primary',
  icon: 'Flag',
  buttonIf (item) {
    return !item.defaultFlag && !item.matchPattern
  },
  click: item => {
    $coreConfirm($i18nBundle('mock.msg.configSetDefault'))
      .then(() => {
        item.defaultFlag = 1
        return markDefault(item)
      })
      .then(() => loadMockData())
  }
}, {
  labelKey: 'common.label.delete',
  type: 'danger',
  icon: 'DeleteFilled',
  enabled: props.editable,
  click: item => {
    $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
      .then(() => MockDataApi.deleteById(item.id, { loading: true }))
      .then(() => loadMockData())
  }
}]))
const deleteDataList = () => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockDataApi.removeByIds(selectedRows.value.map(item => item.id), { loading: true }))
    .then(() => loadMockData())
}
const showEditWindow = ref(false)
const currentDataItem = ref()
const selectDataItem = ref()
const newDataItem = () => ({
  requestId: requestItem.value.id,
  groupId: requestItem.value.groupId,
  status: 1,
  statusCode: ALL_STATUS_CODES[0].code,
  contentType: DEFAULT_CONTENT_TYPE,
  headerParams: [],
  defaultCharset: 'UTF-8'
})
const newOrEdit = async id => {
  if (id) {
    await MockDataApi.getById(id).then(data => {
      data.resultData && (currentDataItem.value = data.resultData)
      contentRef.value = currentDataItem.value?.responseBody
      languageRef.value = currentDataItem.value?.responseFormat || languageRef.value
      currentDataItem.value.headerParams = JSON.parse(currentDataItem.value.headers || '[]')
    })
  } else {
    currentDataItem.value = newDataItem()
  }
  showEditWindow.value = true
}
const { contentRef, languageRef, monacoEditorOptions, languageSelectOption } = useMonacoEditorOptions({ readOnly: false })
const { contentRef: patternContentRef, languageRef: patternLanguageRef, monacoEditorOptions: patternMonacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })

const editFormOptions = computed(() => {
  const status = currentDataItem.value?.statusCode || 200
  const isRedirect = status >= 300 && status < 400 // redirect
  const statusOptions = ALL_STATUS_CODES.map(status => {
    const label = $i18nMsg(`${status.labelCn} - ${(status.labelEn)}`, `${status.labelEn} - ${(status.labelCn)}`)
    return {
      value: status.code,
      label: `${status.code} - ${label}`
    }
  })
  return defineFormOptions([{
    labelKey: 'mock.label.statusCode',
    prop: 'statusCode',
    type: 'select',
    children: statusOptions,
    attrs: {
      filterable: true,
      allowCreate: true,
      clearable: false
    },
    rules: [{
      validator (_, value) {
        return /^\d{3}$/g.test(value)
      },
      message: $i18nBundle('common.msg.patternInvalid')
    }]
  }, {
    labelKey: 'mock.label.responseName',
    prop: 'dataName'
  }, { ...useFormStatus(), style: getStyleGrow(3) }, {
    labelKey: 'mock.label.default',
    prop: 'defaultFlag',
    style: getStyleGrow(3),
    type: 'switch',
    attrs: {
      activeValue: 1,
      inactiveValue: 0,
      activeText: $i18nBundle('common.label.yes'),
      inactiveText: $i18nBundle('common.label.no')
    }
  }, { ...useFormDelay(), style: getStyleGrow(3) }, {
    labelKey: 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltips: [{
      tooltip: $i18nBundle('common.label.newWindowEdit'),
      tooltipIcon: 'EditPen',
      tooltipFunc: () => showCodeWindow(currentDataItem.value?.matchPattern, {
        language: 'javascript',
        title: $i18nKey('common.label.commonEdit', 'mock.label.matchPattern'),
        readOnly: false,
        change: (value, lang) => {
          currentDataItem.value.matchPattern = value
          patternContentRef.value = lang
        }
      })
    }, {
      tooltip: $i18nBundle('mock.label.clickToShowDetails'),
      tooltipFunc: () => showMockTips('matchPattern')
    }],
    attrs: {
      class: 'common-resize-vertical',
      defaultValue: currentDataItem.value?.matchPattern,
      value: currentDataItem.value?.matchPattern,
      'onUpdate:value': (value) => {
        currentDataItem.value.matchPattern = value
        patternContentRef.value = value
        patternLanguageRef.value = 'javascript'
      },
      language: patternLanguageRef.value || 'javascript',
      height: '100px',
      options: patternMonacoEditorOptions
    }
  }, {
    labelKey: 'mock.label.responseHeaders',
    slot: 'headerParams'
  }, {
    ...useContentTypeOption(),
    slot: 'newContentType',
    enabled: !isRedirect
  }, {
    ...languageSelectOption.value,
    change (val) {
      if (currentDataItem.value) {
        currentDataItem.value.responseFormat = val
        currentDataItem.value.contentType = calcContentType(val, currentDataItem.value?.responseBody, currentDataItem.value.contentType)
      }
    }
  }, {
    labelKey: isRedirect ? 'mock.label.redirectUrl' : 'mock.label.mockResponseBody',
    type: 'vue-monaco-editor',
    prop: 'responseBody',
    required: isRedirect,
    tooltips: [{
      tooltip: $i18nBundle('common.label.newWindowEdit'),
      tooltipIcon: 'EditPen',
      tooltipFunc: () => showCodeWindow(currentDataItem.value?.responseBody, {
        language: currentDataItem.value?.responseFormat,
        forceLanguage: false,
        autoCheckLang: false,
        title: $i18nKey('common.label.commonEdit', isRedirect ? 'mock.label.redirectUrl' : 'mock.label.mockResponseBody'),
        readOnly: false,
        change: (value, lang) => {
          currentDataItem.value.responseBody = value
          console.log('===============================value,lang', value, lang)
          currentDataItem.value.responseFormat = lang
          languageRef.value = lang
        }
      })
    }, {
      tooltip: $i18nBundle('mock.label.clickToShowDetails'),
      tooltipFunc: () => showMockTips()
    }],
    attrs: {
      class: 'common-resize-vertical',
      value: currentDataItem.value?.responseBody,
      'onUpdate:value': (value) => {
        currentDataItem.value.responseBody = value
        contentRef.value = value
      },
      language: currentDataItem.value?.responseFormat || (isRedirect ? 'text' : languageRef.value),
      height: '200px',
      options: monacoEditorOptions
    }
  }, {
    labelKey: 'common.label.description',
    prop: 'description',
    attrs: {
      type: 'textarea'
    }
  }])
})

const saveMockData = (data) => {
  const dataItem = { ...data }
  if (dataItem.headerParams) {
    dataItem.headers = JSON.stringify(dataItem.headerParams)
    delete dataItem.headerParams
  }
  return MockDataApi.saveOrUpdate(dataItem)
    .then((data) => {
      if (data.success && data.resultData && selectDataItem.value?.id !== data.resultData.id) {
        onSelectDataItem(data.resultData)
      }
      loadMockData()
      return data
    })
}

const dataResponseEditRef = ref()

const toEditDataResponse = (mockData) => {
  dataResponseEditRef.value?.toEditDataResponse(mockData)
}

const saveDataResponse = (mockData) => {
  return saveMockData(mockData).then(() => ElMessage.success($i18nBundle('common.msg.saveSuccess')))
}

const changeBatchMode = () => {
  batchMode.value = !batchMode.value
  if (!batchMode.value) {
    selectedRows.value = []
  }
}

const dataTableRef = ref()
const mockPreviewRef = ref()

const onSelectDataItem = (dataItem) => {
  selectDataItem.value = dataItem
  if (dataItem) {
    mockPreviewRef.value?.toPreviewRequest(props.groupItem, requestItem.value, selectDataItem.value, () => loadMockData())
  }
}

const toShowHistoryWindow = (current) => {
  showHistoryListWindow({
    columns: defineTableColumns([{
      labelKey: 'mock.label.statusCode',
      property: 'statusCode',
      minWidth: '100px',
      formatter: getStatusCode,
      attrs: {
        align: 'center'
      }
    }, {
      labelKey: 'mock.label.responseName',
      property: 'dataName',
      minWidth: '100px'
    }, {
      labelKey: 'common.label.status',
      minWidth: '100px',
      formatter (data) {
        return <DelFlagTag v-model={data.status} clickToToggle={false}/>
      }
    }, {
      labelKey: 'mock.label.responseBody',
      property: 'responseBody',
      minWidth: '200px',
      formatter (data) {
        return limitStr(data.responseBody, 60)
      }
    }, {
      labelKey: 'common.label.modifyDate',
      property: 'modifyDate',
      dateFormat: 'YYYY-MM-DD HH:mm:ss',
      minWidth: '160px'
    }, {
      labelKey: 'common.label.modifier',
      formatter: item => item.modifier || item.creator
    }, {
      labelKey: 'mock.label.version',
      minWidth: '120px',
      formatter (item) {
        return <>
          {item.version}
          {item.current ? <ElTag type="success" class="margin-left1">{$i18nBundle('mock.label.current')}</ElTag> : ''}
        </>
      }
    }, {
      labelKey: 'mock.label.matchPattern',
      property: 'matchPattern',
      minWidth: '150px',
      formatter (data) {
        return limitStr(data.matchPattern, 60)
      }
    }, {
      label: 'Content Type',
      property: 'contentType',
      minWidth: '150px'
    }]),
    searchFunc: (param, config) => searchHistories(current.id, param, config),
    compareFunc: async (modified, target, previous) => {
      let original = modified
      if (previous) {
        await loadHistoryDiff({
          id: modified.id,
          version: modified.version
        }).then(data => {
          modified = data.resultData?.modified || {}
          original = data.resultData?.original || {}
          modified.current = !modified.modifyFrom
        })
      } else {
        modified = target
      }
      showCompareWindowNew({
        modified,
        original,
        historyOptionsMethod: getDataHistoryViewOptions
      })
    },
    recoverFunc: props.editable ? recoverFromHistory : null
  })
}
const pageAttrs = {
  layout: 'prev, pager, next',
  background: true,
  hideOnSinglePage: true
}
</script>

<template>
  <el-container class="flex-column padding-left2 padding-right2">
    <common-table
      ref="dataTableRef"
      :key="batchMode"
      v-model:page="searchParam.page"
      :page-attrs="pageAttrs"
      :data="tableData"
      :columns="columns"
      :loading="loading"
      class="request-table"
      @selection-change="selectedRows=$event"
      @current-change="onSelectDataItem"
      @current-page-change="loadMockData()"
      @page-size-change="loadMockData()"
    >
      <template #buttonHeader>
        {{ $t('common.label.operation') }}
        <el-tag
          v-if="searchParam.page?.totalCount"
          :title="$t('mock.label.mockDataCount')"
          class="margin-left1 pointer"
          type="success"
          size="small"
          effect="plain"
          round
        >
          <span v-if="selectedRows.length">{{ selectedRows.length }}/</span><span>{{ searchParam.page?.totalCount }}</span>
        </el-tag>
        <template v-if="editable">
          <el-button
            v-common-tooltip="$t('common.label.batchMode')"
            class="margin-left2"
            round
            :type="batchMode?'success':'default'"
            size="small"
            @click="changeBatchMode"
          >
            <common-icon :icon="batchMode?'LibraryAddCheckFilled':'LibraryAddCheckOutlined'" />
          </el-button>
          <el-button
            v-if="!batchMode"
            v-common-tooltip="$i18nKey('common.label.commonAdd', 'mock.label.mockData')"
            type="primary"
            size="small"
            round
            @click="newOrEdit()"
          >
            <common-icon icon="Plus" />
          </el-button>
          <el-button
            v-if="selectedRows.length"
            v-common-tooltip="$t('common.label.delete')"
            type="danger"
            size="small"
            round
            @click="deleteDataList()"
          >
            <common-icon icon="DeleteFilled" />
          </el-button>
        </template>
      </template>
      <template #buttons="{item}">
        <template v-for="(button, index) in buttons">
          <el-button
            v-if="button.enabled!==false&&(!button.buttonIf||button.buttonIf(item))"
            :key="index"
            v-common-tooltip="button.label || $t(button.labelKey)"
            :type="button.type"
            :size="button.size||'small'"
            :disabled="button.disabled"
            :round="button.round"
            :circle="button.circle??true"
            @click="button.click?.(item)"
          >
            <common-icon :icon="button.icon" />
          </el-button>
        </template>
      </template>
    </common-table>
    <simple-edit-window
      v-model="currentDataItem"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      :name="$t('mock.label.mockData')"
      label-width="140px"
      :save-current-item="saveMockData"
      inline-auto-mode
      :editable="editable"
    >
      <template #headerParams="{option}">
        <common-form-control
          :model="currentDataItem"
          :option="option"
        >
          <common-params-edit
            v-model="currentDataItem.headerParams"
            form-prop="headerParams"
            :name-suggestions="DEFAULT_HEADERS"
          />
        </common-form-control>
      </template>
      <template #newContentType="{option}">
        <el-row v-if="option.enabled!==false">
          <el-col :span="12">
            <common-form-control
              :model="currentDataItem"
              :option="option"
            />
          </el-col>
          <el-col :span="12">
            <common-form-control
              :model="currentDataItem"
              :option="option.charsetOption"
            />
          </el-col>
        </el-row>
      </template>
    </simple-edit-window>
    <mock-data-response-edit
      ref="dataResponseEditRef"
      :editable="editable"
      @save-data-response="saveDataResponse"
    />
    <mock-request-preview
      ref="mockPreviewRef"
      :mock-response-editable="editable"
      class="margin-top2"
      affix-enabled
    />
  </el-container>
</template>

<style scoped>

</style>

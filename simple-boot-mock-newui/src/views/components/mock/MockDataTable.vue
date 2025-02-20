<script setup lang="jsx">
import { onMounted, ref, computed, watch } from 'vue'
import { defineTableColumns, defineFormOptions, defineTableButtons } from '@/components/utils'
import { $coreConfirm, checkShowColumn } from '@/utils'
import MockDataApi, { ALL_STATUS_CODES, ALL_CONTENT_TYPES, markDefault, copyMockData } from '@/api/mock/MockDataApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import CommonIcon from '@/components/common-icon/index.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useFormDelay, useFormStatus } from '@/consts/GlobalConstants'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { toTestMatchPattern } from '@/utils/DynamicUtils'
import { $i18nBundle, $i18nKey, $i18nMsg } from '@/messages'
import { ElMessage, ElTag, ElText } from 'element-plus'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import MockDataResponseEdit from '@/views/components/mock/MockDataResponseEdit.vue'
import ViewDataLink from '@/views/components/utils/ViewDataLink.vue'
import MockRequestPreview from '@/views/components/mock/MockRequestPreview.vue'
import { calcContentType, DEFAULT_HEADERS } from '@/consts/MockConstants'
import { useContentTypeOption } from '@/services/mock/MockCommonService'

const props = defineProps({
  groupItem: {
    type: Object,
    required: true
  },
  requestItem: {
    type: Object,
    required: true
  }
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
    minWidth: '80px',
    formatter (data) {
      let type = 'danger'
      if (data.statusCode < 300) {
        type = 'success'
      } else if (data.statusCode < 400) {
        type = 'primary'
      } else if (data.statusCode < 500) {
        type = 'warning'
      }
      const status = ALL_STATUS_CODES.find(status => data.statusCode === status.code)
      const statusLabel = status ? $i18nMsg(`${status.labelCn} - ${(status.labelEn)}`, `${status.labelEn} - ${(status.labelCn)}`) : ''
      return <ElText type="success">
          {data.defaultFlag
            ? <CommonIcon type="success"
                          v-common-tooltip={$i18nBundle('mock.label.default')}
                          icon="Flag"/>
            : ''}
          <ElTag v-common-tooltip={statusLabel} type={type} class="margin-left1">{data.statusCode}</ElTag>
        </ElText>
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
      let showStr = data.matchPattern
      if (data.matchPattern && data.matchPattern.length > 100) {
        showStr = data.matchPattern.substring(0, 100) + '...'
      }
      return <ViewDataLink data={showStr} icon="RuleFilled" style="word-break: break-all;"
                             tooltip={$i18nKey('common.label.commonConfig', 'mock.label.matchPattern')}
                             onViewDataDetails={() => toTestMatchPattern(props.groupItem, props.requestItem, data)
                               .then(() => loadMockData())}/>
    }
  }, {
    labelKey: 'common.label.status',
    minWidth: '80px',
    formatter (data) {
      return <DelFlagTag v-model={data.status} clickToToggle={true}
                         onToggleValue={(status) => saveMockData({ ...data, status })}/>
    },
    attrs: {
      align: 'center',
      filterMultiple: false,
      filters: [{
        value: 1,
        text: $i18nBundle('common.label.statusEnabled')
      }, {
        value: 0,
        text: $i18nBundle('common.label.statusDisabled')
      }],
      filterMethod: (value, row) => value === row.status
    }
  }, {
    labelKey: 'mock.label.responseBody',
    property: 'responseBody',
    minWidth: '220px',
    formatter (data) {
      let showStr = data.responseBody
      if (data.responseBody && data.responseBody.length > 100) {
        showStr = data.responseBody.substring(0, 100) + '...'
      }
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
  defaultParam: { requestId: props.requestItem.id },
  searchMethod: MockDataApi.search,
  saveParam: false
})

const loadMockData = (...args) => {
  const lastId = selectDataItem.value?.id
  searchMockData(...args)
    .then(() => {
      if (!tableData.value?.length) {
        mockPreviewRef.value?.clearParamsAndResponse()
      } else {
        const selectData = tableData.value.find(data => data.id === lastId) || tableData.value[0]
        dataTableRef.value?.table?.setCurrentRow(selectData, true)
      }
    })
}

onMounted(() => {
  loadMockData()
})
watch(() => props.requestItem?.id, requestId => {
  searchParam.value.requestId = requestId
  loadMockData()
})
const buttons = defineTableButtons([{
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
  click: item => {
    $coreConfirm($i18nBundle('common.msg.confirmCopy'))
      .then(() => copyMockData(item.id))
      .then(() => loadMockData())
  }
}, {
  labelKey: 'mock.label.setDefault',
  type: 'primary',
  icon: 'Flag',
  buttonIf (item) {
    return !item.defaultFlag && !item.matchPattern
  },
  click: item => {
    item.defaultFlag = 1
    markDefault(item).then(() => loadMockData())
  }
}, {
  labelKey: 'common.label.delete',
  type: 'danger',
  icon: 'DeleteFilled',
  click: item => {
    $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
      .then(() => MockDataApi.deleteById(item.id))
      .then(() => loadMockData())
  }
}])
const deleteDataList = () => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockDataApi.removeByIds(selectedRows.value.map(item => item.id)), { loading: true })
    .then(() => loadMockData())
}
const showEditWindow = ref(false)
const currentDataItem = ref()
const selectDataItem = ref()
const newDataItem = () => ({
  requestId: props.requestItem.id,
  groupId: props.requestItem.groupId,
  status: 1,
  statusCode: ALL_STATUS_CODES[0].code,
  contentType: ALL_CONTENT_TYPES[0],
  headerParams: []
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
  }, {
    labelKey: 'mock.label.default',
    prop: 'defaultFlag',
    type: 'switch',
    attrs: {
      activeValue: 1,
      inactiveValue: 0,
      activeText: $i18nBundle('common.label.yes'),
      inactiveText: $i18nBundle('common.label.no')
    }
  }, useFormStatus(), useFormDelay(), {
    labelKey: 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltip: $i18nBundle('mock.msg.matchPatternTooltip'),
    attrs: {
      class: 'common-resize-vertical',
      defaultValue: currentDataItem.value?.matchPattern,
      'onUpdate:value': (value) => {
        currentDataItem.value.matchPattern = value
        patternContentRef.value = value
        patternLanguageRef.value = 'javascript'
      },
      language: patternLanguageRef.value,
      height: '100px',
      options: patternMonacoEditorOptions
    }
  }, {
    labelKey: 'mock.label.responseHeaders',
    slot: 'headerParams'
  }, {
    ...useContentTypeOption(),
    enabled: !isRedirect
  }, {
    ...languageSelectOption.value,
    change (val) {
      if (currentDataItem.value) {
        currentDataItem.value.responseFormat = val
        currentDataItem.value.contentType = calcContentType(val, currentDataItem.value?.responseBody)
      }
    }
  }, {
    labelKey: isRedirect ? 'mock.label.redirectUrl' : 'mock.label.responseBody',
    type: 'vue-monaco-editor',
    prop: 'responseBody',
    required: isRedirect,
    attrs: {
      defaultValue: currentDataItem.value?.responseBody,
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
        selectDataItem.value = data.resultData
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
    mockPreviewRef.value?.toPreviewRequest(props.groupItem, props.requestItem, selectDataItem.value, (newItem) => {
      Object.assign(dataItem, newItem)
    })
  }
}

</script>

<template>
  <el-container class="flex-column padding-left2 padding-right2">
    <common-table
      ref="dataTableRef"
      :key="batchMode"
      :data="tableData"
      :columns="columns"
      :loading="loading"
      class="request-table margin-bottom3"
      @selection-change="selectedRows=$event"
      @current-change="onSelectDataItem"
    >
      <template #buttonHeader>
        {{ $t('common.label.operation') }}
        <el-button
          v-common-tooltip="$t('common.label.batchMode')"
          class="margin-left1"
          round
          :type="batchMode?'success':'default'"
          size="small"
          @click="changeBatchMode"
        >
          <common-icon :icon="batchMode?'LibraryAddCheckFilled':'LibraryAddCheckOutlined'" />
        </el-button>
        <el-button
          v-common-tooltip="$t('common.label.new')"
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
      show-fullscreen
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
    </simple-edit-window>
    <mock-data-response-edit
      ref="dataResponseEditRef"
      @save-data-response="saveDataResponse"
    />
    <mock-request-preview ref="mockPreviewRef" />
  </el-container>
</template>

<style scoped>

</style>

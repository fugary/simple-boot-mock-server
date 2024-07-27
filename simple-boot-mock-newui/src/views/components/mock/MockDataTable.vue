<script setup lang="jsx">
import { onMounted, ref, computed, watch } from 'vue'
import { defineTableColumns, defineFormOptions, defineTableButtons } from '@/components/utils'
import { $coreConfirm, checkShowColumn, getSingleSelectOptions } from '@/utils'
import MockDataApi, { ALL_STATUS_CODES, ALL_CONTENT_TYPES, markDefault } from '@/api/mock/MockDataApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import CommonIcon from '@/components/common-icon/index.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useFormDelay, useFormStatus } from '@/consts/GlobalConstants'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { previewMockRequest, toTestMatchPattern } from '@/utils/DynamicUtils'
import { $i18nBundle } from '@/messages'
import { ElLink, ElMessage, ElTag } from 'element-plus'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'
import MockDataResponseEdit from '@/views/components/mock/MockDataResponseEdit.vue'
import ViewDataLink from '@/views/components/utils/ViewDataLink.vue'

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
const columns = computed(() => {
  return defineTableColumns([{
    width: '50px',
    attrs: {
      type: 'selection'
    }
  }, {
    labelKey: 'mock.label.default',
    width: '80px',
    formatter (data) {
      return data.defaultFlag ? <CommonIcon color="#2d8cf0" icon="Flag"/> : ''
    }
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
      return <ElTag type={type}>{data.statusCode}</ElTag>
    }
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
    enabled: checkShowColumn(tableData.value, 'matchPattern'),
    minWidth: '150px',
    formatter (data) {
      if (data.matchPattern) {
        return <ViewDataLink data={data.matchPattern} tooltip={$i18nBundle('mock.msg.matchPatternTest')}
                             onViewDataDetails={() => toTestMatchPattern(props.groupItem, props.requestItem, data)
                               .then(() => loadMockData())}/>
      }
    }
  }, {
    labelKey: 'common.label.status',
    minWidth: '80px',
    formatter (data) {
      return <DelFlagTag v-model={data.status} clickToToggle={true}
                         onToggleValue={(status) => saveMockData({ ...data, status })}/>
    }
  }, {
    labelKey: 'mock.label.responseBody',
    property: 'responseBody',
    minWidth: '220px',
    formatter (data) {
      let showStr = data.responseBody
      if (data.responseBody && data.responseBody.length > 120) {
        showStr = data.responseBody.substring(0, 120) + '...'
      }
      return <>
        <ElLink type="primary" onClick={() => toEditDataResponse(data)}>
          {showStr}
        </ElLink>
      </>
    }
  }, {
    headerSlot: 'buttonHeader',
    slot: 'buttons',
    minWidth: '220px'
  }])
})
const { searchParam, tableData, loading, searchMethod: loadMockData } = useTableAndSearchForm({
  defaultParam: { requestId: props.requestItem.id },
  searchMethod: MockDataApi.search,
  saveParam: false
})
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
  labelKey: 'common.label.test',
  type: 'success',
  icon: 'RemoveRedEyeFilled',
  click: item => {
    previewMockRequest(props.groupItem, props.requestItem, item, (newItem) => {
      Object.assign(item, newItem)
    })
  }
}, {
  labelKey: 'common.label.copy',
  type: 'warning',
  icon: 'FileCopyFilled',
  click: item => {
    $coreConfirm($i18nBundle('common.msg.confirmCopy'))
      .then(() => {
        saveMockData({ ...item, id: undefined })
      })
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
const newDataItem = () => ({
  requestId: props.requestItem.id,
  groupId: props.requestItem.groupId,
  status: 1,
  statusCode: ALL_STATUS_CODES[0],
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
  return defineFormOptions([{
    labelKey: 'mock.label.statusCode',
    prop: 'statusCode',
    type: 'select',
    children: getSingleSelectOptions(...ALL_STATUS_CODES),
    attrs: {
      clearable: false
    }
  }, {
    label: 'Content Type',
    prop: 'contentType',
    type: 'select',
    children: getSingleSelectOptions(...ALL_CONTENT_TYPES),
    attrs: {
      clearable: false
    }
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
      value: currentDataItem.value?.matchPattern,
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
    ...languageSelectOption.value,
    change (val) {
      if (currentDataItem.value) {
        currentDataItem.value.responseFormat = val
      }
    }
  }, {
    labelKey: 'mock.label.responseBody',
    type: 'vue-monaco-editor',
    prop: 'responseBody',
    attrs: {
      value: currentDataItem.value?.responseBody,
      'onUpdate:value': (value) => {
        currentDataItem.value.responseBody = value
        contentRef.value = value
      },
      language: languageRef.value,
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
    .then(() => loadMockData())
}

const dataResponseEditRef = ref()

const toEditDataResponse = (mockData) => {
  dataResponseEditRef.value?.toEditDataResponse(mockData)
}

const saveDataResponse = (mockData) => {
  return saveMockData(mockData).then(() => ElMessage.success($i18nBundle('common.msg.saveSuccess')))
}

</script>

<template>
  <el-container class="flex-column padding-10">
    <common-table
      :data="tableData"
      :columns="columns"
      :loading="loading"
      @selection-change="selectedRows=$event"
    >
      <template #buttonHeader>
        {{ $t('common.label.operation') }}
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
            header-flag
          />
        </common-form-control>
      </template>
    </simple-edit-window>
    <mock-data-response-edit
      ref="dataResponseEditRef"
      @save-data-response="saveDataResponse"
    />
  </el-container>
</template>

<style scoped>

</style>

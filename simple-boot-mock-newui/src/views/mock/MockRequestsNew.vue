<script setup lang="jsx">
import { useRoute } from 'vue-router'
import { $coreConfirm, getStyleGrow, useBackUrl } from '@/utils'
import { useMockGroupItem } from '@/hooks/mock/MockGroupHooks'
import MockRequestApi, {
  ALL_METHODS,
  loadHistoryDiff,
  recoverFromHistory,
  searchHistories
} from '@/api/mock/MockRequestApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableColumns, limitStr } from '@/components/utils'
import { ref, computed } from 'vue'
import { useFormDelay, useFormDisableMock, useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useDefaultPage } from '@/config'
import { $i18nBundle, $i18nConcat, $i18nKey } from '@/messages'
import MockDataTable from '@/views/components/mock/MockDataTable.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import {
  previewMockRequest,
  showCodeWindow,
  showHistoryListWindow,
  showMockTips,
  toEditGroupEnvParams,
  toTestMatchPattern
} from '@/utils/DynamicUtils'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import MockRequestMenuItem from '@/views/components/mock/MockRequestMenuItem.vue'
import CommonIcon from '@/components/common-icon/index.vue'
import { checkProjectEdit } from '@/api/mock/MockProjectApi'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { ElTag, ElText } from 'element-plus'
import {
  getLoadBalancerLabel,
  getRequestHistoryViewOptions,
  showCompareWindowNew
} from '@/services/mock/NewMockDiffService'
import { MOCK_LOAD_BALANCE_OPTIONS } from '@/consts/MockConstants'
import { calcUrl, pasteCurl2Request } from '@/services/mock/CurlProcessService'
import { useContentTypeOption } from '@/services/mock/MockCommonService'
import { set } from 'lodash-es'

const route = useRoute()
const groupId = route.params.groupId

const { goBack } = useBackUrl('/mock/groups')
const { groupItem, loadGroup, mockProject, groupUrl, loadSuccess } = useMockGroupItem(groupId)

const { tableData, loading, searchParam, searchMethod: searchMockRequests } = useTableAndSearchForm({
  defaultParam: { groupId, page: useDefaultPage(8) },
  searchMethod: (param) => MockRequestApi.search(param, { loading: true })
})

const pageAttrs = {
  layout: 'prev, pager, next',
  background: true,
  hideOnSinglePage: true,
  pagerCount: 5
}

const loadMockRequests = (...args) => {
  return searchMockRequests(...args).then((result) => {
    if (tableData.value?.length) {
      onSelectRequest(tableData.value.find(req => req.id === searchParam.value.selectRequestId) || tableData.value[0])
      const countMap = result.infos?.countMap || {}
      const historyMap = result.infos?.historyMap || {}
      tableData.value.forEach(request => {
        request.dataCount = countMap[request.id] || 0
        request.historyCount = historyMap[request.id] || 0
      })
    }
    return result
  })
}

loadMockRequests()

const methodOptions = ALL_METHODS.map(method => {
  return {
    value: method.method,
    label: method.method
  }
})

//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [
    {
      labelKey: 'common.label.keywords',
      prop: 'keyword'
    }, {
      labelKey: 'mock.label.method',
      prop: 'method',
      type: 'select',
      children: methodOptions,
      change () {
        loadMockRequests()
      }
    },
    useSearchStatus({ change () { loadMockRequests() } }), {
      labelKey: 'mock.label.hasMockData',
      prop: 'hasData',
      type: 'select',
      children: [{
        value: true,
        label: $i18nBundle('common.label.yes')
      }, {
        value: false,
        label: $i18nBundle('common.label.no')
      }],
      change () {
        loadMockRequests()
      }
    }
  ]
})
const resetSearchForm = () => {
  searchFormOptions.value.map(option => option.prop || option.property)
    .forEach((prop) => set(searchParam.value, prop, undefined))
  loadMockRequests()
}

const selectedRows = ref([])
const requestTableRef = ref()

const deleteRequests = () => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockRequestApi.removeByIds(selectedRows.value.map(item => item.id)), { loading: true })
    .then(() => loadMockRequests())
}

const newRequestItem = () => ({
  status: 1,
  method: 'GET',
  groupId
})
const showEditWindow = ref(false)
const currentRequest = ref(newRequestItem())
const selectRequest = ref()
const newOrEdit = async id => {
  if (id) {
    await MockRequestApi.getById(id).then(data => {
      data.resultData && (currentRequest.value = data.resultData)
    })
  } else {
    currentRequest.value = newRequestItem()
  }
  showEditWindow.value = true
}
const onSelectRequest = request => {
  selectRequest.value = request
  searchParam.value.selectRequestId = request?.id
  requestTableRef.value?.table?.setCurrentRow(selectRequest.value, true)
}
const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({
  readOnly: false,
  lineNumbers: 'off',
  minimap: { enabled: false }
})
const editFormOptions = computed(() => {
  return defineFormOptions([{
    labelKey: 'mock.label.requestPath',
    style: getStyleGrow(3),
    prop: 'method',
    required: true,
    type: 'select',
    children: methodOptions,
    attrs: {
      clearable: false
    }
  }, {
    labelKey: 'mock.label.requestPath',
    prop: 'requestPath',
    labelWidth: '10px',
    showLabel: false,
    style: getStyleGrow(7),
    required: true,
    trim: true,
    placeholder: `${$i18nKey('common.msg.commonInput', 'mock.label.requestPath')}, ${$i18nConcat($i18nBundle('common.label.paste'), 'CURL')}`,
    change (val) {
      if (pasteCurl2Request(currentRequest.value, val)) { // curl格式处理
        return
      }
      const urlObj = calcUrl(val)
      val = urlObj?.pathname
      if (urlObj?.origin) {
        currentRequest.value.proxyUrl = urlObj.origin
      }
      currentRequest.value.requestPath = val && !val.startsWith('/') ? `/${val.trim()}` : val
    }
  }, {
    labelKey: 'mock.label.proxyUrl',
    prop: 'proxyUrl',
    tooltip: $i18nBundle('mock.msg.proxyUrlTooltip'),
    rules: [{
      message: $i18nBundle('mock.msg.proxyUrlMsg'),
      validator: () => {
        return !currentRequest.value?.proxyUrl || /^https?:\/\/.+/.test(currentRequest.value?.proxyUrl)
      }
    }]
  }, { ...useFormStatus(), style: getStyleGrow(4) },
  { ...useFormDisableMock(), style: getStyleGrow(6) },
  { ...useFormDelay(), style: getStyleGrow(4) },
  { ...useContentTypeOption({ clearable: true }), style: getStyleGrow(6) },
  {
    labelKey: 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltips: [{
      tooltip: $i18nBundle('common.label.newWindowEdit'),
      tooltipIcon: 'EditPen',
      tooltipFunc: () => showCodeWindow(currentRequest.value?.matchPattern, {
        language: 'javascript',
        title: $i18nKey('common.label.commonEdit', 'mock.label.matchPattern'),
        readOnly: false,
        change: (value, lang) => {
          currentRequest.value.matchPattern = value
          languageRef.value = lang
        }
      })
    }, {
      tooltip: $i18nBundle('mock.label.clickToShowDetails'),
      tooltipFunc: () => showMockTips('matchPattern')
    }],
    attrs: {
      class: 'common-resize-vertical',
      defaultValue: currentRequest.value?.matchPattern,
      value: currentRequest.value?.matchPattern,
      'onUpdate:value': (value) => {
        currentRequest.value.matchPattern = value
        contentRef.value = value
        languageRef.value = 'javascript'
      },
      language: languageRef.value || 'javascript',
      height: '100px',
      options: monacoEditorOptions
    }
  }, {
    labelKey: 'mock.label.loadBalanceType',
    prop: 'loadBalancer',
    type: 'select',
    children: MOCK_LOAD_BALANCE_OPTIONS,
    tooltip: $i18nBundle('mock.msg.loadBalanceTips')
  }, {
    labelKey: 'mock.label.requestName',
    prop: 'requestName',
    tooltip: $i18nBundle('mock.msg.requestNameTooltip')
  }, {
    labelKey: 'common.label.description',
    prop: 'description',
    attrs: {
      type: 'textarea'
    }
  }])
})

const saveMockRequest = item => {
  return MockRequestApi.saveOrUpdate(item)
    .then((data) => {
      if (data.success && data.resultData && searchParam.value.selectRequestId !== data.resultData.id) {
        onSelectRequest(data.resultData)
      }
      loadMockRequests()
      return data
    })
}

const batchMode = ref(false)

const newColumns = computed(() => {
  return [{
    width: '50px',
    enabled: batchMode.value,
    attrs: {
      type: 'selection'
    }
  }, {
    headerSlot: 'buttonHeader',
    property: 'requestPath',
    formatter (data) {
      return <MockRequestMenuItem v-model={data}
                                  editable={projectEditable.value}
                                  onToTestMockRequest={() => previewMockRequest(groupItem.value, data, null, () => loadMockRequests(), projectEditable.value)}
                                  onToTestMatchPattern={() => { toTestMatchPattern(groupItem.value, data, null, projectEditable.value) }}
                                  onToEditMockRequest={() => { newOrEdit(data.id) }}
                                  onToShowRequestHistory={() => toShowHistoryWindow(data)}
                                  onToEditDelay={() => { newOrEdit(data.id) }}
                                  onRequestChanged={() => loadMockRequests()}
                                  onSaveMockRequest={(item) => saveMockRequest(item)}
                                  onDblclick={() => newOrEdit(data.id)}
                                  group-item={groupItem.value} />
    },
    minWidth: '150px'
  }]
})

const changeBatchMode = () => {
  batchMode.value = !batchMode.value
  if (!batchMode.value) {
    selectedRows.value = []
  }
}

const editGroupEnvParams = () => {
  toEditGroupEnvParams(groupItem.value?.id)
    .then(() => loadGroup())
    .then(() => loadMockRequests())
}

const projectEditable = computed(() => checkProjectEdit(mockProject.value))
const methodsConfig = Object.fromEntries(ALL_METHODS.map(method => [method.method, method]))

const methodFormatter = item => {
  return methodsConfig[item.method]
    ? <ElTag type={methodsConfig[item.method]?.type}
               size="small"
               effect="dark">{item.method}</ElTag>
    : ''
}

const toShowHistoryWindow = (current) => {
  showHistoryListWindow({
    columns: defineTableColumns([{
      labelKey: 'mock.label.requestPath',
      property: 'requestPath',
      minWidth: '100px'
    }, {
      labelKey: 'mock.label.method',
      formatter: methodFormatter,
      minWidth: '100px'
    }, {
      labelKey: 'common.label.status',
      minWidth: '100px',
      formatter (data) {
        let disableMockStr = ''
        if (data.disableMock) {
          disableMockStr = <ElText type="danger"
                                   style="vertical-align: middle;"
                                   class="margin-left1 pointer"
                                   v-common-tooltip={$i18nBundle('mock.label.disabledMock')}>
            <CommonIcon size={18} icon="DoDisturbFilled"/>
          </ElText>
        }
        return <>
          <DelFlagTag v-model={data.status} clickToToggle={false}/>
          {disableMockStr}
        </>
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
      minWidth: '200px',
      formatter (data) {
        return limitStr(data.matchPattern, 100)
      }
    }, {
      labelKey: 'mock.label.loadBalanceType',
      formatter: getLoadBalancerLabel
    }, {
      labelKey: 'mock.label.proxyUrl',
      property: 'proxyUrl',
      minWidth: '200px'
    }, {
      labelKey: 'mock.label.requestName',
      property: 'requestName',
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
        historyOptionsMethod: getRequestHistoryViewOptions
      })
    },
    recoverFunc: projectEditable.value ? recoverFromHistory : null,
    onUpdateHistory: () => loadMockRequests()
  })
}

</script>

<template>
  <el-container class="flex-column">
    <el-page-header @back="goBack">
      <template #content>
        <span class="text-large font-600 mr-3">
          {{ groupItem?.groupName }} 【{{ groupUrl }}】
        </span>
        <mock-url-copy-link :url-path="groupUrl" />
      </template>
    </el-page-header>
    <common-form
      class="margin-top3"
      inline
      :model="searchParam"
      :options="searchFormOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="loadMockRequests()"
    >
      <template #buttons>
        <el-button
          type="default"
          @click="resetSearchForm"
        >
          {{ $t('common.label.reset') }}
        </el-button>
        <el-button
          v-if="projectEditable"
          v-common-tooltip="$i18nKey('common.label.commonAdd', 'mock.label.mockRequest')"
          type="info"
          @click="newOrEdit()"
        >
          {{ $t('common.label.new') }}
        </el-button>
        <el-button
          v-if="projectEditable"
          type="success"
          @click="editGroupEnvParams"
        >
          {{ $t('mock.label.mockEnv') }}
        </el-button>
        <el-button
          @click="goBack()"
        >
          {{ $t('common.label.back') }}
        </el-button>
      </template>
    </common-form>
    <el-container v-if="loadSuccess">
      <div class="form-edit-width-100">
        <common-split
          :min-size="150"
          :max-size="[500, Infinity]"
        >
          <template #split-0>
            <div class="padding-right2">
              <common-table
                :key="batchMode"
                ref="requestTableRef"
                v-model:page="searchParam.page"
                class="request-table"
                :data="tableData"
                :columns="newColumns"
                :loading="loading"
                row-key="id"
                :page-attrs="pageAttrs"
                @page-size-change="loadMockRequests()"
                @current-page-change="loadMockRequests()"
                @selection-change="selectedRows=$event"
                @current-change="onSelectRequest"
              >
                <template #buttonHeader>
                  {{ $t('mock.label.mockRequests') }}
                  <el-tag
                    v-if="searchParam.page?.totalCount"
                    :title="$t('mock.label.mockRequestCount')"
                    class="margin-left1 pointer"
                    type="primary"
                    size="small"
                    effect="plain"
                    round
                  >
                    <span v-if="selectedRows.length">{{ selectedRows.length }}/</span><span>{{ searchParam.page?.totalCount }}</span>
                  </el-tag>
                  <div
                    v-if="projectEditable"
                    class="float-right"
                  >
                    <el-button
                      v-common-tooltip="$t('common.label.batchMode')"
                      round
                      :type="batchMode?'success':'default'"
                      size="small"
                      @click="changeBatchMode"
                    >
                      <common-icon :icon="batchMode?'LibraryAddCheckFilled':'LibraryAddCheckOutlined'" />
                    </el-button>
                    <el-button
                      v-if="!batchMode"
                      v-common-tooltip="$i18nKey('common.label.commonAdd', 'mock.label.mockRequest')"
                      round
                      type="primary"
                      size="small"
                      @click="newOrEdit()"
                    >
                      <common-icon icon="Plus" />
                    </el-button>
                    <el-button
                      v-if="selectedRows.length"
                      v-common-tooltip="$t('common.label.delete')"
                      round
                      type="danger"
                      size="small"
                      @click="deleteRequests()"
                    >
                      <common-icon icon="DeleteFilled" />
                    </el-button>
                  </div>
                </template>
              </common-table>
            </div>
          </template>
          <template #split-1>
            <mock-data-table
              v-if="selectRequest"
              v-model:request-item="selectRequest"
              :editable="projectEditable"
              class="form-edit-width-100"
              :group-item="groupItem"
            />
          </template>
        </common-split>
      </div>
    </el-container>
    <simple-edit-window
      v-model="currentRequest"
      v-model:show-edit-window="showEditWindow"
      width="900px"
      :form-options="editFormOptions"
      :name="$t('mock.label.mockRequests')"
      :save-current-item="saveMockRequest"
      label-width="140px"
      inline-auto-mode
      :editable="projectEditable"
    />
  </el-container>
</template>

<style scoped>

</style>

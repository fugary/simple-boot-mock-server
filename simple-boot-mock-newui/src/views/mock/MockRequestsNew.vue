<script setup lang="jsx">
import { useRoute } from 'vue-router'
import { $coreConfirm, $reload, useBackUrl } from '@/utils'
import { useMockGroupItem } from '@/hooks/mock/MockGroupHooks'
import MockRequestApi, { ALL_METHODS, loadHistoryDiff, searchHistories } from '@/api/mock/MockRequestApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableColumns, limitStr } from '@/components/utils'
import { ref, computed, nextTick } from 'vue'
import { useFormDelay, useFormDisableMock, useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useDefaultPage } from '@/config'
import { $i18nBundle } from '@/messages'
import MockDataTable from '@/views/components/mock/MockDataTable.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import {
  previewMockRequest,
  showCompareWindow,
  showHistoryListWindow,
  showMockTips,
  toEditGroupEnvParams,
  toTestMatchPattern
} from '@/utils/DynamicUtils'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import MockRequestMenuItem from '@/views/components/mock/MockRequestMenuItem.vue'
import CommonIcon from '@/components/common-icon/index.vue'
import { checkProjectEdit } from '@/api/mock/MockProjectApi'
import { getMockCompareItem } from '@/services/mock/MockDiffService'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { ElTag, ElText } from 'element-plus'

const route = useRoute()
const groupId = route.params.groupId

const { goBack } = useBackUrl('/mock/groups')
const { groupItem, mockProject, groupUrl, loadSuccess } = useMockGroupItem(groupId)

const { tableData, loading, searchParam, searchMethod: searchMockRequests } = useTableAndSearchForm({
  defaultParam: { groupId, page: useDefaultPage(10) },
  searchMethod: (param) => MockRequestApi.search(param, { loading: true }),
  saveParam: false
})

const pageAttrs = {
  layout: 'prev, pager, next',
  background: true,
  hideOnSinglePage: true,
  pagerCount: 5
}

const loadMockRequests = (...args) => {
  return searchMockRequests(...args).then((result) => {
    nextTick(() => {
      if (tableData.value?.length) {
        selectRequest.value = tableData.value.find(req => req.id === selectRequest.value?.id) || tableData.value[0]
        requestTableRef.value?.table?.setCurrentRow(selectRequest.value, true)
        const countMap = result.infos?.countMap || {}
        const historyMap = result.infos?.historyMap || {}
        tableData.value.forEach(request => {
          request.dataCount = countMap[request.id] || 0
          request.historyCount = historyMap[request.id] || 0
        })
      }
    })
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
const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({
  readOnly: false,
  lineNumbers: 'off',
  minimap: { enabled: false }
})
const editFormOptions = computed(() => {
  return defineFormOptions([{
    labelKey: 'mock.label.requestPath',
    prop: 'requestPath',
    required: true,
    change (val) {
      if (val && !val.startsWith('/')) {
        currentRequest.value.requestPath = `/${val.trim()}`
      }
    }
  }, {
    labelKey: 'mock.label.method',
    prop: 'method',
    required: true,
    type: 'select',
    children: methodOptions,
    attrs: {
      clearable: false
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
  }, useFormStatus(), useFormDisableMock(), useFormDelay(),
  {
    labelKey: 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltip: $i18nBundle('mock.label.clickToShowDetails'),
    tooltipFunc: () => showMockTips(),
    attrs: {
      class: 'common-resize-vertical',
      defaultValue: currentRequest.value?.matchPattern,
      'onUpdate:value': (value) => {
        currentRequest.value.matchPattern = value
        contentRef.value = value
        languageRef.value = 'javascript'
      },
      language: languageRef.value,
      height: '100px',
      options: monacoEditorOptions
    }
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
      if (data.success && data.resultData && selectRequest.value?.id !== data.resultData.id) {
        selectRequest.value = data.resultData
        requestTableRef.value?.table?.setCurrentRow(selectRequest.value, true)
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
                                  onToTestMockRequest={() => previewMockRequest(groupItem.value, data)}
                                  onToTestMatchPattern={() => { toTestMatchPattern(groupItem.value, data) }}
                                  onToEditMockRequest={() => { newOrEdit(data.id) }}
                                  onToShowRequestHistory={() => toShowHistoryWindow(data)}
                                  onToEditDelay={() => { newOrEdit(data.id) }}
                                  onRequestChanged={() => loadMockRequests()}
                                  onSaveMockRequest={(item) => saveMockRequest(item)}
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
    .then(() => $reload(route))
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

const calcMockCompareItems = (original, modified) => {
  if (original && modified) {
    const newTag = !original.id ? <ElTag class="margin-left1" type="warning">{ $i18nBundle('common.label.new') }</ElTag> : ''
    const currentFlag = modified.current ? <ElTag class="margin-left1" type="success" round={true}>{$i18nBundle('mock.label.current')}</ElTag> : ''
    const getDelFlagFormatter = (item) => () => <DelFlagTag v-model={item.status}/>
    const getDisableMockFormatter = (item) => () => item.disableMock
      ? <ElText type="danger"
                  style="vertical-align: middle;"
                  class="margin-left1 pointer"
                  v-common-tooltip={$i18nBundle('mock.label.disabledMock')}>
          <CommonIcon size={18} icon="DoDisturbFilled"/>
        </ElText>
      : ''
    const getMethodFormatter = (item) => () => methodFormatter(item)
    const getContentFormatter = (item, modFlag) => () => <ElText type={modFlag && original.matchPattern !== modified.matchPattern ? 'warning' : ''}>
      <CommonIcon icon="ArrowDownBold"/>
      <MockUrlCopyLink showLink={!!item.matchPattern} class="margin-left1" content={item.matchPattern} />
    </ElText>
    return [
      ...getMockCompareItem({ original, modified, labelKey: 'mock.label.version', key: 'version', modifiedAppend: <>{newTag}{currentFlag}</> }),
      ...getMockCompareItem({ original, modified, labelKey: 'common.label.modifyDate', key: 'modifyDate', date: true }),
      ...getMockCompareItem({ original, modified, labelKey: 'common.label.modifier', key: 'modifier' }),
      ...getMockCompareItem({ original, modified, labelKey: 'mock.label.requestPath', key: 'requestPath', copy: true }),
      ...getMockCompareItem({
        original,
        modified,
        labelKey: 'mock.label.method',
        key: 'method',
        originalFormatter: getMethodFormatter(original),
        modifiedFormatter: getMethodFormatter(modified)
      }),
      ...getMockCompareItem({ original, modified, labelKey: 'mock.label.requestName', key: 'requestName', copy: true }),
      ...getMockCompareItem({
        original,
        modified,
        labelKey: 'common.label.status',
        key: 'status',
        originalFormatter: getDelFlagFormatter(original),
        modifiedFormatter: getDelFlagFormatter(modified)
      }),
      ...getMockCompareItem({
        original,
        modified,
        labelKey: 'mock.label.disableMock',
        key: 'disableMock',
        originalFormatter: getDisableMockFormatter(original),
        modifiedFormatter: getDisableMockFormatter(modified)
      }),
      ...getMockCompareItem({ original, modified, labelKey: 'mock.label.proxyUrl', key: 'proxyUrl', copy: true }),
      ...getMockCompareItem({ original, modified, labelKey: 'common.label.delay', key: 'delay' }),
      ...getMockCompareItem({ original, modified, labelKey: 'common.label.description', key: 'description', copy: true }),
      ...getMockCompareItem({ original, modified, labelKey: 'mock.label.queryParams', key: 'mockParams', limit: 50, copy: true }),
      ...getMockCompareItem({
        original,
        modified,
        labelKey: 'mock.label.matchPattern',
        key: 'matchPattern',
        originalFormatter: getContentFormatter(original),
        modifiedFormatter: getContentFormatter(modified, true),
        enabled: true
      })]
  }
  return []
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
      showCompareWindow({
        modified,
        original,
        contentKey: 'matchPattern',
        compareItems: calcMockCompareItems(original, modified),
        configOption: {
          language: 'javascript'
        }
      })
    }
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
                @current-change="selectRequest=$event"
              >
                <template #buttonHeader>
                  {{ $t('mock.label.mockRequests') }}
                  <el-tag
                    v-if="searchParam.page?.totalCount"
                    title="filtered requests count"
                    class="margin-left1"
                    type="primary"
                    size="small"
                    effect="plain"
                    round
                  >
                    {{ searchParam.page?.totalCount }}
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
                      v-common-tooltip="$t('common.label.new')"
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
      :form-options="editFormOptions"
      :name="$t('mock.label.mockRequests')"
      :save-current-item="saveMockRequest"
      label-width="130px"
    />
  </el-container>
</template>

<style scoped>

</style>

<script setup lang="jsx">
import { useRoute } from 'vue-router'
import { $coreConfirm, $reload, useBackUrl } from '@/utils'
import { useMockGroupItem } from '@/hooks/mock/MockGroupHooks'
import MockRequestApi, { ALL_METHODS } from '@/api/mock/MockRequestApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions } from '@/components/utils'
import { ref, computed, nextTick } from 'vue'
import { useFormDelay, useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useDefaultPage } from '@/config'
import { $i18nBundle } from '@/messages'
import MockDataTable from '@/views/components/mock/MockDataTable.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { previewMockRequest, toEditGroupEnvParams, toTestMatchPattern } from '@/utils/DynamicUtils'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import MockRequestMenuItem from '@/views/components/mock/MockRequestMenuItem.vue'
import CommonIcon from '@/components/common-icon/index.vue'

const route = useRoute()
const groupId = route.params.groupId

const { goBack } = useBackUrl('/mock/groups')
const { groupItem, groupUrl, loadSuccess } = useMockGroupItem(groupId)

const { tableData, loading, searchParam, searchMethod: searchMockRequests } = useTableAndSearchForm({
  defaultParam: { groupId, page: useDefaultPage(50) },
  searchMethod: (param) => MockRequestApi.search(param, { loading: true }),
  saveParam: false
})

const loadMockRequests = (...args) => {
  return searchMockRequests(...args).then((result) => {
    nextTick(() => {
      if (tableData.value?.length) {
        selectRequest.value = tableData.value.find(req => req.id === selectRequest.value?.id) || tableData.value[0]
        requestTableRef.value?.table?.setCurrentRow(selectRequest.value, true)
        const countMap = result.infos?.countMap || {}
        tableData.value.forEach(request => {
          request.dataCount = countMap[request.id] || 0
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
    useSearchStatus({ change () { loadMockRequests() } })
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
  }, useFormStatus(), useFormDelay(),
  {
    labelKey: 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltip: $i18nBundle('mock.msg.matchPatternTooltip'),
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
                                  onToTestMockRequest={() => previewMockRequest(groupItem.value, data)}
                                  onToTestMatchPattern={() => { toTestMatchPattern(groupItem.value, data) }}
                                  onToEditMockRequest={() => { newOrEdit(data.id) }}
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
                class="request-table"
                :data="tableData"
                :columns="newColumns"
                :loading="loading"
                row-key="id"
                @selection-change="selectedRows=$event"
                @current-change="selectRequest=$event"
              >
                <template #buttonHeader>
                  {{ $t('mock.label.mockRequests') }}
                  <div class="float-right">
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

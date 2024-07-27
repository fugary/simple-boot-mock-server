<script setup lang="jsx">
import { useRoute } from 'vue-router'
import { $coreConfirm, useBackUrl } from '@/utils'
import { useMockGroupItem } from '@/hooks/mock/MockGroupHooks'
import MockRequestApi, { ALL_METHODS } from '@/api/mock/MockRequestApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions } from '@/components/utils'
import { ref, computed, nextTick } from 'vue'
import { useFormDelay, useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useDefaultPage } from '@/config'
import { $i18nBundle } from '@/messages'
import MockDataTable from '@/views/components/mock/MockDataTable.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { previewMockRequest, toTestMatchPattern } from '@/utils/DynamicUtils'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import MockRequestMenuItem from '@/views/components/mock/MockRequestMenuItem.vue'
import CommonIcon from '@/components/common-icon/index.vue'

const route = useRoute()
const groupId = route.params.groupId

const { goBack } = useBackUrl('/mock/groups')
const { groupItem, groupUrl, loadSuccess } = useMockGroupItem(groupId)

const { tableData, loading, searchParam, searchMethod: loadMockRequests } = useTableAndSearchForm({
  defaultParam: { groupId, page: useDefaultPage() },
  searchMethod: (param) => MockRequestApi.search(param, { loading: true }),
  saveParam: false
})

loadMockRequests().then(() => {
  if (tableData.value?.length) {
    nextTick(() => {
      requestTableRef.value?.table?.setCurrentRow(tableData.value[0], true)
      selectRequest.value = tableData.value[0]
    })
  }
})

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
        loadMockRequests(1)
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
  }, useFormStatus(), useFormDelay(),
  {
    labelKey: 'mock.label.matchPattern',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltip: $i18nBundle('mock.msg.matchPatternTooltip'),
    attrs: {
      value: currentRequest.value?.matchPattern,
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
    .then(() => loadMockRequests())
}

const newColumns = computed(() => {
  return [{
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
          v-if="selectedRows?.length"
          type="danger"
          @click="deleteRequests()"
        >
          {{ $t('common.label.delete') }}
        </el-button>
        <el-button
          @click="goBack()"
        >
          {{ $t('common.label.back') }}
        </el-button>
      </template>
    </common-form>
    <el-container v-if="loadSuccess">
      <el-row class="form-edit-width-100">
        <el-col
          :span="6"
          style="min-width: 250px;"
        >
          <common-table
            ref="requestTableRef"
            class="request-table"
            :data="tableData"
            :columns="newColumns"
            :loading="loading"
            row-key="id"
            @current-change="selectRequest=$event"
          >
            <template #buttonHeader>
              {{ $t('mock.label.mockRequests') }}
              <div class="float-right">
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
        </el-col>
        <el-col
          :span="18"
        >
          <mock-data-table
            v-if="selectRequest"
            :group-item="groupItem"
            :request-item="selectRequest"
          />
        </el-col>
      </el-row>
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
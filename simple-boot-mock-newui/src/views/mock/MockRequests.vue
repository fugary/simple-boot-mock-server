<script setup lang="jsx">
import { useRoute } from 'vue-router'
import { ElTag, ElText } from 'element-plus'
import { $coreConfirm, checkShowColumn, useBackUrl } from '@/utils'
import { useMockGroupItem } from '@/hooks/mock/MockGroupHooks'
import MockRequestApi, { ALL_METHODS, copyMockRequest } from '@/api/mock/MockRequestApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons, defineTableColumns } from '@/components/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { ref, computed } from 'vue'
import { useFormDelay, useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useDefaultPage } from '@/config'
import { $i18nBundle } from '@/messages'
import MockDataTable from '@/views/components/mock/MockDataTable.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { previewMockRequest, toTestMatchPattern } from '@/utils/DynamicUtils'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import ViewDataLink from '@/views/components/utils/ViewDataLink.vue'

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
  if (!expandRequestRows.value?.length && tableData.value?.length) {
    requestTableRef.value?.table?.toggleRowExpansion(tableData.value[0], true)
  }
})

const methodOptions = ALL_METHODS.map(method => {
  return {
    value: method.method,
    label: method.method
  }
})

const methodsConfig = Object.fromEntries(ALL_METHODS.map(method => [method.method, method]))

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
const columns = computed(() => {
  return defineTableColumns([{
    width: '50px',
    attrs: {
      type: 'selection'
    }
  }, {
    labelKey: 'mock.label.requestPath',
    property: 'requestPath',
    formatter (data) {
      const path = `/mock/${groupItem.value?.groupPath}${data.requestPath}`
      return <>
        {data.requestPath}&nbsp;
        <MockUrlCopyLink urlPath={path}/>
      </>
    },
    minWidth: '150px'
  }, {
    labelKey: 'mock.label.method',
    property: 'method',
    minWidth: '60px',
    formatter (data) {
      const config = methodsConfig[data.method]
      return <ElTag type={config.type} effect="dark">{data.method}</ElTag>
    }
  }, {
    labelKey: 'common.label.delay',
    property: 'delay',
    minWidth: '60px',
    enabled: checkShowColumn(tableData.value, 'delay')
  }, {
    labelKey: 'mock.label.matchPattern',
    enabled: checkShowColumn(tableData.value, 'matchPattern'),
    minWidth: '120px',
    formatter (data) {
      if (data.matchPattern) {
        return <ViewDataLink data={data.matchPattern} tooltip={$i18nBundle('mock.msg.testMatchPattern')}
                             onViewDataDetails={() => toTestMatchPattern(groupItem, data)
                               .then(() => loadMockRequests())}/>
      }
    }
  }, {
    labelKey: 'mock.label.requestName',
    property: 'requestName',
    enabled: checkShowColumn(tableData.value, 'requestName')
  }, {
    labelKey: 'common.label.description',
    property: 'description',
    enabled: checkShowColumn(tableData.value, 'description'),
    formatter (data) {
      return <ElText line-clamp="2">
        {data.description}
      </ElText>
    },
    minWidth: '150px'
  }, {
    labelKey: 'common.label.status',
    formatter (data) {
      return <DelFlagTag v-model={data.status} clickToToggle={true}
                         onToggleValue={(status) => saveMockRequest({ ...data, status })}/>
    },
    minWidth: '70px'
  }])
})
const requestTableRef = ref()
const expandRequestRows = ref([])
const requestButtons = computed(() => {
  return defineTableButtons([{
    labelKey: 'common.label.edit',
    type: 'primary',
    click: item => {
      newOrEdit(item.id)
    }
  }, {
    labelKey: 'common.label.expand',
    type: 'primary',
    click: item => {
      requestTableRef.value?.table?.toggleRowExpansion(item)
      console.log('==================================', expandRequestRows.value)
    },
    dynamicButton (item) {
      const expanded = expandRequestRows.value.map(req => req.id).includes(item.id)
      return {
        labelKey: expanded ? 'common.label.collapse' : 'common.label.expand',
        type: expanded ? 'info' : 'primary'
      }
    }
  }, {
    labelKey: 'common.label.test',
    type: 'success',
    click: item => {
      previewMockRequest(groupItem.value, item)
    }
  }, {
    labelKey: 'common.label.copy',
    type: 'warning',
    click: item => {
      $coreConfirm($i18nBundle('common.msg.confirmCopy'))
        .then(() => copyMockRequest(item.id))
        .then(() => loadMockRequests())
    }
  }, {
    labelKey: 'common.label.delete',
    type: 'danger',
    click: item => {
      $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [`${item.requestPath}#${item.method}`]))
        .then(() => MockRequestApi.deleteById(item.id))
        .then(() => loadMockRequests())
    }
  }])
})

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
    <el-main v-if="loadSuccess">
      <common-form
        inline
        :model="searchParam"
        :options="searchFormOptions"
        :submit-label="$t('common.label.search')"
        @submit-form="loadMockRequests()"
      >
        <template #buttons>
          <el-button
            type="info"
            @click="newOrEdit()"
          >
            {{ $t('common.label.new') }}
          </el-button>
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
      <common-table
        ref="requestTableRef"
        v-model:page="searchParam.page"
        :data="tableData"
        :columns="columns"
        :buttons="requestButtons"
        :buttons-column-attrs="{minWidth:'240px'}"
        :loading="loading"
        expand-table
        row-key="id"
        :expanded-row-keys="expandRequestRows.map(item=>item.id)"
        @row-dblclick="requestTableRef?.table?.toggleRowExpansion($event)"
        @expand-change="(_,rows)=>{expandRequestRows=rows}"
        @page-size-change="loadMockRequests()"
        @current-page-change="loadMockRequests()"
        @selection-change="selectedRows=$event"
      >
        <template #expand="{item}">
          <mock-data-table
            style="margin-left:40px"
            :group-item="groupItem"
            :request-item="item"
          />
        </template>
      </common-table>
    </el-main>
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

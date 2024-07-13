<script setup lang="jsx">
import { useRoute } from 'vue-router'
import { ElTag } from 'element-plus'
import { $coreConfirm, checkShowColumn, useBackUrl } from '@/utils'
import { useMockGroupItem } from '@/hooks/mock/MockGroupHooks'
import MockRequestApi, { ALL_METHODS } from '@/api/mock/MockRequestApi'
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

loadMockRequests()

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
      label: '请求方法',
      prop: 'method',
      type: 'select',
      children: methodOptions,
      change () {
        loadMockRequests(1)
      }
    }
  ]
})

const columns = computed(() => {
  return defineTableColumns([{
    label: '请求路径',
    property: 'requestPath',
    formatter (data) {
      const path = `/mock/${groupItem.value?.groupPath}${data.requestPath}`
      return <>
        {data.requestPath}&nbsp;
        <MockUrlCopyLink urlPath={path}/>
      </>
    },
    minWidth: '200px'
  }, {
    label: '请求方法',
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
    label: '匹配规则',
    enabled: checkShowColumn(tableData.value, 'matchPattern'),
    minWidth: '150px',
    formatter (data) {
      if (data.matchPattern) {
        return <ViewDataLink data={data.matchPattern} tooltip="测试匹配规则"
                             onViewDataDetails={() => toTestMatchPattern(groupItem, data)
                               .then(() => loadMockRequests())}/>
      }
    }
  }, {
    label: '请求名称',
    property: 'requestName',
    enabled: checkShowColumn(tableData.value, 'requestName')
  }, {
    labelKey: 'common.label.description',
    property: 'description',
    enabled: checkShowColumn(tableData.value, 'description')
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
    label: '展开响应',
    type: 'primary',
    click: item => {
      requestTableRef.value?.table?.toggleRowExpansion(item)
      console.log('==================================', expandRequestRows.value)
    },
    dynamicButton (item) {
      const expanded = expandRequestRows.value.map(req => req.id).includes(item.id)
      return {
        label: expanded ? '收起响应' : '展开响应',
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
    labelKey: 'common.label.delete',
    type: 'danger',
    click: item => {
      $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [`${item.requestPath}#${item.method}`]))
        .then(() => MockRequestApi.deleteById(item.id))
        .then(() => loadMockRequests())
    }
  }])
})

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
    label: '请求路径',
    prop: 'requestPath',
    required: true,
    change (val) {
      if (val && !val.startsWith('/')) {
        currentRequest.value.requestPath = `/${val.trim()}`
      }
    }
  }, {
    label: '请求方法',
    prop: 'method',
    required: true,
    type: 'select',
    children: methodOptions,
    attrs: {
      clearable: false
    }
  }, useFormStatus(), useFormDelay(),
  {
    label: '匹配规则',
    type: 'vue-monaco-editor',
    prop: 'matchPattern',
    tooltip: `匹配规则支持javascript表达式，可以使用request请求数据: <br>
        request.body——body内容对象（仅json）<br>
        request.bodyStr——body内容字符串<br>
        request.headers——头信息对象<br>
        request.parameters——请求参数对象<br>
        request.pathParameters——路径参数对象
    `,
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
    label: '请求名称',
    prop: 'requestName',
    tooltip: '简单接口名称，可不填写'
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
          {{ groupItem.groupName }} 【{{ groupUrl }}】
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
        </template>
      </common-form>
      <common-table
        ref="requestTableRef"
        v-model:page="searchParam.page"
        :data="tableData"
        :columns="columns"
        :buttons="requestButtons"
        :buttons-column-attrs="{minWidth:'200px'}"
        :loading="loading"
        expand-table
        row-key="id"
        :expanded-row-keys="expandRequestRows.map(item=>item.id)"
        @row-dblclick="requestTableRef?.table?.toggleRowExpansion($event)"
        @expand-change="(_,rows)=>{expandRequestRows=rows}"
        @page-size-change="loadMockRequests()"
        @current-page-change="loadMockRequests()"
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
      name="Mock请求"
      :save-current-item="saveMockRequest"
    />
  </el-container>
</template>

<style scoped>

</style>

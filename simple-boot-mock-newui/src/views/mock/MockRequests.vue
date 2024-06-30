<script setup lang="jsx">
import { useRoute } from 'vue-router'
import { ElTag } from 'element-plus'
import { $coreConfirm, $goto, useBackUrl } from '@/utils'
import { useMockGroupItem } from '@/hooks/mock/MockGroupHooks'
import MockRequestApi, { ALL_METHODS } from '@/api/mock/MockRequestApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons, defineTableColumns } from '@/components/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { ref, computed } from 'vue'
import { useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useDefaultPage } from '@/config'
import { $i18nBundle } from '@/messages'

const route = useRoute()
const groupId = route.params.groupId

const { goBack } = useBackUrl('/mock/groups')
const page = ref(useDefaultPage())
const { groupItem, groupUrl, loadSuccess } = useMockGroupItem(groupId)

const { tableData, loading, searchParam, searchMethod: loadMockRequests } = useTableAndSearchForm({
  defaultParam: { groupId, page: page.value },
  searchMethod: MockRequestApi.search,
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
      children: methodOptions
    }
  ]
})

const columns = defineTableColumns([{
  label: '请求路径',
  property: 'requestPath'
}, {
  label: '请求方法',
  property: 'method',
  formatter (data) {
    const config = methodsConfig[data.method]
    return <ElTag type={config.type} effect="dark">{data.method}</ElTag>
  }
}, {
  label: '描述',
  property: 'description'
}, {
  labelKey: 'common.label.status',
  property: 'status',
  formatter (data) {
    return <DelFlagTag v-model={data.status}/>
  }
}])

const requestButtons = defineTableButtons([{
  labelKey: 'common.label.edit',
  type: 'primary',
  click: item => {
    newOrEdit(item.id)
  }
}, {
  labelKey: 'common.label.config',
  type: 'success',
  click: item => {
    $goto(`/mock/groups/${item.id}`)
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
const editFormOptions = computed(() => {
  return defineFormOptions([{
    label: '请求路径',
    prop: 'requestPath',
    required: true,
    change (val) {
      console.log('====================change1', val)
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
  }, useFormStatus(), {
    labelKey: '备注信息',
    prop: 'description'
  }])
})

const saveMockRequest = item => {
  MockRequestApi.saveOrUpdate(item)
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
        v-model:page="page"
        :data="tableData"
        :columns="columns"
        :buttons="requestButtons"
        :buttons-column-attrs="{width:'250px'}"
        :loading="loading"
        @page-size-change="loadMockRequests()"
        @current-page-change="loadMockRequests()"
      />
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

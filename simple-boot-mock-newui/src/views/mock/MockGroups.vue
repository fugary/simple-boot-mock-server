<script setup lang="jsx">
import { computed, onMounted, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions, defineTableButtons } from '@/components/utils'
import MockGroupApi, { checkExport, downloadByLink, MOCK_GROUP_URL } from '@/api/mock/MockGroupApi'
import { useUserAutocompleteConfig } from '@/api/mock/MockUserApi'
import { $coreConfirm, $goto, checkShowColumn, isAdminUser, $coreError, toGetParams } from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle } from '@/messages'
import { useFormDelay, useFormStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { getMockUrl } from '@/api/mock/MockRequestApi'

const { search, getById, deleteById, saveOrUpdate } = MockGroupApi

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage() },
  searchMethod: search
})
const loadMockGroups = (pageNumber) => searchMethod(pageNumber)

onMounted(() => {
  loadMockGroups()
})

/**
 *
 * @type {[CommonTableColumn]}
 */
const columns = computed(() => {
  return [{
    label: '分组名称',
    property: 'groupName'
  }, {
    label: '路径ID',
    property: 'groupPath',
    minWidth: '150px',
    formatter (data) {
      const path = `/mock/${data.groupPath}`
      return <>
        {data.groupPath}&nbsp;
        <MockUrlCopyLink urlPath={path}/>
      </>
    }
  }, {
    label: '代理地址',
    property: 'proxyUrl',
    minWidth: '150px'
  }, {
    labelKey: 'common.label.delay',
    property: 'delay',
    enabled: checkShowColumn(tableData.value, 'delay')
  }, {
    labelKey: 'common.label.description',
    property: 'description',
    enabled: checkShowColumn(tableData.value, 'description')
  }, {
    labelKey: 'common.label.status',
    property: 'status',
    formatter (data) {
      return <DelFlagTag v-model={data.status} clickToToggle={true}
                         onToggleValue={(status) => saveGroupItem({ ...data, status })}/>
    }
  }, {
    labelKey: 'common.label.createDate',
    property: 'createDate',
    dateFormat: 'YYYY-MM-DD HH:mm:ss'
  }]
})
const buttons = defineTableButtons([{
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
  click: item => deleteGroup(item)
}])
//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'common-autocomplete',
    enabled: isAdminUser(),
    attrs: {
      autocompleteConfig: useUserAutocompleteConfig(),
      emptySearchEnabled: true
    }
  },
  {
    labelKey: 'common.label.keywords',
    prop: 'keyword'
  }]
})

const deleteGroup = group => {
  $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [group.groupName]))
    .then(() => deleteById(group.id))
    .then(() => loadMockGroups())
}

const showEditWindow = ref(false)
const currentGroup = ref()
const newOrEdit = async id => {
  if (id) {
    await getById(id).then(data => {
      data.resultData && (currentGroup.value = data.resultData)
    })
  } else {
    currentGroup.value = {
      status: 1
    }
  }
  showEditWindow.value = true
}
const editFormOptions = defineFormOptions([{
  label: '分组名称',
  prop: 'groupName',
  required: true
}, {
  label: '路径ID',
  prop: 'groupPath',
  placeholder: '建议不要填写，自动生成'
}, {
  label: '代理地址',
  prop: 'proxyUrl',
  tooltip: '配置的请求之外的地址将发送到代理地址获取数据，支持http和https',
  rules: [{
    message: '代理地址必须是正常的http或者https地址',
    validator: () => {
      return !currentGroup.value?.proxyUrl || /^https?:\/\//.test(currentGroup.value?.proxyUrl)
    }
  }]
}, useFormStatus(), useFormDelay(), {
  labelKey: 'common.label.description',
  prop: 'description',
  attrs: {
    type: 'textarea'
  }
}])
const saveGroupItem = (item) => {
  return saveOrUpdate(item).then(() => loadMockGroups())
}
const exportGroups = (groupIds) => {
  const exportConfig = {
    exportAll: !groupIds,
    groupIds,
    userName: searchParam.value.userName
  }
  checkExport(exportConfig, { loading: true, showErrorMessage: false }).then((data) => {
    if (data.success) {
      exportConfig.access_token = useLoginConfigStore().accessToken
      const downloadUrl = `${getMockUrl(`${MOCK_GROUP_URL}/export`)}?${toGetParams(exportConfig)}`
      downloadByLink(downloadUrl)
    } else {
      $coreError('没有需要导出的数据')
    }
  })
}
</script>

<template>
  <el-container
    class="flex-column"
  >
    <common-form
      inline
      :model="searchParam"
      :options="searchFormOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="loadMockGroups"
    >
      <template #buttons>
        <el-button
          type="info"
          @click="newOrEdit()"
        >
          {{ $t('common.label.new') }}
        </el-button>
        <el-button
          type="success"
          @click="exportGroups()"
        >
          全部导出
        </el-button>
      </template>
    </common-form>
    <common-table
      v-model:page="searchParam.page"
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :buttons-column-attrs="{width:'250px'}"
      :loading="loading"
      @page-size-change="loadMockGroups()"
      @current-page-change="loadMockGroups()"
    />
    <simple-edit-window
      v-model="currentGroup"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      name="Mock分组"
      :save-current-item="saveGroupItem"
    />
  </el-container>
</template>

<style scoped>

</style>

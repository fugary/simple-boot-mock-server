<script setup lang="jsx">
import { computed, onActivated, onMounted, ref } from 'vue'
import { checkShowColumn, formatDate, getSingleSelectOptions, isAdminUser } from '@/utils'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useAllUsers } from '@/api/mock/MockUserApi'
import MockLogApi from '@/api/mock/MockLogApi'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { ElText, ElTag } from 'element-plus'
import { useDefaultPage } from '@/config'
import MethodTag from '@/views/components/utils/MethodTag.vue'
import { $i18nKey } from '@/messages'

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { keyword: '', page: useDefaultPage() },
  searchMethod: MockLogApi.search
})

const loadApiLogs = (...args) => {
  searchParam.value.startDate = formatDate(dateParam.value?.createDates?.[0])
  searchParam.value.endDate = formatDate(dateParam.value?.createDates?.[1])
  return searchMethod(...args)
}
const dateParam = ref({
  createDates: []
})
const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam, { current: false })

const { initLoadOnce } = useInitLoadOnce(async () => {
  await loadUsersAndRefreshOptions(false)
  await loadApiLogs()
})

onMounted(initLoadOnce)

onActivated(initLoadOnce)

const columns = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'creator',
    enabled: checkShowColumn(tableData.value, 'creator')
  }, {
    labelKey: 'mock.label.logName',
    prop: 'logName',
    minWidth: '150px'
  }, {
    labelKey: 'mock.label.logResult',
    formatter (data) {
      if (data.logResult) {
        const type = data.logResult === 'SUCCESS' ? 'success' : 'danger'
        return <ElTag type={type}>{data.logResult}</ElTag>
      }
    }
  }, {
    labelKey: 'mock.label.logTime',
    prop: 'logTime'
  }, {
    labelKey: 'mock.label.logType',
    formatter (data) {
      if (data.logType) {
        return <MethodTag size="default" method={data.logType}/>
      }
    }
  }, {
    labelKey: 'mock.label.ipAddress',
    prop: 'ipAddress'
  }, {
    labelKey: 'mock.label.logMessage',
    prop: 'logMessage'
  }, {
    labelKey: 'mock.label.logData',
    minWidth: '150px',
    formatter (data) {
      const dataStr = data.logData || data.responseBody
      return <ElText onClick={() => showCodeWindow(dataStr)}
                     style="white-space: nowrap;cursor: pointer;">
        {dataStr}
      </ElText>
    }
  }, {
    labelKey: 'mock.label.exceptions',
    enabled: checkShowColumn(tableData.value, 'exceptions'),
    formatter (data) {
      return <ElText onClick={() => showCodeWindow(data.exceptions)}
                     style="white-space: nowrap;cursor: pointer;">
        {data.exceptions}
      </ElText>
    }
  }, {
    labelKey: 'common.label.createDate',
    property: 'createDate',
    dateFormat: 'YYYY-MM-DD HH:mm:ss'
  }]
})

const buttons = computed(() => {
  return [{
    labelKey: 'common.label.view',
    type: 'primary',
    click: item => {
      showCodeWindow(JSON.stringify(item), {
        showSelectButton: true,
        buttons: [{
          enabled: !!item.logData,
          type: 'info',
          label: $i18nKey('common.label.commonView', 'mock.label.requestBody1'),
          click: () => {
            showCodeWindow(item.logData)
          }
        }, {
          enabled: !!item.responseBody,
          label: $i18nKey('common.label.commonView', 'mock.label.responseBody1'),
          type: 'info',
          click: () => {
            showCodeWindow(item.responseBody)
          }
        }, {
          enabled: !!item.headers,
          label: $i18nKey('common.label.commonView', 'mock.label.requestHeaders'),
          type: 'info',
          click: () => {
            showCodeWindow(item.headers)
          }
        }, {
          enabled: !!item.responseHeaders,
          label: $i18nKey('common.label.commonView', 'mock.label.responseHeaders'),
          type: 'info',
          click: () => {
            showCodeWindow(item.responseHeaders)
          }
        }]
      })
    }
  }]
})
//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    enabled: isAdminUser(),
    children: userOptions.value,
    change () {
      loadApiLogs()
    }
  }, {
    labelKey: 'mock.label.logName',
    prop: 'logName'
  }, {
    labelKey: 'mock.label.logResult',
    prop: 'logResult',
    type: 'select',
    children: getSingleSelectOptions('SUCCESS', 'FAIL'),
    change () {
      loadApiLogs()
    }
  }, {
    labelKey: 'mock.label.logType',
    prop: 'logType'
  }, {
    labelKey: 'mock.label.ipAddress',
    prop: 'ipAddress'
  }, {
    model: dateParam.value,
    labelKey: 'common.label.createDate',
    prop: 'createDates',
    type: 'date-picker',
    attrs: {
      type: 'datetimerange',
      unlinkPanels: true
    }
  }, {
    labelKey: 'common.label.keywords',
    prop: 'keyword'
  }]
})

</script>

<template>
  <el-container class="flex-column">
    <common-form
      inline
      :model="searchParam"
      :options="searchFormOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="loadApiLogs()"
    />
    <common-table
      v-model:page="searchParam.page"
      :data="tableData"
      :buttons-column-attrs="{minWidth:'100px'}"
      :columns="columns"
      :buttons="buttons"
      :loading="loading"
      @page-size-change="loadApiLogs()"
      @current-page-change="loadApiLogs()"
    />
  </el-container>
</template>

<style scoped>

</style>

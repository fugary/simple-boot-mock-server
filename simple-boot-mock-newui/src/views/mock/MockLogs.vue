<script setup lang="jsx">
import { computed, onActivated, onMounted, ref, watch } from 'vue'
import { checkShowColumn, formatDate, getSingleSelectOptions, isAdminUser, useCurrentUserName } from '@/utils'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useAllUsers } from '@/api/mock/MockUserApi'
import MockLogApi from '@/api/mock/MockLogApi'
import { ALL_STATUS_CODES } from '@/api/mock/MockDataApi'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { ElText, ElTag } from 'element-plus'
import { useDefaultPage } from '@/config'
import MethodTag from '@/views/components/utils/MethodTag.vue'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'
import { $i18nKey, $i18nMsg } from '@/messages'
import { useRoute, useRouter } from 'vue-router'
import { resolveDashboardLogPreset } from '@/services/mock/DashboardLogPreset'
import MockDiagnoseInfo from '@/views/components/mock/MockDiagnoseInfo.vue'

const route = useRoute()
const router = useRouter()
const LOG_SEARCH_QUERY_KEYS = ['mockGroupPath', 'dataId']
const LOG_ROUTE_QUERY_KEYS = ['preset', 'scope', ...LOG_SEARCH_QUERY_KEYS]

const normalizeQueryValue = value => Array.isArray(value) ? value[0] : value

const createDefaultSearchParam = () => ({
  keyword: '',
  page: useDefaultPage()
})

const createDefaultDateParam = () => ({
  createDates: []
})

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: createDefaultSearchParam(),
  searchMethod: MockLogApi.search
})

const dateParam = ref(createDefaultDateParam())
const showDiagnoseWindow = ref(false)
const currentDiagnoseInfo = ref()

const resetLogSearchState = () => {
  searchParam.value = createDefaultSearchParam()
  dateParam.value = createDefaultDateParam()
}

const applyDashboardPreset = () => {
  const preset = resolveDashboardLogPreset(route.query, useCurrentUserName())
  if (!preset.matched) {
    return false
  }
  searchParam.value = {
    ...createDefaultSearchParam(),
    ...preset.searchParam
  }
  dateParam.value = { createDates: preset.dateRange }
  return true
}

const applyLogSearchQuery = () => {
  const routeSearchParam = LOG_SEARCH_QUERY_KEYS.reduce((result, key) => {
    const value = normalizeQueryValue(route.query[key])
    if (value) {
      result[key] = value
    }
    return result
  }, {})
  if (!Object.keys(routeSearchParam).length) {
    return false
  }
  searchParam.value = {
    ...createDefaultSearchParam(),
    ...routeSearchParam
  }
  dateParam.value = createDefaultDateParam()
  return true
}

const applyLogRouteQuery = () => applyDashboardPreset() || applyLogSearchQuery()

const loadApiLogs = (...args) => {
  searchParam.value.startDate = formatDate(dateParam.value?.createDates?.[0])
  searchParam.value.endDate = formatDate(dateParam.value?.createDates?.[1])
  return searchMethod(...args)
}

const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam, { current: false })

const clearLogSearchForm = async () => {
  resetLogSearchState()
  if (LOG_ROUTE_QUERY_KEYS.some(key => route.query[key] != null)) {
    const query = { ...route.query }
    LOG_ROUTE_QUERY_KEYS.forEach(key => delete query[key])
    await router.replace({
      name: route.name,
      query
    })
  }
  await loadUsersAndRefreshOptions(false)
  await loadApiLogs(1)
}

const { initLoadOnce } = useInitLoadOnce(async () => {
  applyLogRouteQuery()
  await loadUsersAndRefreshOptions(false)
  await loadApiLogs()
})

onMounted(initLoadOnce)

onActivated(initLoadOnce)

watch(() => LOG_ROUTE_QUERY_KEYS.map(key => normalizeQueryValue(route.query[key]) || '').join('|'), async (value, oldValue) => {
  if (value === oldValue || !applyLogRouteQuery()) {
    return
  }
  await loadUsersAndRefreshOptions(false)
  await loadApiLogs(1)
})

const statusCodeTagType = statusCode => {
  const code = Number(statusCode)
  if (!Number.isFinite(code)) return 'info'
  if (code >= 500) return 'danger'
  if (code >= 400) return 'warning'
  if (code >= 300) return 'info'
  return 'success'
}

const statusCodeOptions = computed(() => ALL_STATUS_CODES.map(status => {
  const label = $i18nMsg(`${status.labelCn} - ${(status.labelEn)}`, `${status.labelEn} - ${(status.labelCn)}`)
  return {
    value: status.code,
    label: `${status.code} - ${label}`
  }
}))

const formatLogResult = data => {
  if (data.logResult) {
    return <ElTag type={data.logResult === 'SUCCESS' ? 'success' : 'danger'}>{data.logResult}</ElTag>
  }
}

const formatStatusCode = data => {
  if (data.responseStatusCode != null) {
    return <ElTag type={statusCodeTagType(data.responseStatusCode)}>{data.responseStatusCode}</ElTag>
  }
}

const columns = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'creator',
    enabled: checkShowColumn(tableData.value, 'creator')
  }, {
    labelKey: 'mock.label.logName',
    prop: 'logName',
    minWidth: '200px'
  }, {
    labelKey: 'mock.label.logResult',
    minWidth: '100px',
    formatter: formatLogResult
  }, {
    labelKey: 'mock.label.statusCode',
    minWidth: '90px',
    formatter: formatStatusCode
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
    minWidth: '150px',
    prop: 'ipAddress'
  }, {
    labelKey: 'mock.label.mockReturn',
    minWidth: '200px',
    formatter (data) {
      return <>
        {data.dataId
          ? <MockUrlCopyLink urlPath={data.dataId}>
              {data.dataId}
            </MockUrlCopyLink>
          : ''}
        {data.dataId && data.proxyUrl ? ' | ' : ''}
        {data.proxyUrl
          ? <MockUrlCopyLink urlPath={data.proxyUrl}>
              {data.proxyUrl}
            </MockUrlCopyLink>
          : ''}
      </>
    }
  }, {
    labelKey: 'mock.label.logData',
    minWidth: '200px',
    formatter (data) {
      const dataStr = data.logData || data.responseBody
      return <ElText onClick={() => showCodeWindow(dataStr)}
                     style="white-space: nowrap;cursor: pointer;">
        {dataStr}
      </ElText>
    }
  }, {
    labelKey: 'mock.label.logMessage',
    minWidth: '150px',
    prop: 'logMessage'
  }, {
    labelKey: 'mock.label.exceptions',
    enabled: checkShowColumn(tableData.value, 'exceptions'),
    minWidth: '200px',
    formatter (data) {
      return <ElText onClick={() => showCodeWindow(data.exceptions)}
                     style="white-space: nowrap;cursor: pointer;">
        {data.exceptions}
      </ElText>
    }
  }, {
    labelKey: 'common.label.createDate',
    property: 'createDate',
    minWidth: '150px',
    dateFormat: 'YYYY-MM-DD HH:mm:ss'
  }]
})

const showLogDetail = item => showCodeWindow(JSON.stringify(item), {
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

const showDiagnoseDetail = item => {
  try {
    currentDiagnoseInfo.value = JSON.parse(item.diagnoseData)
    showDiagnoseWindow.value = true
  } catch {
    showCodeWindow(item.diagnoseData)
  }
}

const buttons = computed(() => {
  return [{
    labelKey: 'common.label.view',
    type: 'primary',
    click: showLogDetail
  }, {
    labelKey: 'mock.label.diagnose',
    type: 'warning',
    buttonIf: item => !!item.diagnoseData,
    click: showDiagnoseDetail
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
    labelKey: 'mock.label.dataId',
    prop: 'dataId'
  }, {
    labelKey: 'mock.label.logResult',
    prop: 'logResult',
    type: 'select',
    children: getSingleSelectOptions('SUCCESS', 'FAIL'),
    change () {
      loadApiLogs()
    }
  }, {
    labelKey: 'mock.label.statusCode',
    prop: 'responseStatusCode',
    type: 'select',
    children: statusCodeOptions.value,
    attrs: {
      filterable: true
    },
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
    labelKey: 'mock.label.pathId',
    prop: 'mockGroupPath'
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
    >
      <template #buttons>
        <el-button @click="clearLogSearchForm">
          {{ $t('common.label.reset') }}
        </el-button>
      </template>
    </common-form>
    <common-table
      v-model:page="searchParam.page"
      :data="tableData"
      :buttons-column-attrs="{minWidth:'150px',fixed:'right'}"
      :columns="columns"
      :buttons="buttons"
      :loading="loading"
      @page-size-change="loadApiLogs()"
      @current-page-change="loadApiLogs()"
      @row-dblclick="showLogDetail($event)"
    />
    <common-window
      v-model="showDiagnoseWindow"
      width="920px"
      :show-cancel="false"
      :ok-label="$t('common.label.close')"
      destroy-on-close
    >
      <template #header>
        <span class="el-dialog__title">
          {{ $t('mock.label.diagnose') }}
        </span>
      </template>
      <mock-diagnose-info
        v-if="currentDiagnoseInfo"
        :diagnose-info="currentDiagnoseInfo"
      />
    </common-window>
  </el-container>
</template>

<style scoped>

</style>

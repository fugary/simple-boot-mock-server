<script setup lang="jsx">
import { computed, onMounted } from 'vue'
import { ElMessage, ElTag, ElText } from 'element-plus'
import { $i18nBundle } from '@/messages'
import { $coreConfirm } from '@/utils'
import { showCodeWindow } from '@/utils/DynamicUtils'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import ScheduledTaskApi from '@/api/mock/ScheduledTaskApi'

const { tableData, loading, searchMethod: loadTasks } = useTableAndSearchForm({
  searchMethod: ScheduledTaskApi.search,
  saveParam: false
})

onMounted(() => loadTasks())

const i18nText = (key, fallback = '') => {
  if (!key) {
    return fallback
  }
  const value = $i18nBundle(key)
  return value && value !== key ? value : (fallback || value)
}

const formatEnabled = item => {
  const labelKey = item.enabled ? 'common.label.statusEnabled' : 'common.label.statusDisabled'
  return <ElTag type={item.enabled ? 'success' : 'info'}>{$i18nBundle(labelKey)}</ElTag>
}

const formatTaskName = item => i18nText(item.nameKey, item.code)

const formatTaskDescription = item => i18nText(item.descriptionKey)

const formatTriggerType = item => {
  const triggerType = item.lastResult?.triggerType
  return triggerType ? i18nText(`mock.label.triggerType${triggerType}`, triggerType) : '-'
}

const formatTaskMessage = item => i18nText(item.lastResult?.messageKey, item.lastResult?.message || '-')

const formatLastStatus = item => {
  const lastResult = item.lastResult
  if (!lastResult) {
    return '-'
  }
  if (lastResult.skipped) {
    return <ElTag type="info">{$i18nBundle('mock.label.skipped')}</ElTag>
  }
  return <ElTag type={lastResult.success ? 'success' : 'danger'}>
    {lastResult.success ? $i18nBundle('mock.label.success') : $i18nBundle('mock.label.failed')}
  </ElTag>
}

const showLastResult = item => {
  showCodeWindow(JSON.stringify(item.lastResult || {}, null, 2), {
    language: 'json'
  })
}

const triggerTask = item => {
  return $coreConfirm($i18nBundle('mock.msg.triggerScheduledTaskConfirm'))
    .then(() => ScheduledTaskApi.trigger(item.code, { loading: true }))
    .then(result => {
      if (result?.success) {
        const executeResult = result.resultData
        ElMessage({
          type: executeResult?.success ? 'success' : 'warning',
          message: i18nText(executeResult?.messageKey, executeResult?.message || $i18nBundle('mock.msg.scheduledTaskCompleted'))
        })
        loadTasks()
      }
    })
}

const columns = computed(() => {
  return [{
    labelKey: 'mock.label.scheduledTaskCode',
    property: 'code',
    minWidth: '160px'
  }, {
    labelKey: 'mock.label.scheduledTaskName',
    formatter: formatTaskName,
    minWidth: '180px'
  }, {
    labelKey: 'mock.label.scheduledTaskDescription',
    formatter: formatTaskDescription,
    minWidth: '260px'
  }, {
    labelKey: 'mock.label.cronExpression',
    property: 'cron',
    minWidth: '140px'
  }, {
    labelKey: 'common.label.status',
    formatter: formatEnabled,
    attrs: {
      align: 'center'
    }
  }, {
    labelKey: 'mock.label.triggerType',
    formatter: formatTriggerType,
    minWidth: '120px'
  }, {
    labelKey: 'mock.label.lastResult',
    formatter: formatLastStatus,
    minWidth: '110px'
  }, {
    labelKey: 'mock.label.lastRunTime',
    property: 'lastResult.startDate',
    dateFormat: 'YYYY-MM-DD HH:mm:ss',
    minWidth: '160px'
  }, {
    labelKey: 'mock.label.duration',
    minWidth: '90px',
    formatter: item => item.lastResult?.duration == null ? '-' : `${item.lastResult.duration} ms`
  }, {
    labelKey: 'mock.label.affectedRows',
    property: 'lastResult.affectedRows',
    minWidth: '100px'
  }, {
    labelKey: 'mock.label.message',
    minWidth: '220px',
    formatter (item) {
      return item.lastResult
        ? <ElText truncated>{formatTaskMessage(item)}</ElText>
        : '-'
    }
  }]
})

const buttons = computed(() => {
  return [{
    labelKey: 'mock.label.runNow',
    type: 'primary',
    dynamicButton (item) {
      return {
        disabled: !item.enabled,
        tooltip: item.enabled ? '' : $i18nBundle('mock.msg.scheduledTaskDisabled')
      }
    },
    click: triggerTask
  }, {
    labelKey: 'common.label.view',
    type: 'info',
    buttonIf: item => !!item.lastResult,
    click: showLastResult
  }]
})
</script>

<template>
  <el-container class="flex-column">
    <div class="scheduled-task-toolbar">
      <el-button
        type="primary"
        @click="loadTasks()"
      >
        {{ $t('common.label.refresh') }}
      </el-button>
    </div>
    <common-table
      :data="tableData"
      :columns="columns"
      :buttons="buttons"
      :buttons-column-attrs="{width:'210px',fixed:'right'}"
      :loading="loading"
      @row-dblclick="showLastResult($event)"
    />
  </el-container>
</template>

<style scoped>
.scheduled-task-toolbar {
  margin-bottom: 10px;
}
</style>

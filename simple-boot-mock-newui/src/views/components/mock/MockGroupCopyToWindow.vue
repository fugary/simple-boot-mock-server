<script setup lang="jsx">
import { computed, ref } from 'vue'
import { isAdminUser, isCurrentUser, useCurrentUserName } from '@/utils'
import { assignProjectValue, filterProjectOptionsByAuthority, useProjectEditHook, useSelectProjects } from '@/hooks/mock/MockProjectHooks'
import { defineFormOptions } from '@/components/utils'
import { $i18nBundle, $i18nConcat, $i18nKey } from '@/messages'
import { ElMessage, ElText } from 'element-plus'
import { useAllUsers } from '@/api/mock/MockUserApi'
import { MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { transferMockGroup } from '@/api/mock/MockGroupApi'
import MockProjectApi, { checkProjectWritable } from '@/api/mock/MockProjectApi'
import { isArray } from 'lodash-es'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'

const props = defineProps({
  action: {
    type: String,
    default: 'copy'
  },
  allowMove: {
    type: Boolean,
    default: false
  }
})

const showWindow = ref(false)
const searchParam = ref({
  action: props.action || 'copy',
  projectId: null,
  userName: useCurrentUserName(),
  publicFlag: false
})
const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam)
const { projects, projectOptions, loadProjectsAndRefreshOptions } = useSelectProjects(searchParam, true)
const currentGroups = ref([])
const writableProjectOptions = computed(() => {
  return filterProjectOptionsByAuthority(projects.value, projectOptions.value, checkProjectWritable)
})
const { showEditWindow: showEditProjectWindow, currentProject, newOrEditProject, editFormOptions: editProjectFormOptions } = useProjectEditHook(searchParam, userOptions)

const getActionLabel = (action) => {
  return action === 'move'
    ? $i18nBundle('common.label.move')
    : $i18nBundle('common.label.copy')
}

const actionOptions = computed(() => {
  const options = [{
    label: getActionLabel('copy'),
    value: 'copy'
  }]
  if (props.allowMove) {
    options.push({
      label: getActionLabel('move'),
      value: 'move'
    })
  }
  return options
})

const currentActionTargetLabel = computed(() => {
  return searchParam.value.action === 'move'
    ? $i18nBundle('common.label.moveTo')
    : $i18nBundle('common.label.copyTo')
})

const currentWindowTitle = computed(() => {
  return $i18nConcat(currentActionTargetLabel.value, $i18nBundle('mock.label.project'))
})

const reloadProjectsAndRefreshOptions = async (project) => {
  await loadProjectsAndRefreshOptions()
  const savedProject = project?.id != null
    ? projects.value.find(item => `${item.id || ''}` === `${project.id}`)
    : projects.value.find(item => item.projectCode === project?.projectCode)
  if (savedProject) {
    assignProjectValue(searchParam.value, savedProject)
  }
}

const saveProjectItem = (item) => {
  return MockProjectApi.saveOrUpdate(item).then(data => {
    return reloadProjectsAndRefreshOptions(data?.resultData || item)
  })
}

const toCopyGroupTo = async (group) => {
  currentGroups.value = isArray(group) ? group : [group]
  searchParam.value.groupIds = currentGroups.value.map(grp => grp.id)
  searchParam.value.action = props.action || 'copy'
  searchParam.value.userName = isAdminUser() ? currentGroups.value[0]?.userName : useCurrentUserName()
  if (isAdminUser() || isCurrentUser(currentGroups.value[0]?.userName)) {
    assignProjectValue(searchParam.value, currentGroups.value[0])
  } else {
    assignProjectValue(searchParam.value, { projectCode: MOCK_DEFAULT_PROJECT, projectId: null })
  }
  await loadUsersAndRefreshOptions()
  await loadProjectsAndRefreshOptions()
  showWindow.value = true
}

const handleTargetUserChange = async () => {
  searchParam.value.projectId = null
  searchParam.value.projectCode = MOCK_DEFAULT_PROJECT
  await loadProjectsAndRefreshOptions()
}

const options = computed(() => {
  return defineFormOptions([{
    labelKey: 'common.label.operation',
    prop: 'action',
    type: 'segmented',
    required: true,
    enabled: props.allowMove,
    attrs: {
      clearable: false,
      options: actionOptions.value
    }
  }, {
    labelKey: 'mock.label.groupName',
    type: 'common-form-label',
    formatter () {
      return <>
        {currentGroups.value.map(currentGroup => {
          return <>{currentGroup.groupName}
            <ElText class="margin-left1" type="success" tag="b"
                    v-common-tooltip={$i18nBundle('mock.label.owner')}>({currentGroup.userName})</ElText>
            <br/></>
        })}
      </>
    }
  }, {
    label: $i18nConcat(currentActionTargetLabel.value, $i18nBundle('common.label.user')),
    prop: 'userName',
    type: 'select',
    required: true,
    enabled: isAdminUser(),
    children: userOptions.value,
    attrs: {
      filterable: true,
      clearable: false
    },
    change: handleTargetUserChange
  }, {
    label: $i18nConcat(currentActionTargetLabel.value, $i18nBundle('common.label.user')),
    enabled: !isAdminUser(),
    type: 'common-form-label',
    formatter () {
      return <ElText class="margin-left1" type="primary" tag="b">
        {searchParam.value.userName || useCurrentUserName()}
      </ElText>
    }
  }, {
    label: $i18nConcat(currentActionTargetLabel.value, $i18nBundle('mock.label.project')),
    prop: 'projectCode',
    type: 'select',
    required: true,
    children: writableProjectOptions.value,
    attrs: {
      filterable: true,
      clearable: false
    },
    change (value) {
      const option = writableProjectOptions.value.find(item => item.value === value)
      searchParam.value.projectId = option?.projectId || null
      searchParam.value.projectCode = option?.projectCode || value || null
    },
    tooltip: $i18nKey('common.label.commonAdd', 'mock.label.project'),
    tooltipIcon: 'CirclePlusFilled',
    tooltipLinkAttrs: {
      type: 'primary'
    },
    tooltipFunc (event) {
      newOrEditProject()
      event.preventDefault()
    }
  }])
})

const emit = defineEmits(['transferSuccess', 'copySuccess'])

const saveCopyGroupTo = ({ form }) => {
  form.validate().then(valid => {
    if (valid) {
      transferMockGroup({
        action: searchParam.value.action,
        groupIds: searchParam.value.groupIds,
        projectId: searchParam.value.projectId,
        projectCode: searchParam.value.projectCode,
        userName: searchParam.value.userName
      }).then((data) => {
        if (data?.success) {
          const successKey = searchParam.value.action === 'move'
            ? 'common.msg.moveSuccess'
            : 'common.msg.copySuccess'
          ElMessage.success($i18nBundle(successKey))
          emit('transferSuccess', data.resultData)
          emit('copySuccess', data.resultData)
        }
      })
    }
  })
}

defineExpose({
  toCopyGroupTo
})
</script>

<template>
  <common-window
    v-model="showWindow"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
    :ok-click="saveCopyGroupTo"
    :title="currentWindowTitle"
  >
    <el-container class="flex-column">
      <common-form
        class="form-edit-width-90"
        :options="options"
        :model="searchParam"
        label-width="140px"
        :show-buttons="false"
      />
      <simple-edit-window
        v-model="currentProject"
        v-model:show-edit-window="showEditProjectWindow"
        inline-auto-mode
        :form-options="editProjectFormOptions"
        :name="$t('mock.label.mockProjects')"
        :save-current-item="saveProjectItem"
        label-width="130px"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>

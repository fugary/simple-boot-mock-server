<script setup lang="jsx">
import { computed, ref } from 'vue'
import { isAdminUser, isCurrentUser, useCurrentUserName } from '@/utils'
import { useSelectProjects } from '@/api/mock/MockProjectApi'
import { defineFormOptions } from '@/components/utils'
import { $i18nBundle, $i18nConcat } from '@/messages'
import { ElMessage, ElText } from 'element-plus'
import { useAllUsers } from '@/api/mock/MockUserApi'
import { MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { copyMockGroup } from '@/api/mock/MockGroupApi'
import { isArray } from 'lodash-es'

const showWindow = ref(false)
const searchParam = ref({
  userName: useCurrentUserName(),
  publicFlag: false
})
const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam)
const { projectOptions, loadProjectsAndRefreshOptions } = useSelectProjects(searchParam, true)
const currentGroups = ref([])
const toCopyGroupTo = async (group) => {
  currentGroups.value = isArray(group) ? group : [group]
  searchParam.value.groupId = currentGroups.value.map(grp => grp.id).join(',')
  searchParam.value.userName = isAdminUser() ? group.userName : useCurrentUserName()
  searchParam.value.projectCode = isAdminUser() || isCurrentUser(group.userName) ? group.projectCode : MOCK_DEFAULT_PROJECT
  await loadUsersAndRefreshOptions()
  await loadProjectsAndRefreshOptions()
  showWindow.value = true
}

const options = computed(() => {
  return defineFormOptions([{
    labelKey: 'mock.label.groupName',
    type: 'common-form-label',
    formatter () {
      return <>
        {currentGroups.value.map(currentGroup => {
          return <>{currentGroup.groupName}
        <ElText class="margin-left1" type="primary" tag="b"
                v-common-tooltip={$i18nBundle('mock.label.owner')}>({currentGroup.userName})</ElText>
        <br/></>
        })}
      </>
    }
  }, {
    label: $i18nConcat($i18nBundle('common.label.copyTo'), $i18nBundle('common.label.user')),
    prop: 'userName',
    type: 'select',
    required: true,
    enabled: isAdminUser(),
    children: userOptions.value,
    attrs: {
      filterable: true,
      clearable: false
    },
    change: async () => {
      await loadProjectsAndRefreshOptions()
    }
  }, {
    label: $i18nConcat($i18nBundle('common.label.copyTo'), $i18nBundle('common.label.user')),
    enabled: !isAdminUser(),
    type: 'common-form-label',
    formatter () {
      return <ElText class="margin-left1" type="primary" tag="span">{searchParam.value.userName || useCurrentUserName()}</ElText>
    }
  }, {
    label: $i18nConcat($i18nBundle('common.label.copyTo'), $i18nBundle('mock.label.project')),
    prop: 'projectCode',
    type: 'select',
    required: true,
    children: projectOptions.value,
    attrs: {
      filterable: true,
      clearable: false
    }
  }])
})
const emit = defineEmits(['copySuccess'])
const saveCopyGroupTo = ({ form }) => {
  form.validate().then(valid => {
    if (valid) {
      copyMockGroup({
        groupId: searchParam.value.groupId,
        projectCode: searchParam.value.projectCode,
        userName: searchParam.value.userName
      }).then((data) => {
        if (data?.success) {
          ElMessage.success($i18nBundle('common.msg.operationSuccess'))
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
    :title="$i18nConcat($i18nBundle('common.label.copyTo'), $i18nBundle('mock.label.project'))"
  >
    <common-form
      class="form-edit-width-90"
      :options="options"
      :model="searchParam"
      label-width="140px"
      :show-buttons="false"
    />
  </common-window>
</template>

<style scoped>

</style>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { defineFormOptions, defineTableButtons, defineTableColumns } from '@/components/utils'
import { $coreConfirm, isAdminUser, useCurrentUserName } from '@/utils'
import { $i18nBundle } from '@/messages'
import MockProjectUserApi from '@/api/mock/MockProjectUserApi'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useAllUsers } from '@/api/mock/MockUserApi'
import { ElMessage } from 'element-plus'

const props = defineProps({
  showWindow: Boolean,
  project: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:showWindow'])

const visible = computed({
  get: () => props.showWindow,
  set: (val) => emit('update:showWindow', val)
})

const loading = ref(false)
const projectUsers = ref([])
const showEditWindow = ref(false)
const currentUser = ref({})
const searchParam = ref({})

const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam, { current: false })

const currentUserName = useCurrentUserName()

const canManage = computed(() => {
  return isAdminUser() || props.project?.userName === currentUserName
})

const selectableUserOptions = computed(() => {
  const excludedUsers = new Set(projectUsers.value.map(item => item.userName))
  if (currentUserName) {
    excludedUsers.add(currentUserName)
  }
  if (props.project?.userName) {
    excludedUsers.add(props.project.userName)
  }
  if (currentUser.value?.userName) {
    excludedUsers.delete(currentUser.value.userName)
  }
  return userOptions.value.filter(item => !excludedUsers.has(item?.value || item?.userName))
})

const loadProjectUsers = () => {
  if (!props.project?.id) {
    projectUsers.value = []
    return Promise.resolve([])
  }
  loading.value = true
  return MockProjectUserApi.search({ projectId: props.project.id }).then(res => {
    projectUsers.value = res?.resultData || []
    return projectUsers.value
  }).finally(() => {
    loading.value = false
  })
}

watch(() => props.showWindow, (val) => {
  if (val) {
    loadProjectUsers()
  }
})

onMounted(() => {
  loadUsersAndRefreshOptions()
})

const removeUser = (id) => {
  return $coreConfirm($i18nBundle('common.msg.deleteConfirm')).then(() => {
    return MockProjectUserApi.deleteById(id, { loading: true }).then(() => {
      loadProjectUsers()
      ElMessage.success($i18nBundle('common.msg.deleteSuccess'))
    })
  })
}

const newUser = () => {
  currentUser.value = {
    projectId: props.project?.id,
    authorities: ['readable']
  }
  showEditWindow.value = true
}

const editUser = (user) => {
  currentUser.value = {
    ...user,
    authorities: user.authorities ? user.authorities.split(',') : []
  }
  showEditWindow.value = true
}

const saveUser = (user) => {
  const data = {
    ...user,
    authorities: Array.isArray(user.authorities) ? user.authorities.join(',') : user.authorities
  }
  return MockProjectUserApi.saveOrUpdate(data, { loading: true }).then(() => {
    loadProjectUsers()
  })
}

const authOptions = computed(() => {
  return [
    { label: $i18nBundle('common.label.authorityReadable'), value: 'readable', tagType: 'primary' },
    { label: $i18nBundle('common.label.authorityWritable'), value: 'writable', tagType: 'success' },
    { label: $i18nBundle('common.label.authorityDeletable'), value: 'deletable', tagType: 'warning' }
  ]
})

const editFormOptions = computed(() => defineFormOptions([
  {
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    required: true,
    children: selectableUserOptions.value,
    attrs: {
      filterable: true
    }
  },
  {
    labelKey: 'common.label.authorities',
    prop: 'authorities',
    type: 'checkbox-group',
    required: true,
    children: authOptions.value.map(item => ({
      ...item,
      attrs: {
        class: `authority-checkbox-item ${item.tagType}`
      }
    })),
    attrs: {
      class: 'project-user-authorities'
    }
  }
]))

const parseAuthorities = (auths) => {
  if (!auths) return []
  return auths.split(',').filter(Boolean)
}

const getAuthorityItems = (auths) => {
  const items = Array.isArray(auths) ? auths : parseAuthorities(auths)
  return items.map(item => {
    const opt = authOptions.value.find(o => o.value === item)
    return opt || { label: item, value: item, tagType: 'info' }
  })
}

const columns = computed(() => defineTableColumns([
  {
    labelKey: 'common.label.user',
    property: 'userName',
    minWidth: '180px'
  },
  {
    labelKey: 'common.label.authorities',
    minWidth: '260px',
    slot: 'authorities'
  }
]))

const buttons = computed(() => defineTableButtons([
  {
    tooltip: $i18nBundle('common.label.edit'),
    icon: 'Edit',
    round: true,
    type: 'primary',
    click: item => editUser(item),
    buttonIf: () => canManage.value
  },
  {
    tooltip: $i18nBundle('common.label.delete'),
    icon: 'DeleteFilled',
    round: true,
    type: 'danger',
    click: item => removeUser(item.id),
    buttonIf: () => canManage.value
  }
]))

</script>

<template>
  <common-window
    v-model="visible"
    :title="$t('common.label.authorities')"
    width="700px"
    :show-cancel="false"
    :ok-label="$t('common.label.close')"
    append-to-body
  >
    <el-container class="flex-column">
      <el-container
        v-if="canManage"
        class="margin-bottom2"
      >
        <el-button
          type="primary"
          @click="newUser"
        >
          <common-icon icon="Plus" />
          {{ $t('common.label.add') }}
        </el-button>
      </el-container>

      <common-table
        :data="projectUsers"
        :loading="loading"
        :columns="columns"
        :buttons="buttons"
        :buttons-column-attrs="{ width: '130px', align: 'center' }"
      >
        <template #authorities="{ item }">
          <div class="authority-tags">
            <el-tag
              v-for="authority in getAuthorityItems(item.authorities)"
              :key="authority.value"
              :type="authority.tagType"
              effect="dark"
              size="small"
              class="margin-right1 margin-bottom1"
            >
              {{ authority.label }}
            </el-tag>
          </div>
        </template>
      </common-table>
    </el-container>

    <simple-edit-window
      v-model="currentUser"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      :name="$t('common.label.authorities')"
      :save-current-item="saveUser"
      label-width="100px"
    />
  </common-window>
</template>

<style scoped>
.authority-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
</style>

<style>
.project-user-authorities {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 18px;
}

.project-user-authorities .authority-checkbox-item {
  margin-right: 0;
  min-width: 96px;
}

.project-user-authorities .authority-checkbox-item.primary {
  --el-checkbox-text-color: var(--el-color-primary);
  --el-checkbox-checked-text-color: var(--el-color-primary);
}

.project-user-authorities .authority-checkbox-item.success {
  --el-checkbox-text-color: var(--el-color-success);
  --el-checkbox-checked-text-color: var(--el-color-success);
}

.project-user-authorities .authority-checkbox-item.warning {
  --el-checkbox-text-color: var(--el-color-warning);
  --el-checkbox-checked-text-color: var(--el-color-warning);
}

.project-user-authorities .authority-checkbox-item.primary .el-checkbox__label,
.project-user-authorities .authority-checkbox-item.primary .el-checkbox__input.is-checked + .el-checkbox__label {
  color: var(--el-color-primary) !important;
}

.project-user-authorities .authority-checkbox-item.success .el-checkbox__label,
.project-user-authorities .authority-checkbox-item.success .el-checkbox__input.is-checked + .el-checkbox__label {
  color: var(--el-color-success) !important;
}

.project-user-authorities .authority-checkbox-item.warning .el-checkbox__label,
.project-user-authorities .authority-checkbox-item.warning .el-checkbox__input.is-checked + .el-checkbox__label {
  color: var(--el-color-warning) !important;
}
</style>

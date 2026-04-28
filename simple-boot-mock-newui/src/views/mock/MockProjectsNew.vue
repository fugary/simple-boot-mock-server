<script setup lang="jsx">
import { computed, onMounted, onActivated, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useAllUsers } from '@/api/mock/MockUserApi'
import MockProjectApi, {
  checkProjectDeletable,
  checkProjectEdit,
  selectProjects,
  sortProjects,
  transferMockProject
} from '@/api/mock/MockProjectApi'
import { $coreConfirm, $goto, formatDate, isAdminUser, useCurrentUserName } from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle, $i18nConcat, $i18nKey } from '@/messages'
import { useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { chunk } from 'lodash-es'
import CommonIcon from '@/components/common-icon/index.vue'
import MockProjectUserManageWindow from '@/views/components/mock/MockProjectUserManageWindow.vue'
import { useRoute } from 'vue-router'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { useElementSize } from '@vueuse/core'
import { ElLink, ElMessage, ElTag, ElText } from 'element-plus'
import { useProjectEditHook } from '@/hooks/mock/MockProjectHooks'

const props = defineProps({
  publicFlag: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()

const { search, deleteById, saveOrUpdate } = MockProjectApi

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage(50), publicFlag: props.publicFlag, onlyMine: false },
  searchMethod: search
})
const sharedProjectOptions = ref([])
const showOnlyMineFilter = computed(() => !isAdminUser() && !props.publicFlag && sharedProjectOptions.value.length > 0)
const loadSharedProjectOptions = () => {
  if (props.publicFlag) {
    sharedProjectOptions.value = []
    return Promise.resolve([])
  }
  return selectProjects({
    userName: searchParam.value?.userName || useCurrentUserName(),
    publicFlag: false
  }).then(result => {
    sharedProjectOptions.value = (result || []).filter(project => {
      return !isDefaultProject(project?.projectCode) && project?.userName && project.userName !== useCurrentUserName()
    })
    if (!sharedProjectOptions.value.length) {
      searchParam.value.onlyMine = false
    }
    return sharedProjectOptions.value
  })
}
const loadMockProjects = (pageNumber) => {
  tableData.value = []
  if (colSize.value && searchParam.value.page) {
    searchParam.value.page.pageSize = Math.floor(10 / colSize.value) * colSize.value
  }
  return searchMethod(pageNumber)
}
const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam, { current: !props.publicFlag })

const { initLoadOnce } = useInitLoadOnce(async () => {
  await loadUsersAndRefreshOptions()
  await loadSharedProjectOptions()
  return loadMockProjects()
})

onMounted(initLoadOnce)

onActivated(initLoadOnce)

const handleOnlyMineChange = (value) => {
  if (value) {
    searchParam.value.userName = useCurrentUserName()
  }
  loadMockProjects(1)
}

const handleUserChange = async () => {
  await loadSharedProjectOptions()
  loadMockProjects(1)
}

const reloadProjectsAfterAuthorityChange = async () => {
  await loadSharedProjectOptions()
  await loadMockProjects(searchParam.value?.page?.pageNumber || 1)
}

const gotoMockGroups = (project) => {
  if (project.status === 1) {
    const query = new URLSearchParams({ backUrl: route.fullPath })
    if (!isDefaultProject(project.projectCode) && project.id != null) {
      query.set('projectId', `${project.id}`)
    }
    const userPath = isDefaultProject(project.projectCode) && project.userName ? `/${project.userName}` : ''
    $goto(`/mock/groups/${props.publicFlag ? 'pubProject' : 'project'}/${project.projectCode}${userPath}?${query.toString()}`)
  }
}

//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [...(showOnlyMineFilter.value
    ? [{
        labelKey: 'mock.label.myData',
        prop: 'onlyMine',
        type: 'switch',
        enabled: true,
        change: handleOnlyMineChange
      }]
    : []),
  {
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    enabled: (isAdminUser() && !searchParam.value.onlyMine) || props.publicFlag,
    children: userOptions.value,
    attrs: {
      filterable: true,
      clearable: props.publicFlag
    },
    change: handleUserChange
  },
  {
    ...useSearchStatus({ change () { loadMockProjects(1) } }),
    enabled: !props.publicFlag
  },
  {
    labelKey: 'common.label.keywords',
    prop: 'keyword'
  }]
})

const deleteProject = (project, $event) => {
  $event?.stopPropagation()
  $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [project.projectName]))
    .then(() => deleteById(project.id, { loading: true }))
    .then(() => loadMockProjects())
}

const deleteProjects = () => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockProjectApi.removeByIds(selectedRows.value.map(item => item.id), { loading: true }))
    .then(() => loadMockProjects())
}

const { showEditWindow, currentProject, newOrEditProject: newOrEdit, editFormOptions } = useProjectEditHook(searchParam, userOptions)
const saveProjectItem = (item) => {
  return saveOrUpdate(item, { loading: true }).then(() => {
    loadMockProjects()
  })
}
const cancelProjectPublicFlag = (project, $event) => {
  $event?.stopPropagation()
  if (!project?.publicFlag || !checkProjectEdit(project)) {
    return Promise.resolve()
  }
  return $coreConfirm($i18nKey('common.msg.commonConfirm', 'mock.label.cancelPublic'))
    .then(() => saveProjectItem({ ...project, publicFlag: false }))
}

const minWidth = '100px'

const projectAuthorityOptions = computed(() => {
  return [{
    label: $i18nBundle('common.label.authorityReadable'),
    shortLabel: 'R',
    value: 'readable'
  }, {
    label: $i18nBundle('common.label.authorityWritable'),
    shortLabel: 'W',
    value: 'writable'
  }, {
    label: $i18nBundle('common.label.authorityDeletable'),
    shortLabel: 'D',
    value: 'deletable'
  }]
})

const normalizeProjectAuthorities = (authorities) => {
  if (Array.isArray(authorities)) {
    return authorities.filter(Boolean)
  }
  if (typeof authorities === 'string') {
    return authorities.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

const getProjectAuthorityItems = (authorities) => {
  return normalizeProjectAuthorities(authorities).map(authority => {
    return projectAuthorityOptions.value.find(item => item.value === authority) || {
      label: authority,
      shortLabel: authority.slice(0, 1).toUpperCase(),
      value: authority
    }
  })
}

const getProjectAuthorityCode = (authorities) => {
  return getProjectAuthorityItems(authorities).map(item => item.shortLabel).join('') || '-'
}

const getProjectAuthorityTooltip = (authorities) => {
  const authorityItems = getProjectAuthorityItems(authorities)
  if (!authorityItems.length) {
    return $i18nBundle('common.label.authorityForbidden')
  }
  return authorityItems.map(item => `${item.shortLabel}: ${item.label}`).join('<br/>')
}

const tableProjectItems = computed(() => {
  return sortProjects(tableData.value).map(project => {
    const defaultProject = isDefaultProject(project.projectCode)
    const publicProject = !!project.publicFlag
    const editable = !defaultProject && checkProjectEdit(project)
    return {
      projectUsers: project.projectUsers || [],
      defaultProject,
      project,
      projectItems: [{
        labelKey: 'mock.label.owner',
        enabled: !!project.userName && project.userName !== useCurrentUserName(),
        formatter () {
          return <ElText class="margin-left1" type="success" tag="b">{project.userName}</ElText>
        }
      }, {
        labelKey: 'common.label.status',
        formatter () {
          const groupCount = Number(project.groupCount) || 0
          const gotoProjectGroups = (event) => {
            event?.stopPropagation()
            if (project.status === 1) {
              gotoMockGroups(project)
            }
          }
          return <>
            <DelFlagTag v-model={project.status} clickToToggle={editable}
                               onToggleValue={(status) => saveProjectItem({ ...project, status })} />
            {publicProject
              ? <ElTag
                  v-common-tooltip={$i18nBundle(editable ? 'mock.label.cancelPublic' : 'mock.label.public')}
                  type="primary"
                  class={editable ? 'margin-left1 pointer' : 'margin-left1'}
                  onClick={editable ? ($event) => cancelProjectPublicFlag(project, $event) : undefined}
                >
                  {$i18nBundle('mock.label.public')}
                </ElTag>
              : ''}
            {groupCount > 0
              ? <ElTag
                  v-common-tooltip={$i18nBundle('mock.label.mockGroupCount')}
                  type="primary"
                  size="small"
                  effect="plain"
                  class={project.status === 1 ? 'margin-left1 pointer' : 'margin-left1'}
                  round={true}
                  onClick={gotoProjectGroups}
                >
                  {groupCount}
                </ElTag>
              : ''}
          </>
        }
      }, {
        labelKey: 'common.label.createDate',
        enabled: !!project.createDate || !!project.modifyDate,
        formatter () {
          let modifyStr = ''
          const format = 'YYYY-MM-DD HH:mm'
          if (project.modifyDate) {
            modifyStr = <span v-common-tooltip={$i18nBundle('common.label.modifyDate')}>
              <CommonIcon icon="EditCalendarFilled" size={20} class="margin-left1" style="top: 4px;"/>
              {formatDate(project.modifyDate, format)}
            </span>
          }
          return <>
            <span v-common-tooltip={$i18nBundle('common.label.createDate')}>
            <CommonIcon icon="CalendarMonthFilled" size={20} style="top: 4px;"/>
            {formatDate(project.createDate, format)}
            </span>
            {modifyStr}
          </>
        }
      }, {
        labelKey: 'common.label.description',
        value: project.description,
        enabled: !!project.description
      }]
    }
  })
})

const dataRows = computed(() => {
  return chunk(tableProjectItems.value, colSize.value)
})

const selectedRows = computed(() => tableProjectItems.value.map(item => item.project).filter(project => project?.selected))

const showProjectUserWindow = ref(false)
const projectUserManageTarget = ref(null)
const projectListRef = ref()

const toManageUsers = (project) => {
  projectUserManageTarget.value = project
  showProjectUserWindow.value = true
}

const copyToModel = ref({
  action: 'copy',
  projectId: null,
  userName: useCurrentUserName()
})
const showCopyToWindow = ref(false)
const sourceProjectUserName = ref('')
const allowMoveProjectTransfer = ref(false)
const transferActionOptions = computed(() => {
  const options = [{
    label: $i18nBundle('common.label.copy'),
    value: 'copy'
  }]
  if (allowMoveProjectTransfer.value) {
    options.push({
      label: $i18nBundle('common.label.move'),
      value: 'move'
    })
  }
  return options
})
const currentTransferTargetLabel = computed(() => {
  return copyToModel.value.action === 'move'
    ? $i18nBundle('common.label.moveTo')
    : $i18nBundle('common.label.copyTo')
})
const projectTransferWindowTitle = computed(() => {
  return $i18nConcat(currentTransferTargetLabel.value, $i18nBundle('common.label.user'))
})
const copyToOptions = computed(() => {
  return [{
    labelKey: 'common.label.operation',
    prop: 'action',
    type: 'segmented',
    required: true,
    enabled: allowMoveProjectTransfer.value,
    attrs: {
      clearable: false,
      options: transferActionOptions.value
    }
  }, {
    label: $i18nConcat(currentTransferTargetLabel.value, $i18nBundle('common.label.user')),
    prop: 'userName',
    type: 'select',
    required: true,
    enabled: isAdminUser(),
    children: userOptions.value,
    attrs: {
      filterable: true,
      clearable: false
    }
  }]
})
const toCopyProject = (project) => {
  sourceProjectUserName.value = project?.userName || ''
  copyToModel.value.projectId = project.id
  copyToModel.value.userName = useCurrentUserName()
  copyToModel.value.action = 'copy'
  allowMoveProjectTransfer.value = checkProjectDeletable(project)
  if (isAdminUser()) {
    showCopyToWindow.value = true
  } else {
    $coreConfirm($i18nBundle('common.msg.confirmCopy'))
      .then(() => saveTransferProject())
  }
}
const saveTransferProject = () => {
  if (copyToModel.value.action === 'move' && copyToModel.value.userName === sourceProjectUserName.value) {
    ElMessage.error($i18nBundle('common.msg.moveSameUser'))
    return Promise.reject($i18nBundle('common.msg.moveSameUser'))
  }
  return transferMockProject(copyToModel.value, { loading: true })
    .then((data) => {
      if (data?.success) {
        const successKey = copyToModel.value.action === 'move'
          ? 'common.msg.moveSuccess'
          : 'common.msg.copySuccess'
        ElMessage.success($i18nBundle(successKey))
      }
      loadMockProjects()
    })
}
const PROJECT_CARD_MIN_WIDTH = 360
const PROJECT_CARD_GUTTER = 20
const { width: projectListWidth } = useElementSize(projectListRef)
const colSize = computed(() => {
  const availableWidth = projectListWidth.value || 0
  return Math.max(Math.floor((availableWidth + PROJECT_CARD_GUTTER) / (PROJECT_CARD_MIN_WIDTH + PROJECT_CARD_GUTTER)), 1)
})
const pageAttrs = {
  layout: 'total, prev, pager, next',
  background: true
}
</script>

<template>
  <el-container
    class="flex-column"
  >
    <el-page-header
      v-if="publicFlag"
      class="margin-bottom3"
      @back="$goto('/mock/projects')"
    >
      <template #content>
        <span class="text-large font-600 mr-3">
          {{ $t('mock.label.publicMockProjects') }}
          <el-link
            type="primary"
            class="margin-left2"
            @click="$goto('/mock/projects')"
          >
            {{ $i18nKey('common.label.commonBack','mock.label.mockProjects') }}
          </el-link>
        </span>
      </template>
    </el-page-header>
    <common-form
      inline
      :model="searchParam"
      :options="searchFormOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="loadMockProjects"
    >
      <template #buttons>
        <el-button
          v-if="!publicFlag"
          type="info"
          @click="newOrEdit()"
        >
          {{ $t('common.label.new') }}
        </el-button>
        <el-button
          v-if="selectedRows?.length"
          type="danger"
          @click="deleteProjects()"
        >
          {{ $t('common.label.delete') }}
        </el-button>
      </template>
    </common-form>
    <el-empty
      v-if="!dataRows.length"
      v-loading="loading"
      :description="$t('common.msg.noData')"
    />
    <el-container
      v-else
      ref="projectListRef"
      class="flex-column"
    >
      <el-row
        v-for="(dataRow, index) in dataRows"
        :key="index"
        :gutter="20"
        class="project-list-row"
        :class="{'margin-top2': index>0}"
      >
        <el-col
          v-for="{project, projectItems, defaultProject, projectUsers} in dataRow"
          :key="project.id"
          :span="Math.floor(24/colSize)"
          class="project-list-col"
          @mouseenter="project.showOperations=true"
          @mouseleave="project.showOperations=false"
        >
          <el-card
            shadow="hover"
            class="small-card operation-card project-card"
            :class="{
              pointer: project.status===1,
              'project-selected': project.selected,
              'project-disabled': project.status!==1
            }"
            @click="gotoMockGroups(project)"
          >
            <template #header>
              <div class="card-header project-card__header">
                <div class="project-card__title-wrap">
                  <el-checkbox
                    v-model="project.selected"
                    class="project-card__title"
                    :disabled="defaultProject||!checkProjectEdit(project)"
                    @click="$event.stopPropagation()"
                  >
                    <el-text
                      tag="b"
                      class="project-card__name"
                      :type="project.status===1?'':'danger'"
                    >
                      <span class="project-card__name-main">
                        <common-icon
                          v-if="defaultProject"
                          icon="LockFilled"
                          :size="16"
                          class="project-card__name-icon"
                        />
                        <span class="project-card__name-text">
                          {{ project.projectCode===MOCK_DEFAULT_PROJECT?$t('mock.label.defaultProject'):project.projectName }}
                        </span>
                      </span>
                    </el-text>
                  </el-checkbox>
                </div>
                <div
                  class="project-operations"
                  :class="{'project-operations--visible': project.showOperations}"
                  @click.stop
                >
                  <el-button
                    v-if="checkProjectEdit(project)&&!defaultProject"
                    v-common-tooltip="$t('common.label.edit')"
                    type="primary"
                    size="small"
                    round
                    @click="newOrEdit(project.id, $event)"
                  >
                    <common-icon icon="Edit" />
                  </el-button>
                  <el-button
                    v-if="checkProjectEdit(project)&&!defaultProject"
                    v-common-tooltip="$t('common.label.authorities')"
                    type="info"
                    size="small"
                    round
                    @click.stop="toManageUsers(project)"
                  >
                    <common-icon icon="UserFilled" />
                  </el-button>
                  <el-button
                    v-if="!defaultProject"
                    v-common-tooltip="$t('common.label.copy')"
                    type="warning"
                    size="small"
                    round
                    @click.stop="toCopyProject(project)"
                  >
                    <common-icon icon="FileCopyFilled" />
                  </el-button>
                  <el-button
                    v-if="checkProjectEdit(project)&&!defaultProject"
                    v-common-tooltip="$t('common.label.delete')"
                    type="danger"
                    size="small"
                    round
                    @click="deleteProject(project, $event)"
                  >
                    <common-icon icon="DeleteFilled" />
                  </el-button>
                </div>
              </div>
            </template>
            <common-descriptions
              :column="1"
              :min-width="minWidth"
              :items="projectItems"
            />
            <div
              v-if="projectUsers.length"
              class="project-authority-row"
              @click.stop="toManageUsers(project)"
            >
              <span class="project-authority-row__label">
                {{ $t('common.label.authorities') }}
              </span>
              <el-text
                tag="div"
                class="project-authority-row__content"
              >
                <template
                  v-for="(projectUser, userIndex) in projectUsers"
                  :key="`${project.id}-${projectUser.userName}`"
                >
                  <span class="project-authority-inline">
                    <span class="project-authority-inline__name">
                      {{ projectUser.userName }}
                    </span>
                    <el-tag
                      v-common-tooltip="getProjectAuthorityTooltip(projectUser.authorities)"
                      size="small"
                      type="primary"
                      effect="plain"
                      class="project-authority-inline__tag"
                    >
                      {{ getProjectAuthorityCode(projectUser.authorities) }}
                    </el-tag>
                  </span>
                  <span
                    v-if="userIndex < projectUsers.length - 1"
                    class="project-authority-inline__separator"
                  >, </span>
                </template>
              </el-text>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-pagination
        class="project-list-pagination"
        :total="searchParam.page.totalCount"
        :page-size="searchParam.page.pageSize"
        :current-page="searchParam.page.pageNumber"
        v-bind="pageAttrs"
        @current-change="loadMockProjects($event)"
      />
    </el-container>
    <simple-edit-window
      v-model="currentProject"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      :name="$t('mock.label.mockProjects')"
      :save-current-item="saveProjectItem"
      label-width="130px"
      inline-auto-mode
    />
    <simple-edit-window
      v-model="copyToModel"
      v-model:show-edit-window="showCopyToWindow"
      width="500px"
      :form-options="copyToOptions"
      :title="projectTransferWindowTitle"
      :save-current-item="saveTransferProject"
      :show-fullscreen="false"
    />
    <mock-project-user-manage-window
      v-model:show-window="showProjectUserWindow"
      :project="projectUserManageTarget"
      :user-options="userOptions"
      @updated="reloadProjectsAfterAuthorityChange"
    />
  </el-container>
</template>

<style scoped>
.project-list-row {
  align-items: stretch;
}

.project-list-col {
  display: flex;
}

.project-list-pagination {
  justify-content: center;
  margin-top: 28px;
  padding-bottom: 8px;
}

.project-card {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  border: 1px solid var(--el-border-color);
  border-top: 3px solid rgba(64, 158, 255, 0.22);
  background:
    linear-gradient(180deg, rgba(64, 158, 255, 0.06) 0%, rgba(64, 158, 255, 0.01) 96px, transparent 100%),
    var(--el-bg-color-overlay);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  overflow: hidden;
}

.dark .project-card {
  border-color: rgba(255, 255, 255, 0.1);
  border-top-color: rgba(64, 158, 255, 0.5);
  background:
    linear-gradient(180deg, rgba(64, 158, 255, 0.12) 0%, rgba(64, 158, 255, 0.03) 108px, transparent 100%),
    rgba(255, 255, 255, 0.02);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.28);
}

.project-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.16);
  border-color: rgba(64, 158, 255, 0.38);
  border-top-color: var(--el-color-primary);
}

.dark .project-card:hover {
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.36);
}

.project-selected {
  border-color: var(--el-color-primary);
  border-top-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.3), 0 18px 36px rgba(15, 23, 42, 0.18);
}

.project-card :deep(.el-card__header) {
  padding: 14px 18px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.project-card :deep(.el-card__body) {
  padding: 14px 18px 16px;
}

.project-card__header {
  display: flex;
  position: relative;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.project-card__title-wrap {
  flex: 1;
  min-width: 0;
}

.project-card__title {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 100%;
}

.project-card__title :deep(.el-checkbox__input) {
  flex-shrink: 0;
  margin-top: 2px;
}

.project-card__title :deep(.el-checkbox__label) {
  display: block;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  line-height: 1.5;
  padding-left: 10px;
}

.project-card__name {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  line-height: 1.5;
}

.project-card__name-main {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
}

.project-card__name-icon {
  flex-shrink: 0;
  color: var(--el-color-warning);
}

.project-card__name-text {
  display: block;
  flex: 1;
  font-size: 15px;
  font-weight: 700;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-operations {
  display: flex;
  position: absolute;
  top: 0;
  right: 0;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  max-width: calc(100% - 48px);
  flex-shrink: 0;
  z-index: 1;
  padding: 2px 0 2px 16px;
  background: linear-gradient(270deg, var(--el-bg-color-overlay) 70%, transparent 100%);
  opacity: 0;
  transform: translateY(-4px);
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.project-card:hover .project-operations,
.project-card:focus-within .project-operations,
.project-operations--visible {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.project-operations :deep(.el-button) {
  margin-left: 0;
  min-height: 32px;
  padding: 0 10px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.project-disabled {
  opacity: 0.72;
}

.project-disabled:hover {
  transform: none;
  cursor: not-allowed;
}

.project-card :deep(.el-descriptions__label) {
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

.project-card :deep(.el-descriptions__body) {
  background: transparent;
}

.project-card :deep(.el-descriptions__table) {
  table-layout: fixed;
}

.project-card :deep(.el-descriptions__cell) {
  padding-bottom: 6px;
}

.project-card :deep(.el-descriptions__content) {
  line-height: 24px;
}

.project-authority-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color);
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.project-authority-row:hover {
  opacity: 0.85;
}

.project-authority-row__label {
  flex-shrink: 0;
  min-width: 42px;
  font-size: 14px;
  font-weight: 500;
  line-height: 24px;
  color: var(--el-text-color-secondary);
}

.project-authority-row__content {
  flex: 1;
  min-width: 0;
  line-height: 24px;
  color: var(--el-text-color-primary);
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.project-authority-inline {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  vertical-align: middle;
}

.project-authority-inline__name {
  display: inline-block;
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.project-authority-inline__tag {
  margin-left: 4px;
  vertical-align: middle;
}

.project-authority-inline__separator {
  margin-right: 4px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 768px) {
  .project-card {
    border-radius: 14px;
  }

  .project-card :deep(.el-card__header) {
    padding: 14px 14px 10px;
  }

  .project-card :deep(.el-card__body) {
    padding: 12px 14px 14px;
  }

  .project-card__header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .project-operations {
    position: static;
    opacity: 1;
    transform: none;
    pointer-events: auto;
    max-width: none;
    justify-content: flex-start;
    padding-left: 26px;
    background: transparent;
  }

  .project-authority-row {
    flex-direction: column;
    gap: 4px;
  }

  .project-authority-row__label {
    min-width: 0;
    line-height: 20px;
  }
}
</style>

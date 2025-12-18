<script setup lang="jsx">
import { computed, onMounted, onActivated } from 'vue'
import { useDefaultPage } from '@/config'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { useAllUsers } from '@/api/mock/MockUserApi'
import MockProjectApi, { checkProjectEdit, copyMockProject, useProjectEditHook } from '@/api/mock/MockProjectApi'
import { $coreConfirm, $goto, formatDate, isAdminUser, useCurrentUserName } from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle } from '@/messages'
import { useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { chunk } from 'lodash-es'
import CommonIcon from '@/components/common-icon/index.vue'
import { useRoute } from 'vue-router'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { useWindowSize } from '@vueuse/core'
import { ElText, ElTag } from 'element-plus'

const props = defineProps({
  publicFlag: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()

const { search, deleteById, saveOrUpdate } = MockProjectApi

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage(50), publicFlag: props.publicFlag },
  searchMethod: search
})
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
  return loadMockProjects()
})

onMounted(initLoadOnce)

onActivated(initLoadOnce)

const gotoMockGroups = (project) => {
  if (project.status === 1) {
    $goto(`/mock/groups/${props.publicFlag ? 'pubProject' : 'project'}/${project.projectCode}/${project.userName}?backUrl=${route.fullPath}`)
  }
}

//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    enabled: isAdminUser() || props.publicFlag,
    children: userOptions.value,
    attrs: {
      filterable: true,
      clearable: props.publicFlag
    },
    change () {
      loadMockProjects(1)
    }
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
  return saveOrUpdate(item).then(() => loadMockProjects())
}

const minWidth = '100px'

const tableProjectItems = computed(() => {
  return tableData.value.map(project => {
    const defaultProject = isDefaultProject(project.projectCode)
    const publicProject = !!project.publicFlag
    const editable = !defaultProject && checkProjectEdit(project)
    return {
      defaultProject,
      project,
      projectItems: [{
        labelKey: 'common.label.status',
        formatter () {
          return <>
            <DelFlagTag v-model={project.status} clickToToggle={editable}
                               onToggleValue={(status) => saveProjectItem({ ...project, status })} />
            {publicProject
              ? <ElTag type="primary" class="margin-left1">
                  {$i18nBundle('mock.label.public')}
                </ElTag>
              : ''}
          </>
        }
      }, {
        labelKey: 'mock.label.projectCode',
        value: project.projectCode
      }, {
        enabled: !!project.userName && project.userName !== useCurrentUserName(),
        labelFormatter () {
          return <ElText type="primary" tag="b">
            {$i18nBundle('mock.label.owner')}
          </ElText>
        },
        formatter () {
          return <ElText type="primary">
            {project.userName}
          </ElText>
        }
      }, {
        labelKey: 'common.label.modifyDate',
        enabled: !!project.createDate || !!project.modifyDate,
        formatter () {
          let modifyStr = ''
          const format = 'YYYY-MM-DD HH:mm'
          if (project.modifyDate) {
            modifyStr = <>
              <CommonIcon icon="EditCalendarFilled" size={20} class="margin-left1" style="top: 4px;"/>
              {formatDate(project.modifyDate, format)}
            </>
          }
          return <>
            <CommonIcon icon="CalendarMonthFilled" size={20} style="top: 4px;"/>
            {formatDate(project.createDate, format)}
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

const toCopyProject = (project, $event) => {
  $event?.stopPropagation()
  $coreConfirm($i18nBundle('common.msg.confirmCopy'))
    .then(() => copyMockProject(project.id, { loading: true }))
    .then(() => loadMockProjects())
}
const { width } = useWindowSize()
const colSize = computed(() => {
  return Math.floor(width.value / 420) || 1
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
    <common-form
      inline
      :model="searchParam"
      :options="searchFormOptions"
      :submit-label="$t('common.label.search')"
      @submit-form="loadMockProjects"
    >
      <template
        v-if="!publicFlag"
        #buttons
      >
        <el-button
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
      class="flex-column"
    >
      <el-row
        v-for="(dataRow, index) in dataRows"
        :key="index"
        :gutter="20"
        :class="{'margin-top2': index>0}"
      >
        <el-col
          v-for="{project, projectItems, defaultProject} in dataRow"
          :key="project.id"
          :span="Math.floor(24/colSize)"
          @mouseenter="project.showOperations=true"
          @mouseleave="project.showOperations=false"
        >
          <el-card
            shadow="hover"
            class="small-card operation-card"
            style="border-radius: 10px;"
            :class="{pointer: project.status===1, 'project-selected': project.selected}"
            @click="gotoMockGroups(project)"
          >
            <template #header>
              <div class="card-header">
                <el-checkbox
                  v-model="project.selected"
                  style="margin-right: auto;"
                  :disabled="defaultProject||!checkProjectEdit(project)"
                  @click="$event.stopPropagation()"
                >
                  <el-text
                    tag="b"
                    :type="project.status===1?'':'danger'"
                  >
                    <common-icon
                      v-if="defaultProject"
                      icon="LockFilled"
                      :size="16"
                    />
                    {{ project.projectCode===MOCK_DEFAULT_PROJECT?$t('mock.label.defaultProject'):project.projectName }}
                  </el-text>
                </el-checkbox>
                <template v-if="checkProjectEdit(project)&&project.showOperations">
                  <el-button
                    v-if="!defaultProject"
                    v-common-tooltip="$t('common.label.edit')"
                    type="primary"
                    size="small"
                    round
                    @click="newOrEdit(project.id, $event)"
                  >
                    <common-icon icon="Edit" />
                  </el-button>
                  <el-button
                    v-if="!defaultProject"
                    v-common-tooltip="$t('common.label.copy')"
                    type="warning"
                    size="small"
                    round
                    @click="toCopyProject(project, $event)"
                  >
                    <common-icon icon="FileCopyFilled" />
                  </el-button>
                  <el-button
                    v-if="!defaultProject"
                    v-common-tooltip="$t('common.label.delete')"
                    type="danger"
                    size="small"
                    round
                    @click="deleteProject(project, $event)"
                  >
                    <common-icon icon="DeleteFilled" />
                  </el-button>
                </template>
              </div>
            </template>
            <common-descriptions
              :column="1"
              :min-width="minWidth"
              :items="projectItems"
            />
          </el-card>
        </el-col>
      </el-row>
      <el-pagination
        style="justify-content: center;"
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
  </el-container>
</template>

<style scoped>
.project-selected {
  border-color: var(--el-color-primary);
}
</style>

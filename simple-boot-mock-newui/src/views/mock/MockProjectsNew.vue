<script setup lang="jsx">
import { computed, onMounted, onActivated, ref } from 'vue'
import { useDefaultPage } from '@/config'
import { useInitLoadOnce, useTableAndSearchForm } from '@/hooks/CommonHooks'
import { defineFormOptions } from '@/components/utils'
import { useAllUsers } from '@/api/mock/MockUserApi'
import MockProjectApi from '@/api/mock/MockProjectApi'
import { $coreConfirm, $goto, formatDate, isAdminUser, useCurrentUserName } from '@/utils'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import { $i18nBundle } from '@/messages'
import { useFormStatus, useSearchStatus } from '@/consts/GlobalConstants'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { chunk } from 'lodash-es'
import CommonIcon from '@/components/common-icon/index.vue'
import { useRoute } from 'vue-router'
import { isDefaultProject } from '@/consts/MockConstants'

const route = useRoute()

const { search, getById, deleteById, saveOrUpdate } = MockProjectApi

const { tableData, loading, searchParam, searchMethod } = useTableAndSearchForm({
  defaultParam: { page: useDefaultPage(50) },
  searchMethod: search
})
const loadMockProjects = (pageNumber) => searchMethod(pageNumber)
const { userOptions, loadUsersAndRefreshOptions } = useAllUsers(searchParam)

const { initLoadOnce } = useInitLoadOnce(async () => {
  await loadUsersAndRefreshOptions()
  return loadMockProjects()
})

onMounted(initLoadOnce)

onActivated(initLoadOnce)

const gotoMockGroups = (project) => {
  if (project.status === 1) {
    $goto(`/mock/groups/project/${project.projectCode}/${project.userName}?backUrl=${route.fullPath}`)
  }
}

//* ************搜索框**************//
const searchFormOptions = computed(() => {
  return [{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    enabled: isAdminUser(),
    children: userOptions.value,
    attrs: {
      clearable: false
    },
    change () {
      loadMockProjects(1)
    }
  },
  useSearchStatus({ change () { loadMockProjects(1) } }),
  {
    labelKey: 'common.label.keywords',
    prop: 'keyword'
  }]
})

const deleteProject = (project, $event) => {
  $event?.stopPropagation()
  $coreConfirm($i18nBundle('common.msg.commonDeleteConfirm', [project.projectName]))
    .then(() => deleteById(project.id))
    .then(() => loadMockProjects())
}

const deleteProjects = () => {
  $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
    .then(() => MockProjectApi.removeByIds(selectedRows.value.map(item => item.id)), { loading: true })
    .then(() => loadMockProjects())
}

const showEditWindow = ref(false)
const currentProject = ref()
const newOrEdit = async (id, $event) => {
  $event?.stopPropagation()
  if (id) {
    await getById(id).then(data => {
      data.resultData && (currentProject.value = data.resultData)
    })
  } else {
    currentProject.value = {
      status: 1,
      userName: searchParam.value?.userName || useCurrentUserName()
    }
  }
  showEditWindow.value = true
}
const editFormOptions = computed(() => defineFormOptions([{
  labelKey: 'common.label.user',
  prop: 'userName',
  type: 'select',
  enabled: isAdminUser(),
  children: userOptions.value,
  attrs: {
    clearable: false
  }
}, {
  labelKey: 'mock.label.projectCode',
  prop: 'projectCode',
  tooltip: $i18nBundle('mock.msg.projectCodeTooltip'),
  required: true,
  upperCase: true,
  rules: [{
    validator (val) {
      return /[A-Za-z0-9_-]+/.test(val)
    },
    message: $i18nBundle('mock.msg.projectCodeTooltip')
  }]
}, {
  labelKey: 'mock.label.projectName',
  prop: 'projectName',
  required: true
}, useFormStatus(), {
  labelKey: 'common.label.description',
  prop: 'description',
  attrs: {
    type: 'textarea'
  }
}]))
const saveProjectItem = (item) => {
  return saveOrUpdate(item).then(() => loadMockProjects())
}

const colSize = ref(4)
const minWidth = '110px'

const tableProjectItems = computed(() => {
  return tableData.value.map(project => {
    const defaultProject = isDefaultProject(project.projectCode)
    return {
      defaultProject,
      project,
      projectItems: [{
        labelKey: 'mock.label.projectCode',
        formatter () {
          return <>
            <span class="margin-right2">{project.projectCode}</span>
            <DelFlagTag v-model={project.status} clickToToggle={!defaultProject}
                        onToggleValue={(status) => saveProjectItem({ ...project, status })} />
          </>
        }
      }, {
        labelKey: 'common.label.createDate',
        value: formatDate(project.createDate),
        enabled: !!project.createDate
      }, {
        labelKey: 'common.label.modifyDate',
        value: formatDate(project.modifyDate),
        enabled: !!project.modifyDate
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
      <template #buttons>
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
          :span="6"
        >
          <el-card
            shadow="hover"
            class="small-card operation-card"
            :class="{pointer: project.status===1, 'project-selected': project.selected}"
            @click="gotoMockGroups(project)"
          >
            <template #header>
              <div class="card-header">
                <el-checkbox
                  v-model="project.selected"
                  style="margin-right: auto;"
                  :disabled="defaultProject"
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
                    {{ project.projectName }}
                  </el-text>
                </el-checkbox>
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
                  v-common-tooltip="$t('common.label.delete')"
                  type="danger"
                  size="small"
                  round
                  @click="deleteProject(project, $event)"
                >
                  <common-icon icon="DeleteFilled" />
                </el-button>
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
    </el-container>
    <simple-edit-window
      v-model="currentProject"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      :name="$t('mock.label.mockProjects')"
      :save-current-item="saveProjectItem"
      label-width="130px"
    />
  </el-container>
</template>

<style scoped>
.project-selected {
  border-color: var(--el-color-primary);
}
</style>

import MockProjectApi, { selectProjects, sortProjects } from '@/api/mock/MockProjectApi'
import { getStyleGrow, isAdminUser, useCurrentUserName } from '@/utils'
import { useFormStatus } from '@/consts/GlobalConstants'
import { computed, ref } from 'vue'
import { defineFormOptions } from '@/components/utils'
import { $i18nBundle } from '@/messages'
import { isDefaultProject, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { ElText } from 'element-plus'

export const assignProjectValue = (target, project) => {
  if (!target) {
    return
  }
  if (!project) {
    target.projectId = null
    target.projectCode = null
    return
  }
  const defaultProject = isDefaultProject(project.projectCode)
  target.projectId = defaultProject ? null : (project.projectId || project.id || null)
  target.projectCode = project.projectCode || (defaultProject ? MOCK_DEFAULT_PROJECT : null)
}

export const findProjectByValue = (projects = [], searchParam = {}) => {
  if (searchParam?.projectId != null) {
    return projects.find(proj => `${proj.id}` === `${searchParam.projectId}`)
  }
  if (searchParam?.projectCode) {
    return projects.find(proj => proj.projectCode === searchParam.projectCode)
  }
  return null
}

export const findProjectByOption = (projects = [], option = {}) => {
  if (option?.projectId != null) {
    return projects.find(project => `${project.id}` === `${option.projectId}`)
  }
  if (option?.projectCode) {
    return projects.find(project => project.projectCode === option.projectCode)
  }
  return null
}

export const filterProjectOptionsByAuthority = (projects = [], options = [], authorityChecker) => {
  if (typeof authorityChecker !== 'function') {
    return [...options]
  }
  return options.filter(option => {
    const project = findProjectByOption(projects, option)
    return project && authorityChecker(project)
  })
}

export const useSelectProjects = (searchParam, autoSelect) => {
  const projects = ref([])
  const projectOptions = ref([])
  const loadSelectProjects = (data, config) => {
    return selectProjects(data, config).then(result => {
      projects.value = sortProjects(result || [])
      projectOptions.value = projects.value.map(project => {
        if (project.projectCode === MOCK_DEFAULT_PROJECT) {
          return {
            labelKey: 'mock.label.defaultProject',
            value: project.projectCode,
            projectCode: project.projectCode,
            projectId: null,
            userName: project.userName
          }
        }
        const label = project.userName && project.userName !== useCurrentUserName()
          ? `${project.projectName} - ${project.userName}`
          : project.projectName
        const labelComp = () => {
          if (project.labelKey) {
            return $i18nBundle(project.labelKey)
          }
          if (project.userName && project.userName !== useCurrentUserName()) {
            return <>
              {project.projectName}
              <ElText class="margin-left1" type="success" tag="b"
                      v-common-tooltip={$i18nBundle('mock.label.owner')}>({project.userName})</ElText>
            </>
          }
          return project.projectName
        }
        return {
          label,
          labelComp,
          value: project.projectCode,
          userName: project.userName,
          projectId: project.id,
          projectCode: project.projectCode,
          slots: {
            default: labelComp
          }
        }
      })
    })
  }
  const loadProjectsAndRefreshOptions = async () => {
    await loadSelectProjects({
      userName: searchParam.value?.userName || useCurrentUserName(),
      publicFlag: searchParam.value?.publicFlag,
      projectId: searchParam.value?.projectId || undefined,
      onlyMine: searchParam.value?.onlyMine || undefined
    })
    const currentProj = findProjectByValue(projects.value, searchParam.value)
    if (autoSelect) {
      assignProjectValue(searchParam.value, currentProj || { projectCode: MOCK_DEFAULT_PROJECT, projectId: null })
      if (!searchParam.value.projectCode) {
        searchParam.value.projectCode = MOCK_DEFAULT_PROJECT
      }
    } else if (currentProj?.projectCode) {
      assignProjectValue(searchParam.value, currentProj)
    } else {
      searchParam.value.projectId = null
      searchParam.value.projectCode = null
    }
  }
  return {
    projects,
    projectOptions,
    loadSelectProjects,
    loadProjectsAndRefreshOptions
  }
}
/**
 * 编辑项目的工具
 *
 * @param searchParam
 * @param userOptions
 * @returns {{showEditWindow: Ref<UnwrapRef<boolean>, UnwrapRef<boolean> | boolean>, newOrEditProject: ((function(*, *): Promise<void>)|*), editFormOptions: ComputedRef<CommonFormOption|CommonFormOption[]>, currentProject: Ref<any>}}
 */
export const useProjectEditHook = (searchParam, userOptions) => {
  const showEditWindow = ref(false)
  const currentProject = ref()
  const newOrEditProject = async (id, $event) => {
    $event?.stopPropagation()
    if (id) {
      await MockProjectApi.getById(id).then(data => {
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
    tooltipFunc: () => {
      if (currentProject.value) {
        currentProject.value.projectCodeEditable = !currentProject.value.projectCodeEditable
      }
    },
    disabled: !currentProject.value?.projectCodeEditable,
    enabled: !!currentProject.value?.id,
    rules: [{
      validator (_, value) {
        return !value || /^[A-Z0-9_-]+$/ig.test(value)
      },
      message: $i18nBundle('mock.msg.projectCodeTooltip')
    }],
    attrs: {
      clearable: true
    }
  }, {
    labelKey: 'mock.label.projectName',
    prop: 'projectName',
    required: true
  }, { ...useFormStatus(), style: getStyleGrow(4) }, {
    labelKey: 'mock.label.publicMockProject',
    style: getStyleGrow(6),
    prop: 'publicFlag',
    type: 'switch'
  }, {
    labelKey: 'common.label.description',
    prop: 'description',
    attrs: {
      type: 'textarea'
    }
  }]))
  return {
    currentProject,
    newOrEditProject,
    showEditWindow,
    editFormOptions
  }
}

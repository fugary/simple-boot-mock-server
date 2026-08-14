<script setup lang="jsx">
import { ref, computed, watch } from 'vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { $coreAlert, $coreConfirm, $coreError, formatFileSize, getStyleGrow, isAdminUser } from '@/utils'
import { defineFormOptions } from '@/components/utils'
import { ElButton } from 'element-plus'
import { IMPORT_DUPLICATE_STRATEGY, IMPORT_TYPES, detectImportFileType, uploadFiles } from '@/api/mock/MockGroupApi'
import { DEFAULT_MAX_IMPORT_FILE_SIZE, MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { $i18nBundle, $i18nKey } from '@/messages'
import MockProjectApi from '@/api/mock/MockProjectApi'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useProjectEditHook } from '@/hooks/mock/MockProjectHooks'

const props = defineProps({
  defaultUser: {
    type: String,
    default: ''
  },
  userOptions: {
    type: Array,
    default: () => []
  },
  defaultProject: {
    type: String,
    default: MOCK_DEFAULT_PROJECT
  },
  defaultProjectId: {
    type: Number,
    default: null
  },
  projectOptions: {
    type: Array,
    default: () => []
  }
})
const showWindow = defineModel('modelValue', { type: Boolean, default: false })
const accountInfo = useLoginConfigStore().accountInfo
const importModel = ref({
  userName: !isAdminUser() ? accountInfo?.userName : (props.defaultUser || accountInfo?.userName),
  type: 'simple',
  projectId: props.defaultProjectId,
  projectCode: props.defaultProject,
  duplicateStrategy: IMPORT_DUPLICATE_STRATEGY[0].value,
  singleGroup: true // 调整为默认为单个组
})
const detectedType = ref(null)
const userChangedTypeManually = ref(false)

watch(() => props.defaultUser, (val) => {
  importModel.value.userName = !isAdminUser() ? accountInfo?.userName : (val || accountInfo?.userName)
})
watch(() => props.defaultProject, (val) => {
  importModel.value.projectCode = val
})
watch(() => props.defaultProjectId, (val) => {
  importModel.value.projectId = val
})
watch(showWindow, (val) => {
  if (val && !importFiles.value?.length) {
    detectedType.value = null
    userChangedTypeManually.value = false
  }
})

const importFiles = ref([])
const calcUserOptions = computed(() => props.userOptions)
const { showEditWindow: showEditProjectWindow, currentProject, newOrEditProject, editFormOptions: editProjectFormOptions } = useProjectEditHook(importModel, calcUserOptions)
const saveProjectItem = (item) => {
  return MockProjectApi.saveOrUpdate(item).then(data => emit('updateProjects', data?.resultData || item, importModel))
}

const getFormatLabel = (type) => {
  const item = IMPORT_TYPES.find(opt => opt.value === type)
  return item ? $i18nBundle(item.labelKey) : type
}

const onFileListUpdate = async (files) => {
  const validFiles = []
  const oversizedFiles = []
  for (const file of files || []) {
    const rawFile = file.raw || file
    if (rawFile?.size && rawFile.size > DEFAULT_MAX_IMPORT_FILE_SIZE) {
      oversizedFiles.push(file)
    } else {
      validFiles.push(file)
    }
  }
  if (oversizedFiles.length > 0) {
    const detail = oversizedFiles
      .map(f => `${f.name} (${formatFileSize(f.size || f.raw?.size)})`)
      .join(', ')
    $coreError($i18nBundle('mock.msg.importFileSizeExceed', [detail, formatFileSize(DEFAULT_MAX_IMPORT_FILE_SIZE)]))
  }
  importFiles.value = validFiles
  if (validFiles.length) {
    const firstFile = validFiles[0]?.raw || validFiles[0]
    const detected = await detectImportFileType(firstFile)
    detectedType.value = detected
    if (detected && !userChangedTypeManually.value) {
      importModel.value.type = detected
    }
  } else {
    detectedType.value = null
  }
}

const formOptions = computed(() => {
  const fileLimits = 3
  const maxFileSizeStr = formatFileSize(DEFAULT_MAX_IMPORT_FILE_SIZE)
  return defineFormOptions([{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    disabled: !isAdminUser(),
    children: props.userOptions,
    attrs: {
      clearable: false
    },
    change (value) {
      importModel.value.projectId = null
      importModel.value.projectCode = MOCK_DEFAULT_PROJECT
      emit('changedUser', value)
    }
  }, {
    labelKey: 'mock.label.project',
    prop: 'projectCode',
    type: 'select',
    children: props.projectOptions,
    attrs: {
      clearable: false
    },
    change (value) {
      const option = props.projectOptions.find(item => item.value === value)
      importModel.value.projectId = option?.projectId || null
      importModel.value.projectCode = option?.projectCode || value || null
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
  }, {
    labelKey: 'mock.label.source',
    prop: 'type',
    type: 'select',
    children: IMPORT_TYPES.filter(option => option.enabled !== false)
      .map(option => ({ ...option, label: $i18nBundle(option.labelKey) })),
    attrs: {
      clearable: false
    },
    change () {
      userChangedTypeManually.value = true
    }
  }, {
    labelKey: 'mock.label.duplicateStrategy',
    prop: 'duplicateStrategy',
    type: 'select',
    children: IMPORT_DUPLICATE_STRATEGY,
    tooltip: $i18nBundle('mock.msg.duplicateStrategy'),
    attrs: {
      clearable: false
    }
  }, {
    labelKey: 'mock.label.combineSingleGroup',
    prop: 'singleGroup',
    style: getStyleGrow(4),
    type: 'switch',
    enabled: ['swagger', 'postman', 'har'].includes(importModel.value.type),
    tooltip: $i18nBundle('mock.msg.combineSingleGroup'),
    attrs: {
      activeValue: true,
      inactiveValue: false,
      activeText: $i18nBundle('common.label.yes'),
      inactiveText: $i18nBundle('common.label.no')
    }
  }, {
    labelKey: 'mock.label.groupName',
    enabled: ['swagger', 'postman', 'har'].includes(importModel.value.type) && importModel.value.singleGroup,
    prop: 'groupName',
    style: getStyleGrow(6)
  }, {
    labelKey: 'mock.label.importFile',
    type: 'upload',
    attrs: {
      style: 'width: 100%',
      fileList: importFiles.value,
      'onUpdate:fileList': onFileListUpdate,
      multiple: true,
      limit: fileLimits,
      showFileList: true,
      autoUpload: false,
      onExceed () {
        $coreError($i18nBundle('common.msg.exceedFiles'))
      }
    },
    slots: {
      trigger () {
        return <>
          <ElButton type="primary">{$i18nBundle('mock.label.selectFile')}</ElButton>
          <span className="margin-left2">{$i18nBundle('mock.msg.importFileLimit', [maxFileSizeStr, fileLimits])}</span>
        </>
      }
    }
  }]).map(option => {
    const style = { ...getStyleGrow(10), ...option.style || {} }
    return { ...option, style }
  })
})

const emit = defineEmits(['import-success', 'updateProjects', 'changedUser'])

const executeUpload = () => {
  return uploadFiles(importFiles.value, importModel.value, {
    loading: true
  }).then(data => {
    if (data.success) {
      $coreAlert($i18nBundle('mock.msg.importFileSuccess', [data.resultData]))
      showWindow.value = false
      emit('import-success', data)
    }
  })
}

const doImportGroups = () => {
  if (!importFiles.value?.length) {
    $coreError($i18nBundle('mock.msg.importFileNoFile'))
    return false
  }
  const oversizedFile = importFiles.value.find(file => {
    const raw = file.raw || file
    return raw?.size && raw.size > DEFAULT_MAX_IMPORT_FILE_SIZE
  })
  if (oversizedFile) {
    $coreError($i18nBundle('mock.msg.importFileSizeExceed', [
      `${oversizedFile.name} (${formatFileSize(oversizedFile.size || oversizedFile.raw?.size)})`,
      formatFileSize(DEFAULT_MAX_IMPORT_FILE_SIZE)
    ]))
    return false
  }
  if (detectedType.value && detectedType.value !== importModel.value.type) {
    $coreConfirm(
      $i18nBundle('mock.msg.importTypeMismatchConfirm', [getFormatLabel(detectedType.value), getFormatLabel(importModel.value.type)]),
      $i18nBundle('common.label.reminder'),
      {
        confirmButtonText: $i18nBundle('mock.msg.importTypeSwitchAndImport'),
        cancelButtonText: $i18nBundle('mock.msg.importTypeContinueDirectly'),
        distinguishCancelAndClose: true
      }
    ).then(() => {
      importModel.value.type = detectedType.value
      executeUpload()
    }).catch((action) => {
      if (action === 'cancel') {
        executeUpload()
      }
    })
    return false
  }
  return executeUpload()
}

</script>

<template>
  <common-window
    v-model="showWindow"
    :title="$t('mock.msg.importFileTitle')"
    append-to-body
    destroy-on-close
    width="800px"
    :ok-click="doImportGroups"
  >
    <el-container class="flex-column">
      <el-alert
        v-if="detectedType && detectedType !== importModel.type"
        type="warning"
        :closable="false"
        show-icon
        class="margin-bottom2"
      >
        <template #title>
          <div class="flex-center">
            <span>{{ $t('mock.msg.importTypeMismatchWarning', [getFormatLabel(detectedType), getFormatLabel(importModel.type)]) }}</span>
            <el-button
              link
              type="primary"
              class="margin-left2"
              @click="importModel.type = detectedType"
            >
              {{ $t('mock.msg.importTypeSwitchTo', [getFormatLabel(detectedType)]) }}
            </el-button>
          </div>
        </template>
      </el-alert>
      <el-alert
        v-else-if="detectedType && detectedType === importModel.type"
        type="success"
        :closable="false"
        show-icon
        class="margin-bottom2"
        :title="$t('mock.msg.importTypeAutoDetected', [getFormatLabel(detectedType)])"
      />
      <common-form
        label-width="150px"
        class="form-edit-width-90"
        class-name="common-form-auto"
        :options="formOptions"
        :show-buttons="false"
        :model="importModel"
        v-bind="$attrs"
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

<script setup lang="jsx">
import { ref, computed, watch } from 'vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { $coreAlert, $coreConfirm, $coreError, formatFileSize, getStyleGrow, isAdminUser } from '@/utils'
import { defineFormOptions } from '@/components/utils'
import { ElButton } from 'element-plus'
import { IMPORT_DUPLICATE_STRATEGY, IMPORT_TYPES, detectImportFileType, loadImportConfig, uploadFiles } from '@/api/mock/MockGroupApi'
import { DEFAULT_MAX_IMPORT_FILE_SIZE, DEFAULT_MAX_IMPORT_REQUEST_SIZE, MOCK_DEFAULT_PROJECT, SUPPORTED_IMPORT_FILE_ACCEPT, SUPPORTED_IMPORT_FILE_EXTS } from '@/consts/MockConstants'
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
const maxFileSize = ref(DEFAULT_MAX_IMPORT_FILE_SIZE)
const maxRequestSize = ref(DEFAULT_MAX_IMPORT_REQUEST_SIZE)
let configPromise
const refreshImportConfig = () => {
  configPromise = loadImportConfig().then(data => {
    if (data.success) {
      maxFileSize.value = data.resultData.maxFileSize
      maxRequestSize.value = data.resultData.maxRequestSize
    }
  }).catch(() => {}) // 旧服务端保持默认值，最终由服务端校验
  return configPromise
}
const formatLimit = size => size < 0 ? $i18nBundle('mock.label.importUnlimited') : formatFileSize(size)
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
  if (val) {
    refreshImportConfig()
  }
  if (val && !importFiles.value?.length) {
    detectedType.value = null
    userChangedTypeManually.value = false
  }
})

const importFiles = ref([])
if (showWindow.value) {
  refreshImportConfig()
}
const calcUserOptions = computed(() => props.userOptions)
const { showEditWindow: showEditProjectWindow, currentProject, newOrEditProject, editFormOptions: editProjectFormOptions } = useProjectEditHook(importModel, calcUserOptions)
const saveProjectItem = (item) => {
  return MockProjectApi.saveOrUpdate(item).then(data => emit('updateProjects', data?.resultData || item, importModel))
}

const getFormatLabel = (type) => {
  const item = IMPORT_TYPES.find(opt => opt.value === type)
  return item ? $i18nBundle(item.labelKey) : type
}

const getFileName = (file) => file?.name || file?.raw?.name || ''
const isSupportedFile = (file) => {
  const lowerName = getFileName(file).toLowerCase()
  return SUPPORTED_IMPORT_FILE_EXTS.some(ext => lowerName.endsWith(ext))
}
const isOversizedFile = (file) => {
  const raw = file?.raw || file
  return maxFileSize.value >= 0 && raw?.size > maxFileSize.value
}

const onFileListUpdate = async (files) => {
  await (configPromise || refreshImportConfig())
  const validFiles = []
  const oversizedFiles = []
  const invalidExtFiles = []
  for (const file of files || []) {
    if (!isSupportedFile(file)) {
      invalidExtFiles.push(file)
    } else if (isOversizedFile(file)) {
      oversizedFiles.push(file)
    } else {
      validFiles.push(file)
    }
  }
  if (invalidExtFiles.length > 0) {
    const detail = invalidExtFiles.map(getFileName).join(', ')
    $coreError($i18nBundle('mock.msg.importFileTypeInvalid', [detail, SUPPORTED_IMPORT_FILE_EXTS.join(', ')]))
  }
  if (oversizedFiles.length > 0) {
    const detail = oversizedFiles
      .map(f => `${getFileName(f)} (${formatFileSize(f.size || f.raw?.size)})`)
      .join(', ')
    $coreError($i18nBundle('mock.msg.importFileSizeExceed', [detail, formatLimit(maxFileSize.value)]))
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
  const maxFileSizeStr = formatLimit(maxFileSize.value)
  const supportedExtsStr = SUPPORTED_IMPORT_FILE_EXTS.join('/')
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
      accept: SUPPORTED_IMPORT_FILE_ACCEPT,
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
          <span className="margin-left2">{$i18nBundle('mock.msg.importFileLimit', [supportedExtsStr, maxFileSizeStr, fileLimits, formatLimit(maxRequestSize.value)])}</span>
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

const validateAndImportGroups = async () => {
  await (configPromise || refreshImportConfig())
  if (!importFiles.value?.length) {
    $coreError($i18nBundle('mock.msg.importFileNoFile'))
    return false
  }
  const invalidExtFile = importFiles.value.find(file => !isSupportedFile(file))
  if (invalidExtFile) {
    $coreError($i18nBundle('mock.msg.importFileTypeInvalid', [
      getFileName(invalidExtFile),
      SUPPORTED_IMPORT_FILE_EXTS.join(', ')
    ]))
    return false
  }
  const oversizedFile = importFiles.value.find(file => isOversizedFile(file))
  if (oversizedFile) {
    $coreError($i18nBundle('mock.msg.importFileSizeExceed', [
      `${getFileName(oversizedFile)} (${formatFileSize(oversizedFile.size || oversizedFile.raw?.size)})`,
      formatLimit(maxFileSize.value)
    ]))
    return false
  }
  const totalSize = importFiles.value.reduce((total, file) => total + (file.raw?.size ?? file.size ?? 0), 0)
  if (maxRequestSize.value >= 0 && totalSize >= maxRequestSize.value) {
    $coreError($i18nBundle('mock.msg.importRequestSizeExceed', [formatLimit(maxRequestSize.value)]))
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

// common-window 使用同步返回值决定是否关闭，成功上传后再主动关闭。
const doImportGroups = () => {
  validateAndImportGroups()
  return false
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

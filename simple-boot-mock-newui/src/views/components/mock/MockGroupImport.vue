<script setup lang="jsx">
import { ref, computed, watch } from 'vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { $coreAlert, $coreError, isAdminUser } from '@/utils'
import { defineFormOptions } from '@/components/utils'
import { ElButton } from 'element-plus'
import { IMPORT_DUPLICATE_STRATEGY, IMPORT_TYPES, uploadFiles } from '@/api/mock/MockGroupApi'
import { MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'
import { $i18nBundle, $i18nKey } from '@/messages'
import MockProjectApi, { useProjectEditHook } from '@/api/mock/MockProjectApi'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'

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
  projectCode: props.defaultProject,
  duplicateStrategy: IMPORT_DUPLICATE_STRATEGY[0].value
})
watch(() => props.defaultUser, (val) => {
  importModel.value.userName = !isAdminUser() ? accountInfo?.userName : (val || accountInfo?.userName)
})
watch(() => props.defaultProject, (val) => {
  importModel.value.projectCode = val
})
const importFiles = ref([])
const calcUserOptions = computed(() => props.userOptions)
const { showEditWindow: showEditProjectWindow, currentProject, newOrEditProject, editFormOptions: editProjectFormOptions } = useProjectEditHook(importModel, calcUserOptions)
const saveProjectItem = (item) => {
  return MockProjectApi.saveOrUpdate(item).then(() => emit('updateProjects', item, importModel))
}
const formOptions = computed(() => {
  const fileLimits = 3
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
    type: 'segmented',
    attrs: {
      clearable: false,
      options: IMPORT_TYPES.filter(option => option.enabled !== false)
        .map(option => ({ ...option, label: $i18nBundle(option.labelKey) }))
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
    type: 'switch',
    enabled: importModel.value.type === 'swagger',
    tooltip: $i18nBundle('mock.msg.combineSingleGroup'),
    attrs: {
      activeValue: true,
      inactiveValue: false,
      activeText: $i18nBundle('common.label.yes'),
      inactiveText: $i18nBundle('common.label.no')
    }
  }, {
    labelKey: 'mock.label.importFile',
    type: 'upload',
    attrs: {
      style: 'width: 100%',
      fileList: importFiles.value,
      'onUpdate:fileList': (files) => {
        importFiles.value = files
      },
      multiple: true,
      limit: fileLimits,
      showFileList: true,
      autoUpload: false,
      onExceed () {
        // importFiles.value = [...files.map(file => ({
        //   name: file.name,
        //   status: 'ready',
        //   size: file.size,
        //   raw: file
        // }))] // 文件覆盖
        $coreError($i18nBundle('common.msg.exceedFiles'))
      }
    },
    slots: {
      trigger () {
        return <>
          <ElButton type="primary">{$i18nBundle('mock.label.selectFile')}</ElButton>
          <span className="margin-left2">{$i18nBundle('mock.msg.importFileLimit', [fileLimits])}</span>
        </>
      }
    }
  }])
})
const emit = defineEmits(['import-success', 'updateProjects', 'changedUser'])
const doImportGroups = () => {
  if (importFiles.value?.length) {
    uploadFiles(importFiles.value, importModel.value, {
      loading: true
    }).then(data => {
      if (data.success) {
        $coreAlert($i18nBundle('mock.msg.importFileSuccess', [data.resultData]))
        showWindow.value = false
        emit('import-success', data)
      }
    })
  } else {
    $coreError($i18nBundle('mock.msg.importFileNoFile'))
  }
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
      <common-form
        label-width="150px"
        class="form-edit-width-90"
        :options="formOptions"
        :show-buttons="false"
        :model="importModel"
        v-bind="$attrs"
      />
      <simple-edit-window
        v-model="currentProject"
        v-model:show-edit-window="showEditProjectWindow"
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

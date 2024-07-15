<script setup lang="jsx">
import { ref, computed, watch } from 'vue'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { $coreAlert, $coreError, isAdminUser } from '@/utils'
import { defineFormOptions } from '@/components/utils'
import { ElButton } from 'element-plus'
import { IMPORT_DUPLICATE_STRATEGY, IMPORT_TYPES, uploadFiles } from '@/api/mock/MockGroupApi'
import { MOCK_DEFAULT_PROJECT } from '@/consts/MockConstants'

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
const formOptions = computed(() => {
  return defineFormOptions([{
    labelKey: 'common.label.user',
    prop: 'userName',
    type: 'select',
    disabled: !isAdminUser(),
    children: props.userOptions,
    attrs: {
      clearable: false
    }
  }, {
    label: '项目',
    prop: 'projectCode',
    type: 'select',
    enabled: props.projectOptions.length > 1,
    children: props.projectOptions,
    attrs: {
      clearable: false
    }
  }, {
    label: '数据来源',
    prop: 'type',
    type: 'select',
    children: IMPORT_TYPES,
    attrs: {
      clearable: false
    }
  }, {
    label: '重复路径处理',
    prop: 'duplicateStrategy',
    type: 'select',
    children: IMPORT_DUPLICATE_STRATEGY,
    tooltip: '路径是全局唯一的，所有用户共享，因此通常为自动生成的uuid',
    attrs: {
      clearable: false
    }
  }, {
    label: '导入文件',
    type: 'upload',
    attrs: {
      fileList: importFiles.value,
      'onUpdate:fileList': (files) => {
        importFiles.value = files
      },
      limit: 1,
      showFileList: false,
      autoUpload: false,
      onExceed (files) {
        importFiles.value = [...files.map(file => ({
          name: file.name,
          status: 'ready',
          size: file.size,
          raw: file
        }))] // 文件覆盖
      }
    },
    slots: {
      trigger () {
        return <>
          <ElButton type="primary">选择文件</ElButton>
          <span style="display: inline-block; margin-left: 10px;">{importFiles.value?.[0]?.name}</span>
        </>
      },
      tip: () => <div className="el-upload__tip">文件大小最大限制为5MB</div>
    }
  }])
})
const emit = defineEmits(['import-success'])
const doImportGroups = () => {
  if (importFiles.value?.length) {
    uploadFiles(importFiles.value, importModel.value, {
      loading: true
    }).then(data => {
      if (data.success) {
        $coreAlert(`导入成功，共${data.resultData}条`)
        showWindow.value = false
        emit('import-success', data)
      }
    })
  } else {
    $coreError('请选择导入文件')
  }
  return false
}

</script>

<template>
  <common-window
    v-model="showWindow"
    title="导入mock数据"
    append-to-body
    destroy-on-close
    width="800px"
    :ok-click="doImportGroups"
  >
    <el-container class="flex-column">
      <common-form
        label-width="130px"
        class="form-edit-width-90"
        :options="formOptions"
        :show-buttons="false"
        :model="importModel"
        v-bind="$attrs"
      />
    </el-container>
  </common-window>
</template>

<style scoped>

</style>

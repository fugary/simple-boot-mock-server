<script setup lang="jsx">
import { onMounted, ref, computed } from 'vue'
import { defineTableColumns, defineFormOptions, defineTableButtons } from '@/components/utils'
import { $coreConfirm, getSingleSelectOptions } from '@/utils'
import MockDataApi, { ALL_STATUS_CODES, ALL_CONTENT_TYPES, markDefault } from '@/api/mock/MockDataApi'
import { useTableAndSearchForm } from '@/hooks/CommonHooks'
import CommonIcon from '@/components/common-icon/index.vue'
import DelFlagTag from '@/views/components/utils/DelFlagTag.vue'
import SimpleEditWindow from '@/views/components/utils/SimpleEditWindow.vue'
import { useFormStatus } from '@/consts/GlobalConstants'
import { useMonacoEditorOptions } from '@/vendors/monaco-editor'
import { previewMockRequest } from '@/utils/DynamicUtils'
import { $i18nBundle } from '@/messages'
import CommonParamsEdit from '@/views/components/utils/CommonParamsEdit.vue'

const props = defineProps({
  groupItem: {
    type: Object,
    required: true
  },
  requestItem: {
    type: Object,
    required: true
  }
})
const columns = defineTableColumns([{
  label: '默认',
  width: '80px',
  formatter (data) {
    return data.defaultFlag ? <CommonIcon icon="Flag"/> : ''
  }
}, {
  label: '状态码',
  property: 'statusCode'
}, {
  label: 'Content Type',
  property: 'contentType'
}, {
  labelKey: 'common.label.status',
  formatter (data) {
    return <DelFlagTag v-model={data.status}/>
  }
}, {
  label: 'Response',
  property: 'responseBody'
}, {
  headerSlot: 'buttonHeader',
  slot: 'buttons',
  width: '300px'
}])
console.log('========================', props.requestItem)
const { tableData, loading, searchMethod: loadMockData } = useTableAndSearchForm({
  defaultParam: { requestId: props.requestItem.id },
  searchMethod: MockDataApi.search,
  saveParam: false
})
onMounted(() => {
  loadMockData()
})
const buttons = defineTableButtons([{
  labelKey: 'common.label.edit',
  type: 'primary',
  click: item => {
    newOrEdit(item.id)
  }
}, {
  labelKey: 'common.label.preview',
  type: 'success',
  click: item => {
    previewMockRequest(props.groupItem, props.groupItem, item)
  }
}, {
  label: '设为默认',
  type: 'success',
  click: item => {
    item.defaultFlag = 1
    markDefault(item).then(() => loadMockData())
  }
}, {
  labelKey: 'common.label.delete',
  type: 'danger',
  click: item => {
    $coreConfirm($i18nBundle('common.msg.deleteConfirm'))
      .then(() => MockDataApi.deleteById(item.id))
      .then(() => loadMockData())
  }
}])
const showEditWindow = ref(false)
const currentDataItem = ref()
const newDataItem = () => ({
  requestId: props.requestItem.id,
  groupId: props.requestItem.groupId,
  status: 1,
  statusCode: ALL_STATUS_CODES[0],
  contentType: ALL_CONTENT_TYPES[0],
  headerParams: []
})
const newOrEdit = async id => {
  if (id) {
    await MockDataApi.getById(id).then(data => {
      data.resultData && (currentDataItem.value = data.resultData)
      contentRef.value = currentDataItem.value?.responseBody
      currentDataItem.value.headerParams = JSON.parse(currentDataItem.value.headers || '[]')
    })
  } else {
    currentDataItem.value = newDataItem()
  }
  showEditWindow.value = true
}
const { contentRef, languageRef, monacoEditorOptions } = useMonacoEditorOptions({ readOnly: false })

const editFormOptions = computed(() => {
  console.log('========================languageRef', languageRef)
  return defineFormOptions([{
    label: '状态码',
    prop: 'statusCode',
    type: 'select',
    children: getSingleSelectOptions(...ALL_STATUS_CODES),
    attrs: {
      clearable: false
    }
  }, {
    label: 'Content Type',
    prop: 'contentType',
    type: 'select',
    children: getSingleSelectOptions(...ALL_CONTENT_TYPES),
    attrs: {
      clearable: false
    }
  }, {
    label: '默认请求',
    prop: 'defaultFlag',
    type: 'switch',
    attrs: {
      activeValue: 1,
      inactiveValue: 0,
      activeText: $i18nBundle('common.label.yes'),
      inactiveText: $i18nBundle('common.label.no')
    }
  }, useFormStatus(), {
    label: '响应头',
    slot: 'headerParams'
  }, {
    label: '响应体',
    type: 'vue-monaco-editor',
    prop: 'responseBody',
    required: true,
    attrs: {
      value: currentDataItem.value?.responseBody,
      'onUpdate:value': (value) => {
        currentDataItem.value.responseBody = value
        contentRef.value = value
      },
      language: languageRef.value,
      height: '200px',
      options: monacoEditorOptions
    }
  }, {
    labelKey: 'common.label.description',
    prop: 'description',
    attrs: {
      type: 'textarea'
    }
  }])
})

const saveMockData = (data) => {
  const dataItem = { ...data }
  dataItem.headers = JSON.stringify(dataItem.headerParams)
  delete dataItem.headerParams
  return MockDataApi.saveOrUpdate(dataItem)
    .then(() => loadMockData())
}
</script>

<template>
  <el-container class="flex-column padding-10">
    <common-table
      :data="tableData"
      :columns="columns"
      :loading="loading"
    >
      <template #buttonHeader>
        {{ $t('common.label.operation') }}
        <el-button
          type="primary"
          size="small"
          @click="newOrEdit()"
        >
          <common-icon icon="Plus" />
        </el-button>
      </template>
      <template #buttons="{item}">
        <template v-for="(button, index) in buttons">
          <el-button
            v-if="button.enabled!==false"
            :key="index"
            :type="button.type"
            :icon="button.icon"
            :size="button.size||'small'"
            :disabled="button.disabled"
            :round="button.round??true"
            :circle="button.circle"
            @click="button.click?.(item)"
          >
            {{ button.label || $t(button.labelKey) }}
          </el-button>
        </template>
      </template>
    </common-table>
    <simple-edit-window
      v-model="currentDataItem"
      v-model:show-edit-window="showEditWindow"
      :form-options="editFormOptions"
      name="Mock数据"
      label-width="120px"
      :save-current-item="saveMockData"
      show-fullscreen
    >
      <template #headerParams="{option}">
        <common-form-control
          :model="currentDataItem"
          :option="option"
        >
          <common-params-edit
            v-model="currentDataItem.headerParams"
            form-prop="headerParams"
          />
        </common-form-control>
      </template>
    </simple-edit-window>
  </el-container>
</template>

<style scoped>

</style>

<script setup lang="jsx">
import { defineFormOptions } from '@/components/utils'
import { computed, ref } from 'vue'
import { getSingleSelectOptions, toFlatKeyValue } from '@/utils'
import { $i18nBundle } from '@/messages'
import { ElMessage, ElButton } from 'element-plus'
import { DEFAULT_HEADERS } from '@/consts/MockConstants'

const props = defineProps({
  formProp: {
    type: String,
    default: 'requestParams'
  },
  nameReadOnly: {
    type: Boolean,
    default: false
  },
  showAddButton: {
    type: Boolean,
    default: true
  },
  showPasteButton: {
    type: Boolean,
    default: true
  },
  showRemoveButton: {
    type: Boolean,
    default: true
  },
  nameKey: {
    type: String,
    default: 'name'
  },
  valueKey: {
    type: String,
    default: 'value'
  },
  headerFlag: {
    type: Boolean,
    default: false
  },
  fileFlag: {
    type: Boolean,
    default: false
  }
})

const params = defineModel('modelValue', {
  type: Array,
  default: () => []
})

params.value.forEach(param => (param.enabled = param.enabled ?? true))

const addRequestParam = () => {
  params.value.push({
    enabled: true
  })
}

const calcPasteParams = value => {
  let calcParams = []
  if (value.startsWith('{')) { // json
    try {
      let objValue = JSON.parse(value)
      if (objValue != null) {
        objValue = toFlatKeyValue(objValue)
        calcParams = Object.keys(objValue).map(key => {
          return {
            enabled: true,
            [props.nameKey]: key,
            [props.valueKey]: objValue[key]
          }
        })
      }
    } catch (e) {
      ElMessage.error(e.message)
    }
  } else {
    if (value.indexOf('?') > -1) {
      value = value.slice(value.indexOf('?') + 1)
    }
    calcParams = new URLSearchParams(value).entries().map(entry => {
      return {
        enabled: true,
        [props.nameKey]: entry[0],
        [props.valueKey]: entry[1]
      }
    })
  }
  return calcParams
}

const showTextModel = ref(false)
const inputTextModel = ref({
  text: ''
})
const inputTextOption = {
  tooltip: $i18nBundle('mock.msg.pasteToProcess'),
  prop: 'text',
  labelWidth: '40px',
  change (value) {
    if (value) {
      const calcParams = calcPasteParams(value)
      params.value = [...calcParams]
      inputTextModel.value.text = ''
    }
  }
}

const paramsOptions = computed(() => {
  return params.value.map((param) => {
    const nvSpan = props.fileFlag ? 7 : 9
    return defineFormOptions([{
      labelWidth: '30px',
      prop: 'enabled',
      disabled: props.nameReadOnly,
      type: 'switch',
      colSpan: 2
    }, {
      label: 'Key',
      prop: props.nameKey,
      required: props.nameReadOnly,
      disabled: props.nameReadOnly,
      type: props.headerFlag ? 'autocomplete' : 'input',
      attrs: {
        fetchSuggestions: (queryString, cb) => {
          const dataList = DEFAULT_HEADERS.filter(item => item.toLowerCase().includes(queryString?.toLowerCase()))
            .map(value => ({ value }))
          cb(dataList)
        },
        triggerOnFocus: false
      },
      colSpan: nvSpan
    }, {
      labelWidth: '1px',
      prop: 'type',
      type: 'select',
      value: 'text',
      children: getSingleSelectOptions('text', 'file'),
      attrs: {
        clearable: false
      },
      enabled: props.fileFlag,
      colSpan: 3,
      change () {
        param[props.valueKey] = param.type === 'text' ? '' : []
      }
    }, {
      label: 'Value',
      prop: props.valueKey,
      required: props.nameReadOnly,
      colSpan: nvSpan,
      enabled: param.type === 'text'
    }, {
      label: 'Files',
      type: 'upload',
      enabled: props.fileFlag && param.type === 'file',
      attrs: {
        fileList: param[props.valueKey],
        'onUpdate:fileList': (files) => {
          param[props.valueKey] = files
        },
        showFileList: true,
        autoUpload: false
      },
      slots: {
        trigger () {
          return <ElButton type="primary" size="small">{$i18nBundle('mock.label.selectFile')}</ElButton>
        }
      },
      colSpan: nvSpan + 1
    }])
  })
})

</script>

<template>
  <el-container class="flex-column common-params-edit">
    <el-row
      v-for="(item, index) in params"
      :key="index"
      class="padding-bottom2"
    >
      <template
        v-for="option in paramsOptions[index]"
        :key="`${index}_${option.prop}`"
      >
        <el-col
          v-if="option.enabled!==false"
          :span="option.colSpan"
        >
          <common-form-control
            label-width="80px"
            :model="item"
            :option="option"
            :prop="`${formProp}.${index}.${option.prop}`"
          />
        </el-col>
      </template>
      <el-col
        :span="4"
        class="padding-left2"
      >
        <el-button
          v-if="showRemoveButton"
          type="danger"
          size="small"
          circle
          @click="params.splice(index, 1)"
        >
          <common-icon icon="Delete" />
        </el-button>
      </el-col>
    </el-row>
    <el-row>
      <el-col>
        <el-button
          v-if="showAddButton"
          type="primary"
          size="small"
          @click="addRequestParam()"
        >
          <common-icon icon="Plus" />
          {{ $t('common.label.add') }}
        </el-button>
        <el-button
          v-if="showPasteButton"
          :type="showTextModel?'success':'info'"
          size="small"
          @click="showTextModel=!showTextModel"
        >
          <common-icon icon="ContentPasteGoFilled" />
          {{ $t('common.label.paste') }}
        </el-button>
        <common-form-control
          v-if="showTextModel"
          class="text-model-cls"
          :model="inputTextModel"
          :option="inputTextOption"
        />
      </el-col>
    </el-row>
  </el-container>
</template>

<style scoped>

</style>

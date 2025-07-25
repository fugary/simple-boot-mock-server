<script setup lang="jsx">
import { defineFormOptions } from '@/components/utils'
import { computed, ref } from 'vue'
import { getSingleSelectOptions, toFlatKeyValue } from '@/utils'
import { $i18nBundle } from '@/messages'
import { ElMessage, ElButton } from 'element-plus'
import { calcSuggestionsFunc, concatValueSuggestions } from '@/services/mock/MockCommonService'
import { isFunction } from 'lodash-es'

const props = defineProps({
  formProp: {
    type: String,
    default: 'requestParams'
  },
  nameReadOnly: {
    type: Boolean,
    default: false
  },
  nameRequired: {
    type: Boolean,
    default: false
  },
  valueRequired: {
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
  nameSuggestions: {
    type: [Array, Function],
    default: () => []
  },
  valueSuggestions: {
    type: [Array, Function],
    default: () => []
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

const calcSuggestions = (key = 'name') => {
  const keySuggestions = props[`${key}Suggestions`]
  return calcSuggestionsFunc(keySuggestions)
}

const paramsOptions = computed(() => {
  const nameSuggestions = calcSuggestions('name')
  const valueSuggestions = calcSuggestions('value')
  return params.value.map((param, index) => {
    const nvSpan = 8
    const paramValueSuggestions = concatValueSuggestions(param.valueSuggestions, valueSuggestions)
    return defineFormOptions([{
      labelWidth: '30px',
      prop: 'enabled',
      disabled: props.nameReadOnly,
      type: 'switch',
      colSpan: 2
    }, {
      label: 'Key',
      prop: props.nameKey,
      required: props.nameReadOnly || props.nameRequired,
      disabled: props.nameReadOnly,
      colSpan: nvSpan,
      type: nameSuggestions ? 'autocomplete' : 'input',
      attrs: {
        fetchSuggestions: nameSuggestions,
        triggerOnFocus: false,
        tabindex: index * 2 + 1
      },
      dynamicOption: (item, ...args) => {
        if (isFunction(item.dynamicOption)) {
          return item.dynamicOption(item, ...args)
        }
      }
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
        param[props.valueKey] = param.type === 'file' ? [] : ''
      }
    }, {
      label: 'Value',
      prop: props.valueKey,
      required: props.nameReadOnly || props.valueRequired,
      colSpan: nvSpan,
      enabled: param.type !== 'file',
      type: paramValueSuggestions ? 'autocomplete' : 'input',
      attrs: {
        fetchSuggestions: paramValueSuggestions,
        triggerOnFocus: true,
        tabindex: 100 + index * 2 + 2
      },
      dynamicOption: (item, ...args) => {
        if (isFunction(item.dynamicOption)) {
          return item.dynamicOption(item, ...args)
        }
      }
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
      colSpan: 6
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
        :span="3"
        class="padding-left2 padding-top1"
      >
        <el-button
          v-if="item.array"
          type="success"
          size="small"
          circle
          @click="params.splice(index + 1, 0, item)"
        >
          <common-icon icon="Plus" />
        </el-button>
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

<script setup lang="jsx">
import { defineFormOptions } from '@/components/utils'
import { computed, ref } from 'vue'
import { $copyText, toFlatKeyValue } from '@/utils'
import { $i18nBundle, $i18nKey } from '@/messages'
import { ElMessage, ElButton } from 'element-plus'
import { calcSuggestionsFunc, concatValueSuggestions } from '@/services/mock/MockCommonService'
import { isFunction, cloneDeep } from 'lodash-es'
import { useRenderKey, useSortableParams } from '@/hooks/CommonHooks'
import { useTabFocus } from '@/hooks/useTabFocus'

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
  showCopyButton: {
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
  showValueConfig: {
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
  nameDynamicOption: {
    type: Function,
    default: undefined
  },
  valueDynamicOption: {
    type: Function,
    default: undefined
  },
  nameSpan: {
    type: Number,
    default: 8
  },
  valueSpan: {
    type: Number,
    default: 8
  },
  fileFlag: {
    type: Boolean,
    default: false
  },
  singleEnable: {
    type: Boolean,
    default: false
  }
})

const params = defineModel('modelValue', {
  type: Array,
  default: () => []
})

const VALUE_TYPE_INPUT = 'input'
const VALUE_TYPE_NUMBER = 'number'
const VALUE_TYPE_DATE = 'date'
const VALUE_TYPE_DATETIME = 'datetime'
const VALUE_TYPE_FILE = 'file'
const LEGACY_VALUE_TYPE_INPUT = 'text'
const VALUE_TYPE_OPTIONS = [{
  value: VALUE_TYPE_INPUT,
  labelKey: 'common.label.input'
}, {
  value: VALUE_TYPE_NUMBER,
  labelKey: 'common.label.number'
}, {
  value: VALUE_TYPE_DATE,
  labelKey: 'common.label.date'
}, {
  value: VALUE_TYPE_DATETIME,
  labelKey: 'common.label.dateTime'
}, {
  value: VALUE_TYPE_FILE,
  labelKey: 'common.label.file'
}]

const getParamMeta = (param) => {
  param.meta = param.meta || {}
  return param.meta
}

const normalizeParamValueType = (param) => {
  const meta = param.meta || {}
  const valueType = meta.type ?? param.type
  let result = valueType
  if (!valueType || valueType === LEGACY_VALUE_TYPE_INPUT) {
    result = VALUE_TYPE_INPUT
  }
  if (param.type && meta.type !== result) {
    getParamMeta(param).type = result
  }
  return result
}

const isFileParam = param => normalizeParamValueType(param) === VALUE_TYPE_FILE

const isInputParam = param => normalizeParamValueType(param) === VALUE_TYPE_INPUT

const isNumberParam = param => normalizeParamValueType(param) === VALUE_TYPE_NUMBER

const isDateParam = param => [VALUE_TYPE_DATE, VALUE_TYPE_DATETIME].includes(normalizeParamValueType(param))

const getDateValueFormat = type => type === VALUE_TYPE_DATETIME ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'

params.value.forEach(param => {
  param.enabled = param.enabled ?? true
})

const addRequestParam = () => {
  params.value.push({
    enabled: !props.singleEnable || !params.value.filter(param => param.enabled).length,
    meta: {
      type: VALUE_TYPE_INPUT
    }
  })
}

const validParams = computed(() => {
  return params.value.filter(param => !!param.name && !isFileParam(param))
})

const copyParams = () => $copyText(JSON.stringify(validParams.value))

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
  } else if (value.startsWith('[')) {
    calcParams = JSON.parse(value)
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
  attrs: {
    type: 'textarea'
  },
  change (value) {
    if (value) {
      const calcParams = calcPasteParams(value)
      params.value = [...calcParams]
      inputTextModel.value.text = ''
      showTextModel.value = false
    }
  }
}

const showValueSuggestionsDialog = ref(false)
const valueSuggestionsModel = ref({
  readonlyItems: [],
  items: []
})
const currentValueSuggestionsParam = ref()

const valueTypeOptions = computed(() => {
  return VALUE_TYPE_OPTIONS.filter(option => props.fileFlag || option.value !== VALUE_TYPE_FILE)
})

const normalizeValueSuggestion = (item) => {
  if (item && typeof item === 'object') {
    return {
      value: item.value,
      description: item.description
    }
  }
  return {
    value: item
  }
}

const formatValueSuggestion = (item = {}) => {
  if (item.description && hasValueSuggestionText(item.value)) {
    return `${item.value} - ${item.description}`
  }
  return item.description || item.value || ''
}

const openValueSuggestions = (item) => {
  currentValueSuggestionsParam.value = item
  valueSuggestionsModel.value.readonlyItems = (Array.isArray(item.valueSuggestions) ? item.valueSuggestions : [])
    .map(normalizeValueSuggestion)
  valueSuggestionsModel.value.items = (Array.isArray(item.meta?.valueSuggestions) ? item.meta.valueSuggestions : [])
    .map(normalizeValueSuggestion)
  showValueSuggestionsDialog.value = true
}

const addValueSuggestion = () => {
  valueSuggestionsModel.value.items.push({})
}

const hasValueSuggestionText = value => value !== undefined && value !== ''

const valueSuggestionItems = computed(() => [
  ...valueSuggestionsModel.value.readonlyItems.map(item => ({
    ...item,
    readonly: true
  })),
  ...valueSuggestionsModel.value.items.map((item, index) => ({
    ...item,
    index,
    readonly: false
  }))
])

const saveValueSuggestions = () => {
  const valueSuggestions = valueSuggestionsModel.value.items
    .map(normalizeValueSuggestion)
    .filter(item => hasValueSuggestionText(item.value) || item.description)
    .map(item => {
      const suggestion = {}
      if (item.description) {
        suggestion.description = item.description
      }
      if (hasValueSuggestionText(item.value)) {
        suggestion.value = item.value
      }
      return suggestion
    })
  if (currentValueSuggestionsParam.value) {
    const meta = getParamMeta(currentValueSuggestionsParam.value)
    meta.valueSuggestions = valueSuggestions
  }
}

const hasMetaConfig = item => {
  return !!item.meta?.valueSuggestions?.length
}

const getValueOption = (param, paramValueSuggestions, nvSpan) => {
  const valueType = normalizeParamValueType(param)
  const option = {
    labelKey: 'common.label.value',
    prop: props.valueKey,
    required: props.nameReadOnly || props.valueRequired || param.valueRequired,
    colSpan: props.valueSpan || nvSpan,
    enabled: !isFileParam(param)
  }
  if (isDateParam(param)) {
    const valueFormat = getDateValueFormat(valueType)
    return {
      ...option,
      type: 'date-picker',
      attrs: {
        type: valueType,
        format: valueFormat,
        valueFormat,
        style: {
          width: '100%'
        }
      }
    }
  }
  if (isNumberParam(param)) {
    return {
      ...option,
      type: 'input-number',
      attrs: {
        controlsPosition: 'right',
        style: {
          width: '100%'
        }
      }
    }
  }
  return {
    ...option,
    type: paramValueSuggestions ? 'autocomplete' : 'input',
    attrs: {
      fetchSuggestions: paramValueSuggestions,
      triggerOnFocus: true
    },
    slots: paramValueSuggestions
      ? {
          default: ({ item }) => formatValueSuggestion(item)
        }
      : undefined,
    dynamicOption: (item, ...args) => {
      if (isFunction(item.dynamicOption)) {
        return item.dynamicOption(item, ...args)
      }
      if (isFunction(props.valueDynamicOption)) {
        return props.valueDynamicOption(item, ...args)
      }
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
  return params.value.map((param) => {
    const nvSpan = 8
    const paramValueSuggestions = concatValueSuggestions(
      param.meta?.valueSuggestions,
      param.valueSuggestions,
      valueSuggestions
    )
    return defineFormOptions([{
      labelWidth: '30px',
      prop: 'enabled',
      disabled: props.nameReadOnly,
      type: 'switch',
      colSpan: 2,
      dynamicOption (item) {
        if (props.singleEnable) {
          return {
            change (value) {
              console.log('=========================item', item, value)
              if (value) {
                params.value.filter(param => param !== item).forEach(param => (param.enabled = false))
              }
            }
          }
        }
      }
    }, {
      labelKey: 'common.label.name',
      prop: props.nameKey,
      required: props.nameReadOnly || props.nameRequired || param.nameRequired || param.valueRequired,
      disabled: props.nameReadOnly,
      colSpan: props.nameSpan || nvSpan,
      type: nameSuggestions ? 'autocomplete' : 'input',
      attrs: {
        fetchSuggestions: nameSuggestions,
        triggerOnFocus: false
      },
      dynamicOption: (item, ...args) => {
        if (isFunction(item.dynamicOption)) {
          return item.dynamicOption(item, ...args)
        }
        if (isFunction(props.nameDynamicOption)) {
          return props.nameDynamicOption(item, ...args)
        }
      }
    }, {
      labelWidth: '1px',
      prop: 'meta.type',
      type: 'select',
      value: VALUE_TYPE_INPUT,
      children: valueTypeOptions.value,
      attrs: {
        clearable: false,
        style: {
          paddingTop: '2px'
        }
      },
      enabled: props.showValueConfig && valueTypeOptions.value.length > 1,
      colSpan: 3,
      change (value) {
        param[props.valueKey] = value === VALUE_TYPE_FILE ? [] : (value === VALUE_TYPE_NUMBER ? undefined : '')
      }
    }, getValueOption(param, isInputParam(param) ? paramValueSuggestions : undefined, nvSpan), {
      labelKey: 'common.label.files',
      type: 'upload',
      enabled: props.fileFlag && isFileParam(param),
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

const { sortableRef, hoverIndex, dragging } = useSortableParams(params, '.common-params-item')

const { renderKey } = useRenderKey()

useTabFocus(sortableRef)

</script>

<template>
  <el-container
    ref="sortableRef"
    class="flex-column common-params-edit"
    style="line-height: normal;"
  >
    <el-row
      v-for="(item, index) in params"
      :key="renderKey(item)"
      class="padding-bottom2 common-params-item"
      @mouseenter="hoverIndex=index"
      @mouseleave="hoverIndex=-1"
    >
      <template
        v-for="(option, idx) in paramsOptions[index]"
        :key="`${index}_${option.prop}_${option.type}`"
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
          >
            <template #beforeLabel>
              <common-icon
                v-if="idx===0&&hoverIndex===index&&!dragging"
                :size="20"
                class="margin-top1 move-indicator"
                icon="DragIndicatorFilled"
                style="cursor: move;"
              />
            </template>
          </common-form-control>
        </el-col>
      </template>
      <el-col
        :span="3"
        class="padding-left2 padding-top1 common-params-actions"
      >
        <el-button
          v-if="item.array"
          type="success"
          size="small"
          circle
          @click="params.splice(index + 1, 0, cloneDeep(item))"
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
        <el-tooltip
          v-if="props.showValueConfig && isInputParam(item)"
          :content="$i18nKey('common.label.commonConfig', 'common.label.value')"
          placement="top"
        >
          <el-button
            :type="hasMetaConfig(item)?'warning':'info'"
            size="small"
            circle
            @click="openValueSuggestions(item)"
          >
            <common-icon icon="Setting" />
          </el-button>
        </el-tooltip>
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
          <common-icon
            class="margin-right1"
            icon="Plus"
          />
          {{ $t('common.label.add') }}
        </el-button>
        <el-button
          v-if="showCopyButton&&validParams?.length"
          type="success"
          size="small"
          @click="copyParams()"
        >
          <common-icon
            class="margin-right1"
            icon="DocumentCopy"
          />
          {{ $i18nKey('common.label.commonCopy', 'mock.label.params') }}
        </el-button>
        <el-button
          v-if="showPasteButton"
          :type="showTextModel?'success':'info'"
          size="small"
          @click="showTextModel=!showTextModel"
        >
          <common-icon
            class="margin-right1"
            icon="ContentPasteGoFilled"
          />
          {{ $t('common.label.paste') }}
        </el-button>
        <common-form-control
          v-if="showTextModel"
          class="padding-top2"
          :model="inputTextModel"
          :option="inputTextOption"
        />
      </el-col>
    </el-row>
    <common-window
      v-model="showValueSuggestionsDialog"
      :title="$i18nKey('common.label.commonConfig', 'common.label.value')"
      width="650px"
      :ok-click="saveValueSuggestions"
    >
      <el-container class="flex-column value-suggestions-window">
        <el-row class="padding-bottom2 value-suggestions-header">
          <el-col :span="10">
            {{ $t('common.label.value') }}
          </el-col>
          <el-col
            :span="11"
            class="padding-left2"
          >
            {{ $t('common.label.description') }}
          </el-col>
          <el-col :span="3" />
        </el-row>
        <el-row
          v-for="(suggestion, index) in valueSuggestionItems"
          :key="`${suggestion.readonly?'readonly':'custom'}_${index}`"
          class="padding-bottom2"
        >
          <el-col :span="10">
            <el-input
              v-if="suggestion.readonly"
              :model-value="suggestion.value"
              disabled
            />
            <el-input
              v-else
              v-model="valueSuggestionsModel.items[suggestion.index].value"
              clearable
            />
          </el-col>
          <el-col
            :span="11"
            class="padding-left2"
          >
            <el-input
              v-if="suggestion.readonly"
              :model-value="suggestion.description"
              disabled
            />
            <el-input
              v-else
              v-model="valueSuggestionsModel.items[suggestion.index].description"
              clearable
            />
          </el-col>
          <el-col
            :span="3"
            class="padding-left2"
          >
            <el-button
              v-if="!suggestion.readonly"
              type="danger"
              size="small"
              circle
              @click="valueSuggestionsModel.items.splice(suggestion.index, 1)"
            >
              <common-icon icon="Delete" />
            </el-button>
          </el-col>
        </el-row>
        <el-row>
          <el-col>
            <el-button
              type="primary"
              size="small"
              @click="addValueSuggestion()"
            >
              <common-icon
                class="margin-right1"
                icon="Plus"
              />
              {{ $t('common.label.add') }}
            </el-button>
          </el-col>
        </el-row>
      </el-container>
    </common-window>
  </el-container>
</template>

<style scoped>
.common-params-edit :deep(.common-params-actions) {
  white-space: nowrap;
}

.common-params-edit :deep(.common-params-actions .el-tooltip) {
  margin-left: 12px;
}

.value-suggestions-window {
  padding-top: 4px;
}

.value-suggestions-header {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>

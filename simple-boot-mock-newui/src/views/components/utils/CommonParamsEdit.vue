<script setup>
import { defineFormOptions } from '@/components/utils'
import { computed, ref } from 'vue'
import { toFlatKeyValue } from '@/utils'

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
      console.log('========================error', e)
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
  label: '粘贴自动计算',
  tooltip: '支持浏览器GET字符串或者JSON',
  prop: 'text',
  labelWidth: '120px',
  change (value) {
    if (value) {
      const calcParams = calcPasteParams(value)
      params.value = [...calcParams]
      inputTextModel.value.text = ''
    }
  }
}

const defaultHeaders = ['Accept',
  'Accept-Charset',
  'Accept-Encoding',
  'Accept-Language',
  'Authorization',
  'Cookie',
  'Connection',
  'Content-Type',
  'Origin',
  'Pragma',
  'User-Agent'
]

const paramOptions = computed(() => {
  return defineFormOptions([{
    labelWidth: '30px',
    prop: 'enabled',
    disabled: props.nameReadOnly,
    type: 'switch'
  }, {
    label: 'Key',
    prop: props.nameKey,
    required: true,
    disabled: props.nameReadOnly,
    type: props.headerFlag ? 'autocomplete' : 'input',
    attrs: {
      fetchSuggestions: (queryString, cb) => {
        const dataList = defaultHeaders.filter(item => item.toLowerCase().includes(queryString?.toLowerCase()))
          .map(value => ({ value }))
        cb(dataList)
      },
      triggerOnFocus: false
    }
  }, {
    label: 'Value',
    prop: props.valueKey,
    required: true
  }])
})

</script>

<template>
  <el-container class="flex-column">
    <el-row
      v-for="(item, index) in params"
      :key="index"
      class="padding-bottom2"
    >
      <el-col
        v-for="option in paramOptions"
        :key="`${index}_${option.prop}`"
        :span="option.prop==='enabled'?2:9"
      >
        <common-form-control
          label-width="80px"
          :model="item"
          :option="option"
          :prop="`${formProp}.${index}.${option.prop}`"
        />
      </el-col>
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
          添加
        </el-button>
        <el-button
          v-if="showPasteButton"
          :type="showTextModel?'success':'info'"
          size="small"
          @click="showTextModel=!showTextModel"
        >
          <common-icon icon="ContentPasteGoFilled" />
          粘贴
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
.text-model-cls {
  float: right;
  width: calc(100% - 140px) !important;
}
</style>

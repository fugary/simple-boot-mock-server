<script setup>
import { defineFormOptions } from '@/components/utils'
import { computed } from 'vue'

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
  }
})

const params = defineModel('modelValue', {
  type: Array,
  default: () => []
})

const addRequestParam = () => {
  params.value.push({})
}

const paramOptions = computed(() => {
  return defineFormOptions([{
    label: 'Key',
    prop: props.nameKey,
    required: true,
    disabled: props.nameReadOnly
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
        :span="10"
      >
        <common-form-control
          label-width="100px"
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
          type="info"
          size="small"
          @click="addRequestParam()"
        >
          <common-icon icon="Plus" />
          添加
        </el-button>
      </el-col>
    </el-row>
  </el-container>
</template>

<style scoped>

</style>

<template>
  <div>
    <div v-for="(item, index) in requestParams" :key="index">
      <el-form-item
        size="mini"
        :prop="`${formProp}.${index}.${nameKey}`"
        label="参数名"
        :rules="{required: true, message: `参数名不能为空`, trigger: 'blur'}"
      >
        <el-input v-model="item[nameKey]" />
      </el-form-item>
      <el-form-item
        size="mini"
        :prop="`${formProp}.${index}.${valueKey}`"
        label="参数值"
        :rules="{required: true, message: `参数值不能为空`, trigger: 'blur'}"
      >
        <el-input v-model="item[valueKey]" />
      </el-form-item>
      <el-button
        size="mini"
        round
        type="danger"
        icon="el-icon-delete-solid"
        @click.prevent="deleteRequestParam(index)"
      />
    </div>
    <el-button
      v-if="showAddButton"
      size="mini"
      round
      type="primary"
      icon="el-icon-plus"
      @click.prevent="addRequestParam()"
    >添加参数
    </el-button>
  </div>
</template>

<script>
export default {
  name: 'CommonParamsEdit',
  props: {
    params: {
      type: Array,
      required: true
    },
    formProp: {
      type: String,
      default: 'requestParams'
    },
    showAddButton: {
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
  },
  data () {
    const requestParams = this.$props.params || []
    return {
      requestParams
    }
  },
  watch: {
    requestParams: {
      handler (v) {
        this.$emit('update:params', v)
      },
      deep: true
    },
    params: {
      handler (params) {
        this.requestParams = params
      },
      deep: true
    }
  },
  methods: {
    addRequestParam () {
      const { nameKey, valueKey } = this.$props
      const newParam = {}
      newParam[nameKey] = ''
      newParam[valueKey] = ''
      this.requestParams.push(newParam)
    },
    deleteRequestParam (index) {
      this.requestParams.splice(index, 1)
    }
  }
}
</script>

<style scoped>

</style>

<template>
  <el-form ref="paramTargetForm" size="mini" :model="paramTarget" inline>
    <div v-if="paramTarget.pathParams.length">
      <el-divider content-position="left">
        <strong>路径参数</strong>
        <el-badge :value="paramTarget.pathParams.length" />
      </el-divider>
      <el-form-item
        v-for="(item, index) in paramTarget.pathParams"
        :key="item.name"
        :label="item.name"
        :prop="`pathParams.${index}.value`"
        :rules="{required: true, message: `${item.name}不能为空`, trigger: 'blur'}"
      >
        <el-input v-model="item.value" />
      </el-form-item>
    </div>
    <div>
      <el-divider content-position="left">
        <strong>请求参数</strong>
        <el-badge
          v-if="paramTarget.requestParams.length || paramTarget.showRequestBody"
          :value="paramTarget.requestParams.length + (paramTarget.showRequestBody?1:0)"
        />
        <el-link
          icon="el-icon-plus"
          type="primary"
          title="添加请求参数"
          @click="addRequestParam(paramTarget.requestParams)"
        >添加参数
        </el-link>
        <el-link
          v-if="request.method!=='GET'"
          icon="el-icon-edit"
          :type="paramTarget.showRequestBody?'danger':'primary'"
          :title="paramTarget.showRequestBody?'删除请求体':'添加请求体'"
          @click="paramTarget.showRequestBody=!paramTarget.showRequestBody"
        >{{ paramTarget.showRequestBody?'删除请求体':'添加请求体' }}
        </el-link>
      </el-divider>
      <common-params-edit :params.sync="paramTarget.requestParams" :show-add-button="false" />
      <el-form-item v-if="paramTarget.showRequestBody" prop="requestBody" label="类型">
        <el-select
          v-model="paramTarget.contentType"
          size="mini"
          placeholder="Content Type"
        >
          <el-option v-for="item in allContentTypes" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="paramTarget.showRequestBody" prop="requestBody" label="请求内容">
        <el-input v-model="paramTarget.requestBody" type="textarea" autosize />
      </el-form-item>
    </div>
    <div>
      <el-divider content-position="left">
        <strong>请求头</strong>
        <el-badge v-if="paramTarget.headerParams.length" :value="paramTarget.headerParams.length" />
        <el-link
          icon="el-icon-plus"
          type="primary"
          title="添加请求头"
          @click="addRequestParam(paramTarget.headerParams)"
        >
          添加请求头
        </el-link>
      </el-divider>
      <common-params-edit :params.sync="paramTarget.headerParams" form-prop="headerParams" :show-add-button="false" />
    </div>
  </el-form>
</template>

<script>
import CommonParamsEdit from './CommonParamsEdit'
export default {
  name: 'MockParamsEdit',
  components: { CommonParamsEdit },
  props: {
    request: {
      type: Object,
      required: true
    },
    dataItem: {
      type: Object,
      default: null
    },
    calcRequestUrl: {
      type: String,
      default: ''
    }
  },
  data() {
    const mockParams = (this.dataItem ? this.dataItem.mockParams : null) || this.request.mockParams
    const paramTarget = this.calcTarget(mockParams)
    this.$emit('update:result-param-target', paramTarget)
    return {
      paramTarget,
      allContentTypes: ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
    }
  },
  watch: {
    paramTarget: {
      handler(target) {
        this.$emit('update:result-param-target', target)
      },
      deep: true
    }
  },
  methods: {
    calcTarget(value) {
      const target = this.calcParamTarget(this.calcRequestUrl)
      if (value) {
        const pathParams = target.pathParams
        const savedTarget = JSON.parse(value)
        Object.assign(target, savedTarget || {})
        if (savedTarget.pathParams && savedTarget.pathParams.length) {
          const savePathParams = savedTarget.pathParams.reduce((result, item) => {
            result[item.name] = item.value
            return result
          }, {})
          pathParams.forEach(item => {
            item.value = savePathParams[item.name] || ''
          })
        }
        target.pathParams = pathParams
      }
      return target
    },
    addRequestParam(params) {
      params.push({
        name: '',
        value: ''
      })
    },
    deleteRequestParam(params, index) {
      params.splice(index, 1)
    },
    calcParamTarget(calcRequestUrl) {
      const pathParams = calcRequestUrl.split('/').filter(seg => seg.startsWith(':')).map(seg => seg.substring(1))
        .reduce((newArr, arrItem) => {
          if (newArr.indexOf(arrItem) < 0) {
            newArr.push(arrItem)
          }
          return newArr
        }, []).map(name => {
          return {
            name,
            value: ''
          }
        })
      return {
        pathParams,
        requestParams: [],
        headerParams: [],
        requestBody: '',
        contentType: 'application/json',
        showRequestBody: false
      }
    }
  }
}
</script>

<style scoped>

</style>

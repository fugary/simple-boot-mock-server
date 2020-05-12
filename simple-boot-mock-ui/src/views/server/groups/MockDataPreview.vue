<template>
  <el-dialog :title="`数据预览【${requestUrl}】`" :visible.sync="showDataPreview" width="1000px">
    <el-tabs type="card">
      <el-tab-pane label="配置参数">
        <el-form ref="previewDataForm" size="mini" :model="paramTarget" inline>
          <el-card v-if="paramTarget.pathParams.length" shadow="never">
            <div slot="header">
              <span>路径参数【路径参数不能为空】</span>
            </div>
            <el-form-item
              v-for="(item, index) in paramTarget.pathParams"
              :key="item.name"
              :label="item.name"
              :prop="`pathParams.${index}.value`"
              :rules="{required: true, message: `${item.name}不能为空`, trigger: 'blur'}"
            >
              <el-input v-model="item.value" />
            </el-form-item>
          </el-card>
          <el-card shadow="never">
            <div slot="header">
              请求参数
              <el-button icon="el-icon-plus" size="mini" round type="primary" title="新增请求参数" @click="addRequestParam()">添加参数</el-button>
              <el-button
                v-if="request.method!=='GET'"
                icon="el-icon-edit"
                size="mini"
                round
                type="info"
                :title="paramTarget.showRequestBody?'删除请求内容':'添加请求内容'"
                @click="paramTarget.showRequestBody=!paramTarget.showRequestBody"
              >{{paramTarget.showRequestBody?'删除请求内容':'添加请求内容'}}</el-button>
            </div>
            <div v-for="(item, index) in paramTarget.requestParams" :key="index">
              <el-form-item
                :prop="`requestParams.${index}.name`"
                label="参数名"
                :rules="{required: true, message: `参数名不能为空`, trigger: 'blur'}"
              >
                <el-input v-model="item.name" />
              </el-form-item>
              <el-form-item
                :prop="`requestParams.${index}.value`"
                label="参数值"
                :rules="{required: true, message: `参数值不能为空`, trigger: 'blur'}"
              >
                <el-input v-model="item.value" />
              </el-form-item>
              <el-button
                size="mini"
                round
                type="danger"
                icon="el-icon-delete-solid"
                @click.prevent="deleteRequestParam(index)"
              />
            </div>
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
          </el-card>
        </el-form>
        <el-card v-if="previewDataResult.requestInfo" shadow="never">
          <el-table :data="previewDataResult.requestInfo" :show-header="false">
            <el-table-column prop="name" width="250" />
            <el-table-column prop="value">
              <template slot-scope="scope">
                <span v-if="scope.row.name==='Status Code'">
                  <el-tag effect="dark" disable-transitions :type="scope.row.value===200?'success':'danger'">
                    {{ scope.row.value }}
                  </el-tag>
                </span>
                <span v-else>
                  {{ scope.row.value }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <pre v-if="previewDataResult.data" v-highlightjs="previewDataResult.data">
            <code />
          </pre>
        </el-card>
      </el-tab-pane>
      <el-tab-pane v-if="previewDataResult.requestHeaders" label="请求头信息">
        <el-table :data="previewDataResult.requestHeaders">
          <el-table-column prop="name" label="名称" width="250" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="previewDataResult.responseHeaders" label="响应头信息">
        <el-table :data="previewDataResult.responseHeaders">
          <el-table-column prop="name" label="名称" width="250" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
    <div slot="footer" class="dialog-footer">
      <el-button @click="showDataPreview=false">关闭</el-button>
      <el-button v-loading="previewDataLoading" type="success" @click="doDataPreview()">预览</el-button>
    </div>
  </el-dialog>
</template>

<script>
import MockDataApi from '../../../api/server/MockDataApi'
import 'highlight.js/styles/monokai-sublime.css'
import hljs from 'highlight.js'

export default {
  name: 'MockDataPreview',
  props: {
    request: { type: Object, required: true },
    requestUrl: { type: String },
    dataItemId: { type: Number }
  },
  data() {
    const calcRequestUrl = this.requestUrl.replace(/\{([\w-]+)\}/ig, ':$1')
    const paramTarget = this.calcParamTarget(calcRequestUrl)
    return {
      previewDataResult: {},
      showDataPreview: false,
      previewDataLoading: false,
      pathVariables: {},
      calcRequestUrl,
      paramTarget,
      allContentTypes: ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
    }
  },
  watch: {
    showDataPreview: function(flag) {
      if (!flag) {
        console.info('preview close.............')
        this.$emit('preview-close', flag)
      }
    }
  },
  mounted() {
    this.handleDataPreview()
  },
  methods: {
    addRequestParam() {
      this.paramTarget.requestParams.push({
        name: '',
        value: ''
      })
    },
    deleteRequestParam(index) {
      this.paramTarget.requestParams.splice(index, 1)
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
        requestBody: '',
        contentType: 'application/json',
        showRequestBody: false
      }
    },
    handleDataPreview() {
      this.previewDataResult = {}
      this.showDataPreview = true
      this.$nextTick(this.doDataPreview)
    },
    calcResponse(response) {
      this.previewDataLoading = false
      console.dir(response)
      Object.assign(this.previewDataResult, MockDataApi.processResponse(response))
      this.$forceUpdate()
    },
    doDataPreview() {
      this.$refs.previewDataForm.validate(valid => {
        if (valid) {
          this.previewDataLoading = true
          let requestUrl = this.calcRequestUrl
          this.paramTarget.pathParams.forEach(pathParam => {
            requestUrl = requestUrl.replace(new RegExp(`:${pathParam.name}`, 'g'), pathParam.value)
          })
          const params = this.paramTarget.requestParams.reduce((results, item) => {
            results[item.name] = item.value
            return results
          }, {})
          const data = this.paramTarget.showRequestBody ? this.paramTarget.requestBody : null
          const headers = this.paramTarget.showRequestBody ? { 'content-type': this.paramTarget.contentType } : null
          const config = {
            loading: false,
            params,
            data,
            headers
          }
          MockDataApi.previewRequest(requestUrl, this.request, this.dataItemId, config)
            .then(this.calcResponse, this.calcResponse)
        }
      })
    }
  }
}
</script>

<style scoped>

</style>

<template>
  <el-dialog :title="`数据预览【${requestUrl}】`" :visible.sync="showDataPreview" width="1000px">
    <el-tabs type="card">
      <el-tab-pane v-if="request" label="编辑信息">
        <mock-params-edit
          ref="paramTargetEdit"
          :request="request"
          :data-item="currentDataItem"
          :result-param-target.sync="paramTarget"
          :calc-request-url="calcRequestUrl"
        />
        <el-card v-if="previewDataResult.requestInfo" shadow="never">
          <el-table :data="previewDataResult.requestInfo" :show-header="false">
            <el-table-column prop="name" width="250" />
            <el-table-column prop="value">
              <template slot-scope="scope">
                <span v-if="scope.row.name==='Code'">
                  <el-tag effect="dark" disable-transitions :type="scope.row.value===200?'success':'danger'">
                    {{ scope.row.value }}
                  </el-tag>
                </span>
                <span v-else>
                  {{ scope.row.value }}
                  <el-button
                    v-if="scope.row.name==='URL'"
                    v-clipboard="scope.row.value"
                    type="info"
                    icon="el-icon-document-copy"
                    size="mini"
                    round
                  />
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
      <el-button
        v-if="previewDataResult.data"
        v-clipboard="previewDataResult.data"
        type="info"
        icon="el-icon-document-copy"
      >复制响应内容</el-button>
      <el-button @click="showDataPreview=false">关闭</el-button>
      <el-button v-loading="previewDataLoading" type="success" @click="doDataPreview()">预览</el-button>
    </div>
  </el-dialog>
</template>

<script>
import MockDataApi from '../../../api/server/MockDataApi'
import 'highlight.js/styles/monokai-sublime.css'
import MockRequestApi from '../../../api/server/MockRequestApi'
import MockParamsEdit from './MockParamsEdit'

export default {
  name: 'MockDataPreview',
  components: { MockParamsEdit },
  props: {
    request: { type: Object, required: true },
    requestUrl: { type: String },
    dataItem: { type: Object }
  },
  data () {
    const calcRequestUrl = this.requestUrl.replace(/\{([\w-]+)\}/ig, ':$1')
    const { dataItem } = this.$props
    return {
      currentDataItem: dataItem,
      previewDataItemFlag: !!dataItem,
      previewDataResult: {},
      showDataPreview: false,
      previewDataLoading: false,
      calcRequestUrl,
      paramTarget: null
    }
  },
  watch: {
    showDataPreview: function (flag) {
      if (!flag) {
        console.info('preview close.............')
        this.$emit('preview-close', flag)
      }
    }
  },
  mounted () {
    if (!this.currentDataItem) {
      MockRequestApi.getDefaultData(this.request.id).then(response => {
        this.currentDataItem = response.data
        this.handleDataPreview()
      }, this.handleDataPreview)
    } else {
      MockDataApi.getById(this.currentDataItem.id).then(response => {
        this.currentDataItem = response.data
        this.handleDataPreview()
      })
    }
  },
  methods: {
    handleDataPreview () {
      this.previewDataResult = {}
      this.showDataPreview = true
      this.$nextTick(this.doDataPreview)
    },
    calcResponse (response) {
      this.previewDataLoading = false
      console.dir(response)
      Object.assign(this.previewDataResult, MockDataApi.processResponse(response))
      this.$forceUpdate()
    },
    doSaveMockParams () {
      if (this.paramTarget) {
        const requestId = this.request.id
        const id = this.currentDataItem ? this.currentDataItem.id : null
        const mockParams = JSON.stringify(this.paramTarget)
        MockRequestApi.saveMockParams({
          requestId,
          id,
          mockParams
        }, { loading: false })
      }
    },
    doDataPreview () {
      console.info(this.$refs.paramTargetEdit)
      this.$refs.paramTargetEdit.$refs.paramTargetForm.validate(valid => {
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
          const headers = Object.assign(this.paramTarget.showRequestBody ? { 'content-type': this.paramTarget.contentType } : {},
            this.paramTarget.headerParams.reduce((results, item) => {
              results[item.name] = item.value
              return results
            }, {}))
          const config = {
            loading: false,
            params,
            data,
            headers
          }
          const dataItemId = this.currentDataItem && this.previewDataItemFlag ? this.currentDataItem.id : null
          this.doSaveMockParams()
          MockDataApi.previewRequest(requestUrl, this.request, dataItemId, config)
            .then(this.calcResponse, this.calcResponse)
        }
      })
    }
  }
}
</script>

<style scoped>

</style>

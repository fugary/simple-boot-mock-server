<template>
  <el-dialog :title="`数据预览【${requestUrl}】`" :visible.sync="showDataPreview" width="1000px">
    <el-tabs type="card">
      <el-tab-pane label="基本信息">
        <el-table :data="previewDataResult.requestInfo">
          <el-table-column prop="name" label="名称" width="250" />
          <el-table-column prop="value" label="值">
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
        <div v-if="previewDataResult.data" v-highlightjs="previewDataResult.data">
          <code />
        </div>
      </el-tab-pane>
      <el-tab-pane label="请求头信息">
        <el-table :data="previewDataResult.requestHeaders">
          <el-table-column prop="name" label="名称" width="250" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="响应头信息">
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
import 'highlight.js/scss/monokai.scss'

export default {
  name: 'MockDataPreview',
  props: {
    request: { type: Object, required: true },
    requestUrl: { type: String },
    dataItemId: { type: Number }
  },
  data() {
    return {
      previewDataResult: {},
      showDataPreview: false,
      previewDataLoading: false
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
    handleDataPreview() {
      this.previewDataResult = {}
      this.showDataPreview = true
      this.doDataPreview()
    },
    calcResponse(response) {
      this.previewDataLoading = false
      console.dir(response)
      Object.assign(this.previewDataResult, MockDataApi.processResponse(response))
      this.$forceUpdate()
    },
    doDataPreview() {
      this.previewDataLoading = true
      MockDataApi.previewRequest(this.requestUrl, this.request, this.dataItemId, { loading: false })
        .then(this.calcResponse, this.calcResponse)
    }
  }
}
</script>

<style scoped>

</style>

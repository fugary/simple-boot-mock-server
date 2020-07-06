<template>
  <el-form ref="dataForm" class="table-form padding-lr" :model="currentDataItem">
    <el-table
      v-if="request.id && currentDataItem"
      :data="dataItems"
      element-loading-text="Loading"
      border
      fit
      highlight-current-row
      :header-cell-class-name="'warning-row'"
      :row-class-name="'success-row'"
      @row-dblclick="handleDataEdit($event)"
    >
      <el-table-column label="默认" width="60">
        <template slot-scope="scope">
          <el-icon v-if="scope.row.defaultFlag" class="el-icon-s-flag" type="primary" />
        </template>
      </el-table-column>
      <el-table-column property="" label="状态码" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.id === currentDataItem.id">
            <el-select v-model="currentDataItem.statusCode" size="mini" placeholder="请求方法">
              <el-option v-for="item in allStatusCodes" :key="item" :label="item" :value="item" />
            </el-select>
          </span>
          <span v-else>{{ scope.row.statusCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Content-Type" width="160">
        <template slot-scope="scope">
          <span v-if="scope.row.id === currentDataItem.id">
            <el-select
              v-model="currentDataItem.contentType"
              size="mini"
              placeholder="Content Type"
            >
              <el-option v-for="item in allContentTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </span>
          <span v-else>{{ scope.row.contentType }}</span>
        </template>
      </el-table-column>
      <el-table-column class-name="status-col" label="状态" width="60" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.id === currentDataItem.id">
            <el-switch v-model="currentDataItem.status" :active-value="1" :inactive-value="0" />
          </span>
          <el-tag v-else effect="dark" size="mini" disable-transitions :type="scope.row.status?'success':'danger'">{{
            scope.row.status|statusFilter }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="响应内容">
        <template slot-scope="scope">
          <span v-if="scope.row.id === currentDataItem.id">
            <el-input
              v-model="currentDataItem.responseBody"
              autosize
              type="textarea"
              size="mini"
              placeholder="响应内容"
            />
          </span>
          <span v-else>{{ scope.row.responseBody | limitTo(100) }}</span>
        </template>
      </el-table-column>
      <el-table-column width="220">
        <template slot="header">
          <span>操作</span>
          <el-button icon="el-icon-plus" size="mini" circle type="success" title="新增响应数据" @click="handleDataEdit()" />
        </template>
        <template slot-scope="scope">
          <span v-if="scope.row.id===currentDataItem.id">
            <el-button icon="el-icon-check" size="mini" circle type="success" title="保存" @click="handleDataSave('dataForm')" />
            <el-button
              icon="el-icon-refresh-left"
              size="mini"
              circle
              type="info"
              title="重置"
              @click="handleDataEdit(scope.row)"
            />
            <el-button icon="el-icon-close" size="mini" circle title="取消" @click="cancelDataEdit()" />
          </span>
          <span v-else>
            <el-button
              icon="el-icon-edit-outline"
              size="mini"
              circle
              type="primary"
              title="编辑"
              @click="handleDataEdit(scope.row)"
            />
            <el-button
              icon="el-icon-view"
              size="mini"
              circle
              type="success"
              title="预览"
              @click="handleDataPreview(scope.row)"
            />
            <el-button
              icon="el-icon-delete-solid"
              size="mini"
              circle
              type="danger"
              title="删除"
              @click="handleDataDelete(scope.row)"
            />
            <el-button
              icon="el-icon-s-tools"
              size="mini"
              circle
              type="info"
              title="编辑更多响应详情"
              @click="handleDataDetailEdit(scope.row)"
            />
            <el-button
              v-if="!scope.row.defaultFlag"
              icon="el-icon-s-flag"
              size="mini"
              circle
              type="warning"
              title="设为默认响应"
              @click="markDefault(scope.row)"
            />
          </span>
        </template>
      </el-table-column>
    </el-table>
    <mock-data-preview
      v-if="previewDataConfig.showDataPreview"
      :request="request"
      :request-url="requestUrl"
      :data-item="previewDataConfig.dataItem"
      @preview-close="previewDataConfig.showDataPreview=false"
    />
    <el-dialog v-if="showDataDetailDialog" title="编辑响应数据" :visible.sync="showDataDetailDialog" width="800px" @close="cancelDataEdit">
      <el-form :model="currentDataItem" ref="dataItemForm">
        <el-form-item label="状态码" :label-width="formLabelWidth">
          <el-select v-model="currentDataItem.statusCode" size="mini" placeholder="请求方法">
            <el-option v-for="item in allStatusCodes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="Content Type" :label-width="formLabelWidth">
          <el-select
            v-model="currentDataItem.contentType"
            size="mini"
            placeholder="Content Type"
          >
            <el-option v-for="item in allContentTypes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" :label-width="formLabelWidth">
          <el-switch v-model="currentDataItem.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="是否默认请求" :label-width="formLabelWidth">
          <el-switch v-model="currentDataItem.defaultFlag" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="响应内容" :label-width="formLabelWidth">
          <el-input
            v-model="currentDataItem.responseBody"
            autosize
            type="textarea"
            size="mini"
            placeholder="响应内容"
          />
        </el-form-item>
        <el-form-item label="描述信息" :label-width="formLabelWidth">
          <el-input
            v-model="currentDataItem.description"
            autosize
            type="textarea"
            size="mini"
            placeholder="描述信息"
          />
        </el-form-item>
        <el-form-item label="附加响应头" :label-width="formLabelWidth">
          <div class="el-form--inline">
            <common-params-edit :params.sync="currentDataItem.headerParams" form-prop="headerParams" />
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelDataEdit">取消</el-button>
        <el-button v-loading="saveLoading" type="primary" @click="handleDataSave('dataItemForm')">确定</el-button>
      </div>
    </el-dialog>
  </el-form>
</template>

<script>
import MockDataApi from '../../../api/server/MockDataApi'
import MockDataPreview from './MockDataPreview'
import CommonParamsEdit from './CommonParamsEdit'

export default {
  name: 'MockDataEdit',
  components: { MockDataPreview, CommonParamsEdit },
  props: {
    request: { type: Object, required: true },
    groupItem: { type: Object }
  },
  data () {
    let requestPath = this.request.requestPath || ''
    requestPath = requestPath.startsWith('/') ? requestPath : `/${requestPath}`
    const requestUrl = `/mock/${this.groupItem.groupPath}${requestPath}`
    return {
      requestUrl,
      dataItems: [],
      currentDataItem: {},
      previewDataConfig: {
        dataItem: {},
        showDataPreview: false
      },
      saveLoading: false,
      showDataDetailDialog: false,
      formLabelWidth: '150px',
      allStatusCodes: [200, 302, 400, 404, 500],
      allContentTypes: ['application/json', 'application/xml', 'text/html', 'text/css', 'application/javascript']
    }
  },
  watch: {
    request: function (req) {
      if (req) {
        this.doSearchRequestData()
      }
    }
  },
  mounted () {
    if (this.request) {
      this.doSearchRequestData()
    }
  },
  methods: {
    newDataItem () {
      return {
        requestId: this.request.id,
        groupId: this.request.groupId,
        status: 1,
        statusCode: this.allStatusCodes[0],
        contentType: this.allContentTypes[0],
        headerParams: []
      }
    },
    doSearchRequestData () {
      const request = this.request
      MockDataApi.search({
        groupId: request.groupId,
        requestId: request.id,
        size: 50
      }).then(response => {
        console.info(response)
        this.dataItems = response.data
      }).finally(this.cancelLoading)
    },
    handleDataEdit (item = this.newDataItem()) {
      this.$cleanNewItem(this.dataItems)
      if (item.id) {
        MockDataApi.getById(item.id).then(response => {
          this.currentDataItem = response.data || Object.assign({}, item)
          this.currentDataItem.headerParams = JSON.parse(this.currentDataItem.headers || '[]')
        })
      } else {
        this.currentDataItem = Object.assign({}, item)
      }
      this.currentDataItem.headerParams = JSON.parse(this.currentDataItem.headers || '[]')
      this.$editTableItem(this.dataItems, item)
    },
    handleDataDetailEdit (item) {
      this.handleDataEdit(item)
      this.showDataDetailDialog = true
    },
    cancelDataEdit () {
      this.$cleanNewItem(this.dataItems)
      this.showDataDetailDialog = false
      this.currentDataItem = this.newDataItem()
    },
    cancelLoading () {
      this.saveLoading = false
    },
    handleDataSave (formKey) {
      this.$refs[formKey].validate(valid => {
        if (valid) {
          const saveItem = Object.assign({}, this.currentDataItem)
          saveItem.headers = saveItem.headerParams && saveItem.headerParams.length ? JSON.stringify(saveItem.headerParams) : ''
          delete saveItem.headerParams
          MockDataApi.saveOrUpdate(saveItem, { loading: false }).then(response => {
            console.info(response)
            this.doSearchRequestData()
            this.cancelDataEdit()
          }).finally(this.cancelLoading)
        }
      })
    },
    handleDataDelete (item) {
      this.$confirm('确定要删除该响应数据?', '提示').then(() => {
        console.info(item)
        MockDataApi.deleteById(item.id).then(this.doSearchRequestData)
      })
    },
    handleDataPreview (dataItem = {}) {
      Object.assign(this.previewDataConfig, {
        showDataPreview: true,
        dataItem
      })
    },
    markDefault (dataItem) {
      const { requestId, id } = dataItem
      this.saveLoading = true
      MockDataApi.markDefault({
        requestId,
        id,
        defaultFlag: 1
      }).then(response => {
        console.info(response)
        this.doSearchRequestData()
      }).finally(this.cancelLoading)
    }
  }
}
</script>

<style>
  .el-table .warning-row {
    background: oldlace;
  }

  .el-table .success-row {
    background: #f0f9eb;
  }
</style>

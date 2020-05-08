<template>
  <div>
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
      <el-table-column label="响应内容" width="300">
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
      <el-table-column label="附加头信息">
        <template slot-scope="scope">
          <span v-if="scope.row.id === currentDataItem.id">
            <el-input v-model="currentDataItem.headers" type="textarea" size="mini" autosize placeholder="附加头信息" />
          </span>
          <span v-else>{{ scope.row.headers | limitTo(100) }}</span>
        </template>
      </el-table-column>
      <el-table-column width="180">
        <template slot="header" slot-scope="scope">
          <span>操作</span>
          <el-button icon="el-icon-plus" size="mini" circle type="success" title="新增响应数据" @click="handleDataEdit()" />
        </template>
        <template slot-scope="scope">
          <span v-if="scope.row.id===currentDataItem.id">
            <el-button icon="el-icon-check" size="mini" circle type="success" title="保存" @click="handleDataSave()" />
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
              v-if="!scope.row.defaultFlag"
              icon="el-icon-s-flag"
              size="mini"
              circle
              type="primary"
              title="设为默认响应"
              @click="markDefault(scope.row)"
            />
            <el-button
              icon="el-icon-delete-solid"
              size="mini"
              circle
              type="danger"
              title="删除"
              @click="handleDataDelete(scope.row)"
            />
          </span>
        </template>
      </el-table-column>
    </el-table>
    <mock-data-preview
      v-if="previewDataConfig.showDataPreview"
      :request="request"
      :request-url="requestUrl"
      :data-item-id="previewDataConfig.dataItem.id"
      @preview-close="previewDataConfig.showDataPreview=false"
    />
  </div>
</template>

<script>
import MockDataApi from '../../../api/server/MockDataApi'
import MockDataPreview from './MockDataPreview'

export default {
  name: 'MockDataEdit',
  components: { MockDataPreview },
  props: {
    request: { type: Object, required: true },
    groupItem: { type: Object }
  },
  data() {
    const requestUrl = `/mock/${this.groupItem.groupPath}${this.request.requestPath}`
    return {
      requestUrl,
      dataItems: [],
      currentDataItem: {},
      previewDataConfig: {
        dataItem: {},
        showDataPreview: false
      },
      saveLoading: false,
      allStatusCodes: [200, 500, 404],
      allContentTypes: ['application/json', 'application/xml', 'text/html', 'text/css', 'application/javascript']
    }
  },
  watch: {
    request: function(req) {
      if (req) {
        this.doSearchRequestData()
      }
    }
  },
  mounted() {
    if (this.request) {
      this.doSearchRequestData()
    }
  },
  methods: {
    newDataItem() {
      return {
        requestId: this.request.id,
        groupId: this.request.groupId,
        status: 1,
        statusCode: this.allStatusCodes[0],
        contentType: this.allContentTypes[0]
      }
    },
    doSearchRequestData() {
      const request = this.request
      MockDataApi.search({
        groupId: request.groupId,
        requestId: request.id
      }).then(response => {
        console.info(response)
        this.dataItems = response.data
      }).finally(this.cancelLoading)
    },
    handleDataEdit(item) {
      item = !item ? this.newDataItem() : item
      this.currentDataItem = Object.assign({}, item)
      if (!item.id) {
        if (!this.dataItems.length || this.dataItems[this.dataItems.length - 1].id !== item.id) {
          this.dataItems.push(item)
        }
      }
    },
    cancelDataEdit() {
      if (!this.currentDataItem.id) {
        this.dataItems.pop()
      }
      this.currentDataItem = this.newDataItem()
    },
    cancelLoading() {
      this.saveLoading = false
    },
    handleDataSave() {
      MockDataApi.saveOrUpdate(this.currentDataItem, { loading: false }).then(response => {
        console.info(response)
        this.doSearchRequestData()
        this.cancelDataEdit()
      }).finally(this.cancelLoading)
    },
    handleDataDelete(item) {
      this.$confirm('确定要删除该响应数据?', '提示').then(() => {
        console.info(item)
        MockDataApi.removeById(item.id).then(this.doSearchRequestData)
      })
    },
    handleDataPreview(dataItem = {}) {
      Object.assign(this.previewDataConfig, {
        showDataPreview: true,
        dataItem
      })
    },
    markDefault(dataItem) {
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

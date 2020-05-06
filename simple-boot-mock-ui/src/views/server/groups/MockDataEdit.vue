<template>
  <el-table
    v-if="request.id && currentDataItem"
    v-loading="dataItemsLoading"
    :data="dataItems"
    element-loading-text="Loading"
    border
    fit
    highlight-current-row
    :header-cell-class-name="'warning-row'"
    :row-class-name="'success-row'"
    @row-dblclick="handleDataEdit($event)"
  >
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
    <el-table-column label="Content Type" width="200">
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
    <el-table-column class-name="status-col" label="状态" width="110" align="center">
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
        <span v-else>{{ scope.row.responseBody }}</span>
      </template>
    </el-table-column>
    <el-table-column label="附加头信息">
      <template slot-scope="scope">
        <span v-if="scope.row.id === currentDataItem.id">
          <el-input v-model="currentDataItem.headers" size="mini" placeholder="附加头信息" />
        </span>
        <span v-else>{{ scope.row.headers }}</span>
      </template>
    </el-table-column>
    <el-table-column width="250">
      <template slot="header" slot-scope="scope">
        <span>操作</span>
        <el-button type="success" size="mini" @click="handleDataEdit()">新增</el-button>
      </template>
      <template slot-scope="scope">
        <span v-if="scope.row.id===currentDataItem.id">
          <el-button size="mini" type="primary" @click="handleDataSave()">保存</el-button>
          <el-button size="mini" @click="handleDataEdit(scope.row)">重置</el-button>
          <el-button size="mini" @click="cancelDataEdit()">取消</el-button>
        </span>
        <span v-else>
          <el-button
            size="mini"
            type="primary"
            @click="handleDataEdit(scope.row)"
          >编辑 </el-button>
          <el-button size="mini" type="danger" @click="handleDataDelete(scope.row)">删除</el-button>
        </span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
import MockDataApi from '../../../api/server/MockDataApi'

export default {
  name: 'MockDataEdit',
  props: {
    request: { type: Object, required: true },
    loadData: { type: Boolean }
  },
  data() {
    return {
      dataItems: [],
      currentDataItem: {},
      dataItemsLoading: false,
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
      this.dataItemsLoading = true
      MockDataApi.search({
        groupId: request.groupId,
        requestId: request.id
      }).then(response => {
        console.info(response)
        this.dataItems = response.data
        this.dataItemsLoading = false
      })
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
    handleDataSave() {
      this.dataItemsLoading = true
      MockDataApi.saveOrUpdate(this.currentDataItem).then(response => {
        console.info(response)
        this.doSearchRequestData()
        this.cancelDataEdit()
        this.dataItemsLoading = false
      }, error => this.dataItemsLoading = false)
    },
    handleDataDelete(item) {
      this.$confirm('确定要删除该响应数据?', '提示').then(() => {
        console.info(item)
        MockDataApi.removeById(item.id).then(this.doSearchRequestData)
      })
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

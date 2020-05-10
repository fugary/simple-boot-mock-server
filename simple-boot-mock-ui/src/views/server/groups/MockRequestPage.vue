<template>
  <div class="app-container">
    <el-page-header @back="goBack">
      <div slot="content">
        {{ groupItem.groupName }} 【{{ groupUrl }}】
      </div>
    </el-page-header>
    <el-divider />
    <el-form :inline="true" :model="searchParam" @submit="doSearch">
      <el-form-item label="关键字搜索">
        <el-input v-model="searchParam.keyword" placeholder="关键字搜索" />
      </el-form-item>
      <el-form-item>
        <el-button type="submit" @click="doSearch">查询</el-button>
        <el-button type="success" @click="handleEdit()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-form class="table-form" :model="currentItem" ref="requestForm" :rules="requestFormRules">
      <el-table
        ref="requestsTable"
        :data="items"
        element-loading-text="Loading"
        border
        fit
        highlight-current-row
        @row-dblclick="handleEdit($event)"
        @expand-change="handleDataExpand"
      >
        <el-table-column type="expand">
          <template slot-scope="requestScope">
            <mock-data-edit v-if="groupItem.id && requestScope.row.expandDataFlag" :group-item="groupItem" :request="requestScope.row" />
          </template>
        </el-table-column>
        <el-table-column label="请求路径" width="200">
          <template slot-scope="scope">
            <span v-if="scope.row.id === currentItem.id">
              <el-form-item prop="requestPath">
                <el-input v-model="currentItem.requestPath" size="mini" placeholder="请求路径" />
              </el-form-item>
            </span>
            <span v-else>{{ scope.row.requestPath }}</span>
          </template>
        </el-table-column>
        <el-table-column label="请求方法" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.id === currentItem.id">
              <el-select v-model="currentItem.method" size="mini" placeholder="请求方法">
                <el-option
                  v-for="item in allMethods"
                  :key="item.method"
                  :label="item.method"
                  :value="item.method"
                />
              </el-select>
            </span>
            <el-tag v-else effect="dark" disable-transitions :type="methodType(scope.row.method)">
              {{ scope.row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column class-name="status-col" label="状态" width="110" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.id === currentItem.id">
              <el-switch v-model="currentItem.status" :active-value="1" :inactive-value="0" />
            </span>
            <el-tag v-else effect="dark" size="mini" disable-transitions :type="scope.row.status?'success':'danger'">
              {{ scope.row.status|statusFilter }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求名称" prop="requestName">
          <template slot-scope="scope">
            <span v-if="scope.row.id === currentItem.id">
              <el-input v-model="currentItem.requestName" size="mini" placeholder="请求名称" />
            </span>
            <span v-else>{{ scope.row.requestName }}</span>
          </template>
        </el-table-column>
        <el-table-column align="center" prop="created_at" label="创建时间" width="150">
          <template v-if="scope.row.createDate" slot-scope="scope">
            <i class="el-icon-time" />
            <span>{{ scope.row.createDate|date('YYYY-MM-DD HH:mm') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <span v-if="scope.row.id===currentItem.id">
              <el-button v-loading="saveLoading" icon="el-icon-check" size="mini" round type="success" title="保存" @click="handleSave()" />
              <el-button icon="el-icon-refresh-left" size="mini" round type="info" title="重置" @click="handleEdit(scope.row)" />
              <el-button icon="el-icon-close" size="mini" round title="取消" @click="cancelEdit()" />
            </span>
            <span v-else>
              <el-button icon="el-icon-edit-outline" size="mini" round type="primary" title="编辑" @click="handleEdit(scope.row)" />
              <el-button icon="el-icon-view" size="mini" round type="success" title="预览默认或者第一条响应数据" @click="handleDataPreview(scope.row)" />
              <el-button icon="el-icon-delete-solid" size="mini" round type="danger" title="删除" @click="handleDelete(scope.row)" />
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <mock-data-preview
      v-if="previewDataConfig.showDataPreview"
      :request="previewDataConfig.request"
      :request-url="previewDataConfig.requestUrl"
      @preview-close="previewDataConfig.showDataPreview=false"
    />
  </div>
</template>

<script>
import MockGroupApi from '../../../api/server/MockGroupApi'
import MockRequestApi from '../../../api/server/MockRequestApi'
import MockDataEdit from './MockDataEdit'
import MockDataPreview from './MockDataPreview'

export default {
  name: 'MockGroupPage',
  components: { MockDataEdit, MockDataPreview },
  data() {
    const groupId = this.$route.params.groupId
    return {
      groupId,
      groupItem: {},
      groupUrl: '',
      items: null,
      saveLoading: false,
      page: {},
      searchParam: {
        groupId
      },
      currentItem: this.newRequestItem(),
      requestFormRules: {
        requestPath: { required: true, message: '请求路径不能为空' }
      },
      previewDataConfig: {
        request: null,
        requestUrl: null,
        dataItem: {},
        showDataPreview: false
      },
      // success/info/warning/danger
      allMethods: [{ method: 'GET', type: 'primary' }, { method: 'POST', type: 'success' }, { method: 'DELETE', type: 'danger' }, { method: 'PUT', type: '' }, { method: 'PATCH', type: 'warning' }]
    }
  },
  mounted() {
    this.doLoadGroup()
    this.doSearch()
  },
  methods: {
    methodType(method) {
      const found = this.allMethods.filter(item => item.method === method)[0]
      return found ? found.type : ''
    },
    goBack() {
      this.$router.go(-1)
    },
    newRequestItem() {
      return {
        editing: false,
        status: 1,
        method: 'GET',
        groupId: this.groupId
      }
    },
    doLoadGroup() {
      MockGroupApi.getById(this.searchParam.groupId).then(response => {
        this.groupItem = response.data
        this.groupUrl = `/mock/${this.groupItem.groupPath}`
      })
    },
    doSearch() {
      MockRequestApi.search(this.searchParam).then(response => {
        console.info(response)
        this.items = response.data
        this.$nextTick(() => {
          this.doExpandData(this.items[0])
        })
      }).finally(this.cancelLoading)
    },
    handleEdit(item = this.newRequestItem()) {
      this.$cleanNewItem(this.items)
      this.currentItem = Object.assign({}, item)
      this.$editTableItem(this.items, item)
    },
    cancelEdit() {
      this.$cleanNewItem(this.items)
      this.currentItem = this.newRequestItem()
    },
    cancelLoading() {
      this.saveLoading = false
    },
    handleSave() {
      this.$refs.requestForm.validate(valid => {
        if (valid) {
          this.saveLoading = true
          MockRequestApi.saveOrUpdate(this.currentItem, { loading: false }).then(response => {
            console.info(response)
            this.doSearch()
            this.cancelEdit()
          }).finally(this.cancelLoading)
        }
      })
    },
    handleDelete(item) {
      this.$confirm('确定要删除该请求?', '提示').then(() => {
        console.info(item)
        MockRequestApi.removeById(item.id).then(this.doSearch)
      })
    },
    handleDataExpand(request, expanded) {
      console.info(arguments)
      request.expandDataFlag = expanded.indexOf(request) > -1 // 判断request是否是展开状态
    },
    doExpandData(item) {
      if (item) {
        this.$refs.requestsTable.toggleRowExpansion(item)
      }
    },
    handleDataPreview(request) {
      const requestUrl = `/mock/${this.groupItem.groupPath}${request.requestPath}`
      Object.assign(this.previewDataConfig, {
        showDataPreview: true,
        request,
        requestUrl
      })
    }
  }
}
</script>

<style scoped>

</style>

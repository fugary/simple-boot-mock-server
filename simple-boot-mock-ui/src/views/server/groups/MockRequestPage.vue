<template>
  <div class="app-container">
    <el-page-header @back="goBack">
      <div slot="content">
        {{ groupItem.groupName }} 【{{ `/mock/${groupItem.groupPath}` }}】
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
    <el-table
      ref="requestsTable"
      v-loading="itemsLoading"
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
          <mock-data-edit v-if="requestScope.row.expandDataFlag" :request="requestScope.row" />
        </template>
      </el-table-column>
      <el-table-column label="请求路径" width="200">
        <template slot-scope="scope">
          <span v-if="scope.row.id === currentItem.id">
            <el-input v-model="currentItem.requestPath" size="mini" placeholder="请求路径" />
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
      <el-table-column label="操作" width="250">
        <template slot-scope="scope">
          <span v-if="scope.row.id===currentItem.id">
            <el-button size="mini" type="primary" @click="handleSave()">保存</el-button>
            <el-button size="mini" @click="handleEdit(scope.row)">重置</el-button>
            <el-button size="mini" @click="cancelEdit()">取消</el-button>
          </span>
          <span v-else>
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑 </el-button>
            <el-button size="mini" type="info" @click="doExpandData(scope.row)">配置响应 </el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import MockGroupApi from '../../../api/server/MockGroupApi'
import MockRequestApi from '../../../api/server/MockRequestApi'
import MockDataEdit from './MockDataEdit'

export default {
  name: 'MockGroupPage',
  components: { MockDataEdit },
  data() {
    const groupId = this.$route.params.groupId
    return {
      groupId,
      groupItem: {},
      items: null,
      itemsLoading: true,
      page: {},
      searchParam: {
        groupId
      },
      currentItem: this.newRequestItem(),
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
      })
    },
    doSearch() {
      MockRequestApi.search(this.searchParam).then(response => {
        console.info(response)
        this.items = response.data
        this.itemsLoading = false
        this.$nextTick(()=>{
          this.doExpandData(this.items[0])
        })
      })
    },
    handleEdit(item = this.newRequestItem()) {
      this.currentItem = Object.assign({}, item)
      if (!item.id) {
        if (!this.items.length || this.items[this.items.length - 1].id !== item.id) {
          this.items.push(item)
        }
      }
    },
    cancelEdit() {
      if (!this.currentItem.id) {
        this.items.pop()
      }
      this.currentItem = this.newRequestItem()
    },
    handleSave() {
      this.itemsLoading = true
      MockRequestApi.saveOrUpdate(this.currentItem).then(response => {
        console.info(response)
        this.doSearch()
        this.cancelEdit()
        this.itemsLoading = false
      }, error => this.itemsLoading = false)
    },
    handleDelete(item) {
      this.$confirm('确定要删除该请求?', '提示').then(() => {
        console.info(item)
        MockRequestApi.removeById(item.id).then(this.doSearch)
      })
    },
    handleDataExpand(request) {
      console.info(arguments)
      request.expandDataFlag = !request.expandDataFlag
    },
    doExpandData(item) {
      if (item) {
        this.$refs.requestsTable.toggleRowExpansion(item)
      }
    }
  }
}
</script>

<style scoped>

</style>

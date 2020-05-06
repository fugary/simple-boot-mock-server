<template>
  <div class="app-container">
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
      v-loading="itemsLoading"
      :data="items"
      element-loading-text="Loading"
      border
      fit
      highlight-current-row
      @row-dblclick="handleEdit($event)">
      <el-table-column label="分组名称" width="200">
        <template slot-scope="scope">
          <span v-if="scope.row.id===currentItem.id">
            <el-input v-model="currentItem.groupName" size="mini" autocomplete="off" />
          </span>
          <span v-else>
            {{ scope.row.groupName }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="描述信息">
        <template slot-scope="scope">
          <span v-if="scope.row.id===currentItem.id">
            <el-input v-model="currentItem.description" autosize type="textarea" size="mini" autocomplete="off" />
          </span>
          <span v-else>
            {{ scope.row.description }}
          </span>
        </template>
      </el-table-column>
      <el-table-column class-name="status-col" label="状态" width="80" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.id===currentItem.id">
            <el-switch v-model="currentItem.status" :active-value="1" :inactive-value="0" />
          </span>
          <span v-else>
            <el-tag effect="dark" size="mini" disable-transitions :type="scope.row.status?'success':'danger'">{{ scope.row.status|statusFilter }}</el-tag>
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="创建时间" width="180">
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
            <el-button size="mini" type="info" @click="handleRequest(scope.row)">配置请求</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import MockGroupApi from '../../../api/server/MockGroupApi'

export default {
  name: 'MockGroupPage',
  data() {
    return {
      items: null,
      itemsLoading: true,
      page: {},
      searchParam: {
        keyword: ''
      },
      currentItem: this.newGroupItem(),
      saveLoading: false
    }
  },
  mounted() {
    this.doSearch()
  },
  methods: {
    newGroupItem() {
      return {
        status: 1
      }
    },
    doSearch() {
      MockGroupApi.search(this.searchParam).then(response => {
        console.info(response)
        this.items = response.data
        this.itemsLoading = false
      })
    },
    handleEdit(item = this.newGroupItem()) {
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
      this.currentItem = this.newGroupItem()
    },
    handleRequest(item) {
      this.$router.push({ name: 'MockRequests', params: { groupId: item.id }})
    },
    handleSave() {
      this.saveLoading = true
      MockGroupApi.saveOrUpdate(this.currentItem).then(response => {
        this.saveLoading = false
        console.info(response)
        this.doSearch()
        this.cancelEdit()
      }, this.cancelEdit)
    },
    handleDelete(item) {
      this.$confirm('确定要删除该分组?', '提示').then(() => {
        console.info(item)
        MockGroupApi.removeById(item.id).then(this.doSearch)
      })
    }
  }
}
</script>

<style scoped>

</style>

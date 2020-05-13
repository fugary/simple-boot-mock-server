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
    <el-form ref="groupForm" class="table-form" :model="currentItem" :rules="groupFormRules">
      <el-table
        :data="items"
        element-loading-text="Loading"
        border
        fit
        highlight-current-row
        @row-dblclick="handleEdit($event)"
      >
        <el-table-column label="分组名称" width="200">
          <template slot-scope="scope">
            <span v-if="scope.row.id===currentItem.id">
              <el-form-item prop="groupName">
                <el-input v-model="currentItem.groupName" size="mini" autocomplete="off" />
              </el-form-item>
            </span>
            <span v-else>
              {{ scope.row.groupName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="groupPath" label="路径ID" width="280" />
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
        <el-table-column class-name="status-col" label="状态" width="60" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.id===currentItem.id">
              <el-switch v-model="currentItem.status" :active-value="1" :inactive-value="0" />
            </span>
            <span v-else>
              <el-tag effect="dark" size="mini" disable-transitions :type="scope.row.status?'success':'danger'">{{ scope.row.status|statusFilter }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="创建时间" width="150">
          <template v-if="scope.row.createDate" slot-scope="scope">
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
              <el-button icon="el-icon-link" size="mini" round type="info" title="配置请求和响应数据" @click="handleRequest(scope.row)" />
              <el-button icon="el-icon-delete-solid" size="mini" round type="danger" title="删除" @click="handleDelete(scope.row)" />
            </span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        layout="total, prev, pager, next"
        :page-size="searchParam.size"
        :current-page.sync="searchParam.current"
        :total="page.total"
        hide-on-single-page
        @current-change="doSearch"
      />
    </el-form>
  </div>
</template>

<script>
import MockGroupApi from '../../../api/server/MockGroupApi'

export default {
  name: 'MockGroupPage',
  data() {
    return {
      items: null,
      searchParam: {
        keyword: '',
        current: 1,
        size: 10
      },
      page: {},
      currentItem: this.newGroupItem(),
      saveLoading: false,
      groupFormRules: {
        groupName: { required: true, message: '分组名称不能为空' }
      }
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
        Object.assign(this.page, response.page || {})
      }).finally(this.cancelLoading)
    },
    handleEdit(item = this.newGroupItem()) {
      this.$cleanNewItem(this.items)
      if (item.id) {
        MockGroupApi.getById(item.id).then(response => {
          this.currentItem = response.data || Object.assign({}, item)
        })
      } else {
        this.currentItem = Object.assign({}, item)
      }
      this.$editTableItem(this.items, item)
    },
    cancelEdit() {
      this.$cleanNewItem(this.items)
      this.currentItem = this.newGroupItem()
    },
    cancelLoading() {
      this.saveLoading = false
    },
    handleRequest(item) {
      this.$router.push({ name: 'MockRequests', params: { groupId: item.id }})
    },
    handleSave() {
      this.$refs.groupForm.validate(valid => {
        if (valid) {
          this.saveLoading = true
          MockGroupApi.saveOrUpdate(this.currentItem, { loading: false }).then(response => {
            console.info(response)
            this.doSearch()
            this.cancelEdit()
          }).finally(this.cancelLoading)
        }
      })
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

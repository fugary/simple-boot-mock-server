<script setup>
import { ref, onMounted, inject, watch } from 'vue'
import DashboardApi from '@/api/mock/DashboardApi'

const topLoading = ref(false)
const topApis = ref([])

const all = inject('dashboard-all', ref(false))

onMounted(() => {
  loadTopApis()
})

watch(all, () => {
  loadTopApis()
})

const loadTopApis = async () => {
  topLoading.value = true
  try {
    const res = await DashboardApi.getTopApis(10, null, all.value)
    if (res && res.success) {
      topApis.value = res.resultData
    }
  } finally {
    topLoading.value = false
  }
}

const getRankTheme = (index) => {
  if (index === 0) return 'danger'
  if (index === 1) return 'warning'
  if (index === 2) return 'primary'
  return 'info'
}
</script>

<template>
  <el-card
    shadow="hover"
    class="chart-card"
  >
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ $t('mock.label.topIntercepted') }}</span>
      </div>
    </template>
    <el-table
      v-loading="topLoading"
      :data="topApis"
      style="width: 100%"
      :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: 'bold' }"
    >
      <el-table-column
        width="80"
        :label="$t('mock.label.rank')"
        align="center"
      >
        <template #default="{ $index }">
          <el-tag
            :type="getRankTheme($index)"
            effect="dark"
            round
            size="small"
            class="rank-tag"
          >
            {{ $index + 1 }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="name"
        :label="$t('mock.label.apiName')"
        min-width="150"
      >
        <template #default="{ row }">
          <div class="api-name">
            {{ row.name || '未命名接口' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="path"
        :label="$t('mock.label.apiPath')"
        min-width="300"
      >
        <template #default="{ row }">
          <div class="api-path">
            <el-icon class="path-icon">
              <Link />
            </el-icon>
            {{ row.path }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="value"
        :label="$t('mock.label.interceptCount')"
        width="180"
        align="right"
      >
        <template #default="{ row }">
          <span class="api-count">{{ row.value }}</span>
          <span class="api-count-unit">{{ $t('mock.label.times') }}</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.chart-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.card-title {
  font-weight: bold;
  font-size: 16px;
  color: var(--el-text-color-primary);
  border-left: 4px solid var(--el-color-warning);
  padding-left: 10px;
}

.rank-tag {
  width: 24px;
  height: 24px;
  padding: 0;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
}

.api-name {
  font-weight: bold;
  color: #303133;
}

.api-path {
  color: #606266;
  background: #f0f2f5;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  font-family: monospace;
  font-size: 13px;
}
.path-icon {
  margin-right: 4px;
  color: #909399;
}

.api-count {
  font-size: 16px;
  font-weight: bold;
  color: var(--el-color-primary);
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
}
.api-count-unit {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}
</style>

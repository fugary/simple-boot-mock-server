<script setup>
import { ref, onMounted, inject, watch, computed } from 'vue'
import DashboardApi from '@/api/mock/DashboardApi'
import MockUrlCopyLink from '@/views/components/mock/MockUrlCopyLink.vue'

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

const columns = computed(() => [
  {
    labelKey: 'mock.label.rank',
    width: '80',
    align: 'center',
    slot: 'rank'
  },
  {
    labelKey: 'mock.label.apiName',
    prop: 'name',
    minWidth: '150',
    slot: 'name'
  },
  {
    labelKey: 'mock.label.apiPath',
    prop: 'path',
    minWidth: '250',
    slot: 'path'
  },
  {
    labelKey: 'mock.label.groupName',
    minWidth: '150',
    slot: 'group'
  },
  {
    labelKey: 'mock.label.interceptCount',
    prop: 'value',
    width: '180',
    align: 'right',
    slot: 'value'
  }
])
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
    <common-table
      v-loading="topLoading"
      :data="topApis"
      :columns="columns"
      style="width: 100%"
      :header-cell-style="{ background: 'var(--el-fill-color-light)', color: 'var(--el-text-color-primary)', fontWeight: 'bold' }"
    >
      <template #rank="{ $index }">
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
      <template #name="{ item }">
        <div class="api-name">
          {{ item.name || $t('mock.label.unnamedApi') }}
        </div>
      </template>
      <template #path="{ item }">
        <MockUrlCopyLink :url-path="item.path">
          {{ item.path }}
        </MockUrlCopyLink>
      </template>
      <template #group="{ item }">
        <div
          v-if="item.group"
          class="group-path"
        >
          <common-icon
            class="path-icon"
            icon="Folder"
          />
          <span class="group-name">{{ item.group.groupName || $t('mock.label.unnamedGroup') }}</span>
          <el-text
            v-if="item.group.userName"
            type="info"
            class="group-user"
          >
            ({{ item.group.userName }})
          </el-text>
        </div>
      </template>
      <template #value="{ item }">
        <span class="api-count">{{ item.value }}</span>
        <span class="api-count-unit">{{ $t('mock.label.times') }}</span>
      </template>
    </common-table>
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
  color: var(--el-text-color-primary);
}

.api-path-container {
  margin-bottom: 4px;
}
.api-path {
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
  word-break: break-all;
  font-family: monospace;
  font-size: 13px;
}
.group-path {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.group-name {
  font-weight: bold;
}
.group-user {
  margin-left: 6px;
  font-style: italic;
  font-size: 12px;
}
.path-icon {
  margin-right: 4px;
  color: var(--el-text-color-placeholder);
  vertical-align: middle;
}
.path-icon.margin-left {
  margin-left: 8px;
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

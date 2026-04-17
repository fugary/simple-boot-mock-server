<script setup>
import { computed, provide } from 'vue'
import { useGlobalSaveSearchParam } from '@/hooks/CommonHooks'
import DashboardMetrics from './components/mock/dashboard/DashboardMetrics.vue'
import DashboardTrendChart from './components/mock/dashboard/DashboardTrendChart.vue'
import DashboardProjectActivity from './components/mock/dashboard/DashboardProjectActivity.vue'
import DashboardPublicVsPrivate from './components/mock/dashboard/DashboardPublicVsPrivate.vue'
import DashboardTopApis from './components/mock/dashboard/DashboardTopApis.vue'
import DashboardMockVsProxy from './components/mock/dashboard/DashboardMockVsProxy.vue'
import DashboardUserCalls from './components/mock/dashboard/DashboardUserCalls.vue'
import DashboardUserGroups from './components/mock/dashboard/DashboardUserGroups.vue'

const { searchParam, saveSearchParam } = useGlobalSaveSearchParam({ all: false })
const all = computed({
  get: () => !!searchParam.value.all,
  set: (value) => {
    searchParam.value.all = !!value
    saveSearchParam()
  }
})

provide('dashboard-all', all)
</script>

<template>
  <el-container class="flex-column dashboard-container">
    <div class="dashboard-header">
      <el-radio-group v-model="all">
        <el-radio-button :value="false">
          {{ $t('mock.label.myData') }}
        </el-radio-button>
        <el-radio-button :value="true">
          {{ $t('mock.label.allData') }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <dashboard-metrics />

    <el-row
      :gutter="20"
      class="margin-top3"
    >
      <el-col :span="14">
        <dashboard-trend-chart />
      </el-col>
      <el-col :span="10">
        <dashboard-mock-vs-proxy />
      </el-col>
    </el-row>

    <el-row
      v-if="all"
      :gutter="20"
      class="margin-top3"
    >
      <el-col :span="12">
        <dashboard-user-calls />
      </el-col>
      <el-col :span="12">
        <dashboard-user-groups />
      </el-col>
    </el-row>

    <el-row
      :gutter="20"
      class="margin-top3"
    >
      <el-col :span="12">
        <dashboard-project-activity />
      </el-col>
      <el-col :span="12">
        <dashboard-public-vs-private />
      </el-col>
    </el-row>

    <el-row
      :gutter="20"
      class="margin-top3"
    >
      <el-col :span="24">
        <dashboard-top-apis />
      </el-col>
    </el-row>
  </el-container>
</template>

<style scoped>
.dashboard-container {
  padding: 0;
}
.dashboard-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}
.margin-top3 {
  margin-top: 20px;
}
</style>

<script setup>
import DashboardMetrics from './components/mock/dashboard/DashboardMetrics.vue'
import DashboardTrendChart from './components/mock/dashboard/DashboardTrendChart.vue'
import DashboardProjectActivity from './components/mock/dashboard/DashboardProjectActivity.vue'
import DashboardTopApis from './components/mock/dashboard/DashboardTopApis.vue'
</script>

<template>
  <el-container class="flex-column dashboard-container">
    <dashboard-metrics />

    <el-row
      :gutter="20"
      class="margin-top3"
    >
      <el-col :span="14">
        <dashboard-trend-chart />
      </el-col>
      <el-col :span="10">
        <dashboard-project-activity />
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
  padding: 10px;
}
.margin-top3 {
  margin-top: 20px;
}
</style>

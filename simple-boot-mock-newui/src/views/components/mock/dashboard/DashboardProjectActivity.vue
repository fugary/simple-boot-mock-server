<script setup>
import { ref, onMounted, inject, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import DashboardApi from '@/api/mock/DashboardApi'
import { $i18nBundle } from '@/messages'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const projectOption = ref({})

const all = inject('dashboard-all', ref(false))

onMounted(() => {
  loadProjectActivity()
})

watch(all, () => {
  loadProjectActivity()
})

const chartLoading = ref(false)

const loadProjectActivity = async () => {
  chartLoading.value = true
  try {
    const res = await DashboardApi.getProjectActivity(30, all.value)
    if (res && res.success) {
      const data = res.resultData.map(item => ({ name: item.name, value: item.value }))
      projectOption.value = {
        color: ['#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'],
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          bottom: '0%',
          left: 'center',
          icon: 'circle',
          itemWidth: 10,
          itemHeight: 10
        },
        series: [
          {
            name: $i18nBundle('mock.label.projectActivity'),
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: { show: false, position: 'center' },
            emphasis: {
              label: {
                show: true,
                fontSize: '18',
                fontWeight: 'bold'
              }
            },
            labelLine: { show: false },
            data
          }
        ]
      }
    }
  } finally {
    chartLoading.value = false
  }
}
</script>

<template>
  <el-card
    v-loading="chartLoading"
    shadow="hover"
    class="chart-card"
  >
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ $t('mock.label.projectActivity30Days') }}</span>
      </div>
    </template>
    <v-chart
      class="chart"
      :option="projectOption"
      autoresize
    />
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
  border-left: 4px solid var(--el-color-success);
  padding-left: 10px;
}
.chart {
  height: 320px;
  width: 100%;
}
</style>

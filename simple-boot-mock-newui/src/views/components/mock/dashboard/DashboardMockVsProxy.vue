<script setup>
import { ref, onMounted, inject, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import DashboardApi from '@/api/mock/DashboardApi'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const ratioOption = ref({})

const all = inject('dashboard-all', ref(false))

onMounted(() => {
  loadRatioActivity()
})

watch(all, () => {
  loadRatioActivity()
})

const chartLoading = ref(false)

const loadRatioActivity = async () => {
  chartLoading.value = true
  try {
    const res = await DashboardApi.getMockVsProxy(7, all.value)
    if (res && res.success) {
      const data = res.resultData.map(item => ({ name: item.name, value: item.value }))
      ratioOption.value = {
        color: ['#409EFF', '#E6A23C'],
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
            name: '调用类型分布',
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
        <span class="card-title">{{ $t('mock.label.mockVsProxy') }}</span>
      </div>
    </template>
    <v-chart
      class="chart"
      :option="ratioOption"
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
  border-left: 4px solid var(--el-color-primary);
  padding-left: 10px;
}
.chart {
  height: 320px;
  width: 100%;
}
</style>

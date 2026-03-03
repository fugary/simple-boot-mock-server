<script setup>
import { ref, onMounted, inject, watch, computed } from 'vue'
import { use, graphic } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import DashboardApi from '@/api/mock/DashboardApi'
import { $i18nBundle } from '@/messages'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

use([CanvasRenderer, LineChart, TooltipComponent, GridComponent])

const globalConfigStore = useGlobalConfigStore()
const isDark = computed(() => globalConfigStore.isDarkTheme)
const themeStr = computed(() => isDark.value ? 'dark' : null)

const trendOption = ref({})

const all = inject('dashboard-all', ref(false))

onMounted(() => {
  loadTrend()
})

watch(all, () => {
  loadTrend()
})

const chartLoading = ref(false)

const loadTrend = async () => {
  chartLoading.value = true
  try {
    const res = await DashboardApi.getTrend(30, all.value)
    if (res && res.success) {
      const data = res.resultData
      const dates = data.map(item => item.name)
      const counts = data.map(item => item.value)
      trendOption.value = {
        dates,
        counts
      }
    }
  } finally {
    chartLoading.value = false
  }
}

const computedOption = computed(() => {
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } }
    },
    backgroundColor: globalConfigStore.currentLocale ? 'transparent' : 'transparent',
    xAxis: {
      type: 'category',
      data: trendOption.value.dates || [],
      boundaryGap: false,
      axisLine: { lineStyle: { color: isDark.value ? '#333' : '#e0e6ed' } },
      axisLabel: { color: isDark.value ? '#C0C4CC' : '#606266' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { type: 'dashed', color: isDark.value ? '#333' : '#ebeef5' } },
      axisLabel: { color: isDark.value ? '#C0C4CC' : '#606266' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    series: [
      {
        name: $i18nBundle('mock.label.totalCalls'),
        type: 'line',
        data: trendOption.value.counts || [],
        smooth: true,
        showSymbol: false,
        areaStyle: {
          color: new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(58,119,255,0.6)' },
            { offset: 1, color: 'rgba(58,119,255,0.05)' }
          ])
        },
        lineStyle: { width: 3, color: '#3a77ff' },
        itemStyle: { color: '#3a77ff' }
      }
    ]
  }
})
</script>

<template>
  <el-card
    v-loading="chartLoading"
    shadow="hover"
    class="chart-card"
  >
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ $t('mock.label.trend30Days') }}</span>
      </div>
    </template>
    <v-chart
      class="chart"
      :option="computedOption"
      :theme="themeStr"
      :update-options="{ notMerge: true }"
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

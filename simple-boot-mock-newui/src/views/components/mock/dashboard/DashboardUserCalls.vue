<script setup>
import { ref, onMounted, inject, watch, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import DashboardApi from '@/api/mock/DashboardApi'
import { $i18nBundle } from '@/messages'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const globalConfigStore = useGlobalConfigStore()
const isDark = computed(() => globalConfigStore.isDarkTheme)
const themeStr = computed(() => isDark.value ? 'dark' : null)

const chartData = ref([])

const all = inject('dashboard-all', ref(false))

onMounted(() => {
  loadUserCalls()
})

watch(all, () => {
  loadUserCalls()
})

const chartLoading = ref(false)

const loadUserCalls = async () => {
  chartLoading.value = true
  try {
    const res = await DashboardApi.getTopUserCalls(10, 30, all.value)
    if (res && res.success) {
      // ECharts horizontal bar charts draw from bottom to top, so reverse the array order
      chartData.value = (res.resultData || []).reverse()
    }
  } finally {
    chartLoading.value = false
  }
}

const computedOption = computed(() => {
  globalConfigStore.currentLocale // Dependency for i18n reactivity
  const textColor = isDark.value ? '#E5EAF3' : '#303133'
  const axisLineColor = isDark.value ? '#4C4D4F' : '#DCDFE6'

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLabel: { color: textColor },
      splitLine: {
        lineStyle: { color: isDark.value ? '#333' : '#eee' }
      }
    },
    yAxis: {
      type: 'category',
      data: chartData.value.map(item => String(item.name || 'Unknown')),
      axisLabel: { color: textColor },
      axisLine: { lineStyle: { color: axisLineColor } }
    },
    series: [
      {
        name: $i18nBundle('mock.label.totalCalls'),
        type: 'bar',
        barWidth: '40%',
        itemStyle: {
          color: '#409EFF',
          borderRadius: [0, 4, 4, 0]
        },
        data: chartData.value.map(item => item.value)
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
        <span class="card-title">{{ $t('mock.label.topActiveUsers') }}</span>
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

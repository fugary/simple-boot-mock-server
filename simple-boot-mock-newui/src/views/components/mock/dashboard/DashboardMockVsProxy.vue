<script setup>
import { ref, onMounted, inject, watch, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import DashboardApi from '@/api/mock/DashboardApi'
import { $i18nBundle } from '@/messages'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const globalConfigStore = useGlobalConfigStore()
const isDark = computed(() => globalConfigStore.isDarkTheme)
const themeStr = computed(() => isDark.value ? 'dark' : null)

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
    const res = await DashboardApi.getMockVsProxy(30, all.value)
    if (res && res.success) {
      const data = res.resultData.map(item => {
        return { rawName: item.name, value: item.value }
      })
      ratioOption.value = {
        data
      }
    }
  } finally {
    chartLoading.value = false
  }
}
const computedOption = computed(() => {
  // Trigger dependency to ensure reactivity on locale swap
  globalConfigStore.currentLocale
  const dynamicData = (ratioOption.value.data || []).map(item => {
    let name = item.rawName
    if (name === 'Mock返回') name = $i18nBundle('mock.label.mockReturn')
    else if (name === '代理返回') name = $i18nBundle('mock.label.proxyReturn')
    else if (name === '无返回') name = $i18nBundle('mock.label.noReturn')
    else if (name === '未匹配') name = $i18nBundle('mock.label.unmatched')
    return { name, value: item.value }
  })

  return {
    color: ['#409EFF', '#E6A23C', '#F56C6C'],
    backgroundColor: 'transparent',
    textStyle: {
      color: isDark.value ? '#E5EAF3' : '#303133'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      bottom: '0%',
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        color: isDark.value ? '#C0C4CC' : '#606266'
      }
    },
    series: [
      {
        name: $i18nBundle('mock.label.mockVsProxy'),
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: isDark.value ? '#141414' : '#fff',
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
        data: dynamicData
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
        <span class="card-title">{{ $t('mock.label.mockVsProxy') }}</span>
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

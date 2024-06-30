import { use, Axis } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import VChart from 'vue-echarts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { computed } from 'vue'
import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'

export const useEchartsConfig = () => {
  const globalConfigStore = useGlobalConfigStore()
  use([
    Axis,
    CanvasRenderer,
    BarChart,
    PieChart,
    LineChart,
    GridComponent,
    TitleComponent,
    TooltipComponent,
    LegendComponent
  ])
  const theme = computed(() => {
    return globalConfigStore.isDarkTheme ? 'dark' : 'light'
  })
  return {
    theme,
    VChart
  }
}

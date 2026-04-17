<script setup>
import { ref, onMounted, inject, watch } from 'vue'
import { useRouter } from 'vue-router'
import DashboardApi from '@/api/mock/DashboardApi'
import {
  buildDashboardLogRoute,
  DashboardLogPreset
} from '@/services/mock/DashboardLogPreset'

const metrics = ref({
  todayTotal: 0,
  todayError: 0,
  totalCalls: 0,
  totalMockGroups: 0,
  totalMockApis: 0,
  totalMockData: 0
})

const router = useRouter()

const all = inject('dashboard-all', ref(false))

const loading = ref(false)

const openMockGroups = () => {
  router.push({ name: 'MockGroups' })
}

const openMockLogs = (preset) => {
  router.push(buildDashboardLogRoute(preset, all.value))
}

const loadMetrics = async () => {
  loading.value = true
  try {
    const res = await DashboardApi.getMetrics(all.value)
    if (res && res.success) {
      metrics.value = res.resultData
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMetrics()
})

watch(all, () => {
  loadMetrics()
})
</script>

<template>
  <el-row
    v-loading="loading"
    :gutter="20"
    class="metric-row"
  >
    <el-col :span="6">
      <el-card
        shadow="hover"
        class="metric-card metric-card--interactive bg-primary"
        @click="openMockGroups()"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.mockGroupCount') }}
            </div>
            <div class="metric-value">
              {{ metrics.totalMockGroups }}
            </div>
          </div>
          <div class="metric-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              class="icon-svg"
            ><path
              d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            /></svg>
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="6">
      <el-card
        shadow="hover"
        class="metric-card metric-card--interactive bg-purple"
        @click="openMockGroups()"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.mockRequestAndData') }}
            </div>
            <div class="metric-value">
              <span>{{ metrics.totalMockApis }}</span>
              <span class="slash-separator">/</span>
              <span>{{ metrics.totalMockData }}</span>
            </div>
          </div>
          <div class="metric-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              class="icon-svg"
            ><path
              d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71m-2.22 8.7a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            /></svg>
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="6">
      <el-card
        shadow="hover"
        class="metric-card metric-card--interactive bg-success"
        @click="openMockLogs(DashboardLogPreset.TODAY_CALLS)"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.todayCallsAndError') }}
            </div>
            <div class="metric-value">
              <span>{{ metrics.todayTotal }}</span>
              <span class="slash-separator">/</span>
              <span :class="{'error-count': metrics.todayError > 0}">{{ metrics.todayError }}</span>
            </div>
          </div>
          <div class="metric-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              class="icon-svg"
            ><path
              d="M23 6l-9.5 9.5-5-5L1 18m22-12v6m0-6h-6"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            /></svg>
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="6">
      <el-card
        shadow="hover"
        class="metric-card metric-card--interactive bg-warning"
        @click="openMockLogs(DashboardLogPreset.TOTAL_CALLS)"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.totalCalls') }}
            </div>
            <div class="metric-value">
              {{ metrics.totalCalls }}
            </div>
          </div>
          <div class="metric-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              class="icon-svg"
            ><path
              d="M22 12h-4l-3 9L9 3l-3 9H2"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            /></svg>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.metric-row {
  display: flex;
  align-items: stretch;
}

.metric-row > .el-col {
  display: flex;
  flex-direction: column;
}

.metric-card {
  border-radius: 12px;
  border: none;
  color: white;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  overflow: hidden;
  position: relative;
  flex: 1;
  height: 100%;
}

.metric-card--interactive {
  cursor: pointer;
}

.metric-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.15);
}

.metric-card :deep(.el-card__body) {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.metric-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
  width: 100%;
}

.metric-info {
  display: flex;
  flex-direction: column;
  width: calc(100% - 66px); /* 100% minus icon width + gap */
}

.metric-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.metric-value {
  font-size: 34px;
  font-weight: bold;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
  line-height: 1;
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
}

.slash-separator {
  margin: 0 6px;
  font-size: 24px;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.6);
}

.error-count {
  color: #ffcccc;
}

.metric-icon {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.icon-svg {
  width: 32px;
  height: 32px;
  opacity: 0.9;
}

.bg-primary {
  background: linear-gradient(135deg, #409EFF 0%, #79bbff 100%);
}
.bg-purple {
  background: linear-gradient(135deg, #B37FEB 0%, #D3ADF7 100%);
}
.bg-cyan {
  background: linear-gradient(135deg, #36cfc9 0%, #87e8de 100%);
}
.bg-success {
  background: linear-gradient(135deg, #67C23A 0%, #95d475 100%);
}
.bg-warning {
  background: linear-gradient(135deg, #E6A23C 0%, #eebe77 100%);
}
.bg-danger {
  background: linear-gradient(135deg, #F56C6C 0%, #fab6b6 100%);
}

.metric-card::after {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  z-index: 1;
}
</style>

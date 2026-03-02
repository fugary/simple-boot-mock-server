<script setup>
import { ref, onMounted, inject, watch } from 'vue'
import { useRouter } from 'vue-router'
import DashboardApi from '@/api/mock/DashboardApi'

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
  >
    <el-col :span="4">
      <el-card
        shadow="hover"
        class="metric-card bg-primary"
        style="cursor: pointer;"
        @click="router.push({ name: 'MockGroups' })"
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

    <el-col :span="4">
      <el-card
        shadow="hover"
        class="metric-card bg-purple"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.mockRequestCount') }}
            </div>
            <div class="metric-value">
              {{ metrics.totalMockApis }}
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

    <el-col :span="4">
      <el-card
        shadow="hover"
        class="metric-card bg-cyan"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.mockDataCount') }}
            </div>
            <div class="metric-value">
              {{ metrics.totalMockData }}
            </div>
          </div>
          <div class="metric-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              class="icon-svg"
            ><path
              d="M12 2l10 5-10 5-10-5 10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            /></svg>
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="4">
      <el-card
        shadow="hover"
        class="metric-card bg-success"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.todayTotal') }}
            </div>
            <div class="metric-value">
              {{ metrics.todayTotal }}
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

    <el-col :span="4">
      <el-card
        shadow="hover"
        class="metric-card bg-warning"
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

    <el-col :span="4">
      <el-card
        shadow="hover"
        class="metric-card bg-danger"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.todayError') }}
            </div>
            <div class="metric-value">
              {{ metrics.todayError }}
            </div>
          </div>
          <div class="metric-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              class="icon-svg"
            ><path
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
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
.metric-card {
  border-radius: 12px;
  border: none;
  color: white;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  overflow: hidden;
  position: relative;
}
.metric-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.15);
}

.metric-card :deep(.el-card__body) {
  padding: 24px;
}

.metric-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
}

.metric-info {
  display: flex;
  flex-direction: column;
}

.metric-title {
  font-size: 15px;
  opacity: 0.9;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 1px;
}

.metric-value {
  font-size: 36px;
  font-weight: bold;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
  line-height: 1;
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

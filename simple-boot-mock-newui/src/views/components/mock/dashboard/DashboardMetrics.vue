<script setup>
import { ref, onMounted } from 'vue'
import DashboardApi from '@/api/mock/DashboardApi'

const metrics = ref({
  todayTotal: 0,
  todayError: 0,
  totalProjects: 0,
  totalMockApis: 0
})

onMounted(async () => {
  const res = await DashboardApi.getMetrics()
  if (res && res.success) {
    metrics.value = res.resultData
  }
})
</script>

<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card
        shadow="hover"
        class="metric-card bg-primary"
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
              d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"
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
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
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
        class="metric-card bg-success"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.mockProjects') }}
            </div>
            <div class="metric-value">
              {{ metrics.totalProjects }}
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
        class="metric-card bg-warning"
      >
        <div class="metric-content">
          <div class="metric-info">
            <div class="metric-title">
              {{ $t('mock.label.mockRequests') }}
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
              d="M13 10V3L4 14h7v7l9-11h-7z"
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
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
}
.bg-danger {
  background: linear-gradient(135deg, #f5222d 0%, #ff7875 100%);
}
.bg-success {
  background: linear-gradient(135deg, #52c41a 0%, #95de64 100%);
}
.bg-warning {
  background: linear-gradient(135deg, #fa8c16 0%, #ffd666 100%);
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

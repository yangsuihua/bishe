<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in statsList" :key="stat.title">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">{{ stat.title }}</div>
              <div class="stat-value">{{ stat.value }}</div>
            </div>
            <div class="stat-icon" :style="{ color: stat.color }">
              <component :is="stat.icon" :size="48" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminAPI } from '@/api/admin'
import { User, VideoCamera, Clock, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const statsList = ref([
  { title: '总用户数', value: 0, icon: 'User', color: '#409EFF' },
  { title: '总视频数', value: 0, icon: 'VideoCamera', color: '#67C23A' },
  { title: '待审核视频', value: 0, icon: 'Clock', color: '#E6A23C' },
  { title: '今日新增用户', value: 0, icon: 'TrendCharts', color: '#F56C6C' }
])

const loadStats = async () => {
  try {
    const response = await adminAPI.getStats()
    if (response.data.code === 200) {
      const data = response.data.data
      statsList.value[0].value = data.totalUsers || 0
      statsList.value[1].value = data.totalVideos || 0
      statsList.value[2].value = data.pendingAuditVideos || 0
      statsList.value[3].value = data.todayNewUsers || 0
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-icon {
  font-size: 48px;
  opacity: 0.8;
}
</style>

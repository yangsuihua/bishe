<template>
  <div class="video-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>视频管理</span>
          <div>
            <el-input
              v-model="query.keyword"
              placeholder="搜索标题或描述"
              style="width: 200px; margin-right: 10px"
              clearable
              @keyup.enter="loadVideoList"
            />
            <el-select v-model="query.status" placeholder="状态" style="width: 120px; margin-right: 10px" clearable>
              <el-option label="待审核" :value="0" />
              <el-option label="已审核" :value="1" />
              <el-option label="审核失败" :value="2" />
            </el-select>
            <el-button type="primary" @click="loadVideoList">搜索</el-button>
          </div>
        </div>
      </template>
      
      <el-table :data="videoList" v-loading="loading" style="width: 100%">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="封面" width="120">
          <template #default="{ row }">
            <el-image
              :src="row.coverUrl"
              style="width: 100px; height: 60px"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="username" label="上传者" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" type="danger" @click="deleteVideo(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadVideoList"
          @current-change="loadVideoList"
        />
      </div>
    </el-card>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="视频详情" width="800px">
      <div v-if="currentVideo">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentVideo.id }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentVideo.title }}</el-descriptions-item>
          <el-descriptions-item label="上传者">{{ currentVideo.username }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentVideo.status)">
              {{ getStatusText(currentVideo.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentVideo.description }}</el-descriptions-item>
          <el-descriptions-item label="上传时间" :span="2">{{ formatDate(currentVideo.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 20px">
          <video v-if="currentVideo.videoUrl" :src="currentVideo.videoUrl" controls style="width: 100%; max-height: 400px" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { videoAPI } from '@/api/video'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const videoList = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const query = ref({
  keyword: '',
  status: null
})

const detailDialogVisible = ref(false)
const currentVideo = ref(null)

const loadVideoList = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      ...query.value
    }
    const response = await videoAPI.getVideoList(params)
    if (response.data.code === 200) {
      const data = response.data.data
      videoList.value = data.records || []
      total.value = data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载视频列表失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = async (video) => {
  try {
    const response = await videoAPI.getVideoDetail(video.id)
    if (response.data.code === 200) {
      currentVideo.value = response.data.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载视频详情失败')
  }
}

const deleteVideo = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该视频吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await videoAPI.deleteVideo(id)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      loadVideoList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待审核',
    1: '已审核',
    2: '审核失败'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadVideoList()
})
</script>

<style scoped>
.video-manage {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

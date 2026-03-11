<template>
  <div class="video-audit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>待审核视频</span>
          <el-button type="primary" @click="loadPendingVideos">刷新</el-button>
        </div>
      </template>
      
      <el-table :data="videoList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="封面" width="120">
          <template #default="{ row }">
            <el-image
              :src="row.coverUrl"
              style="width: 100px; height: 60px"
              fit="cover"
              :preview-src-list="[row.coverUrl]"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="username" label="上传者" width="120" />
        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="previewVideo(row)">预览</el-button>
            <el-button type="success" size="small" @click="showApproveDialog(row.id)">通过</el-button>
            <el-button type="danger" size="small" @click="showRejectDialog(row)">拒绝</el-button>
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
          @size-change="loadPendingVideos"
          @current-change="loadPendingVideos"
        />
      </div>
    </el-card>
    
    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="500px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入拒绝原因"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="rejectVideo">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 通过原因对话框 -->
    <el-dialog v-model="approveDialogVisible" title="通过原因" width="500px">
      <el-input
        v-model="approveReason"
        type="textarea"
        :rows="4"
        placeholder="请输入通过原因（可选）"
      />
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="success" @click="confirmApprove">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 审核记录操作对话框（通过/拒绝） -->
    <el-dialog v-model="auditActionDialogVisible" :title="auditActionTitle" width="500px">
      <el-input
        v-model="auditActionReason"
        type="textarea"
        :rows="4"
        :placeholder="auditActionPlaceholder"
      />
      <template #footer>
        <el-button @click="auditActionDialogVisible = false">取消</el-button>
        <el-button :type="auditActionType" @click="confirmAuditAction">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 视频预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="视频预览" width="800px">
      <div v-if="currentVideo">
        <video
          :src="currentVideo.videoUrl"
          controls
          style="width: 100%; max-height: 500px"
        />
        <div style="margin-top: 20px">
          <p><strong>标题：</strong>{{ currentVideo.title }}</p>
          <p><strong>描述：</strong>{{ currentVideo.description }}</p>
          <p v-if="currentVideo.lastRejectReason" style="margin-top: 10px;">
            <strong style="color: #f56c6c;">上次审核失败原因：</strong>
            <span style="color: #f56c6c;">{{ currentVideo.lastRejectReason }}</span>
          </p>
        </div>
        <div style="margin-top: 20px; display: flex; gap: 10px;">
          <!-- 如果没有最新审核记录，或者最新状态是拒绝，显示"通过"按钮 -->
          <el-button
            v-if="!currentVideo.hasLatestAuditLog || currentVideo.latestAuditStatus === 0"
            type="success"
            @click="showApproveDialog(currentVideo.id)"
          >
            {{ currentVideo.hasLatestAuditLog ? '改为通过' : '通过' }}
          </el-button>
          <!-- 如果没有最新审核记录，或者最新状态是通过，显示"拒绝"按钮 -->
          <el-button
            v-if="!currentVideo.hasLatestAuditLog || currentVideo.latestAuditStatus === 1"
            type="danger"
            @click="showRejectDialogWithReason(currentVideo)"
          >
            {{ currentVideo.hasLatestAuditLog ? '改为拒绝' : '拒绝' }}
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 审核记录 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>审核记录</span>
          <div class="audit-log-filters">
            <el-input
              v-model="logVideoId"
              placeholder="按视频ID筛选"
              style="width: 180px; margin-right: 10px;"
              clearable
              @keyup.enter="loadAuditLogs"
            />
            <el-button @click="loadAuditLogs">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="auditLogs" v-loading="logsLoading" style="width: 100%;">
        <el-table-column prop="id" label="记录ID" width="90" />
        <el-table-column prop="videoId" label="视频ID" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="previewVideoFromLog(row.videoId)">
              {{ row.videoId }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="adminId" label="审核管理员ID" width="120" />
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus === 1 ? 'success' : 'danger'">
              {{ row.auditStatus === 1 ? '通过' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isLatest"
              :type="row.auditStatus === 1 ? 'danger' : 'success'"
              size="small"
              @click="showAuditActionDialog(row)"
            >
              {{ row.auditStatus === 1 ? '改为拒绝' : '改为通过' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="logsPage"
          v-model:page-size="logsSize"
          :total="logsTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadAuditLogs"
          @current-change="loadAuditLogs"
        />
      </div>
    </el-card>
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

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectVideoId = ref(null)

const approveDialogVisible = ref(false)
const approveReason = ref('')
const currentApproveVideoId = ref(null)

const previewDialogVisible = ref(false)
const currentVideo = ref(null)

// 审核记录相关
const logsLoading = ref(false)
const auditLogs = ref([])
const logsPage = ref(1)
const logsSize = ref(10)
const logsTotal = ref(0)
const logVideoId = ref('')

// 审核记录操作对话框
const auditActionDialogVisible = ref(false)
const auditActionReason = ref('')
const auditActionType = ref('success')
const auditActionTitle = ref('')
const auditActionPlaceholder = ref('')
const currentAuditLogId = ref(null)
const currentAuditAction = ref(null) // 'approve' or 'reject'

const loadPendingVideos = async () => {
  loading.value = true
  try {
    const response = await videoAPI.getPendingVideos({ page: page.value, size: size.value })
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

const showApproveDialog = (videoId) => {
  currentApproveVideoId.value = videoId
  approveReason.value = ''
  approveDialogVisible.value = true
}

const confirmApprove = async () => {
  try {
    const response = await videoAPI.approveVideo(currentApproveVideoId.value, approveReason.value)
    if (response.data.code === 200) {
      ElMessage.success('审核通过')
      approveDialogVisible.value = false
      loadPendingVideos()
      loadAuditLogs()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const showRejectDialog = (video) => {
  currentRejectVideoId.value = video.id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const rejectVideo = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  
  try {
    const response = await videoAPI.rejectVideo(currentRejectVideoId.value, rejectReason.value)
    if (response.data.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      loadPendingVideos()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const previewVideo = (video) => {
  currentVideo.value = video
  previewDialogVisible.value = true
}

// 从审核记录打开视频详情
const previewVideoFromLog = async (videoId) => {
  try {
    const response = await videoAPI.getVideoDetailForAudit(videoId)
    if (response.data.code === 200) {
      currentVideo.value = response.data.data
      previewDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载视频详情失败')
  }
}

// 显示拒绝对话框（带预填充原因）
const showRejectDialogWithReason = (video) => {
  currentRejectVideoId.value = video.id
  // 如果有上次拒绝原因，预填充
  rejectReason.value = video.lastRejectReason || ''
  rejectDialogVisible.value = true
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const loadAuditLogs = async () => {
  logsLoading.value = true
  try {
    const params = {
      page: logsPage.value,
      size: logsSize.value
    }
    if (logVideoId.value) {
      const idNum = Number(logVideoId.value)
      if (!Number.isNaN(idNum)) {
        params.videoId = idNum
      }
    }
    const response = await videoAPI.getAuditLogs(params)
    if (response.data.code === 200) {
      const data = response.data.data
      const records = data.records || []
      
      // 获取每个视频的最新审核记录
      const videoIdSet = new Set(records.map(r => r.videoId))
      const latestLogsMap = new Map()
      
      // 并行获取每个视频的最新记录
      await Promise.all(Array.from(videoIdSet).map(async (vid) => {
        try {
          const latestResponse = await videoAPI.getLatestAuditLog(vid)
          if (latestResponse.data.code === 200 && latestResponse.data.data) {
            latestLogsMap.set(vid, latestResponse.data.data.id)
          }
        } catch (error) {
          console.error(`获取视频${vid}的最新审核记录失败:`, error)
        }
      }))
      
      // 标记每条记录是否为最新
      auditLogs.value = records.map(log => ({
        ...log,
        isLatest: latestLogsMap.get(log.videoId) === log.id
      }))
      
      logsTotal.value = data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载审核记录失败')
  } finally {
    logsLoading.value = false
  }
}

const showAuditActionDialog = (log) => {
  currentAuditLogId.value = log.id
  if (log.auditStatus === 1) {
    // 当前是通过，要改为拒绝
    currentAuditAction.value = 'reject'
    auditActionType.value = 'danger'
    auditActionTitle.value = '改为拒绝'
    auditActionPlaceholder.value = '请输入拒绝原因'
    auditActionReason.value = ''
  } else {
    // 当前是拒绝，要改为通过
    currentAuditAction.value = 'approve'
    auditActionType.value = 'success'
    auditActionTitle.value = '改为通过'
    auditActionPlaceholder.value = '请输入通过原因'
    auditActionReason.value = ''
  }
  auditActionDialogVisible.value = true
}

const confirmAuditAction = async () => {
  if (!auditActionReason.value.trim()) {
    ElMessage.warning(`请输入${currentAuditAction.value === 'approve' ? '通过' : '拒绝'}原因`)
    return
  }
  
  try {
    let response
    if (currentAuditAction.value === 'approve') {
      response = await videoAPI.approveFromLog(currentAuditLogId.value, auditActionReason.value)
    } else {
      response = await videoAPI.rejectFromLog(currentAuditLogId.value, auditActionReason.value)
    }
    
    if (response.data.code === 200) {
      ElMessage.success('操作成功')
      auditActionDialogVisible.value = false
      loadPendingVideos()
      loadAuditLogs()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  loadPendingVideos()
  loadAuditLogs()
})
</script>

<style scoped>
.video-audit {
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

.audit-log-filters {
  display: flex;
  align-items: center;
}
</style>

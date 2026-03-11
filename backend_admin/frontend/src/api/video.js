import api from './index'

export const videoAPI = {
  // 视频审核
  getPendingVideos(params) {
    return api.get('/admin/audit/pending', { params })
  },
  approveVideo(videoId, reason) {
    return api.post(`/admin/audit/${videoId}/approve`, reason ? { reason } : {})
  },
  rejectVideo(videoId, reason) {
    return api.post(`/admin/audit/${videoId}/reject`, { action: 'reject', reason })
  },
  getAuditLogs(params) {
    return api.get('/admin/audit/logs', { params })
  },
  // 审核记录改为通过
  approveFromLog(logId, reason) {
    return api.post(`/admin/audit/logs/${logId}/approve`, { reason })
  },
  // 审核记录改为拒绝
  rejectFromLog(logId, reason) {
    return api.post(`/admin/audit/logs/${logId}/reject`, { reason })
  },
  // 获取视频详情（用于审核，包含审核失败原因）
  getVideoDetailForAudit(videoId) {
    return api.get(`/admin/audit/video/${videoId}`)
  },
  // 获取视频的最新审核记录
  getLatestAuditLog(videoId) {
    return api.get(`/admin/audit/logs/latest/${videoId}`)
  },
  // 视频管理
  getVideoList(params) {
    return api.get('/admin/videos', { params })
  },
  getVideoDetail(id) {
    return api.get(`/admin/videos/${id}`)
  },
  updateVideo(id, data) {
    return api.put(`/admin/videos/${id}`, data)
  },
  deleteVideo(id) {
    return api.delete(`/admin/videos/${id}`)
  },
  updateVideoStatus(id, status) {
    return api.post(`/admin/videos/${id}/status`, null, { params: { status } })
  }
}

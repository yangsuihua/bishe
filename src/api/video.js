import apiClient from './index';

// 视频相关API
export const videoAPI = {
  // 上传视频文件
  uploadVideo: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    
    return apiClient.post('/api/video/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 100000, // 60秒超时，因为视频压缩需要时间
    });
  },

  // 上传封面图片
  uploadCover: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    
    return apiClient.post('/api/video/upload/cover', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  // 发布视频
  publishVideo: (data) => {
    return apiClient.post('/api/video/publish', data);
  },

  // 获取视频推荐流
  getVideoFeed: (page = 1, size = 10) => {
    return apiClient.get(`/api/video/feed?page=${page}&size=${size}`);
  },

  // 获取视频详情
  getVideoDetail: (videoId) => {
    return apiClient.get(`/api/video/${videoId}`);
  },

  // 获取用户视频列表
  getUserVideos: (userId, page = 1, size = 10) => {
    return apiClient.get(`/api/video/user/${userId}?page=${page}&size=${size}`);
  },

  // 获取视频分类
  getCategories: () => {
    return apiClient.get('/api/video/categories');
  },
  
  // 根据分类获取视频
  getVideosByCategory: (categoryId, page = 1, size = 10) => {
    return apiClient.get(`/api/video/category/${categoryId}?page=${page}&size=${size}`);
  },
  
  // 获取视频标签
  getVideoTags: () => {
    return apiClient.get('/api/video/tags');
  },
  
  // 批量获取视频详情
  getVideosByIds: (videoIds) => {
    return apiClient.get('/api/video/batch', {
      params: { ids: videoIds.join(',') }
    });
  },
  
  // 获取视频的审核失败原因
  getRejectReason: (videoId) => {
    return apiClient.get(`/api/video/${videoId}/reject-reason`);
  }
};
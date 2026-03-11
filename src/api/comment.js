import apiClient from './index';

// 评论相关API
export const commentAPI = {
  // 获取视频评论列表
  getComments: (videoId, page = 1, size = 20) => {
    return apiClient.get(`/api/interaction/comment/${videoId}`, {
      params: { page, size }
    });
  },

  // 发表评论
  postComment: (commentData) => {
    return apiClient.post('/api/interaction/comment', commentData);
  },

  // 删除评论
  deleteComment: (commentId) => {
    return apiClient.delete(`/api/interaction/comment/${commentId}`);
  }
};
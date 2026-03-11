import apiClient from './index';

/**
 * 用户相关API
 */
const userAPI = {
  /**
   * 获取用户信息
   * @param {Number} userId - 用户ID
   * @returns {Promise}
   */
  getUserProfile(userId) {
    return apiClient.get(`/api/user/profile/${userId}`);
  },

  /**
   * 获取当前用户信息
   * @returns {Promise}
   */
  getCurrentUser() {
    return apiClient.get('/api/user/me');
  },

  /**
   * 更新用户信息
   * @param {Object} userData - 用户数据
   * @returns {Promise}
   */
  updateUser(userData) {
    return apiClient.put('/api/user/update', userData);
  },

  /**
   * 更新用户头像
   * @param {File} avatar - 头像文件
   * @returns {Promise}
   */
  updateAvatar(avatar) {
    const formData = new FormData();
    formData.append('avatar', avatar);
    return apiClient.post('/api/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  /**
   * 获取用户作品列表
   * @param {Number} userId - 用户ID
   * @param {Number} page - 页码
   * @param {Number} size - 每页数量
   * @returns {Promise}
   */
  getUserVideos(userId, page = 1, size = 10) {
    return apiClient.get(`/api/video/user/${userId}`, {
      params: { page, size }
    });
  },

  /**
   * 获取用户收藏的视频列表
   * @param {Number} page - 页码
   * @param {Number} size - 每页数量
   * @returns {Promise}
   */
  getUserFavorites(userId, page = 1, size = 10) {
    return apiClient.get('/api/interaction/favorite/user', {
      params: { page, size },
      headers: {
        'X-User-Id': userId
      }
    });
  },
  
  /**
   * 获取指定用户收藏的视频列表
   * @param {Number} userId - 用户ID
   * @param {Number} page - 页码
   * @param {Number} size - 每页数量
   * @returns {Promise}
   */
  getUserFavoritesByUserId(userId, page = 1, size = 10) {
    return apiClient.get(`/api/interaction/favorite/user/${userId}`, {
      params: { page, size }
    });
  },

  /**
   * 检查视频是否被用户点赞
   * @param {Number} videoId - 视频ID
   * @returns {Promise}
   */
  checkVideoLiked(videoId) {
    return apiClient.get(`/api/interaction/like/check/${videoId}`);
  },
  
  /**
   * 点赞视频
   * @param {Number} videoId - 视频ID
   * @returns {Promise}
   */
  likeVideo(videoId) {
    return apiClient.post(`/api/interaction/like/${videoId}`);
  },
  
  /**
   * 取消点赞视频
   * @param {Number} videoId - 视频ID
   * @returns {Promise}
   */
  unlikeVideo(videoId) {
    return apiClient.delete(`/api/interaction/like/${videoId}`);
  },
  
  /**
   * 获取用户点赞的视频列表
   * @param {Number} userId - 用户ID
   * @param {Number} page - 页码
   * @param {Number} size - 每页数量
   * @returns {Promise}
   */
  getUserLikes(userId, page = 1, size = 10) {
    return apiClient.get('/api/interaction/like/user', {
      params: { page, size },
      headers: {
        'X-User-Id': userId
      }
    });
  },
  
  /**
   * 获取指定用户点赞的视频列表
   * @param {Number} userId - 用户ID
   * @param {Number} page - 页码
   * @param {Number} size - 每页数量
   * @returns {Promise}
   */
  getUserLikesByUserId(userId, page = 1, size = 10) {
    return apiClient.get(`/api/interaction/like/user/${userId}`, {
      params: { page, size }
    });
  },

  /**
   * 获取用户历史记录
   * @param {Number} userId - 用户ID
   * @param {Number} page - 页码
   * @param {Number} size - 每页数量
   * @returns {Promise}
   */
  getUserHistory(userId, page = 1, size = 10) {
    return apiClient.get('/api/interaction/history', {
      params: { page, size },
      headers: {
        'X-User-Id': userId
      }
    });
  },

  /**
   * 记录观看历史
   * @param {Number} videoId - 视频ID
   * @param {Number} watchDuration - 观看时长(秒)
   * @param {Number} watchProgress - 观看进度(百分比 0-100)
   * @returns {Promise}
   */
  recordWatchHistory(videoId, watchDuration = 0, watchProgress = 0) {
    return apiClient.post('/api/interaction/history', {
      videoId,
      watchDuration,
      watchProgress
    });
  },

  /**
   * 获取指定视频的观看历史记录
   * @param {Number} videoId - 视频ID
   * @returns {Promise}
   */
  getVideoHistory(videoId) {
    return apiClient.get(`/api/interaction/history/video/${videoId}`);
  },

  /**
   * 获取用户关注列表
   * @param {Number} userId - 用户ID
   * @returns {Promise}
   */
  getFollowingList(userId) {
    return apiClient.get('/api/user/follow/following', {
      headers: {
        'X-User-Id': userId
      }
    });
  },
  
  /**
   * 获取指定用户关注列表
   * @param {Number} userId - 用户ID
   * @returns {Promise}
   */
  getFollowingListByUserId(userId) {
    return apiClient.get(`/api/user/follow/following/${userId}`);
  },

  /**
   * 获取用户粉丝列表
   * @param {Number} userId - 用户ID
   * @returns {Promise}
   */
  getFollowersList(userId) {
    return apiClient.get('/api/user/follow/followers', {
      headers: {
        'X-User-Id': userId
      }
    });
  },
  
  /**
   * 获取指定用户粉丝列表
   * @param {Number} userId - 用户ID
   * @returns {Promise}
   */
  getFollowersListByUserId(userId) {
    return apiClient.get(`/api/user/follow/followers/${userId}`);
  },

  /**
   * 关注用户
   * @param {Number} targetUserId - 被关注用户ID
   * @returns {Promise}
   */
  followUser(targetUserId) {
    return apiClient.post(`/api/user/follow/${targetUserId}`);
  },

  /**
   * 取消关注用户
   * @param {Number} targetUserId - 被取消关注用户ID
   * @returns {Promise}
   */
  unfollowUser(targetUserId) {
    return apiClient.delete(`/api/user/follow/${targetUserId}`);
  },

  /**
   * 检查是否关注用户
   * @param {Number} targetUserId - 被检查用户ID
   * @returns {Promise}
   */
  isFollowing(targetUserId) {
    return apiClient.get(`/api/user/follow/check/${targetUserId}`);
  },
  
  /**
   * 批量获取视频详情
   * @param {Array} videoIds - 视频ID数组
   * @returns {Promise}
   */
  getVideosByIds(videoIds) {
    return apiClient.get('/api/video/batch', {
      params: { ids: videoIds.join(',') }
    });
  },

  /**
   * 检查视频是否被用户收藏
   * @param {Number} videoId - 视频ID
   * @returns {Promise}
   */
  checkVideoFavorited(videoId) {
    return apiClient.get(`/api/interaction/favorite/check/${videoId}`);
  },
  
  /**
   * 收藏视频
   * @param {Number} videoId - 视频ID
   * @returns {Promise}
   */
  favoriteVideo(videoId) {
    return apiClient.post(`/api/interaction/favorite/${videoId}`);
  },
  
  /**
   * 取消收藏视频
   * @param {Number} videoId - 视频ID
   * @returns {Promise}
   */
  unfavoriteVideo(videoId) {
    return apiClient.delete(`/api/interaction/favorite/${videoId}`);
  }
};

export default userAPI;
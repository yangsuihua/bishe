import apiClient from './index';

/**
 * 好友相关API
 */
const friendAPI = {
  /**
   * 获取好友列表
   * @returns {Promise}
   */
  getFriendList() {
    return apiClient.get('/api/interaction/friendship/friend-list');
  },

  /**
   * 删除好友
   * @param {Number} targetUserId - 目标用户ID
   * @returns {Promise}
   */
  deleteFriend(targetUserId) {
    return apiClient.delete(`/api/interaction/friendship/${targetUserId}`);
  },

  /**
   * 检查是否为好友
   * @param {Number} targetUserId - 目标用户ID
   * @returns {Promise}
   */
  checkFriendship(targetUserId) {
    return apiClient.get('/api/interaction/friendship/check', {
      params: { targetUserId }
    });
  }
};

export default friendAPI;

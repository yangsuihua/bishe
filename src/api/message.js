import apiClient from './index';

/**
 * 消息相关API
 */
const messageAPI = {
  /**
   * 获取两个用户之间的聊天记录
   * @param {Number} targetUserId - 目标用户ID
   * @param {Number} page - 页码
   * @param {Number} size - 每页数量
   * @returns {Promise}
   */
  getChatMessages(targetUserId, page = 1, size = 50) {
    return apiClient.get(`/api/interaction/message/chat/${targetUserId}`, {
      params: { page, size }
    });
  },

  /**
   * 发送消息
   * @param {Object} messageData - 消息数据 { toUserId, type, content, relatedId }
   * @returns {Promise}
   */
  sendMessage(messageData) {
    return apiClient.post('/api/interaction/message', messageData);
  },

  /**
   * 标记消息为已读
   * @param {Number} messageId - 消息ID
   * @returns {Promise}
   */
  markRead(messageId) {
    return apiClient.post(`/api/interaction/message/${messageId}/read`);
  }
};

export default messageAPI;

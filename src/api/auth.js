import apiClient from './index';

/**
 * 认证相关API
 */
const authAPI = {
  /**
   * 用户登录
   * @param {Object} credentials - 登录凭证 {username, password}
   * @returns {Promise}
   */
  login(credentials) {
    return apiClient.post('/api/auth/login', credentials);
  },

  /**
   * 用户注册
   * @param {Object} userData - 注册数据 {username, email, password}
   * @returns {Promise}
   */
  register(userData) {
    return apiClient.post('/api/auth/register', userData);
  },

  /**
   * 退出登录
   * @param {Number} userId - 用户ID
   * @returns {Promise}
   */
  logout(userId) {
    return apiClient.post('/api/auth/logout', {}, {
      headers: {
        'X-User-Id': userId
      }
    });
  },

  /**
   * 刷新Token
   * @param {String} refreshToken - 刷新token
   * @returns {Promise}
   */
  refreshToken(refreshToken) {
    return apiClient.post('/api/auth/refresh', refreshToken);
  }
};

export default authAPI;
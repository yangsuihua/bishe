/**
 * 认证工具函数
 */

/**
 * 检查用户是否已认证
 * @returns {boolean}
 */
export function isAuthenticated() {
  const token = localStorage.getItem('accessToken');
  return !!token;
}

/**
 * 获取存储的token
 * @returns {string|null}
 */
export function getToken() {
  return localStorage.getItem('accessToken');
}

/**
 * 清除认证信息
 */
export function clearAuth() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('user');
}

/**
 * 设置认证信息
 * @param {string} token - 访问令牌
 * @param {Object} user - 用户信息
 */
export function setAuth(token, user) {
  if (token) {
    localStorage.setItem('accessToken', token);
  }
  if (user) {
    localStorage.setItem('user', JSON.stringify(user));
  }
}
import apiClient from './index';

export default {
  /**
   * 获取搜索建议
   * @param {string} prefix 前缀
   */
  getSuggest(prefix) {
    return apiClient.get('/api/search/suggest', {
      params: { prefix }
    });
  },

  /**
   * 获取热搜榜
   * @param {string} type today, recent
   */
  getHotSearch(type = 'today') {
    return apiClient.get('/api/search/hot', {
      params: { type }
    });
  },

  /**
   * 搜索视频
   */
  search(params) {
    return apiClient.get('/api/search', { params });
  }
};

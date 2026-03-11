import axios from 'axios';
import router from '../router';

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8080', // 后端网关地址
  timeout: 30000, // 请求超时时间（增加到30秒，上传大文件需要）
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('accessToken');
    
    // 如果有token且不是白名单接口，则添加Authorization header
    if (token && !isWhiteListUrl(config.url)) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 为非multipart请求设置默认Content-Type
    if (!config.headers['Content-Type'] && !config.headers['content-type'] && 
        !(config.data instanceof FormData)) {
      config.headers['Content-Type'] = 'application/json';
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      // 清除本地存储的token
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      
      // 从store中清除认证状态
      if (window && window.store) {
        window.store.commit('CLEAR_AUTH');
      }
      
      // 重定向到登录页面
      if (router) {
        router.push('/login');
      } else if (window && window.location) {
        window.location.hash = '#/login';
        window.location.reload();
      }
      
      // 提示用户登录已过期
      if (window && window.alert) {
        alert('登录已过期，请重新登录');
      }
    }
    return Promise.reject(error);
  }
);

// 白名单接口，不需要认证
const whiteListUrls = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/refresh',
  '/api/video/list',
  '/api/video/*/detail',
  '/api/search/**'
];

function isWhiteListUrl(url) {
  // 确保url以/api开头，如果不是则添加
  let normalizedUrl = url.startsWith('/api') ? url : '/api' + url;
  
  return whiteListUrls.some(pattern => {
    // 处理通配符模式
    if (pattern.includes('**')) {
      // 处理 /** 模式
      const basePath = pattern.replace('/**', '');
      return normalizedUrl.startsWith(basePath);
    } else if (pattern.includes('*')) {
      // 处理单个 * 模式
      const regexPattern = '^' + pattern.replace(/\*/g, '.*') + '$';
      return new RegExp(regexPattern).test(normalizedUrl);
    } else {
      // 精确匹配
      return normalizedUrl === pattern;
    }
  });
}

export default apiClient;
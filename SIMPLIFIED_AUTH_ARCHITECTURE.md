# 简化版认证架构说明

## 架构概述

按照Vue项目标准最佳实践，我们简化了认证逻辑，移除了不必要的中间层，回归标准实现方式。

## 核心组件

### 1. 路由守卫 (src/router/index.js)
- 使用 `meta: { requiresAuth: true }` 属性标记需要认证的路由
- 在 `beforeEach` 守卫中检查 `to.meta.requiresAuth` 和本地 token
- 未登录用户无法访问需要认证的页面

### 2. 响应拦截器 (src/api/index.js)
- 监听 HTTP 401 状态码
- 自动清除认证信息并跳转到登录页
- 提示用户登录已过期

## 路由配置示例

```javascript
const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }  // 标记需要认证
  },
  {
    path: '/upload',
    name: 'Upload',
    component: Upload,
    meta: { requiresAuth: true }  // 标记需要认证
  },
  {
    path: '/profile/:userId',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }  // 标记需要认证
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { blankLayout: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { blankLayout: true }
  }
]
```

## 路由守卫逻辑

```javascript
router.beforeEach((to, from, next) => {
  // 从本地存储获取认证token
  const token = localStorage.getItem('accessToken');
  
  // 检查目标路由是否需要认证
  if (to.meta.requiresAuth && !token) {
    // 如果需要认证但没有token，重定向到登录页
    next('/login');
  } else {
    // 否则继续
    next();
  }
});
```

## 响应拦截器逻辑

```javascript
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
```

## 优势

1. **简洁性**：移除了复杂的白名单数组和中间件
2. **标准化**：使用Vue Router标准的meta属性
3. **可维护性**：逻辑集中，易于理解和维护
4. **解耦**：页面组件不再需要包含认证检查逻辑
5. **安全性**：保持了原有的安全机制
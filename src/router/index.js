import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Upload from '../views/Upload.vue'
import Profile from '../views/Profile.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Category from '../views/Category.vue'
import Search from '../views/Search.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: Search,
    meta: { requiresAuth: true }
  },
  {
    path: '/upload',
    name: 'Upload',
    component: Upload,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/:userId',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
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
  },
  {
    path: '/category/:categoryId',
    name: 'Category',
    component: Category,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
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

export default router
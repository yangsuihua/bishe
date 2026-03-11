import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const routes = [
  {
    path: '/admin/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAuth: true, title: '仪表盘' }
      },
      {
        path: 'video-audit',
        name: 'VideoAudit',
        component: () => import('@/views/VideoAudit.vue'),
        meta: { requiresAuth: true, title: '视频审核' }
      },
      {
        path: 'video-manage',
        name: 'VideoManage',
        component: () => import('@/views/VideoManage.vue'),
        meta: { requiresAuth: true, title: '视频管理' }
      },
      {
        path: 'tag-manage',
        name: 'TagManage',
        component: () => import('@/views/TagManage.vue'),
        meta: { requiresAuth: true, title: '标签管理' }
      },
      {
        path: 'category-manage',
        name: 'CategoryManage',
        component: () => import('@/views/CategoryManage.vue'),
        meta: { requiresAuth: true, title: '分类管理' }
      },
      {
        path: 'user-manage',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue'),
        meta: { requiresAuth: true, title: '用户管理' }
      }
    ]
  },
  {
    path: '/',
    redirect: '/admin/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/admin/login')
  } else if (to.path === '/admin/login' && authStore.isAuthenticated) {
    next('/admin/dashboard')
  } else {
    next()
  }
})

export default router

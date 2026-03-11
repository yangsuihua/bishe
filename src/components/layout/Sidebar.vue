<template>
    <aside class="sidebar">
      <nav>
        <ul>
          <li 
            class="nav-item" 
            :class="{ active: activeItem === 'home' }"
            @click="navigate('home')"
          >
            🏠 首页推荐
          </li>
          <!-- 动态分类菜单 -->
          <li 
            v-for="category in menuItems" 
            :key="category.id"
            class="nav-item"
            :class="{ active: activeItem === category.id.toString() }"
            @click="navigate(category.id)"
          >
            <component :is="resolveIcon(category.icon)" class="nav-icon" v-if="category.icon" />
            <component :is="resolveIcon('Circle')" class="nav-icon" v-else />
            {{ category.name }}
          </li>
          <div class="divider" style="margin: 10px 0;"></div>
          <li class="nav-item" @click="openMessages">💬 信息</li>
          <li class="nav-item" @click="navigateProfile('following')">❤️ 我的关注</li>
          <li class="nav-item" @click="navigateProfile('favs')">⭐ 我的收藏</li>
          <li class="nav-item" @click="navigateProfile('history')">🕒 观看历史</li>
        </ul>
      </nav>
    </aside>
  </template>
  
  <script>
  import { mapState, mapActions } from 'vuex'
  import * as LucideIcons from 'lucide-vue-next'
  import { videoAPI } from '@/api/video'
  
  export default {
    name: 'Sidebar',
    data() {
      return {
        menuItems: []
      }
    },
    computed: {
      ...mapState(['activeNavItem']),
      activeItem() {
        return this.$route.params.categoryId || this.$route.name?.toLowerCase() || 'home'
      }
    },
    async mounted() {
      await this.loadMenuItems()
    },
    methods: {
      ...mapActions(['setActiveNav']),
      
      async loadMenuItems() {
        try {
          const response = await videoAPI.getCategories()
          if (response.data.code === 200) {
            this.menuItems = response.data.data
          } else {
            console.error('获取分类失败:', response.data.message)
          }
        } catch (error) {
          console.error('获取分类失败:', error)
        }
      },
      
      resolveIcon(iconName) {
        // 如果数据库中没有图标或图标名错误，使用默认图标
        if (!iconName || !LucideIcons[iconName]) {
          return LucideIcons.CircleHelp
        }
        return LucideIcons[iconName]
      },
      
      navigate(item) {
        if (item === 'home') {
          this.$router.push('/')
        } else {
          // 使用ID导航到分类页面
          this.$router.push(`/category/${item}`)
        }
        this.setActiveNav(item.toString())
      },
      
      navigateProfile(tab) {
        this.$router.push(`/profile/me?tab=${tab}`)
      },
      
      openMessages() {
        // 触发打开消息窗口事件
        this.$emit('open-messages')
      }
    }
  }
  </script>
  
  <style scoped>
  .nav-icon {
    width: 16px;
    height: 16px;
    margin-right: 8px;
    vertical-align: middle;
  }
  </style>
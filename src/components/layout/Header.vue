<template>
    <nav class="header">
      <div class="logo" @click="navigateHome">
        <svg class="icon" style="width:24px; height:24px;" viewBox="0 0 24 24">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/>
        </svg>
        <span>VideoPro</span>
      </div>

      <div class="search-bar">
        <div class="search-group">
          <input type="text" placeholder="搜索视频、用户、标签..." v-model="searchQuery">
          <button class="search-btn">🔍</button>
        </div>
      </div>

      <div class="header-right">
        <button class="btn-primary" @click="navigateUpload">上传</button>
        
        <div class="user-menu-container" ref="userMenuContainer">
          <img 
            :src="displayUser.avatar" 
            class="avatar" 
            @click="toggleDropdown"
            id="header-avatar"
          >
          <div class="dropdown-menu" :class="{ show: dropdownOpen }">
            <div class="dropdown-item" @click="navigateProfile('me')">
              <span>👤 个人中心</span>
            </div>
            <div class="dropdown-item" @click="navigateSettings">
              <span>⚙️ 设置与资料</span>
            </div>
            <div class="divider"></div>
            <div class="dropdown-item" @click="toggleTheme">
              <span>{{ themeText }}</span>
            </div>
            <div class="divider"></div>
            <div class="dropdown-item" style="color: #ff4757;" @click="handleLogout">
              <span>🚪 退出登录</span>
            </div>
          </div>
        </div>
      </div>
    </nav>
  </template>
  
  <script>
  import { mapState, mapGetters, mapActions, mapMutations } from 'vuex'
  import authAPI from '../../api/auth'

  export default {
    name: 'Header',
    data() {
      return {
        searchQuery: ''
      }
    },
    computed: {
      ...mapState(['currentUser', 'dropdownOpen']),
      ...mapGetters(['themeText', 'currentUserInfo', 'authToken']),
      // 如果有认证用户信息，则使用认证用户信息，否则使用默认用户信息
      displayUser() {
        return this.currentUserInfo || this.currentUser;
      }
    },
    methods: {
      ...mapActions(['toggleTheme', 'logout']),
      ...mapMutations(['TOGGLE_DROPDOWN', 'CLOSE_DROPDOWN']),
      
      navigateHome() {
        this.$router.push('/')
        this.CLOSE_DROPDOWN()
      },
      
      navigateUpload() {
        this.$router.push('/upload')
        this.CLOSE_DROPDOWN()
      },
      
      navigateProfile(userId) {
        this.$router.push(`/profile/${userId}`)
        this.CLOSE_DROPDOWN()
      },
      
      navigateSettings() {
        this.$router.push('/profile/me?tab=settings')
        this.CLOSE_DROPDOWN()
      },
      
      toggleDropdown() {
        // 阻止事件冒泡，避免触发外部的点击监听器
        event.stopPropagation()
        this.TOGGLE_DROPDOWN()
      },
      
      async handleLogout() {
        try {
          // 调用后端退出登录接口
          const token = this.authToken;
          if (token) { 
            // 从token中解析用户ID（这里假设从store获取用户ID）
            // 实际项目中可能需要从JWT token中解析用户ID
            await authAPI.logout(this.displayUser.id);
          }
        } catch (error) {
          console.error('退出登录接口调用失败:', error);
          // 即使接口调用失败，也要清除本地认证信息
        } finally {
          // 调用store中的logout action清除认证状态
          this.$store.dispatch('logout');
          
          // 确保localStorage中的token也被清除
          localStorage.removeItem('accessToken');
          localStorage.removeItem('user');
          
          // 关闭下拉菜单
          this.CLOSE_DROPDOWN();
          
          // 跳转到登录页面
          this.$router.push('/login');
        }
      }
    },
    mounted() {
      // 点击外部关闭下拉菜单
      document.addEventListener('click', (e) => {
        if (!this.$refs.userMenuContainer?.contains(e.target)) {
          this.CLOSE_DROPDOWN()
        }
      })
    }
  }
  </script>
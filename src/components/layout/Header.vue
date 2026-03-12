<template>
    <nav class="header">
      <div class="logo" @click="navigateHome">
        <svg class="icon" style="width:24px; height:24px;" viewBox="0 0 24 24">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/>
        </svg>
        <span>VideoPro</span>
      </div>

      <div class="search-bar" ref="searchBarContainer">
        <div class="search-group" :class="{ focused: isSearchFocused }">
          <input 
            type="text" 
            placeholder="搜索视频、用户、标签..." 
            v-model="searchQuery"
            @focus="onSearchFocus"
            @input="onSearchInput"
            @keyup.enter="handleSearch(searchQuery)"
          >
          <button class="search-btn" @click="handleSearch(searchQuery)">🔍</button>
        </div>

        <!-- 搜索预览浮层 (Glassmorphism) -->
        <div v-if="isSearchFocused" class="search-floating-layer">
          <!-- 1. 无输入状态：显示热搜榜 -->
          <div v-if="!searchQuery" class="search-section">
            <div class="section-title">
              <span>🔥 全站热搜</span>
              <small @click="fetchHotSearches">刷新</small>
            </div>
            <div class="hot-list">
              <div 
                v-for="(item, index) in hotSearches" 
                :key="index" 
                class="hot-item"
                @click="handleSearch(item.keyword)"
              >
                <span class="hot-index" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                <span class="hot-keyword">{{ item.keyword }}</span>
                <span v-if="index < 3" class="hot-tag">HOT</span>
              </div>
            </div>
          </div>

          <!-- 2. 打字状态：显示搜索建议 -->
          <div v-else class="search-section">
            <div class="section-title">搜索建议</div>
            <div class="suggest-list">
              <div 
                v-for="(suggest, index) in suggestions" 
                :key="index" 
                class="suggest-item"
                @click="handleSearch(suggest)"
              >
                <span class="suggest-icon">🔍</span>
                <span class="suggest-text" v-html="highlightKeyword(suggest, searchQuery)"></span>
              </div>
              <div v-if="suggestions.length === 0" class="empty-suggest">
                未找到相关建议，按回车直接搜索
              </div>
            </div>
          </div>
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
            <div class="dropdown-item" @click="navigateProfile('me?tab=settings')">
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
  import searchAPI from '../../api/search'

  export default {
    name: 'Header',
    data() {
      return {
        searchQuery: '',
        isSearchFocused: false,
        hotSearches: [],
        suggestions: [],
        searchTimer: null
      }
    },
    computed: {
      ...mapState(['currentUser', 'dropdownOpen']),
      ...mapGetters(['themeText', 'currentUserInfo', 'authToken']),
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
      
      navigateProfile(path) {
        this.$router.push(`/profile/${path}`)
        this.CLOSE_DROPDOWN()
      },
      
      onSearchFocus() {
        this.isSearchFocused = true;
        // 强制刷新热搜，确保用户看到最新的
        this.fetchHotSearches();
      },

      async fetchHotSearches() {
        try {
          const res = await searchAPI.getHotSearch('today');
          if (res.data.code === 200) {
            this.hotSearches = res.data.data.items;
          }
        } catch (error) {
          console.error('获取热搜失败', error);
        }
      },

      onSearchInput() {
        // 确保输入时浮层是开启的
        this.isSearchFocused = true;
        
        if (this.searchTimer) clearTimeout(this.searchTimer);
        
        if (!this.searchQuery) {
          this.suggestions = [];
          this.fetchHotSearches(); // 清空时显示热搜
          return;
        }

        this.searchTimer = setTimeout(async () => {
          try {
            const res = await searchAPI.getSuggest(this.searchQuery);
            if (res.data.code === 200) {
              this.suggestions = res.data.data;
            }
          } catch (error) {
            console.error('获取建议失败', error);
          }
        }, 300);
      },

      handleSearch(keyword) {
        if (!keyword || !keyword.trim()) return;
        this.searchQuery = keyword;
        this.isSearchFocused = false;
        this.$router.push({
          path: '/search',
          query: { q: keyword.trim() }
        });
      },

      highlightKeyword(text, keyword) {
        if (!keyword) return text;
        const reg = new RegExp(`(${keyword})`, 'gi');
        return text.replace(reg, '<span class="highlight">$1</span>');
      },

      toggleDropdown(event) {
        event.stopPropagation()
        this.TOGGLE_DROPDOWN()
      },
      
      async handleLogout() {
        try {
          const token = this.authToken;
          if (token) { 
            await authAPI.logout(this.displayUser.id);
          }
        } catch (error) {
          console.error('退出登录接口调用失败:', error);
        } finally {
          this.$store.dispatch('logout');
          localStorage.removeItem('accessToken');
          localStorage.removeItem('user');
          this.CLOSE_DROPDOWN();
          this.$router.push('/login');
        }
      }
    },
    mounted() {
      document.addEventListener('click', (e) => {
        // 点击外部关闭用户下拉菜单
        if (!this.$refs.userMenuContainer?.contains(e.target)) {
          this.CLOSE_DROPDOWN()
        }
        // 点击外部关闭搜索浮层
        if (!this.$refs.searchBarContainer?.contains(e.target)) {
          this.isSearchFocused = false;
        }
      })
    }
  }
  </script>

  <style scoped>
  .search-bar {
    position: relative;
    z-index: 1002;
  }

  .search-group.focused {
    background: var(--bg-card);
    box-shadow: 0 0 0 2px var(--primary-color);
  }

  /* 搜索浮层设计 */
  .search-floating-layer {
    position: absolute;
    top: calc(100% + 10px);
    left: 0;
    right: 0;
    background: rgba(30, 30, 30, 0.85);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 16px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
    padding: 16px;
    animation: slideDown 0.2s ease-out;
  }

  .section-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: var(--text-secondary);
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 12px;
    padding: 0 4px;
  }

  .section-title small {
    cursor: pointer;
    color: var(--primary-color);
  }

  /* 热搜列表样式 */
  .hot-list {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .hot-item {
    display: flex;
    align-items: center;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;
    transition: 0.2s;
  }

  .hot-item:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .hot-index {
    width: 20px;
    font-weight: 800;
    font-size: 14px;
    color: var(--text-secondary);
  }

  .hot-index.rank-1 { color: #ff4757; }
  .hot-index.rank-2 { color: #ffa502; }
  .hot-index.rank-3 { color: #eccc68; }

  .hot-keyword {
    flex: 1;
    font-size: 14px;
    margin: 0 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .hot-tag {
    font-size: 10px;
    background: #ff4757;
    color: white;
    padding: 1px 4px;
    border-radius: 4px;
    font-weight: bold;
  }

  /* 搜索建议样式 */
  .suggest-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: 0.2s;
  }

  .suggest-item:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .suggest-icon {
    margin-right: 12px;
    color: var(--text-secondary);
    font-size: 12px;
  }

  .suggest-text {
    font-size: 14px;
  }

  .suggest-text :deep(.highlight) {
    color: var(--primary-color);
    font-weight: bold;
  }

  .empty-suggest {
    text-align: center;
    padding: 20px;
    color: var(--text-secondary);
    font-size: 13px;
  }

  @keyframes slideDown {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
  }

  /* 适配移动端 */
  @media (max-width: 768px) {
    .search-bar {
      display: none;
    }
  }
  </style>
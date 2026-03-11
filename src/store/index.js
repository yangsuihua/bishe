import { createStore } from 'vuex'
import { videoAPI } from '../api/video';

// 从localStorage中获取初始token
const token = localStorage.getItem('accessToken');

export default createStore({
  state: {
    isDarkMode: true,
    currentUser: null,
    usersDB: {
      'u1': { 
        id: 'u1', 
        name: 'NBA Highlights', 
        handle: '@nba_official', 
        avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=NBA', 
        bio: '篮球精彩集锦', 
        isFollowed: false, 
        privacy: { showFavs: true, showLikes: false },
        stats: { following: 256, followers: '10w', likes: '50k' }
      },
      'u2': { 
        id: 'u2', 
        name: 'AnimeFan', 
        handle: '@anime_lover', 
        avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Anime', 
        bio: '二次元聚集地', 
        isFollowed: true, 
        privacy: { showFavs: false, showLikes: true },
        stats: { following: 512, followers: '25w', likes: '120k' }
      },
      'u3': { 
        id: 'u3', 
        name: 'LinkGaming', 
        handle: '@zelda_pro', 
        avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Link', 
        bio: '任天堂游戏实况', 
        isFollowed: false, 
        privacy: { showFavs: true, showLikes: true },
        stats: { following: 89, followers: '8.2k', likes: '15k' }
      },
      'u4': { 
        id: 'u4', 
        name: 'Foodie', 
        handle: '@yummy_food', 
        avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Food', 
        bio: '深夜食堂', 
        isFollowed: false, 
        privacy: { showFavs: false, showLikes: false },
        stats: { following: 345, followers: '4.5w', likes: '28k' }
      }
    },
    feedVideos: [
      { id: 1, color: '#2f3640', title: '极致控球技巧', userId: 'u1', likes: '2.3w' },
      { id: 2, color: '#c0392b', title: '火影忍者：燃爆剪辑', userId: 'u2', likes: '12w' },
      { id: 3, color: '#27ae60', title: '塞尔达传说：王国之泪', userId: 'u3', likes: '5k' },
      { id: 4, color: '#8e44ad', title: '深夜食堂：日式拉面', userId: 'u4', likes: '8.9k' },
    ],
    // 视频加载控制相关状态
    maxLoadableVideos: 5, // 初始允许加载的视频数量
    watchedVideoCount: 0, // 已观看的视频数量
    activeNavItem: 'home',
    dropdownOpen: false,
    // 认证相关状态
    token: token,
    isAuthenticated: !!token,
    user: token ? JSON.parse(localStorage.getItem('user') || '{}') : null
  },
  mutations: {
    TOGGLE_THEME(state) {
      state.isDarkMode = !state.isDarkMode
      document.body.classList.toggle('light-mode')
    },
    TOGGLE_FOLLOW(state, userId) {
      const user = state.usersDB[userId]
      if (user) {
        user.isFollowed = !user.isFollowed
      }
    },
    SET_ACTIVE_NAV(state, item) {
      state.activeNavItem = item
    },
    TOGGLE_DROPDOWN(state) {
      state.dropdownOpen = !state.dropdownOpen
    },
    CLOSE_DROPDOWN(state) {
      state.dropdownOpen = false
    },
    // 认证相关mutations
    SET_TOKEN(state, token) {
      state.token = token;
      state.isAuthenticated = !!token;
      if (token) {
        localStorage.setItem('accessToken', token);
      } else {
        localStorage.removeItem('accessToken');
      }
    },
    SET_USER(state, user) {
      state.user = user;
      if (user) {
        localStorage.setItem('user', JSON.stringify(user));
      } else {
        localStorage.removeItem('user');
      }
    },
    CLEAR_AUTH(state) {
      state.token = null;
      state.isAuthenticated = false;
      state.user = null;
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
    },
    UPDATE_USERS_DB(state, usersMap) {
      state.usersDB = { ...state.usersDB, ...usersMap };
    },
    SET_FEED_VIDEOS(state, videos) {
      state.feedVideos = videos;
      // 重置加载状态：初始允许加载前5个视频
      state.maxLoadableVideos = 5;
      state.watchedVideoCount = 0;
    },
    INCREMENT_WATCHED_COUNT(state) {
      state.watchedVideoCount++;
      // 每观看5个视频，增加5个可加载视频
      const shouldLoadMore = state.watchedVideoCount % 5 === 0;
      if (shouldLoadMore && state.maxLoadableVideos < state.feedVideos.length) {
        state.maxLoadableVideos = Math.min(
          state.maxLoadableVideos + 5,
          state.feedVideos.length
        );
      }
    }
  },
  actions: {
    toggleTheme({ commit }) {
      commit('TOGGLE_THEME')
    },
    toggleFollow({ commit }, userId) {
      commit('TOGGLE_FOLLOW', userId)
    },
    setActiveNav({ commit }, item) {
      commit('SET_ACTIVE_NAV', item)
    },
    // 认证相关actions
    login({ commit }, { token, user }) {
      commit('SET_TOKEN', token);
      commit('SET_USER', user);
    },
    logout({ commit }) {
      commit('CLEAR_AUTH');
    },
    updateUser({ commit }, user) {
      commit('SET_USER', user);
    },
    setAuthStatus({ commit }, { token, user }) {
      commit('SET_TOKEN', token);
      commit('SET_USER', user);
    },
    async fetchVideoFeed({ commit, dispatch }) {
      try {
        const response = await videoAPI.getVideoFeed();
        if (response.data.code === 200) {
          // 获取视频数据
          const videos = response.data.data || [];
          
          // 为每个视频获取用户信息
          const usersMap = {};
          for (const video of videos) {
            // 后端返回的视频对象中包含用户信息 (username, avatar)
            if (video.userId && (video.username || video.avatar)) {
              usersMap[video.userId] = {
                id: video.userId,
                name: video.username,
                handle: video.handle || `@${video.username ? video.username.toLowerCase() : 'user'}`,
                avatar: video.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${video.username || 'user'}`,
                bio: video.description || '',
                isFollowed: video.isFollowing || false,
                privacy: { showFavs: true, showLikes: true },
                stats: { 
                  following: 0, 
                  followers: (video.viewCount || 0).toString(), 
                  likes: (video.likeCount || 0).toString() 
                }
              };
            }
          }
          
          // 更新用户数据库
          if (Object.keys(usersMap).length > 0) {
            commit('UPDATE_USERS_DB', usersMap);
          }
          
          // 更新视频推荐列表（会自动重置加载状态）
          commit('SET_FEED_VIDEOS', videos);
          
          return videos;
        } else {
          console.error('获取视频推荐失败:', response.data.message);
          return [];
        }
      } catch (error) {
        console.error('获取视频推荐出错:', error);
        throw error;
      }
    },
    // 视频被观看时调用，增加已观看计数
    onVideoWatched({ commit }) {
      commit('INCREMENT_WATCHED_COUNT');
    }
  },
  getters: {
    themeText: state => state.isDarkMode ? '🌙 深色模式' : '☀️ 浅色模式',
    getUser: state => userId => {
      if (userId === 'me') {
        // 如果currentUser为null，返回null而不是undefined
        return state.currentUser;
      }
      return state.usersDB[userId]
    },
    // 认证相关getters
    isAuthenticated: state => state.isAuthenticated,
    currentUserInfo: state => state.user,
    authToken: state => state.token,
    // 判断视频是否可以加载
    isVideoLoadable: state => videoIndex => {
      return videoIndex < state.maxLoadableVideos;
    }
  }
})
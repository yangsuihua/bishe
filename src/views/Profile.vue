<template>
    <div class="view-section">
      <div class="scrollable-content">
        <div v-if="isLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>正在加载用户信息...</p>
        </div>
        <div v-else>
          <!-- 个人头部 -->
          <ProfileHeader 
            :user="profileUser"
            :isMe="isMe"
            @toggleFollow="handleToggleFollow"
            @editProfile="activeTab = 'settings'"
            @update-follow-status="handleFollowStatusUpdate"
          />
    
          <!-- 标签导航 -->
          <ProfileTabs 
            :activeTab="activeTab"
            :isMe="isMe"
            @changeTab="activeTab = $event"
          />
    
          <!-- 标签内容区 -->
          <ProfileContent
            ref="profileContent"
            :activeTab="activeTab"
            :user="profileUser"
            :isMe="isMe"
            @avatar-updated="loadCurrentUser"
          />
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { mapGetters, mapActions } from 'vuex'
  import ProfileHeader from '../components/profile/ProfileHeader.vue'
  import ProfileTabs from '../components/profile/ProfileTabs.vue'
  import ProfileContent from '../components/profile/ProfileContent.vue'
  import userAPI from '../api/user'
  
  export default {
    name: 'Profile',
    components: {
      ProfileHeader,
      ProfileTabs,
      ProfileContent
    },
    data() {
      return {
        activeTab: 'uploads',
        isLoading: false
      }
    },
    computed: {
      ...mapGetters(['getUser', 'currentUserInfo']),
      userId() {
        return this.$route.params.userId
      },
      isMe() {
        return this.userId === 'me'
      },
      profileUser() {
        // 如果是当前用户，使用真实用户数据；否则使用getUser方法
        if (this.isMe) {
          if (this.currentUserInfo) {
            // 将后端返回的用户数据映射到前端期望的格式
            return {
              ...this.currentUserInfo,
              name: this.currentUserInfo.nickname || this.currentUserInfo.username,
              handle: `@${this.currentUserInfo.username || 'user'}`,
              bio: this.currentUserInfo.bio,
              avatar: this.currentUserInfo.avatar,
              birthday: this.currentUserInfo.birthday,
              stats: {
                following: this.currentUserInfo.followingCount,
                followers: this.currentUserInfo.followerCount,
                likes: this.currentUserInfo.likeCount
              },
              isFollowing: this.currentUserInfo.isFollowing
            };
          } else {
            // 如果真实数据还未加载，返回一个基本结构以防止错误
            return {
              name: '加载中...',
              handle: '@loading',
              avatar: '',
              bio: '数据加载中...',
              stats: { following: 0, followers: 0, likes: 0 }
            };
          }
        }
        return this.getUser(this.userId)
      }
    },
    methods: {
      ...mapActions(['toggleFollow']),
      
      handleToggleFollow() {
        if (!this.isMe) {
          this.toggleFollow(this.userId)
        }
      },
      
      handleFollowStatusUpdate({ userId, isFollowing }) {
        // 更新用户的关注状态
        if (this.isMe) {
          // 如果是当前用户，更新vuex中的用户信息
          const updatedUser = { ...this.currentUserInfo, isFollowing };
          this.$store.dispatch('updateUser', updatedUser);
        } else {
          // 如果是其他用户，更新usersDB中的用户信息
          this.$store.commit('UPDATE_USERS_DB', {
            [userId]: { ...this.getUser(userId), isFollowing }
          });
        }
      }
    },
    async mounted() {
      if (this.isMe) {
        this.isLoading = true;
        try {
          await this.loadCurrentUser();
        } finally {
          this.isLoading = false;
        }
      }
    },
    watch: {
      '$route.query.tab': {
        immediate: true,
        handler(tab) {
          if (tab) {
            this.activeTab = tab
            // 当标签变化时，重新加载数据
            this.$nextTick(() => {
              this.$refs.profileContent?.loadActiveTabData?.();
            });
          }
        }
      },
      // 监听路由参数变化，获取用户信息
      'userId': {
        immediate: true,
        handler(newUserId) {
          if (newUserId && !this.isMe) {
            this.loadUserProfile(newUserId);
          }
        }
      },
      // 监听用户信息变化，重新加载内容数据
      'profileUser': {
        handler(newUser, oldUser) {
          if (newUser && newUser.id && (!oldUser || newUser.id !== oldUser.id)) {
            // 用户变化后，重新加载当前标签页的数据
            this.$nextTick(() => {
              this.$refs.profileContent?.loadActiveTabData?.();
            });
          }
        },
        deep: true
      },
      // 监听isMe变化，当从其他用户切换到自己时，重新加载当前用户信息
      'isMe': {
        handler(newIsMe, oldIsMe) {
          if (oldIsMe === false && newIsMe === true) {
            // 从访问其他用户切换到访问自己，重新加载当前用户信息
            this.isLoading = true;
            this.loadCurrentUser().finally(() => {
              this.isLoading = false;
            });
          }
        }
      }
    },
    methods: {
      ...mapActions(['toggleFollow']),
      
      async loadUserProfile(userId) {
        if (!userId) return;
        
        this.isLoading = true;
        try {
          const response = await fetch(`/api/user/profile/${userId}`, {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
            }
          });
          
          if (response.ok) {
            const result = await response.json();
            if (result.code === 200) {
              // 检查当前用户是否关注了该用户
              let isFollowing = result.data.isFollowing;
              
              // 如果后端没有返回 isFollowing 信息，调用 API 检查关注状态
              if (isFollowing === undefined || isFollowing === null) {
                try {
                  const followCheckResponse = await userAPI.isFollowing(userId);
                  isFollowing = followCheckResponse.data.data || false;
                } catch (error) {
                  console.error('检查关注状态失败:', error);
                  isFollowing = false;
                }
              }
              
              // 将用户信息添加到usersDB中，以便其他组件可以访问
              // 注意：不更新vuex中的currentUser，因为这会影响当前登录用户信息
              this.$store.commit('UPDATE_USERS_DB', {
                [userId]: {
                  id: result.data.id, // 确保ID字段正确映射
                  name: result.data.nickname || result.data.username,
                  handle: `@${result.data.username || 'user'}`,
                  avatar: result.data.avatar,
                  bio: result.data.bio,
                  isFollowing: isFollowing,
                  isFavoritesVisible: result.data.isFavoritesVisible,
                  isLikesVisible: result.data.isLikesVisible,
                  followerCount: result.data.followerCount,
                  followingCount: result.data.followingCount,
                  likeCount: result.data.likeCount,
                  videoCount: result.data.videoCount,
                  gender: result.data.gender,
                  birthday: result.data.birthday,
                  stats: {
                    following: result.data.followingCount,
                    followers: result.data.followerCount,
                    likes: result.data.likeCount
                  }
                }
              });
            } else {
              console.error('获取用户信息失败:', result.message);
            }
          } else {
            console.error('获取用户信息请求失败');
          }
        } catch (error) {
          console.error('获取用户信息时出错:', error);
        } finally {
          this.isLoading = false;
        }
      },
      
      loadCurrentUser() {
        return new Promise(async (resolve, reject) => {
          try {
            const response = await fetch('/api/user/me', {
              method: 'GET',
              headers: {
                'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
              }
            });
            
            if (response.ok) {
              const result = await response.json();
              if (result.code === 200) {
                // 更新vuex中的用户信息
                this.$store.dispatch('updateUser', result.data);
                resolve(result.data);
              } else {
                console.error('获取用户信息失败:', result.message);
                reject(new Error(result.message));
              }
            } else {
              console.error('获取用户信息请求失败');
              reject(new Error('获取用户信息请求失败'));
            }
          } catch (error) {
            console.error('获取用户信息时出错:', error);
            reject(error);
          }
        });
      },
      
      handleToggleFollow() {
        if (!this.isMe) {
          this.toggleFollow(this.userId)
        }
      },
      
      handleFollowStatusUpdate({ userId, isFollowing }) {
        // 更新用户的关注状态
        if (this.isMe) {
          // 如果是当前用户，更新vuex中的用户信息
          const updatedUser = { ...this.currentUserInfo, isFollowing };
          this.$store.dispatch('updateUser', updatedUser);
        } else {
          // 如果是其他用户，更新usersDB中的用户信息
          this.$store.commit('UPDATE_USERS_DB', {
            [userId]: { ...this.getUser(userId), isFollowing }
          });
        }
      }
    }
  }
  </script>
  <style scoped>
  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    min-height: 300px;
  }
  
  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 4px solid #f3f3f3;
    border-top: 4px solid var(--primary-color);
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 15px;
  }
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  
  .loading-state p {
    margin: 0;
    color: var(--text-secondary);
    font-size: 14px;
  }
  </style>
<template>
    <div class="profile-header">
      <img :src="user.avatar" class="profile-avatar-lg">
      <div>
        <h1 style="font-size: 24px; margin-bottom: 5px;">{{ user.name }}</h1>
        <p style="color: var(--text-secondary); margin-bottom: 10px;">{{ user.handle }}</p>
        <p style="font-size: 14px; max-width: 600px;">{{ user.bio }}</p>
        <div class="profile-stats">
          <span><span class="stat-num">{{ user.stats.following }}</span> 关注</span>
          <span><span class="stat-num">{{ user.stats.followers }}</span> 粉丝</span>
          <span><span class="stat-num">{{ user.stats.likes }}</span> 获赞</span>
        </div>
      </div>
      <div style="margin-left: auto;">
        <button 
          v-if="isMe"
          class="btn-outline" 
          @click="$emit('editProfile')"
        >
          编辑资料
        </button>
        <button 
          v-else
          v-if="user.id !== $store.getters.currentUserInfo?.id"
          class="btn-outline"
          :class="{ 'btn-followed': user.isFollowing, 'btn-follow': !user.isFollowing }"
          @click="toggleFollowUser"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
    </div>
  </template>
  
  <script>
  import userAPI from '@/api/user';
  
  export default {
    name: 'ProfileHeader',
    props: {
      user: Object,
      isMe: Boolean
    },
    methods: {
      async toggleFollowUser() {
        try {
          if (this.user.isFollowing) {
            // 取消关注
            await userAPI.unfollowUser(this.user.id);
            // 通过 emit 通知父组件更新用户状态
            this.$emit('update-follow-status', { userId: this.user.id, isFollowing: false });
          } else {
            // 关注
            await userAPI.followUser(this.user.id);
            // 通过 emit 通知父组件更新用户状态
            this.$emit('update-follow-status', { userId: this.user.id, isFollowing: true });
          }
        } catch (error) {
          console.error('操作失败:', error);
          alert('操作失败，请重试');
        }
      }
    }
  }
  </script>
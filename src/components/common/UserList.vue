<template>
    <div class="user-list">
      <div 
        v-for="user in users" 
        :key="user.id"
        class="user-card"
      >
        <div class="user-info-group">
          <img 
            :src="user.avatar" 
            style="width:40px; height:40px; border-radius:50%; cursor: pointer;"
            @click="goToUserProfile(user.id)"
          >
          <div>
            <div>{{ user.nickname }}</div>
            <div style="font-size:12px;color:#888">{{ user.bio }}</div>
          </div>
        </div>
        <button 
          v-if="user.id !== $store.getters.currentUserInfo?.id"
          class="btn-outline"
          :class="{ 'btn-followed': user.isFollowing, 'btn-follow': !user.isFollowing }"
          @click="toggleUserFollow(user)"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
    </div>
  </template>
  
  <script>
  import userAPI from '@/api/user';
  import { mapGetters } from 'vuex';
  
  export default {
    name: 'UserList',
    props: {
      users: {
        type: Array,
        default: () => []
      }
    },
    computed: {
      ...mapGetters(['currentUserInfo'])
    },
    methods: {
      async toggleUserFollow(user) {
        try {
          if (user.isFollowing) {
            // 取消关注
            await userAPI.unfollowUser(user.id);
            user.isFollowing = false;
            // 更新关注数
            if (user.followerCount > 0) {
              user.followerCount--;
            }
          } else {
            // 关注
            await userAPI.followUser(user.id);
            user.isFollowing = true;
            user.followerCount++;
          }
        } catch (error) {
          console.error('操作失败:', error);
          alert('操作失败，请重试');
        }
      },
      
      goToUserProfile(userId) {
        // 在当前标签页跳转到用户主页
        this.$router.push(`/profile/${userId}`);
      }
    }
  }
  </script>
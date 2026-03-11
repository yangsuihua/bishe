<template>
    <div class="profile-tabs">
      <div 
        class="tab-btn"
        :class="{ active: activeTab === 'uploads' }"
        @click="$emit('changeTab', 'uploads')"
      >
        作品
      </div>
      <div 
        class="tab-btn"
        :class="{ active: activeTab === 'favs' }"
        @click="$emit('changeTab', 'favs')"
      >
        收藏
      </div>
      <div 
        class="tab-btn"
        :class="{ active: activeTab === 'likes' }"
        @click="$emit('changeTab', 'likes')"
      >
        点赞
      </div>
      <div 
        v-if="isMe"
        class="tab-btn"
        :class="{ active: activeTab === 'history' }"
        @click="$emit('changeTab', 'history')"
      >
        历史
      </div>
      <div 
        class="tab-btn"
        :class="{ active: activeTab === 'following' }"
        @click="$emit('changeTab', 'following')"
      >
        关注
      </div>
      <div 
        class="tab-btn"
        :class="{ active: activeTab === 'followers' }"
        @click="$emit('changeTab', 'followers')"
      >
        粉丝
      </div>
      <div 
        v-if="isMe"
        class="tab-btn"
        :class="{ active: activeTab === 'settings' }"
        @click="$emit('changeTab', 'settings')"
      >
        更新信息
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'ProfileTabs',
    props: {
      activeTab: String,
      isMe: Boolean
    }
  }
  </script>
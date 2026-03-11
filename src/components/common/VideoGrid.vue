<template>
    <div class="video-grid">
      <div 
        v-for="video in videos" 
        :key="video.id"
        class="grid-card"
        @click="handleCardClick(video)"
      >
        <div 
          class="card-cover"
          :style="{ 
            background: `#333 url('${video.cover}') center/cover` 
          }"
        ></div>
        <div class="card-info">
          <div class="card-title">{{ video.title }}</div>
          <div class="card-meta">{{ displayViews(video) }}观看 · {{ video.time }}</div>
          
          <!-- 删除按钮，只在用户访问自己的主页时显示 -->
          <button 
            v-if="showDeleteButton && isMe" 
            class="delete-btn"
            @click.stop="deleteVideo(video.id)"
            title="删除视频"
          >
            🗑️
          </button>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'VideoGrid',
    props: {
      videos: {
        type: Array,
        default: () => []
      },
      showDeleteButton: {
        type: Boolean,
        default: false
      },
      isMe: {
        type: Boolean,
        default: false
      }
    },
    methods: {
      deleteVideo(videoId) {
        this.$emit('delete-video', videoId);
      },
      handleCardClick(video) {
        this.$emit('video-click', video);
      },
      displayViews(video) {
        if (video.viewCount !== undefined && video.viewCount !== null) {
          return video.viewCount;
        }
        return video.views !== undefined ? video.views : 0;
      }
    }
  }
  </script>
  
  <style scoped>
  .grid-card {
    position: relative;
  }
  
  .card-info {
    position: relative;
  }
  
  .delete-btn {
    position: absolute;
    top: 5px;
    right: 5px;
    background: rgba(255, 0, 0, 0.7);
    color: white;
    border: none;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    cursor: pointer;
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10;
  }
  
  .delete-btn:hover {
    background: rgba(255, 0, 0, 0.9);
  }
  </style>
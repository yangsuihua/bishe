<template>
    <div id="view-home" class="view-section">
      <VideoSlide
        v-for="(video, index) in feedVideos"
        :key="video.id"
        :video="video"
        :video-index="index"
        :user="getUser(video.userId)"
        @toggleFollow="handleToggleFollow"
        @video-watched="handleVideoWatched"
      />
    </div>
  </template>
  
  <script>
  import { mapState, mapGetters, mapActions } from 'vuex'
  import VideoSlide from '../components/home/VideoSlide.vue'
  
  export default {
    name: 'Home',
    components: {
      VideoSlide
    },
    computed: {
      ...mapState(['feedVideos']),
      ...mapGetters(['getUser'])
    },
    async mounted() {
      // 在组件挂载时获取视频推荐流
      try {
        await this.fetchVideoFeed();
      } catch (error) {
        console.error('获取视频推荐失败:', error);
      }
    },
    methods: {
      ...mapActions(['toggleFollow', 'fetchVideoFeed', 'onVideoWatched']),
      
      handleToggleFollow(userId) {
        this.toggleFollow(userId)
      },
      
      // 处理视频观看事件
      handleVideoWatched(videoIndex) {
        // 通知store视频已被观看，触发下一批加载逻辑
        this.onVideoWatched();
      }
    }
  }
  </script>
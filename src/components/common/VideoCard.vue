<template>
  <div 
    class="video-card"
    @click="handleClick"
  >
    <div 
      class="card-cover"
      :style="{ 
        background: `#333 url('${cover}') center/cover` 
      }"
    ></div>
    <div class="card-info">
      <div class="card-title">{{ title }}</div>
      <div class="card-meta" v-if="showMeta">
        <span v-if="viewCount !== undefined && viewCount !== null">{{ viewCount }}观看</span>
        <span v-if="viewCount !== undefined && viewCount !== null && time"> · </span>
        <span v-if="time">{{ time }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VideoCard',
  props: {
    video: {
      type: Object,
      required: true
    },
    showMeta: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    cover() {
      return this.video.cover || this.video.coverUrl || '';
    },
    title() {
      return this.video.title || '';
    },
    viewCount() {
      return this.video.viewCount !== undefined ? this.video.viewCount : (this.video.views || 0);
    },
    time() {
      return this.video.time || this.video.createdAt || '';
    }
  },
  methods: {
    handleClick() {
      this.$emit('click', this.video);
    }
  }
}
</script>

<style scoped>
.video-card {
  position: relative;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-color, #1a1a1a);
  transition: transform 0.2s, box-shadow 0.2s;
}

.video-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.card-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.card-info {
  padding: 10px;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color, #fff);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.card-meta {
  font-size: 12px;
  color: #999;
}
</style>

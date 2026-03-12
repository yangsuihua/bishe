<template>
  <div class="search-view scrollable-content">
    <div class="search-header">
      <h2 class="keyword-title">
        <span class="label">搜索结果:</span>
        <span class="keyword">"{{ queryKey }}"</span>
      </h2>
      <div class="search-filters">
        <button 
          v-for="sort in sortOptions" 
          :key="sort.value"
          class="filter-tab"
          :class="{ active: currentSort === sort.value }"
          @click="changeSort(sort.value)"
        >
          {{ sort.label }}
        </button>
      </div>
    </div>

    <!-- 结果网格 -->
    <div v-if="loading" class="loading-state">
      <div class="loader"></div>
      <p>正在为您寻找精彩视频...</p>
    </div>

    <div v-else-if="results.length > 0" class="video-grid">
      <div 
        v-for="video in results" 
        :key="video.id" 
        class="video-card-premium"
        @click="goToVideo(video.id)"
      >
        <div class="card-thumbnail">
          <img :src="video.coverUrl" alt="Thumbnail">
          <div class="video-duration">{{ video.duration || '00:00' }}</div>
          <div class="play-overlay">
            <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
          </div>
        </div>
        <div class="card-content">
          <h3 class="video-title" v-html="highlightKeyword(video.title, queryKey)"></h3>
          <div class="video-metadata">
            <span class="user-link">@{{ video.username || '佚名' }}</span>
            <span class="dot">•</span>
            <span>{{ formatViewCount(video.viewCount) }} 次观看</span>
          </div>
          <div class="video-tags">
            <span v-for="tag in video.tags" :key="tag" class="tag-label">#{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-results">
        <div class="empty-icon">🔍</div>
        <h3>哎呀，没找到相关视频</h3>
        <p>换个关键词试试，或者看看大家都在搜什么？</p>
    </div>
  </div>
</template>

<script>
import searchAPI from '../api/search';

export default {
  name: 'Search',
  data() {
    return {
      queryKey: '',
      results: [],
      loading: false,
      currentSort: 'viewCount',
      sortOptions: [
        { label: '最热优先', value: 'viewCount' },
        { label: '最新发布', value: 'createdAt' }
      ]
    };
  },
  watch: {
    // 监听路由参数变化，实现再次搜索
    '$route.query.q': {
      handler(newVal) {
        if (newVal) {
          this.queryKey = newVal;
          this.performSearch();
        }
      },
      immediate: true
    }
  },
  methods: {
    async performSearch() {
      this.loading = true;
      try {
        const res = await searchAPI.search({
          keyword: this.queryKey,
          sortBy: this.currentSort,
          sortOrder: 'desc',
          page: 1,
          size: 20
        });
        if (res.data.code === 200) {
          this.results = res.data.data;
        }
      } catch (error) {
        console.error('搜索执行失败', error);
      } finally {
        this.loading = false;
      }
    },

    changeSort(sortValue) {
      if (this.currentSort === sortValue) return;
      this.currentSort = sortValue;
      this.performSearch();
    },

    highlightKeyword(text, keyword) {
      if (!keyword) return text;
      const reg = new RegExp(`(${keyword})`, 'gi');
      return text.replace(reg, '<span class="hl">$1</span>');
    },

    formatViewCount(count) {
      if (count >= 10000) return (count / 10000).toFixed(1) + 'w';
      return count || 0;
    },

    goToVideo(id) {
        // 这里的逻辑通常是跳转到主页并播放该视频，或者单视频播放页
        // 暂定跳转到对应的动态（根据你的系统设计）
        this.$router.push(`/?videoId=${id}`);
    }
  }
};
</script>

<style scoped>
.search-view {
  padding: 30px 5% !important;
}

.search-header {
  margin-bottom: 30px;
}

.keyword-title {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 20px;
}

.keyword-title .label {
  color: var(--text-secondary);
  font-weight: 400;
  margin-right: 12px;
}

.keyword-title .keyword {
  color: var(--primary-color);
}

.search-filters {
  display: flex;
  gap: 12px;
}

.filter-tab {
  padding: 6px 16px;
  border-radius: 20px;
  background: var(--bg-input);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.3s;
}

.filter-tab.active {
  background: var(--primary-color);
  color: white;
}

/* 视频卡片样式 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.video-card-premium {
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  border: 1px solid var(--border-color);
}

.video-card-premium:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.3);
}

.card-thumbnail {
  position: relative;
  aspect-ratio: 16/9;
  background: #000;
}

.card-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.8);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-card-premium:hover .play-overlay {
  opacity: 1;
}

.play-overlay svg {
  width: 48px;
  height: 48px;
  fill: white;
}

.card-content {
  padding: 12px;
}

.video-title {
  font-size: 15px;
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-title :deep(.hl) {
  color: var(--primary-color);
}

.video-metadata {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.video-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-label {
  font-size: 11px;
  color: #3498db;
}

/* 状态样式 */
.loading-state, .empty-results {
  text-align: center;
  padding: 100px 0;
}

.loader {
    width: 40px;
    height: 40px;
    border: 4px solid var(--border-color);
    border-top-color: var(--primary-color);
    border-radius: 50%;
    margin: 0 auto 20px;
    animation: spin 1s linear infinite;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

@keyframes spin {
    to { transform: rotate(360deg); }
}
</style>

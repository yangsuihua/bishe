<template>
    <div class="video-slide">
      <div class="video-slide-inner">
        <!-- 高斯模糊背景 -->
        <div 
          class="video-background" 
          :style="{ backgroundImage: `url(${video.coverUrl || 'https://placehold.co/400x300'})`, filter: 'blur(20px) brightness(0.6)' }"
        ></div>
        
        <div class="video-content-wrapper">
          <video 
            ref="videoElement"
            :src="videoSrc" 
            controls
            autoplay
            muted
            class="video-content"
            @click.stop
            @play="onVideoPlay"
            @timeupdate="onVideoTimeUpdate"
            preload="none"
          />
          
          <!-- 评论底部抽屉 -->
          <Transition name="slide-up">
            <div v-if="showComments" class="comment-drawer" @click="closeComments">
              <div class="comment-drawer-content" @click.stop>
                <!-- 抽屉头部 -->
                <div class="comment-drawer-header">
                  <div class="comment-drawer-title">
                    <h3>评论 ({{ comments.length }})</h3>
                  </div>
                  <div class="comment-drawer-close" @click="closeComments">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18 15l-6-6-6 6" />
                    </svg>
                  </div>
                </div>
                
                <!-- 评论列表 -->
                <div class="comments-list">
                  <div v-for="comment in comments" :key="comment.id" class="comment-item">
                    <img :src="comment.avatar || 'https://placehold.co/40x40'" class="comment-avatar" />
                    <div class="comment-content">
                      <div class="comment-header-info">
                        <span class="comment-username">{{ comment.username }}</span>
                        <span class="comment-time">{{ comment.createdAt }}</span>
                      </div>
                      <p class="comment-text">{{ comment.content }}</p>
                      <div class="comment-actions">
                        <span class="reply-btn" @click="replyToComment(comment)">回复</span>
                        <span 
                          v-if="comment.userId === $store.state.user?.id" 
                          class="delete-btn" 
                          @click="deleteComment(comment.id)"
                        >
                          删除
                        </span>
                      </div>
                      
                      <!-- 回复列表 -->
                      <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
                        <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                          <img :src="reply.avatar || 'https://placehold.co/40x40'" class="comment-avatar" />
                          <div class="comment-content">
                            <div class="comment-header-info">
                              <span class="comment-username">{{ reply.username }}</span>
                              <span class="comment-time">{{ reply.createdAt }}</span>
                            </div>
                            <p class="comment-text">
                              <span class="reply-to">回复 @{{ reply.replyUsername }}:</span> 
                              {{ reply.content }}
                            </p>
                            <div class="comment-actions">
                              <span 
                                v-if="reply.userId === $store.state.user?.id" 
                                class="delete-btn" 
                                @click="deleteComment(reply.id)"
                              >
                                删除
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 输入框区域 -->
                <div class="comment-input">
                  <div class="comment-input-wrapper">
                    <!-- 回复提示标签 -->
                    <div v-if="replyToUsername" class="reply-tag">
                      <span class="reply-text">回复 @{{ replyToUsername }}</span>
                      <span class="reply-close" @click="cancelReply">×</span>
                    </div>
                              
                    <input 
                      v-model="newCommentContent" 
                      :placeholder="replyToUsername ? '写回复...' : '写评论...'" 
                      maxlength="500"
                      @keydown.ctrl.enter="submitComment"
                      class="comment-input-field"
                    />
                    <button 
                      @click="submitComment" 
                      :disabled="!newCommentContent.trim()" 
                      class="comment-send-btn"
                      :class="{ 'active': newCommentContent.trim() }"
                    >
                      发送
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </div>
        
        <div class="slide-actions">
          <!-- 作者头像 -->
          <div class="action-avatar-box" @click="goToUserProfile(video.userId)">
            <img :src="user.avatar" class="action-avatar-img">
          </div>
  
          <div class="action-btn">
            <div 
              class="action-circle" 
              :class="{ 'liked': isVideoLiked }"
              @click="toggleLike"
              style="cursor: pointer;"
            >
              {{ isVideoLiked ? '❤️' : '🤍' }}
            </div>
            <span>{{ localLikeCount }}</span>
          </div>
          <div class="action-btn">
            <div class="action-circle" @click="toggleComments" style="cursor: pointer;">
              💬
            </div>
            <span>{{ video.commentCount || 0 }}</span>
          </div>
          <div class="action-btn">
            <div 
              class="action-circle" 
              :class="{ 'favorited': isVideoFavorited }"
              @click="toggleFavorite"
              style="cursor: pointer;"
            >
              {{ isVideoFavorited ? '⭐' : '☆' }}
            </div>
            <span>{{ localFavoriteCount }}</span>
          </div>
          <div class="action-btn">
            <div class="action-circle" @click="handleForward" style="cursor: pointer;">↗️</div>
            <span>{{ video.shareCount || 0 }}</span>
          </div>
        </div>
        
        <!-- 转发弹窗 -->
        <ForwardModal 
          :visible="showForwardModal"
          :video-id="video.id"
          :video-title="video.title"
          @update:visible="showForwardModal = $event"
          @forward-success="handleForwardSuccess"
        />
        
        <div class="slide-info">
          <h3 style="margin-bottom:8px;">{{ user.name || video.username }}</h3>
          <p>{{ video.title }} #推荐 #热门</p>
          <div style="margin-top:10px; font-size:12px;">🎵 原始声音 - {{ user.name || video.username }}</div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import userAPI from '@/api/user';
  import { commentAPI } from '@/api/comment';
  import { videoAPI } from '@/api/video';
  import ForwardModal from '@/components/common/ForwardModal.vue';
  
  export default {
    name: 'VideoSlide',
    components: {
      ForwardModal
    },
    props: {
      video: Object,
      user: Object,
      videoIndex: {
        type: Number,
        required: true
      }
    },
    data() {
      return {
        isVideoLiked: false,
        localLikeCount: this.video.likeCount || this.video.likes || 0,
        isVideoFavorited: false,
        localFavoriteCount: this.video.favoriteCount || 0,
        showComments: false,
        comments: [],
        newCommentContent: '',
        loadingComments: false,
        replyToCommentId: null,
        replyToUserId: null,
        replyToUsername: null,
        historyRecorded: false, // 标记是否已记录观看历史
        watchStartTime: null, // 观看开始时间
        watchDuration: 0, // 观看时长（秒）
        nextUpdateInterval: null, // 下次更新的间隔时间（毫秒）
        showForwardModal: false, // 转发弹窗显示状态
        hasNotifiedWatch: false // 标记是否已经通知过观看事件
      };
    },
    computed: {
      // 判断视频是否可以加载
      isLoadable() {
        return this.$store.getters.isVideoLoadable(this.videoIndex);
      },
      // 视频源地址：只有在可加载时才返回真实URL，否则返回空
      videoSrc() {
        if (this.isLoadable && this.video.videoUrl) {
          return this.video.videoUrl;
        }
        // 如果不可加载，返回一个占位符或空字符串，阻止浏览器加载
        return '';
      }
    },
    async mounted() {
      await this.checkLikeStatus();
      await this.checkFavoriteStatus();
    },
    watch: {
      // 监听video prop的变化，更新本地点赞数和点赞状态
      video: {
        handler(newVideo) {
          // 当视频数据更新时，使用服务器返回的最新点赞数
          this.localLikeCount = newVideo.likeCount || newVideo.likes || 0;
          this.localFavoriteCount = newVideo.favoriteCount || 0;
          // 重置观看历史记录状态，以便新视频可以记录
          this.historyRecorded = false;
          this.watchStartTime = null;
          this.watchDuration = 0;
          this.nextUpdateInterval = null;
          this.hasNotifiedWatch = false;
          // 重新检查当前视频的点赞和收藏状态
          this.$nextTick(() => {
            this.checkLikeStatus();
            this.checkFavoriteStatus();
          });
        },
        deep: true
      },
      // 监听isLoadable变化，当视频变为可加载时，确保视频加载
      isLoadable(newVal, oldVal) {
        if (newVal && !oldVal && this.$refs.videoElement) {
          // 如果视频从不可加载变为可加载状态，确保视频元素加载
          this.$nextTick(() => {
            const videoElement = this.$refs.videoElement;
            if (videoElement && this.video.videoUrl && videoElement.src !== this.video.videoUrl) {
              // Vue的响应式绑定应该已经更新了src，但为了确保，我们手动触发load
              videoElement.load();
            }
          });
        }
      }
    },
    methods: {
      goToUserProfile(userId) {
        // 在当前标签页跳转到用户主页
        this.$router.push(`/profile/${userId}`);
      },
      
      async checkLikeStatus() {
        try {
          if (this.video.id) {
            const response = await userAPI.checkVideoLiked(this.video.id);
            if (response.data.code === 200) {
              this.isVideoLiked = response.data.data || false;
            }
          }
        } catch (error) {
          console.error('检查点赞状态失败:', error);
        }
      },
      
      async toggleLike() {
        if (!this.$store.getters.isAuthenticated) {
          // 如果用户未登录，跳转到登录页
          this.$router.push('/login');
          return;
        }
        
        try {
          if (this.isVideoLiked) {
            // 取消点赞
            await userAPI.unlikeVideo(this.video.id);
            this.isVideoLiked = false;
            // 减少本地点赞数
            if (this.localLikeCount > 0) {
              this.localLikeCount--;
            }
          } else {
            // 点赞
            await userAPI.likeVideo(this.video.id);
            this.isVideoLiked = true;
            // 增加本地点赞数
            this.localLikeCount++;
          }
        } catch (error) {
          console.error('点赞操作失败:', error);
          alert('点赞操作失败，请重试');
        }
      },
      
      // 显示评论弹窗
      async toggleComments() {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        
        if (!this.showComments) {
          await this.loadComments();
        }
        this.showComments = !this.showComments;
      },
      
      // 关闭评论弹窗
      closeComments() {
        this.showComments = false;
      },
      
      // 加载评论列表
      async loadComments() {
        if (!this.video.id) return;
        
        this.loadingComments = true;
        try {
          const response = await commentAPI.getComments(this.video.id);
          if (response.data.code === 200) {
            // 处理父子级评论
            this.comments = this.processComments(response.data.data || []);
          }
        } catch (error) {
          console.error('加载评论失败:', error);
          this.comments = [];
        } finally {
          this.loadingComments = false;
        }
      },
      
      // 处理父子级评论结构
      processComments(comments) {
        // 将评论按parentId分组
        const commentMap = {};
        const parentComments = [];
        
        comments.forEach(comment => {
          comment.replies = [];
          if (comment.parentId === 0 || comment.parentId === null) {
            parentComments.push(comment);
          } else {
            commentMap[comment.id] = comment;
          }
        });
        
        // 将回复添加到父评论的replies数组中
        comments.forEach(comment => {
          if (comment.parentId !== 0 && comment.parentId !== null) {
            const parent = parentComments.find(c => c.id === comment.parentId);
            if (parent) {
              parent.replies.push(comment);
            } else {
              // 查找更高层级的评论作为父评论
              const parentInMap = commentMap[comment.parentId];
              if (parentInMap) {
                parentInMap.replies.push(comment);
              }
            }
          }
        });
        
        return parentComments;
      },
      
      // 提交评论
      async submitComment() {
        if (!this.newCommentContent.trim()) return;
        
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        
        try {
          const commentData = {
            videoId: this.video.id,
            content: this.newCommentContent,
            parentId: this.replyToCommentId || 0, // 如果是回复，则设置父评论ID
            replyUserId: this.replyToUserId || null // 如果是回复，则设置被回复用户ID
          };
          
          const response = await commentAPI.postComment(commentData);
          if (response.data.code === 200) {
            const newComment = response.data.data;
            
            if (this.replyToCommentId) {
              // 如果是回复，添加到对应父评论的回复列表中
              const parentComment = this.comments.find(c => c.id === this.replyToCommentId);
              if (parentComment) {
                if (!parentComment.replies) {
                  parentComment.replies = [];
                }
                parentComment.replies.unshift(newComment);
              }
              
              // 清除回复状态
              this.replyToCommentId = null;
              this.replyToUserId = null;
              this.replyToUsername = null;
            } else {
              // 如果是顶级评论，添加到主列表
              this.comments.unshift(newComment);
            }
            
            this.newCommentContent = '';
            
            // 更新视频的评论计数
            if (this.video.commentCount !== undefined) {
              this.video.commentCount++;
            }
          }
        } catch (error) {
          console.error('提交评论失败:', error);
          alert('提交评论失败，请重试');
        }
      },
      
      // 回复评论
      replyToComment(comment) {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        
        // 设置回复的目标评论
        this.replyToCommentId = comment.id;
        this.replyToUserId = comment.userId;
        this.replyToUsername = comment.username;
        
        // 自动聚焦到输入框
        this.$nextTick(() => {
          const input = document.querySelector('.comment-input-field');
          if (input) {
            input.focus();
          }
        });
      },
      
      // 取消回复
      cancelReply() {
        this.replyToCommentId = null;
        this.replyToUserId = null;
        this.replyToUsername = null;
        this.newCommentContent = '';
      },
      
      // 删除评论
      async deleteComment(commentId) {
        if (!confirm('确定要删除这条评论吗？')) {
          return;
        }
        
        try {
          await commentAPI.deleteComment(commentId);
          
          // 从本地列表中移除评论
          this.removeCommentFromList(commentId);
          
          // 重新获取视频详情以更新评论数
          if (this.video.id) {
            const response = await videoAPI.getVideoDetail(this.video.id);
            if (response.data.code === 200) {
              this.video.commentCount = response.data.data.commentCount || 0;
            }
          }
        } catch (error) {
          console.error('删除评论失败:', error);
          alert('删除评论失败，请重试');
        }
      },
      
      // 从列表中移除评论
      removeCommentFromList(commentId) {
        // 先尝试从回复列表中删除
        for (let i = 0; i < this.comments.length; i++) {
          const replies = this.comments[i].replies || [];
          const replyIndex = replies.findIndex(reply => reply.id === commentId);
          if (replyIndex > -1) {
            replies.splice(replyIndex, 1);
            return;
          }
        }
        
        // 如果不在回复列表中，尝试从主评论列表删除
        const commentIndex = this.comments.findIndex(comment => comment.id === commentId);
        if (commentIndex > -1) {
          this.comments.splice(commentIndex, 1);
        }
      },
      
      // 检查收藏状态
      async checkFavoriteStatus() {
        try {
          if (this.video.id) {
            const response = await userAPI.checkVideoFavorited(this.video.id);
            if (response.data.code === 200) {
              this.isVideoFavorited = response.data.data || false;
            }
          }
        } catch (error) {
          console.error('检查收藏状态失败:', error);
        }
      },
      
      // 切换收藏状态
      async toggleFavorite() {
        if (!this.$store.getters.isAuthenticated) {
          // 如果用户未登录，跳转到登录页
          this.$router.push('/login');
          return;
        }
        
        try {
          if (this.isVideoFavorited) {
            // 取消收藏
            await userAPI.unfavoriteVideo(this.video.id);
            this.isVideoFavorited = false;
            // 减少本地收藏数
            if (this.localFavoriteCount > 0) {
              this.localFavoriteCount--;
            }
          } else {
            // 收藏
            await userAPI.favoriteVideo(this.video.id);
            this.isVideoFavorited = true;
            // 增加本地收藏数
            this.localFavoriteCount++;
          }
        } catch (error) {
          console.error('收藏操作失败:', error);
          alert('收藏操作失败，请重试');
        }
      },
      
      // 视频开始播放时记录观看历史
      async onVideoPlay() {
        // 通知父组件视频已被观看（用于分批次加载）
        if (!this.hasNotifiedWatch) {
          this.$emit('video-watched', this.videoIndex);
          this.hasNotifiedWatch = true;
        }
        
        // 只在推荐首页记录观看历史
        if (this.$route.path !== '/') {
          return;
        }
        
        // 如果已经记录过，不再重复记录
        if (this.historyRecorded) {
          return;
        }
        
        // 如果用户未登录，不记录
        if (!this.$store.getters.isAuthenticated) {
          return;
        }
        
        // 记录观看开始时间
        this.watchStartTime = Date.now();
        // 初始化随机更新间隔（3-5秒）
        this.nextUpdateInterval = Math.floor(Math.random() * 2000) + 3000; // 3000-5000毫秒
        
        try {
          // 初始记录，观看时长为0，进度为0
          await userAPI.recordWatchHistory(this.video.id, 0, 0);
          this.historyRecorded = true;
        } catch (error) {
          console.error('记录观看历史失败:', error);
        }
      },
      
      // 视频播放进度更新时更新观看历史
      async onVideoTimeUpdate() {
        // 只在推荐首页且已记录过初始历史时更新
        if (this.$route.path !== '/' || !this.historyRecorded) {
          return;
        }
        
        // 如果用户未登录，不更新
        if (!this.$store.getters.isAuthenticated) {
          return;
        }
        
        const videoElement = this.$refs.videoElement;
        if (!videoElement) {
          return;
        }
        
        // 计算观看时长（秒）
        const currentTime = Math.floor(videoElement.currentTime);
        const duration = videoElement.duration;
        
        if (!duration || duration === 0) {
          return;
        }
        
        // 计算观看进度（百分比）
        const progress = (videoElement.currentTime / duration) * 100;
        
        // 节流：每3-5秒随机更新一次观看历史
        const now = Date.now();
        
        // 如果是第一次更新或已经超过上次设定的间隔时间，则更新
        if (this.watchStartTime && this.nextUpdateInterval && now - this.watchStartTime < this.nextUpdateInterval) {
          return;
        }
        
        // 生成3-5秒之间的随机间隔（3000-5000毫秒）
        this.nextUpdateInterval = Math.floor(Math.random() * 2000) + 3000; // 3000-5000毫秒
        this.watchStartTime = now;
        this.watchDuration = currentTime;
        
        try {
          await userAPI.recordWatchHistory(this.video.id, currentTime, progress);
        } catch (error) {
          console.error('更新观看历史失败:', error);
        }
      },
      
      // 处理转发
      handleForward() {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        this.showForwardModal = true;
      },
      
      // 转发成功回调
      handleForwardSuccess(friend) {
        alert(`已转发给 ${friend.nickname || friend.username}`)
        // 更新分享数
        if (this.video.shareCount !== undefined) {
          this.video.shareCount = (this.video.shareCount || 0) + 1
        } else {
          this.$set(this.video, 'shareCount', (this.video.shareCount || 0) + 1)
        }
      }
    }
  }
  </script>
  
  <style scoped>
  .video-slide {
    position: relative;
    width: 100%;
    height: 100%;
    overflow: hidden;
  }
  
  .action-circle.liked {
    color: #ff4757;
    transform: scale(1.1);
    transition: all 0.2s ease;
  }
  
  .action-circle.favorited {
    color: #ffd700;
    transform: scale(1.1);
    transition: all 0.2s ease;
  }
  
  .video-slide-inner {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  
  .video-background {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-size: cover;
    background-position: center;
    z-index: 1;
  }
  
  .video-content-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
  }
  
  .video-content {
    position: relative;
    width: 100%;
    height: 100%;
    object-fit: contain;
    z-index: 2;
  }
  
  .slide-actions {
    position: absolute;
    right: 20px;
    bottom: 100px; /* 调整位置，避免与视频描述重叠 */
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    z-index: 10;
  }
  
  .action-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }
  
  /* 评论弹窗样式 */
  .comment-modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .comment-modal-content {
    background: white;
    width: 500px;
    max-height: 80vh;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  
  .comment-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #eee;
  }
  
  .comment-header h3 {
    margin: 0;
    font-size: 16px;
    font-weight: bold;
  }
  
  .close-btn {
    font-size: 24px;
    cursor: pointer;
    color: #999;
  }
  
  .close-btn:hover {
    color: #333;
  }
  
  .comment-input {
    display: flex;
    padding: 16px;
    border-bottom: 1px solid #eee;
  }
  
  .comment-input textarea {
    flex: 1;
    padding: 12px;
    border: 1px solid #ddd;
    border-radius: 4px;
    resize: none;
    height: 60px;
    font-size: 14px;
  }
  
  .comment-input textarea:focus {
    outline: none;
    border-color: #007bff;
  }
  
  .submit-btn {
    margin-left: 12px;
    padding: 12px 20px;
    background: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
  }
  
  .submit-btn:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
  
  .comments-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
  }
  
  .comment-item {
    display: flex;
    margin-bottom: 16px;
  }
  
  .comment-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 12px;
    object-fit: cover;
  }
  
  .comment-content {
    flex: 1;
  }
  
  .comment-header-info {
    display: flex;
    align-items: center;
    margin-bottom: 4px;
  }
  
  .comment-username {
    font-weight: bold;
    margin-right: 8px;
  }
  
  .comment-time {
    color: #999;
    font-size: 12px;
  }
  
  .comment-text {
    margin: 8px 0;
    line-height: 1.4;
  }
  
  .reply-to {
    color: #007bff;
    font-weight: bold;
  }
  
  .comment-actions {
    display: flex;
    gap: 16px;
  }
  
  .reply-btn, .delete-btn {
    color: #007bff;
    cursor: pointer;
    font-size: 12px;
  }
  
  .reply-btn:hover, .delete-btn:hover {
    text-decoration: underline;
  }
  
  .delete-btn {
    color: #dc3545;
  }
  
  .replies-list {
    margin-top: 12px;
    padding-left: 30px;
  }
  
  .reply-item {
    display: flex;
    margin-bottom: 12px;
    padding: 8px 0;
    border-bottom: 1px solid #f5f5f5;
  }
  
  .action-circle {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 18px;
  }
  
  .action-avatar-box {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .action-avatar-img {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    border: 2px solid white;
  }
  
  .action-follow-plus {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #ff4757;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    cursor: pointer;
  }
  
  .slide-info {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    padding: 20px 70px 40px 20px;
    background: linear-gradient(to top, rgba(0,0,0,0.9), transparent);
    color: white;
    z-index: 10;
  }
  
  /* 评论底部抽屉样式 */
  .comment-drawer {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.4);
    z-index: 1000;
    display: flex;
    justify-content: center;
    align-items: flex-end;
  }
  
  .comment-drawer-content {
    width: 100%;
    height: 65%;
    background: rgba(0, 0, 0, 0.65);
    border-top-left-radius: 16px;
    border-top-right-radius: 16px;
    display: flex;
    flex-direction: column;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  }
  
  .comment-drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #3f3f46;
    background: rgba(0, 0, 0, 0.65);
  }
  
  .comment-drawer-title h3 {
    margin: 0;
    font-size: 16px;
    font-weight: bold;
    color: white;
  }
  
  .comment-drawer-close {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #a1a1aa;
    transition: all 0.2s ease;
  }
  
  .comment-drawer-close:hover {
    background: #3f3f46;
    color: white;
  }
  
  .comments-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background: transparent;
  }
  
  .comment-item {
    display: flex;
    margin-bottom: 16px;
  }
  
  .comment-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    margin-right: 12px;
    object-fit: cover;
  }
  
  .comment-content {
    flex: 1;
    color: white;
  }
  
  .comment-header-info {
    display: flex;
    align-items: center;
    margin-bottom: 4px;
  }
  
  .comment-username {
    font-weight: bold;
    margin-right: 8px;
    color: white;
  }
  
  .comment-time {
    color: #a1a1aa;
    font-size: 12px;
  }
  
  .comment-text {
    margin: 8px 0;
    line-height: 1.4;
    color: white;
  }
  
  .reply-to {
    color: #60a5fa;
    font-weight: bold;
  }
  
  .comment-actions {
    display: flex;
    gap: 16px;
    margin-top: 4px;
  }
  
  .reply-btn, .delete-btn {
    color: #60a5fa;
    cursor: pointer;
    font-size: 12px;
  }
  
  .reply-btn:hover, .delete-btn:hover {
    text-decoration: underline;
  }
  
  .delete-btn {
    color: #f87171;
  }
  
  .replies-list {
    margin-top: 8px;
    padding-left: 24px;
    border-left: 1px solid #3f3f46;
  }
  
  .reply-item {
    display: flex;
    margin-bottom: 12px;
    padding: 8px 0;
    border-bottom: 1px solid #2d2d2d;
  }
  
  /* 输入框区域 */
  .comment-input {
    padding: 16px;
    border-top: 1px solid #3f3f46;
    background: rgba(0, 0, 0, 0.65);
  }
  
  .comment-input-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(45, 45, 45, 0.5);
    border-radius: 24px;
    padding: 8px 12px;
  }
  
  .reply-tag {
    display: flex;
    align-items: center;
    background: rgba(96, 165, 250, 0.2);
    border: 1px solid rgba(96, 165, 250, 0.3);
    border-radius: 16px;
    padding: 4px 8px;
    margin-right: 8px;
    font-size: 12px;
    color: #60a5fa;
  }
  
  .reply-text {
    margin-right: 6px;
  }
  
  .reply-close {
    cursor: pointer;
    font-size: 14px;
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background-color 0.2s;
  }
  
  .reply-close:hover {
    background: rgba(152, 152, 152, 0.3);
  }
  
  .comment-input-field {
    flex: 1;
    border: none;
    background: transparent;
    color: white;
    font-size: 14px;
    outline: none;
    padding: 4px 0;
  }
  
  .comment-input-field::placeholder {
    color: #71717a;
  }
  
  .comment-send-btn {
    background: #3f3f46;
    color: #a1a1aa;
    border: none;
    border-radius: 16px;
    padding: 6px 12px;
    font-size: 12px;
    cursor: not-allowed;
    transition: all 0.2s ease;
  }
  
  .comment-send-btn.active {
    background: #60a5fa;
    color: white;
    cursor: pointer;
  }
  
  .comment-send-btn:disabled {
    opacity: 0.5;
  }
  
  /* 底部抽屉滑动动画 */
  .slide-up-enter-active, .slide-up-leave-active {
    transition: transform 0.3s ease-out;
  }
  
  .slide-up-enter-from {
    transform: translateY(100%);
  }
  
  .slide-up-leave-to {
    transform: translateY(100%);
  }
  </style>
<template>
  <div class="category-page">
    <div class="category-header">
      <h1 class="category-title">{{ categoryName }}</h1>
    </div>
    
    <div class="video-grid" v-if="videos.length > 0">
      <div 
        v-for="video in videos" 
        :key="video.id" 
        class="video-card"
        @mouseenter="onVideoHover(video)"
        @mouseleave="onVideoLeave(video)"
        @click="openVideoModal(video)"
      >
        <div class="video-thumbnail">
          <img 
            v-if="hoveredVideoId !== video.id" 
            :src="video.coverUrl" 
            :alt="video.title" 
            class="thumbnail-image"
          />
          <video 
            v-else
            :ref="`preview-${video.id}`"
            :src="video.videoUrl" 
            class="preview-video"
            muted
            loop
            playsinline
            @click.stop
          />
          <!-- 悬停播放按钮遮罩 -->
          <div 
            v-if="hoveredVideoId !== video.id" 
            class="hover-overlay"
          >
            <div class="play-icon">▶</div>
          </div>
        </div>
        <div class="video-info">
          <h3 class="video-title">{{ video.title }}</h3>
          <div class="video-meta">
            <span class="video-author">{{ video.username }}</span>
            <span class="video-time">{{ video.createdAt }}</span>
          </div>
          <div class="video-stats">
            <span class="video-views">{{ formatViews(video.viewCount) }} 播放</span>
            <span class="video-likes">{{ video.likeCount }} 赞</span>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else-if="loading" class="loading">
      加载中...
    </div>
    
    <div v-else class="no-videos">
      该分类下暂无视频
    </div>

    <!-- 视频播放弹窗 -->
    <Transition name="modal">
      <div 
        v-if="selectedVideo" 
        class="video-modal-overlay"
        @click="closeVideoModal"
      >
        <div 
          class="video-modal-container"
          @click.stop
        >
          <div class="video-modal-content">
            <!-- 关闭按钮 -->
            <button 
              class="modal-close-btn"
              @click="closeVideoModal"
            >
              ✕
            </button>
            
            <!-- 视频播放区域 -->
            <div class="video-player-wrapper">
              <video 
                ref="modalVideoPlayer"
                :src="selectedVideo.videoUrl" 
                :poster="selectedVideo.coverUrl"
                class="modal-video-player"
                controls
                autoplay
          @play="onModalVideoPlay"
          @timeupdate="onModalVideoTimeUpdate"
              />
            </div>

            <!-- 视频详情区域 -->
            <div class="video-details">
              <div class="video-details-header">
                <div class="video-details-title-section">
                  <h2 class="video-details-title">{{ selectedVideo.title }}</h2>
                  <div class="video-details-meta">
                    <span v-for="tag in selectedVideo.tags" :key="tag" class="video-tag">{{ tag }}</span>
                    <span>{{ formatViews(selectedVideo.viewCount) }}次观看</span>
                    <span>•</span>
                    <span>{{ selectedVideo.createdAt }}</span>
                  </div>
                </div>
                <div class="video-actions">
                  <button 
                    class="action-btn"
                    :class="{ 'liked': isVideoLiked }"
                    @click="toggleLike"
                  >
                    <span>{{ isVideoLiked ? '❤️' : '🤍' }}</span>
                    <span>{{ localLikeCount }}</span>
                  </button>
                  <button 
                    class="action-btn"
                    @click="focusCommentInput"
                  >
                    <span>💬</span>
                    <span>{{ selectedVideo.commentCount || 0 }}</span>
                  </button>
                  <button 
                    class="action-btn"
                    :class="{ 'favorited': isVideoFavorited }"
                    @click="toggleFavorite"
                  >
                    <span>{{ isVideoFavorited ? '⭐' : '☆' }}</span>
                    <span>{{ localFavoriteCount }}</span>
                  </button>
                  <button class="action-btn" @click="handleForward">
                    <span>分享</span>
                    <span>{{ selectedVideo.shareCount || 0 }}</span>
                  </button>
                </div>
              </div>
              
              <!-- 转发弹窗 -->
              <ForwardModal 
                :visible="showForwardModal"
                :video-id="selectedVideo.id"
                :video-title="selectedVideo.title"
                @update:visible="showForwardModal = $event"
                @forward-success="handleForwardSuccess"
              />

              <div class="video-details-divider"></div>

              <!-- 作者信息 -->
              <div class="video-author-section">
                <div class="author-avatar">
                  {{ selectedVideo.username ? selectedVideo.username[0].toUpperCase() : 'U' }}
                </div>
                <div class="author-info">
                  <div class="author-name">{{ selectedVideo.username }}</div>
                  <div class="author-stats">1.2万 粉丝</div>
                  <p class="video-description">
                    {{ selectedVideo.description }}
                  </p>
                </div>
              </div>

              <!-- 评论区域 -->
              <div class="comments-section">
                <div class="comments-header">
                  <h3 class="comments-title">评论 ({{ comments.length }})</h3>
                </div>
                
                <!-- 评论列表 -->
                <div class="comments-list">
                  <div v-if="loadingComments" class="comments-loading">
                    加载中...
                  </div>
                  <div v-else-if="comments.length === 0" class="comments-empty">
                    暂无评论，快来发表第一条评论吧！
                  </div>
                  <div v-else>
                    <div 
                      v-for="comment in comments" 
                      :key="comment.id" 
                      class="comment-item"
                    >
                      <img 
                        :src="comment.avatar || 'https://placehold.co/32x32'" 
                        class="comment-avatar" 
                        :alt="comment.username"
                      />
                      <div class="comment-content">
                        <div class="comment-header-info">
                          <span class="comment-username">{{ comment.username }}</span>
                          <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                        </div>
                        <p class="comment-text">{{ comment.content }}</p>
                        <div class="comment-actions">
                          <span 
                            class="reply-btn" 
                            @click="replyToComment(comment)"
                          >
                            回复
                          </span>
                          <span 
                            v-if="comment.userId === $store.state.user?.id" 
                            class="delete-btn" 
                            @click="deleteComment(comment.id)"
                          >
                            删除
                          </span>
                        </div>
                        
                        <!-- 回复列表 -->
                        <div 
                          v-if="comment.replies && comment.replies.length > 0" 
                          class="replies-list"
                        >
                          <div 
                            v-for="reply in comment.replies" 
                            :key="reply.id" 
                            class="reply-item"
                          >
                            <img 
                              :src="reply.avatar || 'https://placehold.co/28x28'" 
                              class="comment-avatar small" 
                              :alt="reply.username"
                            />
                            <div class="comment-content">
                              <div class="comment-header-info">
                                <span class="comment-username">{{ reply.username }}</span>
                                <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                              </div>
                              <p class="comment-text">
                                <span 
                                  v-if="reply.replyUsername" 
                                  class="reply-to"
                                >
                                  回复 @{{ reply.replyUsername }}:
                                </span>
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
                </div>
                
                <!-- 评论输入区域 -->
                <div class="comment-input-section">
                  <div class="comment-input-wrapper">
                    <!-- 回复提示标签 -->
                    <div v-if="replyToUsername" class="reply-tag">
                      <span class="reply-text">回复 @{{ replyToUsername }}</span>
                      <span class="reply-close" @click="cancelReply">×</span>
                    </div>
                    
                    <input 
                      ref="commentInput"
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
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script>
import { videoAPI } from '@/api/video'
import userAPI from '@/api/user'
import { commentAPI } from '@/api/comment'
import ForwardModal from '@/components/common/ForwardModal.vue'

export default {
  name: 'Category',
  components: {
    ForwardModal
  },
  data() {
    return {
      categoryName: '',
      videos: [],
      loading: false,
      hoveredVideoId: null, // 当前悬停的视频ID
      selectedVideo: null, // 选中的视频（用于弹窗）
      // 点赞相关
      isVideoLiked: false,
      localLikeCount: 0,
      // 收藏相关
      isVideoFavorited: false,
      localFavoriteCount: 0,
      // 评论相关
      comments: [],
      newCommentContent: '',
      loadingComments: false,
      replyToCommentId: null,
      replyToUserId: null,
      replyToUsername: null,
      // 观看历史相关（与首页推荐逻辑一致）
      historyRecorded: false,
      watchStartTime: null,
      nextUpdateInterval: null,
      watchDuration: 0,
      // 转发相关
      showForwardModal: false
    }
  },
  async mounted() {
    await this.loadCategoryVideos()
  },
  watch: {
    '$route.params.categoryId'() {
      this.loadCategoryVideos()
    }
  },
  methods: {
    async loadCategoryVideos() {
      const categoryId = this.$route.params.categoryId
      if (!categoryId) return
      
      this.loading = true
      try {
        // 使用新的按分类获取视频的API
        const response = await videoAPI.getVideosByCategory(categoryId, 1, 20)
        if (response.data.code === 200) {
          this.videos = response.data.data
        }
        
        // 获取分类名称
        const categoriesResponse = await videoAPI.getCategories()
        if (categoriesResponse.data.code === 200) {
          const category = categoriesResponse.data.data.find(cat => cat.id == categoryId)
          this.categoryName = category ? category.name : '未知分类'
        }
      } catch (error) {
        console.error('加载分类视频失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    goToVideoDetail(videoId) {
      this.$router.push(`/video/${videoId}`)
    },
    
    // 鼠标悬停时自动播放预览
    onVideoHover(video) {
      // 如果已经有弹窗打开，不处理hover
      if (this.selectedVideo) return
      
      this.hoveredVideoId = video.id
      this.$nextTick(() => {
        // 使用数组形式获取ref，因为v-for会创建多个元素
        const refKey = `preview-${video.id}`
        const videoElements = this.$refs[refKey]
        if (Array.isArray(videoElements) && videoElements.length > 0) {
          videoElements[0].play().catch(err => {
            console.error('预览播放失败:', err)
          })
        } else if (videoElements) {
          videoElements.play().catch(err => {
            console.error('预览播放失败:', err)
          })
        }
      })
    },
    
    // 鼠标离开时停止预览
    onVideoLeave(video) {
      // 如果已经有弹窗打开，不处理leave
      if (this.selectedVideo) return
      
      this.hoveredVideoId = null
      this.$nextTick(() => {
        const refKey = `preview-${video.id}`
        const videoElements = this.$refs[refKey]
        if (Array.isArray(videoElements) && videoElements.length > 0) {
          videoElements[0].pause()
          videoElements[0].currentTime = 0
        } else if (videoElements) {
          videoElements.pause()
          videoElements.currentTime = 0
        }
      })
    },
    
    // 打开视频播放弹窗
    openVideoModal(video) {
      if (!video || !video.id) {
        return
      }
      
      // 先停止所有预览视频
      this.hoveredVideoId = null
      
      // 停止所有预览视频
      this.$nextTick(() => {
        Object.keys(this.$refs).forEach(refKey => {
          if (refKey && refKey.startsWith('preview-')) {
            const videoElements = this.$refs[refKey]
            if (Array.isArray(videoElements)) {
              videoElements.forEach(el => {
                if (el && typeof el.pause === 'function') {
                  el.pause()
                  el.currentTime = 0
                }
              })
            } else if (videoElements && typeof videoElements.pause === 'function') {
              videoElements.pause()
              videoElements.currentTime = 0
            }
          }
        })
      })
      
      // 重置观看历史状态
      this.historyRecorded = false
      this.watchStartTime = null
      this.nextUpdateInterval = null
      this.watchDuration = 0
      
      // 从列表中获取最新的视频信息（确保评论数等数据是最新的）
      const latestVideo = this.videos.find(v => v.id === video.id)
      const videoData = latestVideo ? { ...latestVideo } : { ...video }
      
      // 立即设置选中视频，让弹窗显示
      this.selectedVideo = videoData  // 创建新对象确保响应式
      this.localLikeCount = videoData.likeCount || 0
      this.isVideoLiked = false
      this.localFavoriteCount = videoData.favoriteCount || 0
      this.isVideoFavorited = false
      this.comments = []
      this.newCommentContent = ''
      this.replyToCommentId = null
      this.replyToUserId = null
      this.replyToUsername = null
      
      // 禁止背景滚动
      document.body.style.overflow = 'hidden'
      
      // 等待弹窗渲染后自动播放和加载数据
      this.$nextTick(async () => {
        // 自动播放视频
        const player = this.$refs.modalVideoPlayer
        if (player) {
          const videoElement = Array.isArray(player) ? player[0] : player
          if (videoElement && typeof videoElement.play === 'function') {
            videoElement.play().catch(err => {
              console.error('自动播放失败:', err)
            })
          }
        }
        
        // 重新获取视频详情以确保数据是最新的（特别是commentCount）
        try {
          const detailResponse = await videoAPI.getVideoDetail(videoData.id)
          if (detailResponse.data.code === 200 && detailResponse.data.data) {
            const videoDetail = detailResponse.data.data
            // 更新selectedVideo的数据，保持响应式
            this.selectedVideo = { ...this.selectedVideo, ...videoDetail }
            this.localLikeCount = videoDetail.likeCount || 0
            this.localFavoriteCount = videoDetail.favoriteCount || 0
          }
        } catch (error) {
          console.error('获取视频详情失败:', error)
          // 如果获取详情失败，继续使用列表中的数据
        }
        
        // 加载点赞状态、收藏状态和评论
        this.checkLikeStatus()
        this.checkFavoriteStatus()
        this.loadComments()
      })
    },
    
    // 关闭视频播放弹窗
    closeVideoModal() {
      // 停止视频播放
      const player = this.$refs.modalVideoPlayer
      if (player) {
        const videoElement = Array.isArray(player) ? player[0] : player
        if (videoElement && videoElement.pause) {
          videoElement.pause()
        }
      }
      
      this.selectedVideo = null
      // 恢复背景滚动
      document.body.style.overflow = 'unset'
      // 清除评论数据
      this.comments = []
      this.newCommentContent = ''
      this.replyToCommentId = null
      this.replyToUserId = null
      this.replyToUsername = null
      this.historyRecorded = false
      this.watchStartTime = null
      this.nextUpdateInterval = null
      this.watchDuration = 0
    },

    // 弹窗视频开始播放时记录观看历史（逻辑与首页推荐一致）
    async onModalVideoPlay() {
      // 如果已经记录过或未登录，则不重复记录
      if (this.historyRecorded || !this.$store.getters.isAuthenticated) {
        return
      }
      if (!this.selectedVideo || !this.selectedVideo.id) return

      // 记录开始时间和随机更新间隔
      this.watchStartTime = Date.now()
      this.nextUpdateInterval = Math.floor(Math.random() * 2000) + 3000 // 3000-5000ms

      try {
        // 初始记录：watchDuration=0, watchProgress=0
        await userAPI.recordWatchHistory(this.selectedVideo.id, 0, 0)
        this.historyRecorded = true
      } catch (error) {
        console.error('记录观看历史失败:', error)
      }
    },

    // 弹窗视频播放进度更新时，按3-5秒随机间隔更新观看历史
    async onModalVideoTimeUpdate() {
      if (!this.historyRecorded || !this.$store.getters.isAuthenticated) {
        return
      }
      if (!this.selectedVideo || !this.selectedVideo.id) return

      const player = this.$refs.modalVideoPlayer
      const videoElement = Array.isArray(player) ? player[0] : player
      if (!videoElement) return

      const duration = videoElement.duration
      if (!duration || duration === 0) return

      const currentTime = Math.floor(videoElement.currentTime)
      const progress = (videoElement.currentTime / duration) * 100

      const now = Date.now()
      if (this.watchStartTime && this.nextUpdateInterval && now - this.watchStartTime < this.nextUpdateInterval) {
        return
      }

      this.nextUpdateInterval = Math.floor(Math.random() * 2000) + 3000
      this.watchStartTime = now
      this.watchDuration = currentTime

      try {
        await userAPI.recordWatchHistory(this.selectedVideo.id, currentTime, progress)
      } catch (error) {
        console.error('更新观看历史失败:', error)
      }
    },
    
    // 检查点赞状态
    async checkLikeStatus() {
      try {
        if (this.selectedVideo && this.selectedVideo.id) {
          const response = await userAPI.checkVideoLiked(this.selectedVideo.id)
          if (response.data.code === 200) {
            this.isVideoLiked = response.data.data || false
          }
        }
      } catch (error) {
        console.error('检查点赞状态失败:', error)
      }
    },
    
    // 检查收藏状态
    async checkFavoriteStatus() {
      try {
        if (this.selectedVideo && this.selectedVideo.id) {
          const response = await userAPI.checkVideoFavorited(this.selectedVideo.id)
          if (response.data.code === 200) {
            this.isVideoFavorited = response.data.data || false
          }
        }
      } catch (error) {
        console.error('检查收藏状态失败:', error)
      }
    },
    
    // 切换点赞
    async toggleLike() {
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      
      if (!this.selectedVideo || !this.selectedVideo.id) return
      
      try {
        if (this.isVideoLiked) {
          // 取消点赞
          await userAPI.unlikeVideo(this.selectedVideo.id)
          this.isVideoLiked = false
          if (this.localLikeCount > 0) {
            this.localLikeCount--
          }
        } else {
          // 点赞
          await userAPI.likeVideo(this.selectedVideo.id)
          this.isVideoLiked = true
          this.localLikeCount++
        }
        // 同步更新selectedVideo中的点赞数
        if (this.selectedVideo) {
          this.selectedVideo.likeCount = this.localLikeCount
        }
      } catch (error) {
        console.error('点赞操作失败:', error)
        alert('点赞操作失败，请重试')
      }
    },
    
    // 切换收藏
    async toggleFavorite() {
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      
      if (!this.selectedVideo || !this.selectedVideo.id) return
      
      try {
        if (this.isVideoFavorited) {
          // 取消收藏
          await userAPI.unfavoriteVideo(this.selectedVideo.id)
          this.isVideoFavorited = false
          if (this.localFavoriteCount > 0) {
            this.localFavoriteCount--
          }
        } else {
          // 收藏
          await userAPI.favoriteVideo(this.selectedVideo.id)
          this.isVideoFavorited = true
          this.localFavoriteCount++
        }
        // 同步更新selectedVideo中的收藏数
        if (this.selectedVideo) {
          this.selectedVideo.favoriteCount = this.localFavoriteCount
        }
      } catch (error) {
        console.error('收藏操作失败:', error)
        alert('收藏操作失败，请重试')
      }
    },
    
    // 加载评论列表
    async loadComments() {
      if (!this.selectedVideo || !this.selectedVideo.id) return
      
      this.loadingComments = true
      try {
        const response = await commentAPI.getComments(this.selectedVideo.id)
        if (response.data.code === 200) {
          // 处理父子级评论
          this.comments = this.processComments(response.data.data || [])
        }
      } catch (error) {
        console.error('加载评论失败:', error)
        this.comments = []
      } finally {
        this.loadingComments = false
      }
    },
    
    // 处理父子级评论结构
    processComments(comments) {
      const commentMap = {}
      const parentComments = []
      
      comments.forEach(comment => {
        comment.replies = []
        if (comment.parentId === 0 || comment.parentId === null) {
          parentComments.push(comment)
        } else {
          commentMap[comment.id] = comment
        }
      })
      
      // 将回复添加到父评论的replies数组中
      comments.forEach(comment => {
        if (comment.parentId !== 0 && comment.parentId !== null) {
          const parent = parentComments.find(c => c.id === comment.parentId)
          if (parent) {
            parent.replies.push(comment)
          } else {
            const parentInMap = commentMap[comment.parentId]
            if (parentInMap) {
              parentInMap.replies.push(comment)
            }
          }
        }
      })
      
      return parentComments
    },
    
    // 提交评论
    async submitComment() {
      if (!this.newCommentContent.trim()) return
      
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      
      if (!this.selectedVideo || !this.selectedVideo.id) return
      
      try {
        const commentData = {
          videoId: this.selectedVideo.id,
          content: this.newCommentContent,
          parentId: this.replyToCommentId || 0,
          replyUserId: this.replyToUserId || null
        }
        
        const response = await commentAPI.postComment(commentData)
        if (response.data.code === 200) {
          const newComment = response.data.data
          
          if (this.replyToCommentId) {
            // 如果是回复，添加到对应父评论的回复列表中
            const parentComment = this.comments.find(c => c.id === this.replyToCommentId)
            if (parentComment) {
              if (!parentComment.replies) {
                parentComment.replies = []
              }
              parentComment.replies.unshift(newComment)
            }
            
            // 清除回复状态
            this.replyToCommentId = null
            this.replyToUserId = null
            this.replyToUsername = null
          } else {
            // 如果是顶级评论，添加到主列表
            this.comments.unshift(newComment)
          }
          
          this.newCommentContent = ''
          
          // 更新视频的评论计数
          if (this.selectedVideo.commentCount !== undefined) {
            this.selectedVideo.commentCount++
          }
        }
      } catch (error) {
        console.error('提交评论失败:', error)
        alert('提交评论失败，请重试')
      }
    },
    
    // 回复评论
    replyToComment(comment) {
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      
      // 设置回复的目标评论
      this.replyToCommentId = comment.id
      this.replyToUserId = comment.userId
      this.replyToUsername = comment.username
      
      // 自动聚焦到输入框
      this.$nextTick(() => {
        const input = this.$refs.commentInput
        if (input) {
          input.focus()
        }
      })
    },
    
    // 取消回复
    cancelReply() {
      this.replyToCommentId = null
      this.replyToUserId = null
      this.replyToUsername = null
    },
    
    // 聚焦到评论输入框
    focusCommentInput() {
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      
      // 清除回复状态
      this.cancelReply()
      
      // 等待DOM更新后聚焦并滚动到输入框
      this.$nextTick(() => {
        const input = this.$refs.commentInput
        if (input) {
          // 聚焦到输入框
          input.focus()
          
          // 滚动到输入框位置，确保输入框可见
          const inputElement = Array.isArray(input) ? input[0] : input
          if (inputElement && inputElement.scrollIntoView) {
            inputElement.scrollIntoView({ 
              behavior: 'smooth', 
              block: 'center' 
            })
          }
        }
      })
    },
    
    // 删除评论
    async deleteComment(commentId) {
      if (!confirm('确定要删除这条评论吗？')) {
        return
      }
      
      try {
        // 先计算要删除的评论数量（包括子评论）
        const deleteCount = this.countCommentsToDelete(commentId)
        
        await commentAPI.deleteComment(commentId)
        
        // 从本地列表中移除评论
        this.removeCommentFromList(commentId)
        
        // 更新视频的评论计数（减去所有被删除的评论，包括子评论）
        if (this.selectedVideo && this.selectedVideo.commentCount !== undefined) {
          this.selectedVideo.commentCount = Math.max(0, this.selectedVideo.commentCount - deleteCount)
        }
      } catch (error) {
        console.error('删除评论失败:', error)
        alert('删除评论失败，请重试')
      }
    },
    
    // 计算要删除的评论数量（包括子评论）
    countCommentsToDelete(commentId) {
      // 递归函数计算评论数量（包括所有子评论）
      const countRecursive = (comment) => {
        if (!comment) return 0
        
        let count = 1  // 当前评论本身
        
        // 如果有子评论，递归计算子评论数量
        if (comment.replies && comment.replies.length > 0) {
          comment.replies.forEach(reply => {
            count += countRecursive(reply)
          })
        }
        
        return count
      }
      
      // 先找到要删除的评论
      let targetComment = null
      
      // 从父评论列表中查找
      for (let i = 0; i < this.comments.length; i++) {
        const comment = this.comments[i]
        if (comment.id === commentId) {
          targetComment = comment
          break
        }
        
        // 从子评论中查找
        if (comment.replies && comment.replies.length > 0) {
          for (let j = 0; j < comment.replies.length; j++) {
            if (comment.replies[j].id === commentId) {
              targetComment = comment.replies[j]
              break
            }
          }
          if (targetComment) break
        }
      }
      
      // 如果找到评论，计算包括子评论的总数
      if (targetComment) {
        return countRecursive(targetComment)
      }
      
      // 如果没找到，返回1（至少会删除1条评论）
      return 1
    },
    
    // 从列表中移除评论（后端已经处理了递归删除，前端只需要移除对应的节点）
    removeCommentFromList(commentId) {
      // 递归查找并删除评论
      const findAndRemove = (comments, targetId) => {
        for (let i = 0; i < comments.length; i++) {
          // 如果找到目标评论，直接删除（包括其所有子评论）
          if (comments[i].id === targetId) {
            comments.splice(i, 1)
            return true
          }
          
          // 如果当前评论有子评论，递归查找
          if (comments[i].replies && comments[i].replies.length > 0) {
            if (findAndRemove(comments[i].replies, targetId)) {
              return true
            }
          }
        }
        return false
      }
      
      // 从主评论列表中查找并删除
      findAndRemove(this.comments, commentId)
    },
    
    // 格式化时间
    formatTime(timeStr) {
      if (!timeStr) return ''
      try {
        const date = new Date(timeStr)
        const now = new Date()
        const diff = now - date
        const minutes = Math.floor(diff / 60000)
        const hours = Math.floor(diff / 3600000)
        const days = Math.floor(diff / 86400000)
        
        if (minutes < 1) return '刚刚'
        if (minutes < 60) return `${minutes}分钟前`
        if (hours < 24) return `${hours}小时前`
        if (days < 7) return `${days}天前`
        return date.toLocaleDateString('zh-CN')
      } catch (e) {
        return timeStr
      }
    },
    
    formatDuration(duration) {
      if (!duration) return '0:00'
      const minutes = Math.floor(duration / 60)
      const seconds = Math.floor(duration % 60)
      return `${minutes}:${seconds.toString().padStart(2, '0')}`
    },
    
    formatViews(views) {
      if (views >= 10000) {
        return `${(views / 10000).toFixed(1)}万`
      }
      return views
    },
    
    // 处理转发
    handleForward() {
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      if (!this.selectedVideo || !this.selectedVideo.id) {
        alert('请先选择要转发的视频')
        return
      }
      this.showForwardModal = true
    },
    
    // 转发成功回调
    handleForwardSuccess(friend) {
      alert(`已转发给 ${friend.nickname || friend.username}`)
      // 更新分享数
      if (this.selectedVideo) {
        this.selectedVideo.shareCount = (this.selectedVideo.shareCount || 0) + 1
      }
    }
  }
}
</script>

<style scoped>
.category-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.category-header {
  margin-bottom: 30px;
}

.category-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.video-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.2s;
}

.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
  background: #000;
}

.video-thumbnail img,
.video-thumbnail video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-image {
  transition: opacity 0.3s;
}

.preview-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-card:hover .hover-overlay {
  opacity: 1;
}

.play-icon {
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  padding-left: 3px;
}


.video-info {
  padding: 12px;
}

.video-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 14px;
  color: #666;
}

.video-stats {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #999;
}

.loading, .no-videos {
  text-align: center;
  padding: 40px;
  font-size: 18px;
  color: #666;
}

/* 视频播放弹窗样式 */
.video-modal-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 240px; /* 侧边栏宽度，从侧边栏右侧开始 */
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.video-modal-container {
  position: relative;
  width: 100%;
  max-width: 1200px;
  max-height: 90vh;
  background: #181818;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
}

.video-modal-content {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 90vh;
  overflow: hidden;
}

.modal-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 20px;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.modal-close-btn:hover {
  background: #dc2626;
}

.video-player-wrapper {
  position: relative;
  width: 100%;
  max-height: 50vh;
  aspect-ratio: 16 / 9;
  flex-shrink: 0;
  background: #000;
}

.modal-video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-details {
  padding: 24px;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.video-details-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.video-details-title-section {
  flex: 1;
}

.video-details-title {
  font-size: 20px;
  font-weight: bold;
  color: white;
  margin: 0 0 8px 0;
}

.video-details-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #a1a1aa;
}

.video-tag {
  background: rgba(220, 38, 38, 0.2);
  color: #dc2626;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.video-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #272727;
  border: none;
  border-radius: 9999px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.action-btn:hover {
  background: #3f3f3f;
}

.action-btn.liked {
  color: #ff4757;
}

.action-btn.liked span:first-child {
  filter: drop-shadow(0 0 4px rgba(255, 71, 87, 0.6));
}

.action-btn.favorited {
  color: #ffc107;
}

.action-btn.favorited span:first-child {
  filter: drop-shadow(0 0 4px rgba(255, 193, 7, 0.6));
}

.video-details-divider {
  height: 1px;
  background: #272727;
  margin: 16px 0;
}

.video-author-section {
  display: flex;
  gap: 16px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8b5cf6, #3b82f6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: bold;
  flex-shrink: 0;
}

.author-info {
  flex: 1;
}

.author-name {
  font-weight: 500;
  color: white;
  margin-bottom: 4px;
}

.author-stats {
  font-size: 12px;
  color: #71717a;
  margin-bottom: 12px;
}

.video-description {
  font-size: 14px;
  color: #d1d5db;
  line-height: 1.6;
  margin: 0;
}

/* 弹窗动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .video-modal-container,
.modal-leave-active .video-modal-container {
  transition: transform 0.3s, opacity 0.3s;
}

.modal-enter-from .video-modal-container,
.modal-leave-to .video-modal-container {
  transform: scale(0.9);
  opacity: 0;
}

/* 评论区域样式 */
.comments-section {
  margin-top: 24px;
  border-top: 1px solid #272727;
  padding-top: 24px;
  flex-shrink: 0;
}

.comments-header {
  margin-bottom: 16px;
}

.comments-title {
  font-size: 18px;
  font-weight: bold;
  color: white;
  margin: 0;
}

.comments-loading,
.comments-empty {
  text-align: center;
  padding: 40px 20px;
  color: #71717a;
  font-size: 14px;
}

.comments-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
  min-height: 0;
}

.comments-list::-webkit-scrollbar {
  width: 6px;
}

.comments-list::-webkit-scrollbar-track {
  background: #272727;
  border-radius: 3px;
}

.comments-list::-webkit-scrollbar-thumb {
  background: #3f3f3f;
  border-radius: 3px;
}

.comments-list::-webkit-scrollbar-thumb:hover {
  background: #525252;
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-avatar.small {
  width: 28px;
  height: 28px;
}

.comment-content {
  flex: 1;
  color: white;
}

.comment-header-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-username {
  font-weight: 600;
  font-size: 14px;
  color: white;
}

.comment-time {
  font-size: 12px;
  color: #71717a;
}

.comment-text {
  margin: 6px 0;
  line-height: 1.6;
  font-size: 14px;
  color: #d1d5db;
  word-wrap: break-word;
}

.comment-actions {
  display: flex;
  gap: 16px;
  margin-top: 6px;
}

.reply-btn,
.delete-btn {
  font-size: 12px;
  color: #71717a;
  cursor: pointer;
  transition: color 0.2s;
}

.reply-btn:hover {
  color: #60a5fa;
}

.delete-btn {
  color: #f87171;
}

.delete-btn:hover {
  color: #ef4444;
}

.replies-list {
  margin-top: 12px;
  padding-left: 24px;
  border-left: 1px solid #3f3f3f;
}

.reply-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px 0;
}

.reply-to {
  color: #60a5fa;
  font-weight: 600;
  margin-right: 4px;
}

.comment-input-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #272727;
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
  background: #3f3f3f;
  color: #a1a1aa;
  border: none;
  border-radius: 16px;
  padding: 6px 16px;
  font-size: 14px;
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
</style>
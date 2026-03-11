<template>
  <div class="chat-window" v-if="activeChatFriend">
    <div class="chat-header">
      <div class="chat-friend-info">
        <img :src="activeChatFriend.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + activeChatFriend.username" 
             class="friend-avatar" 
             alt="avatar">
        <span class="friend-name">{{ activeChatFriend.nickname || activeChatFriend.username }}</span>
      </div>
      <button class="delete-friend-btn" @click="handleDeleteFriend">删除好友</button>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="message in messages" 
        :key="message.id"
        :class="['message-item', message.fromUserId === currentUserId ? 'message-sent' : 'message-received']"
      >
        <div class="message-row">
          <!-- 只有自己发送的消息才显示已读状态，显示在消息框左边 -->
          <div v-if="message.fromUserId === currentUserId" class="read-status-wrapper">
            <span v-if="message.isRead === 1" class="read-icon">✓</span>
            <span v-else class="unread-icon">○</span>
          </div>
          <!-- 视频分享消息 -->
          <div v-if="message.type === 1" class="message-content video-message-content">
            <VideoCard 
              v-if="videoCache[message.relatedId]"
              :video="videoCache[message.relatedId]"
              :show-meta="false"
              @click="openVideoModal(videoCache[message.relatedId])"
            />
            <div v-else class="video-loading">加载视频信息中...</div>
          </div>
          <!-- 普通文本消息 -->
          <div v-else class="message-content">{{ message.content }}</div>
        </div>
        <div class="message-time">{{ formatTime(message.createdAt) }}</div>
      </div>
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
                :poster="selectedVideo.coverUrl || selectedVideo.cover"
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
                    <span class="video-tag">播放</span>
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
                  <button class="action-btn">
                    <span>分享</span>
                  </button>
                </div>
              </div>

              <div class="video-details-divider"></div>

              <!-- 作者信息 -->
              <div class="video-author-section">
                <div class="author-avatar">
                  {{ selectedVideo.username ? selectedVideo.username[0].toUpperCase() : 'U' }}
                </div>
                <div class="author-info">
                  <div class="author-name">{{ selectedVideo.username }}</div>
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
                          <span class="comment-time">{{ formatCommentTime(comment.createdAt) }}</span>
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
                          class="reply-list"
                        >
                          <div 
                            v-for="reply in comment.replies" 
                            :key="reply.id" 
                            class="reply-item"
                          >
                            <img 
                              :src="reply.avatar || 'https://placehold.co/32x32'" 
                              class="comment-avatar" 
                              :alt="reply.username"
                            />
                            <div class="comment-content">
                              <div class="comment-header-info">
                                <span class="comment-username">{{ reply.username }}</span>
                                <span class="comment-time">{{ formatCommentTime(reply.createdAt) }}</span>
                              </div>
                              <p class="comment-text">{{ reply.content }}</p>
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

                <!-- 发表评论 -->
                <div class="comment-input">
                  <div class="comment-input-header">
                    <span class="comment-input-title">
                      {{ replyToUsername ? `回复 ${replyToUsername}` : '发表评论' }}
                    </span>
                    <button 
                      v-if="replyToUsername" 
                      class="cancel-reply-btn" 
                      @click="cancelReply"
                    >
                      取消回复
                    </button>
                  </div>
                  <div class="comment-input-box">
                    <textarea 
                      ref="commentInput"
                      v-model="newCommentContent"
                      placeholder="友善发言，理性讨论~"
                      rows="3"
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
    
    <div class="chat-input">
      <input 
        type="text" 
        v-model="inputMessage" 
        @keyup.enter="sendMessage"
        placeholder="输入消息..."
        class="message-input"
      >
      <button @click="sendMessage" class="send-btn">发送</button>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import messageAPI from '@/api/message'
import friendAPI from '@/api/friend'
import VideoCard from './VideoCard.vue'
import { videoAPI } from '@/api/video'
import userAPI from '@/api/user'
import { commentAPI } from '@/api/comment'

export default {
  name: 'ChatWindow',
  components: {
    VideoCard
  },
  props: {
    activeChatFriend: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      messages: [],
      inputMessage: '',
      currentUserId: null,
      videoCache: {}, // 缓存视频信息
      // 视频弹窗相关
      selectedVideo: null,
      isVideoLiked: false,
      localLikeCount: 0,
      isVideoFavorited: false,
      localFavoriteCount: 0,
      comments: [],
      newCommentContent: '',
      loadingComments: false,
      replyToCommentId: null,
      replyToUserId: null,
      replyToUsername: null,
      // 观看历史相关
      historyRecorded: false,
      watchStartTime: null,
      nextUpdateInterval: null,
      watchDuration: 0
    }
  },
  computed: {
    ...mapGetters(['currentUserInfo'])
  },
  watch: {
    activeChatFriend: {
      immediate: true,
      handler(newFriend) {
        if (newFriend) {
          this.loadMessages()
          this.currentUserId = this.currentUserInfo?.id
        } else {
          this.messages = []
        }
      }
    }
  },
  methods: {
    async loadMessages() {
      if (!this.activeChatFriend) return
      
      try {
        const response = await messageAPI.getChatMessages(this.activeChatFriend.id, 1, 50)
        if (response.data.code === 200) {
          // 反转消息列表，使最新的消息在底部
          this.messages = (response.data.data || []).reverse()
          
          // 批量获取视频信息（type === 1 的消息）
          const videoIds = this.messages
            .filter(msg => msg.type === 1 && msg.relatedId)
            .map(msg => msg.relatedId)
          
          if (videoIds.length > 0) {
            try {
              const videoResponse = await videoAPI.getVideosByIds([...new Set(videoIds)])
              if (videoResponse.data.code === 200 && videoResponse.data.data) {
                videoResponse.data.data.forEach(video => {
                  this.videoCache[video.id] = video
                })
              }
            } catch (error) {
              console.error('加载视频信息失败:', error)
            }
          }
          
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (error) {
        console.error('加载消息失败:', error)
      }
    },
    
    async sendMessage() {
      if (!this.inputMessage.trim() || !this.activeChatFriend) return
      
      try {
        const response = await messageAPI.sendMessage({
          fromUserId: this.currentUserId,
          toUserId: this.activeChatFriend.id,
          type: 0, // 0-纯文本
          content: this.inputMessage.trim()
        })
        
        if (response.data.code === 200) {
          // 添加新消息到列表
          const newMessage = {
            id: response.data.data,
            fromUserId: this.currentUserId,
            toUserId: this.activeChatFriend.id,
            type: 0,
            content: this.inputMessage.trim(),
            isRead: 0, // 新发送的消息默认为未读
            createdAt: new Date().toISOString()
          }
          this.messages.push(newMessage)
          this.inputMessage = ''
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        alert('发送消息失败，请重试')
      }
    },
    
    async handleDeleteFriend() {
      if (!this.activeChatFriend) return
      
      if (!confirm('确定要删除该好友吗？')) {
        return
      }
      
      try {
        const response = await friendAPI.deleteFriend(this.activeChatFriend.id)
        if (response.data.code === 200) {
          // 关闭聊天窗口
          this.$emit('close-chat')
          // 刷新好友列表
          this.$emit('refresh-friends')
          alert('好友已删除')
        }
      } catch (error) {
        console.error('删除好友失败:', error)
        alert('删除好友失败，请重试')
      }
    },
    
    scrollToBottom() {
      const container = this.$refs.messagesContainer
      if (container) {
        container.scrollTop = container.scrollHeight
      }
    },
    
    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      const minutes = Math.floor(diff / 60000)
      
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      
      const hours = Math.floor(minutes / 60)
      if (hours < 24) return `${hours}小时前`
      
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString().slice(0, 5)
    },
    
    /* ================= 视频弹窗与互动（参考 ProfileContent.vue） ================= */
    async openVideoModal(video) {
      if (!video || !video.id) return
      this.selectedVideo = { ...video }
      this.localLikeCount = video.likeCount || 0
      this.localFavoriteCount = video.favoriteCount || 0
      this.isVideoLiked = false
      this.isVideoFavorited = false
      this.comments = []
      this.newCommentContent = ''
      this.replyToCommentId = null
      this.replyToUserId = null
      this.replyToUsername = null
      // 重置观看历史状态
      this.historyRecorded = false
      this.watchStartTime = null
      this.nextUpdateInterval = null
      this.watchDuration = 0

      document.body.style.overflow = 'hidden'

      this.$nextTick(async () => {
        const player = this.$refs.modalVideoPlayer
        const el = Array.isArray(player) ? player[0] : player
        
        if (el && typeof el.play === 'function') {
          el.play().catch(err => console.error('自动播放失败:', err))
        }

        try {
          const detailRes = await videoAPI.getVideoDetail(video.id)
          if (detailRes.data.code === 200 && detailRes.data.data) {
            const detail = detailRes.data.data
            this.selectedVideo = { ...this.selectedVideo, ...detail }
            this.localLikeCount = detail.likeCount || 0
            this.localFavoriteCount = detail.favoriteCount || 0
          }
        } catch (e) {
          console.error('获取视频详情失败:', e)
        }

        this.checkLikeStatus()
        this.checkFavoriteStatus()
        this.loadComments()
      })
    },

    closeVideoModal() {
      const player = this.$refs.modalVideoPlayer
      const el = Array.isArray(player) ? player[0] : player
      if (el && el.pause) {
        el.pause()
      }
      this.selectedVideo = null
      document.body.style.overflow = 'unset'
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

    async checkLikeStatus() {
      try {
        if (this.selectedVideo && this.selectedVideo.id) {
          const res = await userAPI.checkVideoLiked(this.selectedVideo.id)
          if (res.data.code === 200) {
            this.isVideoLiked = res.data.data || false
          }
        }
      } catch (e) {
        console.error('检查点赞状态失败:', e)
      }
    },

    async checkFavoriteStatus() {
      try {
        if (this.selectedVideo && this.selectedVideo.id) {
          const res = await userAPI.checkVideoFavorited(this.selectedVideo.id)
          if (res.data.code === 200) {
            this.isVideoFavorited = res.data.data || false
          }
        }
      } catch (e) {
        console.error('检查收藏状态失败:', e)
      }
    },

    async onModalVideoPlay() {
      if (this.historyRecorded || !this.$store.getters.isAuthenticated) {
        return
      }
      if (!this.selectedVideo || !this.selectedVideo.id) return

      this.watchStartTime = Date.now()
      this.nextUpdateInterval = Math.floor(Math.random() * 2000) + 3000

      try {
        await userAPI.recordWatchHistory(this.selectedVideo.id, 0, 0)
        this.historyRecorded = true
      } catch (error) {
        console.error('记录观看历史失败:', error)
      }
    },

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

    async toggleLike() {
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      if (!this.selectedVideo || !this.selectedVideo.id) return

      try {
        if (this.isVideoLiked) {
          await userAPI.unlikeVideo(this.selectedVideo.id)
          this.isVideoLiked = false
          if (this.localLikeCount > 0) this.localLikeCount--
        } else {
          await userAPI.likeVideo(this.selectedVideo.id)
          this.isVideoLiked = true
          this.localLikeCount++
        }
        if (this.selectedVideo) {
          this.selectedVideo.likeCount = this.localLikeCount
        }
      } catch (e) {
        console.error('点赞操作失败:', e)
        alert('点赞操作失败，请重试')
      }
    },

    async toggleFavorite() {
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }
      if (!this.selectedVideo || !this.selectedVideo.id) return

      try {
        if (this.isVideoFavorited) {
          await userAPI.unfavoriteVideo(this.selectedVideo.id)
          this.isVideoFavorited = false
          if (this.localFavoriteCount > 0) this.localFavoriteCount--
        } else {
          await userAPI.favoriteVideo(this.selectedVideo.id)
          this.isVideoFavorited = true
          this.localFavoriteCount++
        }
        if (this.selectedVideo) {
          this.selectedVideo.favoriteCount = this.localFavoriteCount
        }
      } catch (e) {
        console.error('收藏操作失败:', e)
        alert('收藏操作失败，请重试')
      }
    },

    async loadComments() {
      if (!this.selectedVideo || !this.selectedVideo.id) return
      this.loadingComments = true
      try {
        const res = await commentAPI.getComments(this.selectedVideo.id)
        if (res.data.code === 200) {
          this.comments = res.data.data || []
        }
      } catch (e) {
        console.error('加载评论失败:', e)
        this.comments = []
      } finally {
        this.loadingComments = false
      }
    },

    async submitComment() {
      if (!this.newCommentContent.trim() || !this.selectedVideo) return
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login')
        return
      }

      try {
        const res = await commentAPI.postComment({
          videoId: this.selectedVideo.id,
          content: this.newCommentContent.trim(),
          parentId: this.replyToCommentId || null
        })

        if (res.data.code === 200) {
          this.newCommentContent = ''
          this.replyToCommentId = null
          this.replyToUserId = null
          this.replyToUsername = null
          await this.loadComments()
          if (this.selectedVideo) {
            this.selectedVideo.commentCount = (this.selectedVideo.commentCount || 0) + 1
          }
        }
      } catch (e) {
        console.error('发表评论失败:', e)
        alert('发表评论失败，请重试')
      }
    },

    replyToComment(comment) {
      this.replyToCommentId = comment.id
      this.replyToUserId = comment.userId
      this.replyToUsername = comment.username
      this.focusCommentInput()
    },

    cancelReply() {
      this.replyToCommentId = null
      this.replyToUserId = null
      this.replyToUsername = null
    },

    focusCommentInput() {
      this.$nextTick(() => {
        const input = this.$refs.commentInput
        if (input) {
          const el = Array.isArray(input) ? input[0] : input
          if (el) {
            el.focus()
            el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
          }
        }
      })
    },

    async deleteComment(commentId) {
      if (!confirm('确定要删除这条评论吗？')) return

      try {
        const res = await commentAPI.deleteComment(commentId)
        if (res.data.code === 200) {
          const count = this.countCommentsToDelete(commentId)
          this.removeCommentFromList(commentId)
          if (this.selectedVideo) {
            this.selectedVideo.commentCount = Math.max(0, (this.selectedVideo.commentCount || 0) - count)
          }
        }
      } catch (e) {
        console.error('删除评论失败:', e)
        alert('删除评论失败，请重试')
      }
    },

    countCommentsToDelete(commentId) {
      let count = 1
      const comment = this.comments.find(c => c.id === commentId)
      if (comment && comment.replies) {
        count += comment.replies.length
      }
      return count
    },

    removeCommentFromList(commentId) {
      const index = this.comments.findIndex(c => c.id === commentId)
      if (index !== -1) {
        this.comments.splice(index, 1)
      }
    },

    formatViews(count) {
      if (!count) return 0
      if (count >= 10000) {
        return (count / 10000).toFixed(1) + '万'
      }
      return count
    },

    formatCommentTime(timeStr) {
      if (!timeStr) return ''
      try {
        const date = new Date(timeStr)
        const now = new Date()
        const diff = now - date
        const minutes = Math.floor(diff / 60000)
        
        if (minutes < 1) return '刚刚'
        if (minutes < 60) return `${minutes}分钟前`
        
        const hours = Math.floor(minutes / 60)
        if (hours < 24) return `${hours}小时前`
        
        return date.toLocaleDateString('zh-CN')
      } catch (e) {
        return timeStr
      }
    }
  }
}
</script>

<style scoped>
.chat-window {
  position: fixed;
  right: 0;
  top: 60px;
  width: 400px;
  height: calc(100vh - 60px);
  background: var(--bg-color, #1a1a1a);
  border-left: 1px solid var(--border-color, #333);
  display: flex;
  flex-direction: column;
  z-index: 1000;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid var(--border-color, #333);
}

.chat-friend-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.friend-name {
  font-weight: 500;
  color: var(--text-color, #fff);
}

.delete-friend-btn {
  background: #ff4757;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.delete-friend-btn:hover {
  background: #ff3838;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-item {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-sent {
  align-self: flex-end;
  align-items: flex-end;
}

.message-received {
  align-self: flex-start;
  align-items: flex-start;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}

.message-sent .message-row {
  flex-direction: row-reverse;
}

.message-content {
  padding: 10px 15px;
  border-radius: 12px;
  word-wrap: break-word;
  display: inline-block;
}

.read-status-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  margin-bottom: 2px;
}

.read-icon {
  color: #4caf50;
  font-weight: bold;
  font-size: 14px;
  line-height: 1;
}

.unread-icon {
  color: #999;
  font-size: 14px;
  line-height: 1;
}

.message-sent .message-content {
  background: #007bff;
  color: white;
}

.message-received .message-content {
  background: var(--border-color, #333);
  color: var(--text-color, #fff);
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 5px;
}

.chat-input {
  display: flex;
  padding: 15px;
  border-top: 1px solid var(--border-color, #333);
  gap: 10px;
}

.message-input {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--border-color, #333);
  border-radius: 6px;
  background: var(--bg-color, #1a1a1a);
  color: var(--text-color, #fff);
  outline: none;
}

.send-btn {
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.send-btn:hover {
  background: #0056b3;
}

/* 视频消息样式 */
.video-message-content {
  padding: 0;
  max-width: 200px;
}

.video-loading {
  padding: 10px;
  color: #999;
  font-size: 12px;
}

/* 视频弹窗样式 */
.video-modal-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  z-index: 2000;
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
}

.video-player-wrapper {
  background: #000;
  position: relative;
}

.modal-video-player {
  width: 100%;
  max-height: 60vh;
  background: #000;
}

.video-details {
  padding: 16px 20px 24px;
  overflow-y: auto;
  max-height: 40vh;
}

.video-details-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.video-details-title {
  margin: 0;
  color: #fff;
  font-size: 20px;
}

.video-details-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #aaa;
  font-size: 13px;
}

.video-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #2a2a2a;
  color: #fff;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn.liked {
  background: rgba(255, 71, 87, 0.12);
  color: #ff4757;
}

.action-btn.favorited {
  background: rgba(255, 215, 0, 0.12);
  color: #ffd700;
}

.video-details-divider {
  height: 1px;
  background: #2e2e2e;
  margin: 16px 0;
}

.video-author-section {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #333;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.author-name {
  color: #fff;
  font-weight: 600;
}

.video-description {
  margin: 6px 0 0;
  color: #ccc;
  font-size: 14px;
}

.comments-section {
  background: #111;
  border-radius: 12px;
  padding: 12px;
}

.comments-title {
  color: #fff;
  margin: 0 0 12px;
}

.comments-list {
  max-height: 220px;
  overflow-y: auto;
  padding-right: 6px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #222;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-username {
  color: #fff;
  font-weight: 600;
  margin-right: 8px;
}

.comment-time {
  color: #888;
  font-size: 12px;
}

.comment-text {
  color: #ddd;
  margin: 6px 0;
}

.comment-actions {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.reply-btn, .delete-btn {
  cursor: pointer;
}

.reply-list {
  margin-top: 8px;
  padding-left: 40px;
  border-left: 2px solid #222;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
}

.comment-input {
  margin-top: 12px;
}

.comment-input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.comment-input-title {
  color: #fff;
  font-weight: 600;
}

.cancel-reply-btn {
  background: transparent;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 12px;
}

.comment-input-box {
  display: flex;
  flex-direction: column;
}

.comment-input-field {
  width: 100%;
  background: #1a1a1a;
  color: #fff;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 10px;
  resize: none;
  font-size: 14px;
}

.comment-send-btn {
  margin-top: 8px;
  padding: 8px 14px;
  border: none;
  border-radius: 999px;
  background: #333;
  color: #999;
  cursor: pointer;
  align-self: flex-end;
}

.comment-send-btn.active {
  background: #ff4757;
  color: #fff;
}

.comments-loading,
.comments-empty {
  text-align: center;
  color: #999;
  padding: 20px;
}

/* 弹窗过渡动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>

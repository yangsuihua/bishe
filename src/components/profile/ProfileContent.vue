<template>
    <div>
      <!-- 作品 -->
      <div v-if="activeTab === 'uploads'" class="tab-content active">
        <!-- 待审核 -->
        <div v-if="pendingVideos.length > 0" class="video-status-section">
          <h3 class="status-title">待审核</h3>
          <VideoGrid 
            :videos="pendingVideos" 
            :show-delete-button="true" 
            :is-me="isMe"
            @video-click="openVideoModal"
            @delete-video="handleDeleteVideo"
          />
        </div>
        
        <!-- 审核失败 -->
        <div v-if="failedVideos.length > 0" class="video-status-section">
          <h3 class="status-title">审核失败</h3>
          <div class="video-grid">
            <div 
              v-for="video in failedVideos" 
              :key="video.id"
              class="grid-card"
              @click="openVideoModal(video)"
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
                
                <!-- 失败原因 -->
                <div class="reject-reason" v-if="video.rejectReason">
                  <span class="reject-label">失败原因：</span>
                  <span class="reject-text">{{ video.rejectReason }}</span>
                </div>
                <div class="reject-reason loading" v-else-if="video.loadingReason">
                  <span class="reject-label">加载中...</span>
                </div>
                
                <!-- 删除按钮 -->
                <button 
                  v-if="isMe" 
                  class="delete-btn"
                  @click.stop="handleDeleteVideo(video.id)"
                  title="删除视频"
                >
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 已发布 -->
        <div v-if="publishedVideos.length > 0" class="video-status-section">
          <h3 class="status-title">已发布</h3>
          <VideoGrid 
            :videos="publishedVideos" 
            :show-delete-button="true" 
            :is-me="isMe"
            @video-click="openVideoModal"
            @delete-video="handleDeleteVideo"
          />
        </div>
        
        <!-- 空状态 -->
        <div v-if="uploadedVideos.length === 0 && !uploadedVideosLoading" class="empty-state">
          暂无作品
        </div>
      </div>

      <!-- 收藏 -->
      <div v-if="activeTab === 'favs'" class="tab-content active">
        <VideoGrid 
          v-if="canViewFavs" 
          :videos="favVideos" 
          @video-click="openVideoModal"
        />
        <div v-else class="empty-state">
          🔒 该用户已隐藏收藏列表
        </div>
      </div>

      <!-- 点赞 -->
      <div v-if="activeTab === 'likes'" class="tab-content active">
        <VideoGrid 
          v-if="canViewLikes" 
          :videos="likedVideos" 
          @video-click="openVideoModal"
        />
        <div v-else class="empty-state">
          🔒 该用户已隐藏点赞列表
        </div>
      </div>

      <!-- 历史记录 -->
      <div v-if="activeTab === 'history' && isMe" class="tab-content active">
        <VideoGrid :videos="historyVideos" @video-click="openVideoModal" />
      </div>

      <!-- 关注列表 -->
      <div v-if="activeTab === 'following'" class="tab-content active">
        <UserList :users="followingUsers" />
      </div>

      <!-- 粉丝列表 -->
      <div v-if="activeTab === 'followers'" class="tab-content active">
        <UserList :users="followerUsers" />
      </div>

      <!-- 设置 -->
      <div v-if="activeTab === 'settings' && isMe" class="tab-content active">
        <div class="settings-box">
          <!-- 隐私管理 -->
          <h3 style="margin-bottom: 20px; color: var(--primary-color);">🔒 隐私管理</h3>
          <div class="privacy-box">
            <div class="switch-group">
              <span>允许他人查看我的收藏</span>
              <label class="switch">
                <input type="checkbox" v-model="showFavsPublic">
                <span class="slider"></span>
              </label>
            </div>
            <div class="switch-group">
              <span>允许他人查看我的点赞</span>
              <label class="switch">
                <input type="checkbox" v-model="showLikesPublic">
                <span class="slider"></span>
              </label>
            </div>
            <div style="font-size: 12px; color: var(--text-secondary); margin-top: 10px;">
              关闭后，其他用户访问您的主页时将无法看到对应的内容列表。
            </div>
          </div>

          <h3 style="margin-bottom: 20px;">基本资料</h3>
          <div class="form-group">
            <label class="form-label">更换头像</label>
            <div style="display:flex; gap:10px; align-items:center;">
              <img :src="user.avatar || 'https://via.placeholder.com/50'" style="width:50px; height:50px; border-radius:50%;">
              <input type="file" ref="avatarInput" @change="onAvatarChange" accept="image/*" style="display: none;">
              <button class="btn-outline" @click="$refs.avatarInput.click()">上传新头像</button>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">昵称</label>
            <input type="text" class="form-input" v-model="profileName">
          </div>
          <div class="form-group">
            <label class="form-label">个人简介</label>
            <textarea class="form-input" v-model="profileBio" placeholder="介绍一下自己..." rows="3"></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">性别</label>
            <select class="form-input" v-model="profileGender">
              <option value="0">未知</option>
              <option value="1">男</option>
              <option value="2">女</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">生日</label>
            <input type="date" class="form-input" v-model="profileBirthday">
          </div>

          <!-- 安全设置 -->
          <h3 style="margin: 30px 0 20px;">安全设置</h3>
          <div class="form-group">
            <label class="form-label">旧密码</label>
            <input type="password" class="form-input" v-model="oldPassword" placeholder="请输入当前密码">
          </div>
          <div class="form-group">
            <label class="form-label">新密码</label>
            <input type="password" class="form-input" v-model="newPassword" placeholder="请输入新密码">
          </div>
          <div class="form-group">
            <label class="form-label">确认新密码</label>
            <input type="password" class="form-input" v-model="confirmNewPassword" placeholder="请再次输入新密码">
          </div>
          
          <button class="btn-primary" @click="saveChanges">保存更改</button>
        </div>
      </div>

      <!-- 视频播放弹窗（参考 category.vue 的弹窗效果） -->
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
                                  <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
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
    </div>
  </template>

  <script>
  import VideoGrid from '../common/VideoGrid.vue'
  import UserList from '../common/UserList.vue'
  import ForwardModal from '../common/ForwardModal.vue'
  import userAPI from '../../api/user'
  import { videoAPI } from '../../api/video'
  import { commentAPI } from '../../api/comment'

  export default {
    name: 'ProfileContent',
    components: {
      VideoGrid,
      UserList,
      ForwardModal
    },
    props: {
      activeTab: String,
      user: Object,
      isMe: Boolean
    },
    data() {
      return {
        profileName: '',
        profileBio: '',           // 个人简介
        profileGender: 0,       // 性别
        profileBirthday: '',    // 生日
        oldPassword: '',        // 旧密码
        newPassword: '',        // 新密码
        confirmNewPassword: '', // 确认新密码
        privacy: {
          showFavs: true,
          showLikes: false
        },
        showFavsPublic: true,   // 收藏列表是否公开（布尔值）
        showLikesPublic: false, // 点赞列表是否公开（布尔值）
        // 真实数据
        uploadedVideos: [],
        favVideos: [],
        likedVideos: [],
        historyVideos: [],
        followingUsers: [],
        followerUsers: [],
        // 弹窗/互动相关
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
        // 观看历史相关（与首页推荐逻辑一致）
        historyRecorded: false,
        watchStartTime: null,
        nextUpdateInterval: null,
        watchDuration: 0,
        // 加载状态
        uploadedVideosLoading: false,
        favVideosLoading: false,
        likedVideosLoading: false,
        historyVideosLoading: false,
        followingUsersLoading: false,
        followerUsersLoading: false,
        // 转发相关
        showForwardModal: false
      }
    },
    computed: {
      canViewFavs() {
        // 优先使用后端返回的字段名，兼容模拟数据
        if (this.user.isFavoritesVisible !== undefined) {
          // 后端返回的字段：1表示公开，0表示私密
          return this.isMe || this.user.isFavoritesVisible === 1;
        }
        // 兼容模拟数据格式
        return this.isMe || (this.user.privacy && this.user.privacy.showFavs);
      },
      canViewLikes() {
        // 优先使用后端返回的字段名，兼容模拟数据
        if (this.user.isLikesVisible !== undefined) {
          // 后端返回的字段：1表示公开，0表示私密
          return this.isMe || this.user.isLikesVisible === 1;
        }
        // 兼容模拟数据格式
        return this.isMe || (this.user.privacy && this.user.privacy.showLikes);
      },
      // 按状态分组的视频
      pendingVideos() {
        return this.uploadedVideos.filter(video => video.status === 0);
      },
      failedVideos() {
        return this.uploadedVideos.filter(video => video.status === 2);
      },
      publishedVideos() {
        return this.uploadedVideos.filter(video => video.status === 1 || (video.status !== 0 && video.status !== 2));
      }
    },
    async mounted() {
      if (this.user) {
        this.profileName = this.user.name || this.user.nickname;
        this.profileBio = this.user.bio || '';
        this.profileGender = this.user.gender || 0;
        
        // 处理生日字段
        if (this.user.birthday) {
          // 如果birthday是Date对象，转换为字符串格式
          if (this.user.birthday instanceof Date) {
            this.profileBirthday = this.user.birthday.toISOString().split('T')[0];
          } else {
            // 如果birthday已经是字符串格式，则直接使用
            this.profileBirthday = this.user.birthday;
          }
        } else {
          this.profileBirthday = '';
        }
        
        // 从用户数据中加载隐私设置
        if (this.user.isFavoritesVisible !== undefined) {
          this.showFavsPublic = this.user.isFavoritesVisible === 1;
          this.privacy.showFavs = this.user.isFavoritesVisible === 1;
        }
        if (this.user.isLikesVisible !== undefined) {
          this.showLikesPublic = this.user.isLikesVisible === 1;
          this.privacy.showLikes = this.user.isLikesVisible === 1;
        }
      }
      
      // 监听隐私设置变化，同步到隐私对象
      this.$watch('showFavsPublic', (newVal) => {
        this.privacy.showFavs = newVal;
      });
      
      this.$watch('showLikesPublic', (newVal) => {
        this.privacy.showLikes = newVal;
      });
      
      // 根据当前激活的标签页加载数据
      this.loadActiveTabData();
    },
    
    // 监听标签页变化，加载相应数据
    watch: {
      activeTab: {
        handler(newTab) {
          this.loadActiveTabData();
        }
      },
      user: {
        handler(newUser, oldUser) {
          // 当用户变化时，如果用户ID不同，重新加载当前标签页的数据
          if (newUser && oldUser && newUser.id !== oldUser.id) {
            this.loadActiveTabData();
          }
          // 当用户数据变化时，更新表单字段
          this.updateFormFields();
        },
        deep: true
      }
    },
    
    methods: {
      async handleDeleteVideo(videoId) {
        // 显示确认对话框
        if (!confirm('确定要删除这个视频吗？此操作不可撤销。')) {
          return;
        }
        
        try {
          // 调用后端API删除视频
          const response = await fetch(`/api/video/${videoId}`, {
            method: 'DELETE',
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
              'Content-Type': 'application/json'
            }
          });
          
          if (response.ok) {
            alert('视频删除成功');
            // 从本地列表中移除已删除的视频
            this.uploadedVideos = this.uploadedVideos.filter(video => video.id !== videoId);
            // 重新加载作品列表以确保数据同步
            this.loadUploadedVideos();
          } else {
            const result = await response.json();
            alert(`删除失败: ${result.message || '未知错误'}`);
          }
        } catch (error) {
          console.error('删除视频时出错:', error);
          alert('删除失败，请稍后重试');
        }
      },
      
      updateFormFields() {
        if (this.user && this.isMe) {  // 只有在编辑自己资料时才更新表单字段
          this.profileName = this.user.name || this.user.nickname || '';
          this.profileBio = this.user.bio || '';
          this.profileGender = this.user.gender || 0;
          
          // 处理生日字段
          if (this.user.birthday) {
            // 如果birthday是Date对象，转换为字符串格式
            if (this.user.birthday instanceof Date) {
              this.profileBirthday = this.user.birthday.toISOString().split('T')[0];
            } else {
              // 如果birthday已经是字符串格式，则直接使用
              this.profileBirthday = this.user.birthday;
            }
          } else {
            this.profileBirthday = '';
          }
          
          // 从用户数据中加载隐私设置
          if (this.user.isFavoritesVisible !== undefined) {
            this.showFavsPublic = this.user.isFavoritesVisible === 1;
            this.privacy.showFavs = this.user.isFavoritesVisible === 1;
          }
          if (this.user.isLikesVisible !== undefined) {
            this.showLikesPublic = this.user.isLikesVisible === 1;
            this.privacy.showLikes = this.user.isLikesVisible === 1;
          }
        }
      },
      
      loadActiveTabData() {
        if (!this.user || !this.user.id) return;
        
        // 根据当前激活的标签页加载对应的数据
        switch (this.activeTab) {
          case 'uploads':
            this.loadUploadedVideos();
            break;
          case 'favs':
            this.loadFavVideos();
            break;
          case 'likes':
            this.loadLikedVideos();
            break;
          case 'history':
            if (this.isMe) {
              this.loadHistoryVideos();
            }
            break;
          case 'following':
            this.loadFollowingUsers();
            break;
          case 'followers':
            this.loadFollowerUsers();
            break;
          default:
            // 如果是其他标签页，可以选择性加载数据
            break;
        }
      },
      
      async loadUploadedVideos() {
        if (!this.user || !this.user.id) return;
        
        this.uploadedVideosLoading = true;
        try {
          // 获取用户上传的视频
          const response = await userAPI.getUserVideos(this.user.id, 1, 20);
          if (response.data.code === 200) {
            const videoData = response.data.data || [];
            // 确保数据格式一致
            this.uploadedVideos = videoData.map(video => ({
              id: video.id,
              title: video.title,
              cover: video.coverUrl,
              coverUrl: video.coverUrl,
              videoUrl: video.videoUrl,
              viewCount: video.viewCount,
              likeCount: video.likeCount,
              commentCount: video.commentCount,
              favoriteCount: video.favoriteCount,
              userId: video.userId,
              username: video.username,
              time: video.createdAt,
              createdAt: video.createdAt,
              duration: video.duration,
              description: video.description,
              status: video.status !== undefined ? video.status : 1, // 默认状态为1（已发布）
              rejectReason: null, // 审核失败原因，需要单独加载
              loadingReason: false // 是否正在加载失败原因
            }));
            
            // 对于审核失败的视频，加载失败原因
            this.loadRejectReasons();
          } else {
            console.error('获取用户作品失败:', response.data.message);
            this.uploadedVideos = [];
          }
        } catch (error) {
          console.error('获取用户作品时出错:', error);
          this.uploadedVideos = [];
        } finally {
          this.uploadedVideosLoading = false;
        }
      },
      
      // 加载审核失败的视频的失败原因
      async loadRejectReasons() {
        const failedVideos = this.uploadedVideos.filter(video => video.status === 2);
        if (failedVideos.length === 0) return;
        
        // 并行加载所有失败视频的原因
        await Promise.all(failedVideos.map(async (video) => {
          video.loadingReason = true;
          try {
            const response = await videoAPI.getRejectReason(video.id);
            if (response.data.code === 200 && response.data.data) {
              video.rejectReason = response.data.data;
            }
          } catch (error) {
            console.error(`获取视频${video.id}的失败原因失败:`, error);
            video.rejectReason = '获取失败原因失败';
          } finally {
            video.loadingReason = false;
          }
        }));
      },
      
      async loadFavVideos() {
        if (!this.isMe) {
          // 非本人访问时检查隐私设置
          if (!this.canViewFavs) {
            this.favVideos = [];
            return;
          }
        }
        
        this.favVideosLoading = true;
        try {
          let response;
          if (this.isMe) {
            // 当前用户访问自己的收藏
            response = await userAPI.getUserFavorites(this.user.id, 1, 20);
          } else {
            // 访问其他用户的收藏
            response = await userAPI.getUserFavoritesByUserId(this.user.id, 1, 20);
          }
          if (response.data.code === 200) {
            // 获取收藏记录数据
            const favoriteData = response.data.data || [];
            
            // 提取视频ID列表
            const videoIds = favoriteData.map(fav => fav.videoId);
            
            if (videoIds.length > 0) {
              // 批量获取视频详情
              const videoResponse = await userAPI.getVideosByIds(videoIds);
              if (videoResponse.data.code === 200) {
                const videoMap = {};
                videoResponse.data.data.forEach(video => {
                  videoMap[video.id] = video;
                });
                
                // 将收藏记录和视频详情合并，只返回存在且有效的视频
                this.favVideos = favoriteData
                  .map(fav => {
                    const video = videoMap[fav.videoId];
                    if (video) {
                      return {
                        id: video.id,
                        title: video.title,
                        cover: video.coverUrl,
                        coverUrl: video.coverUrl,
                        videoUrl: video.videoUrl,
                        viewCount: video.viewCount,
                        likeCount: video.likeCount,
                        commentCount: video.commentCount,
                        favoriteCount: video.favoriteCount,
                        userId: video.userId,
                        username: video.username,
                        time: video.createdAt,
                        createdAt: video.createdAt,
                        duration: video.duration,
                        description: video.description
                      };
                    } else {
                      // 如果找不到视频详情，返回null
                      return null;
                    }
                  })
                  .filter(video => video !== null); // 过滤掉null值，即不存在的视频
              } else {
                console.error('获取视频详情失败:', videoResponse.data.message);
                // 如果获取视频详情失败，使用收藏记录中的基础信息
                this.favVideos = favoriteData.map(fav => {
                  return {
                    id: fav.videoId,
                    title: `视频 ${fav.videoId}`,
                    cover: 'https://via.placeholder.com/400x225',
                    views: '未知',
                    time: '未知',
                    duration: 0
                  };
                });
              }
            } else {
              // 如果没有收藏记录，设置为空数组
              this.favVideos = [];
            }
          } else {
            console.error('获取用户收藏失败:', response.data.message);
            this.favVideos = [];
          }
        } catch (error) {
          console.error('获取用户收藏时出错:', error);
          this.favVideos = [];
        } finally {
          this.favVideosLoading = false;
        }
      },
      
      async loadLikedVideos() {
        if (!this.isMe) {
          // 非本人访问时检查隐私设置
          if (!this.canViewLikes) {
            this.likedVideos = [];
            return;
          }
        }
        
        this.likedVideosLoading = true;
        try {
          let response;
          if (this.isMe) {
            // 当前用户访问自己的点赞
            response = await userAPI.getUserLikes(this.user.id, 1, 20);
          } else {
            // 访问其他用户的点赞
            response = await userAPI.getUserLikesByUserId(this.user.id, 1, 20);
          }
          if (response.data.code === 200) {
            // 获取点赞记录数据
            const likeData = response.data.data || [];
            
            // 提取视频ID列表
            const videoIds = likeData.map(like => like.videoId);
            
            if (videoIds.length > 0) {
              // 批量获取视频详情
              const videoResponse = await userAPI.getVideosByIds(videoIds);
              if (videoResponse.data.code === 200) {
                const videoMap = {};
                videoResponse.data.data.forEach(video => {
                  videoMap[video.id] = video;
                });
                
                // 将点赞记录和视频详情合并，只返回存在且有效的视频
                this.likedVideos = likeData
                  .map(like => {
                    const video = videoMap[like.videoId];
                    if (video) {
                      return {
                        id: video.id,
                        title: video.title,
                        cover: video.coverUrl,
                        coverUrl: video.coverUrl,
                        videoUrl: video.videoUrl,
                        viewCount: video.viewCount,
                        likeCount: video.likeCount,
                        commentCount: video.commentCount,
                        favoriteCount: video.favoriteCount,
                        userId: video.userId,
                        username: video.username,
                        time: video.createdAt,
                        createdAt: video.createdAt,
                        duration: video.duration,
                        description: video.description
                      };
                    } else {
                      // 如果找不到视频详情，返回null
                      return null;
                    }
                  })
                  .filter(video => video !== null); // 过滤掉null值，即不存在的视频
              } else {
                console.error('获取视频详情失败:', videoResponse.data.message);
                // 如果获取视频详情失败，使用点赞记录中的基础信息
                this.likedVideos = likeData.map(like => {
                  return {
                    id: like.videoId,
                    title: `视频 ${like.videoId}`,
                    cover: 'https://via.placeholder.com/400x225',
                    views: '未知',
                    time: '未知',
                    duration: 0
                  };
                });
              }
            } else {
              // 如果没有点赞记录，设置为空数组
              this.likedVideos = [];
            }
          } else {
            console.error('获取用户点赞失败:', response.data.message);
            this.likedVideos = [];
          }
        } catch (error) {
          console.error('获取用户点赞时出错:', error);
          this.likedVideos = [];
        } finally {
          this.likedVideosLoading = false;
        }
      },
      
      async loadHistoryVideos() {
        if (!this.isMe) return;
        
        this.historyVideosLoading = true;
        try {
          const response = await userAPI.getUserHistory(this.user.id, 1, 20);
          if (response.data.code === 200) {
            // 获取历史记录数据
            const historyData = response.data.data || [];
            
            // 提取视频ID列表
            const videoIds = historyData.map(history => history.videoId);
            
            if (videoIds.length > 0) {
              // 批量获取视频详情
              const videoResponse = await videoAPI.getVideosByIds(videoIds);
              if (videoResponse.data.code === 200) {
                const videoMap = {};
                videoResponse.data.data.forEach(video => {
                  videoMap[video.id] = video;
                });
                
                // 将历史记录和视频详情合并，只返回存在且有效的视频
                this.historyVideos = historyData
                  .map(history => {
                    const video = videoMap[history.videoId];
                    if (video) {
                      return {
                        id: video.id,
                        title: video.title,
                        cover: video.coverUrl,
                        coverUrl: video.coverUrl,
                        videoUrl: video.videoUrl,
                        viewCount: video.viewCount,
                        likeCount: video.likeCount,
                        commentCount: video.commentCount,
                        favoriteCount: video.favoriteCount,
                        userId: video.userId,
                        username: video.username,
                        time: history.updatedAt || video.createdAt,
                        createdAt: video.createdAt,
                        duration: video.duration,
                        description: video.description,
                        watchDuration: history.watchDuration || 0 // 保存观看时长
                      };
                    } else {
                      // 如果找不到视频详情，返回null
                      return null;
                    }
                  })
                  .filter(video => video !== null); // 过滤掉null值，即不存在的视频
              } else {
                console.error('获取视频详情失败:', videoResponse.data.message);
                // 如果获取视频详情失败，返回空数组
                this.historyVideos = [];
              }
            } else {
              // 如果没有历史记录，设置为空数组
              this.historyVideos = [];
            }
          } else {
            console.error('获取用户历史记录失败:', response.data.message);
            this.historyVideos = [];
          }
        } catch (error) {
          console.error('获取用户历史记录时出错:', error);
          this.historyVideos = [];
        } finally {
          this.historyVideosLoading = false;
        }
      },
      
      async loadFollowingUsers() {
        this.followingUsersLoading = true;
        try {
          let response;
          if (this.isMe) {
            // 当前用户访问自己的关注列表
            response = await userAPI.getFollowingList(this.user.id);
          } else {
            // 访问其他用户的关注列表
            response = await userAPI.getFollowingListByUserId(this.user.id);
          }
          if (response.data.code === 200) {
            let followingUsersData = response.data.data || [];
            
            // 为列表中的每个用户获取关注状态
            for (let i = 0; i < followingUsersData.length; i++) {
              const user = followingUsersData[i];
              try {
                const followCheckResponse = await userAPI.isFollowing(user.id);
                if (followCheckResponse.data.code === 200) {
                  user.isFollowing = followCheckResponse.data.data || false;
                } else {
                  user.isFollowing = false;
                }
              } catch (err) {
                console.error(`检查关注状态失败 用户ID ${user.id}:`, err);
                user.isFollowing = false;
              }
            }
            
            this.followingUsers = followingUsersData;
          } else {
            console.error('获取关注列表失败:', response.data.message);
            this.followingUsers = [];
          }
        } catch (error) {
          console.error('获取关注列表时出错:', error);
          this.followingUsers = [];
        } finally {
          this.followingUsersLoading = false;
        }
      },
      
      async loadFollowerUsers() {
        this.followerUsersLoading = true;
        try {
          let response;
          if (this.isMe) {
            // 当前用户访问自己的粉丝列表
            response = await userAPI.getFollowersList(this.user.id);
          } else {
            // 访问其他用户的粉丝列表
            response = await userAPI.getFollowersListByUserId(this.user.id);
          }
          if (response.data.code === 200) {
            let followerUsersData = response.data.data || [];
            
            // 为列表中的每个用户获取关注状态
            for (let i = 0; i < followerUsersData.length; i++) {
              const user = followerUsersData[i];
              try {
                const followCheckResponse = await userAPI.isFollowing(user.id);
                if (followCheckResponse.data.code === 200) {
                  user.isFollowing = followCheckResponse.data.data || false;
                } else {
                  user.isFollowing = false;
                }
              } catch (err) {
                console.error(`检查关注状态失败 用户ID ${user.id}:`, err);
                user.isFollowing = false;
              }
            }
            
            this.followerUsers = followerUsersData;
          } else {
            console.error('获取粉丝列表失败:', response.data.message);
            this.followerUsers = [];
          }
        } catch (error) {
          console.error('获取粉丝列表时出错:', error);
          this.followerUsers = [];
        } finally {
          this.followerUsersLoading = false;
        }
      },
      
      /* ================= 弹窗与互动（参考 category.vue） ================= */
      async openVideoModal(video) {
        if (!video || !video.id) return;
        this.selectedVideo = { ...video };
        this.localLikeCount = video.likeCount || 0;
        this.localFavoriteCount = video.favoriteCount || 0;
        this.isVideoLiked = false;
        this.isVideoFavorited = false;
        this.comments = [];
        this.newCommentContent = '';
        this.replyToCommentId = null;
        this.replyToUserId = null;
        this.replyToUsername = null;
        // 重置观看历史状态
        this.historyRecorded = false;
        this.watchStartTime = null;
        this.nextUpdateInterval = null;
        this.watchDuration = 0;

        // 如果是历史tab，获取观看历史记录中的watchDuration
        let watchDuration = 0;
        if (this.activeTab === 'history') {
          // 优先使用video对象中的watchDuration
          if (video.watchDuration !== undefined && video.watchDuration !== null) {
            watchDuration = video.watchDuration;
          } else {
            // 如果没有，调用API获取
            try {
              const historyRes = await userAPI.getVideoHistory(video.id);
              if (historyRes.data.code === 200 && historyRes.data.data) {
                watchDuration = historyRes.data.data.watchDuration || 0;
              }
            } catch (e) {
              console.error('获取视频观看历史失败:', e);
            }
          }
        }

        document.body.style.overflow = 'hidden';

        this.$nextTick(async () => {
          const player = this.$refs.modalVideoPlayer;
          const el = Array.isArray(player) ? player[0] : player;
          
          // 如果是历史tab且有watchDuration，设置视频播放位置
          if (this.activeTab === 'history' && watchDuration > 0 && el) {
            // 等待视频元数据加载完成后再设置currentTime
            const setCurrentTime = () => {
              if (el.readyState >= 1) { // HAVE_METADATA
                el.currentTime = watchDuration;
                el.removeEventListener('loadedmetadata', setCurrentTime);
              }
            };
            
            if (el.readyState >= 1) {
              el.currentTime = watchDuration;
            } else {
              el.addEventListener('loadedmetadata', setCurrentTime);
            }
          }
          
          if (el && typeof el.play === 'function') {
            el.play().catch(err => console.error('自动播放失败:', err));
          }

          try {
            const detailRes = await videoAPI.getVideoDetail(video.id);
            if (detailRes.data.code === 200 && detailRes.data.data) {
              const detail = detailRes.data.data;
              this.selectedVideo = { ...this.selectedVideo, ...detail };
              this.localLikeCount = detail.likeCount || 0;
              this.localFavoriteCount = detail.favoriteCount || 0;
            }
          } catch (e) {
            console.error('获取视频详情失败:', e);
          }

          this.checkLikeStatus();
          this.checkFavoriteStatus();
          this.loadComments();
        });
      },

      closeVideoModal() {
        const player = this.$refs.modalVideoPlayer;
        const el = Array.isArray(player) ? player[0] : player;
        if (el && el.pause) {
          el.pause();
        }
        this.selectedVideo = null;
        document.body.style.overflow = 'unset';
        this.comments = [];
        this.newCommentContent = '';
        this.replyToCommentId = null;
        this.replyToUserId = null;
        this.replyToUsername = null;
        this.historyRecorded = false;
        this.watchStartTime = null;
        this.nextUpdateInterval = null;
        this.watchDuration = 0;
      },

      async checkLikeStatus() {
        try {
          if (this.selectedVideo && this.selectedVideo.id) {
            const res = await userAPI.checkVideoLiked(this.selectedVideo.id);
            if (res.data.code === 200) {
              this.isVideoLiked = res.data.data || false;
            }
          }
        } catch (e) {
          console.error('检查点赞状态失败:', e);
        }
      },

      async checkFavoriteStatus() {
        try {
          if (this.selectedVideo && this.selectedVideo.id) {
            const res = await userAPI.checkVideoFavorited(this.selectedVideo.id);
            if (res.data.code === 200) {
              this.isVideoFavorited = res.data.data || false;
            }
          }
        } catch (e) {
          console.error('检查收藏状态失败:', e);
        }
      },

      // 弹窗视频开始播放时记录观看历史（与首页推荐逻辑一致）
      async onModalVideoPlay() {
        if (this.historyRecorded || !this.$store.getters.isAuthenticated) {
          return;
        }
        if (!this.selectedVideo || !this.selectedVideo.id) return;

        this.watchStartTime = Date.now();
        this.nextUpdateInterval = Math.floor(Math.random() * 2000) + 3000; // 3000-5000ms

        try {
          await userAPI.recordWatchHistory(this.selectedVideo.id, 0, 0);
          this.historyRecorded = true;
        } catch (error) {
          console.error('记录观看历史失败:', error);
        }
      },

      // 弹窗视频播放进度更新时，按3-5秒随机间隔更新观看历史
      async onModalVideoTimeUpdate() {
        if (!this.historyRecorded || !this.$store.getters.isAuthenticated) {
          return;
        }
        if (!this.selectedVideo || !this.selectedVideo.id) return;

        const player = this.$refs.modalVideoPlayer;
        const videoElement = Array.isArray(player) ? player[0] : player;
        if (!videoElement) return;

        const duration = videoElement.duration;
        if (!duration || duration === 0) return;

        const currentTime = Math.floor(videoElement.currentTime);
        const progress = (videoElement.currentTime / duration) * 100;

        const now = Date.now();
        if (this.watchStartTime && this.nextUpdateInterval && now - this.watchStartTime < this.nextUpdateInterval) {
          return;
        }

        this.nextUpdateInterval = Math.floor(Math.random() * 2000) + 3000;
        this.watchStartTime = now;
        this.watchDuration = currentTime;

        try {
          await userAPI.recordWatchHistory(this.selectedVideo.id, currentTime, progress);
        } catch (error) {
          console.error('更新观看历史失败:', error);
        }
      },

      async toggleLike() {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        if (!this.selectedVideo || !this.selectedVideo.id) return;

        try {
          if (this.isVideoLiked) {
            await userAPI.unlikeVideo(this.selectedVideo.id);
            this.isVideoLiked = false;
            if (this.localLikeCount > 0) this.localLikeCount--;
          } else {
            await userAPI.likeVideo(this.selectedVideo.id);
            this.isVideoLiked = true;
            this.localLikeCount++;
          }
          if (this.selectedVideo) {
            this.selectedVideo.likeCount = this.localLikeCount;
          }
        } catch (e) {
          console.error('点赞操作失败:', e);
          alert('点赞操作失败，请重试');
        }
      },

      async toggleFavorite() {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        if (!this.selectedVideo || !this.selectedVideo.id) return;

        try {
          if (this.isVideoFavorited) {
            await userAPI.unfavoriteVideo(this.selectedVideo.id);
            this.isVideoFavorited = false;
            if (this.localFavoriteCount > 0) this.localFavoriteCount--;
          } else {
            await userAPI.favoriteVideo(this.selectedVideo.id);
            this.isVideoFavorited = true;
            this.localFavoriteCount++;
          }
          if (this.selectedVideo) {
            this.selectedVideo.favoriteCount = this.localFavoriteCount;
          }
        } catch (e) {
          console.error('收藏操作失败:', e);
          alert('收藏操作失败，请重试');
        }
      },

      async loadComments() {
        if (!this.selectedVideo || !this.selectedVideo.id) return;
        this.loadingComments = true;
        try {
          const res = await commentAPI.getComments(this.selectedVideo.id);
          if (res.data.code === 200) {
            this.comments = this.processComments(res.data.data || []);
          }
        } catch (e) {
          console.error('加载评论失败:', e);
          this.comments = [];
        } finally {
          this.loadingComments = false;
        }
      },

      processComments(comments) {
        const parent = [];
        const map = {};
        comments.forEach(c => {
          c.replies = [];
          if (c.parentId === 0 || c.parentId === null) {
            parent.push(c);
          } else {
            map[c.id] = c;
          }
        });
        comments.forEach(c => {
          if (c.parentId !== 0 && c.parentId !== null) {
            const p = parent.find(x => x.id === c.parentId);
            if (p) {
              p.replies.push(c);
            } else if (map[c.parentId]) {
              map[c.parentId].replies.push(c);
            }
          }
        });
        return parent;
      },

      async submitComment() {
        if (!this.newCommentContent.trim()) return;
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        if (!this.selectedVideo || !this.selectedVideo.id) return;

        try {
          const payload = {
            videoId: this.selectedVideo.id,
            content: this.newCommentContent,
            parentId: this.replyToCommentId || 0,
            replyUserId: this.replyToUserId || null
          };
          const res = await commentAPI.postComment(payload);
          if (res.data.code === 200) {
            const newComment = res.data.data;
            if (this.replyToCommentId) {
              const parent = this.comments.find(c => c.id === this.replyToCommentId);
              if (parent) {
                parent.replies = parent.replies || [];
                parent.replies.unshift(newComment);
              }
              this.replyToCommentId = null;
              this.replyToUserId = null;
              this.replyToUsername = null;
            } else {
              this.comments.unshift(newComment);
            }
            this.newCommentContent = '';
            if (this.selectedVideo && this.selectedVideo.commentCount !== undefined) {
              this.selectedVideo.commentCount++;
            }
          }
        } catch (e) {
          console.error('提交评论失败:', e);
          alert('提交评论失败，请重试');
        }
      },

      replyToComment(comment) {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        this.replyToCommentId = comment.id;
        this.replyToUserId = comment.userId;
        this.replyToUsername = comment.username;
        this.$nextTick(() => {
          const input = this.$refs.commentInput;
          const el = Array.isArray(input) ? input[0] : input;
          el && el.focus();
        });
      },

      cancelReply() {
        this.replyToCommentId = null;
        this.replyToUserId = null;
        this.replyToUsername = null;
      },

      focusCommentInput() {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        this.cancelReply();
        this.$nextTick(() => {
          const input = this.$refs.commentInput;
          const el = Array.isArray(input) ? input[0] : input;
          if (el) {
            el.focus();
            if (el.scrollIntoView) {
              el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
          }
        });
      },

      async deleteComment(commentId) {
        if (!confirm('确定要删除这条评论吗？')) return;
        try {
          const delCount = this.countCommentsToDelete(commentId);
          await commentAPI.deleteComment(commentId);
          this.removeCommentFromList(commentId);
          if (this.selectedVideo && this.selectedVideo.commentCount !== undefined) {
            this.selectedVideo.commentCount = Math.max(0, this.selectedVideo.commentCount - delCount);
          }
        } catch (e) {
          console.error('删除评论失败:', e);
          alert('删除评论失败，请重试');
        }
      },

      countCommentsToDelete(commentId) {
        const countRecursive = (c) => {
          if (!c) return 0;
          let count = 1;
          if (c.replies && c.replies.length > 0) {
            c.replies.forEach(r => count += countRecursive(r));
          }
          return count;
        };

        let target = null;
        for (let i = 0; i < this.comments.length; i++) {
          const c = this.comments[i];
          if (c.id === commentId) {
            target = c; break;
          }
          if (c.replies && c.replies.length > 0) {
            for (let j = 0; j < c.replies.length; j++) {
              if (c.replies[j].id === commentId) {
                target = c.replies[j];
                break;
              }
            }
          }
          if (target) break;
        }
        return target ? countRecursive(target) : 1;
      },

      removeCommentFromList(commentId) {
        const findAndRemove = (list, targetId) => {
          for (let i = 0; i < list.length; i++) {
            if (list[i].id === targetId) {
              list.splice(i, 1);
              return true;
            }
            if (list[i].replies && list[i].replies.length > 0) {
              if (findAndRemove(list[i].replies, targetId)) return true;
            }
          }
          return false;
        };
        findAndRemove(this.comments, commentId);
      },

      formatViews(views) {
        if (!views && views !== 0) return 0;
        if (views >= 10000) return `${(views / 10000).toFixed(1)}万`;
        return views;
      },
      
      displayViews(video) {
        if (video.viewCount !== undefined && video.viewCount !== null) {
          return this.formatViews(video.viewCount);
        }
        return video.views !== undefined ? this.formatViews(video.views) : 0;
      },

      formatTime(timeStr) {
        if (!timeStr) return '';
        try {
          const date = new Date(timeStr);
          const now = new Date();
          const diff = now - date;
          const minutes = Math.floor(diff / 60000);
          const hours = Math.floor(diff / 3600000);
          const days = Math.floor(diff / 86400000);
          if (minutes < 1) return '刚刚';
          if (minutes < 60) return `${minutes}分钟前`;
          if (hours < 24) return `${hours}小时前`;
          if (days < 7) return `${days}天前`;
          return date.toLocaleDateString('zh-CN');
        } catch (e) {
          return timeStr;
        }
      },
      
      onAvatarChange(event) {
        const file = event.target.files[0];
        if (!file) return;
        
        // 这里可以添加文件类型和大小验证
        if (!file.type.startsWith('image/')) {
          alert('请选择图片文件');
          return;
        }
        
        // 创建FormData对象上传头像
        const formData = new FormData();
        formData.append('avatar', file);
        
        fetch('/api/user/avatar', {
          method: 'POST',
          body: formData,
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
          }
        })
        .then(response => {
          if (response.ok) {
            return response.json();
          }
          throw new Error('头像上传失败');
        })
        .then(data => {
          // 更新用户头像
          if (data && data.data) {
            this.user.avatar = data.data;
            // 更新vuex中的用户信息，确保所有组件都能立即看到更新
            if (this.isMe) {
              this.$store.dispatch('updateUser', { ...this.user, avatar: data.data });
            }
            alert('头像上传成功');
            // 触发头像更新事件，通知父组件刷新用户数据
            this.$emit('avatar-updated');
          } else {
            alert('头像上传失败：服务器返回数据异常');
          }
        })
        .catch(error => {
          console.error('头像上传出错:', error);
          alert('头像上传失败');
        });
      },
      
      async saveChanges() {
        try {
          const updateData = {};
          
          // 添加隐私设置
          updateData.isFavoritesVisible = this.showFavsPublic ? 1 : 0;
          updateData.isLikesVisible = this.showLikesPublic ? 1 : 0;
          
          // 添加基本资料信息
          updateData.nickname = this.profileName;
          updateData.bio = this.profileBio;
          updateData.gender = this.profileGender;
          updateData.birthday = this.profileBirthday;
          
          // 添加密码信息（如果用户填写了密码）
          if (this.oldPassword && this.newPassword && this.confirmNewPassword) {
            updateData.oldPassword = this.oldPassword;
            updateData.newPassword = this.newPassword;
            updateData.confirmNewPassword = this.confirmNewPassword;
          }
          
          const response = await fetch('/api/user/update', {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
            },
            body: JSON.stringify(updateData)
          });
          
          if (response.ok) {
            alert('信息更新成功');
            // 清空密码字段
            if (this.oldPassword && this.newPassword && this.confirmNewPassword) {
              this.oldPassword = '';
              this.newPassword = '';
              this.confirmNewPassword = '';
            }
          } else {
            alert('更新失败');
          }
        } catch (error) {
          console.error('保存信息时出错:', error);
          alert('保存失败');
        }
      },
      
      // 处理转发
      handleForward() {
        if (!this.$store.getters.isAuthenticated) {
          this.$router.push('/login');
          return;
        }
        if (!this.selectedVideo || !this.selectedVideo.id) {
          alert('请先选择要转发的视频');
          return;
        }
        this.showForwardModal = true;
      },
      
      // 转发成功回调
      handleForwardSuccess(friend) {
        alert(`已转发给 ${friend.nickname || friend.username}`);
        // 更新分享数
        if (this.selectedVideo) {
          this.selectedVideo.shareCount = (this.selectedVideo.shareCount || 0) + 1;
        }
      }
    },
  }
  </script>
  
  <style scoped>
  .video-modal-overlay {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 240px;
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
  
  .comment-input-title {
    color: #fff;
    font-weight: 600;
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
  }
  
  .comment-send-btn.active {
    background: #ff4757;
    color: #fff;
  }
  
  .video-status-section {
    margin-bottom: 30px;
  }
  
  .status-title {
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    margin-bottom: 15px;
    padding-bottom: 10px;
    border-bottom: 2px solid #333;
  }
  
  .empty-state {
    text-align: center;
    color: #888;
    padding: 40px 20px;
    font-size: 14px;
  }
  
  .reject-reason {
    margin-top: 8px;
    padding: 8px 12px;
    background: rgba(255, 71, 87, 0.1);
    border-left: 3px solid #ff4757;
    border-radius: 4px;
    font-size: 12px;
    color: #ff6b7a;
  }
  
  .reject-reason.loading {
    background: rgba(128, 128, 128, 0.1);
    border-left-color: #888;
    color: #888;
  }
  
  .reject-label {
    font-weight: 600;
    margin-right: 6px;
  }
  
  .reject-text {
    color: #ffb3ba;
  }
  
  .video-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 20px;
    margin-top: 10px;
  }
  
  .grid-card {
    position: relative;
    cursor: pointer;
    transition: transform 0.2s;
  }
  
  .grid-card:hover {
    transform: translateY(-4px);
  }
  
  .card-cover {
    width: 100%;
    height: 120px;
    border-radius: 8px;
    margin-bottom: 8px;
    background-size: cover;
    background-position: center;
  }
  
  .card-info {
    position: relative;
  }
  
  .card-title {
    font-size: 14px;
    color: #fff;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .card-meta {
    font-size: 12px;
    color: #888;
  }
  </style>
<template>
  <Transition name="slide-up">
    <div v-if="visible" class="forward-modal-overlay" @click="handleOverlayClick">
      <div class="forward-modal-content" @click.stop>
        <div class="forward-modal-header">
          <h3 class="forward-modal-title">转发给</h3>
          <button class="forward-modal-close" @click="close">✕</button>
        </div>
        <div class="forward-modal-body">
          <div v-if="loading" class="forward-loading">加载好友列表中...</div>
          <div v-else-if="friends.length === 0" class="forward-empty">暂无好友</div>
          <div v-else class="forward-friends-list">
            <div 
              v-for="friend in friends" 
              :key="friend.id"
              class="forward-friend-item"
              @click="handleForward(friend)"
            >
              <img 
                :src="friend.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + friend.nickname" 
                class="forward-friend-avatar" 
                :alt="friend.nickname || friend.username"
              />
              <span class="forward-friend-name">{{ friend.nickname || friend.username }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script>
import friendAPI from '@/api/friend'
import messageAPI from '@/api/message'

export default {
  name: 'ForwardModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    videoId: {
      type: [Number, String],
      required: true
    },
    videoTitle: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      friends: [],
      loading: false
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadFriends()
      }
    }
  },
  methods: {
    async loadFriends() {
      this.loading = true
      try {
        const response = await friendAPI.getFriendList()
        if (response.data.code === 200) {
          this.friends = response.data.data || []
        } else {
          console.error('获取好友列表失败:', response.data.message)
          this.friends = []
        }
      } catch (error) {
        console.error('获取好友列表时出错:', error)
        this.friends = []
      } finally {
        this.loading = false
      }
    },
    
    async handleForward(friend) {
      if (!this.videoId) {
        alert('视频ID不存在')
        return
      }
      
      const currentUser = this.$store.state.user
      if (!currentUser || !currentUser.id) {
        alert('请先登录')
        return
      }
      
      try {
        const response = await messageAPI.sendMessage({
          fromUserId: currentUser.id,
          toUserId: friend.id,
          type: 1, // 1-视频分享
          content: this.videoTitle || `分享了一个视频`,
          relatedId: this.videoId
        })
        
        if (response.data.code === 200) {
          this.$emit('forward-success', friend)
          this.close()
        } else {
          alert('转发失败: ' + (response.data.message || '未知错误'))
        }
      } catch (error) {
        console.error('转发失败:', error)
        alert('转发失败，请稍后重试')
      }
    },
    
    handleOverlayClick() {
      this.close()
    },
    
    close() {
      this.$emit('update:visible', false)
      this.$emit('close')
    }
  }
}
</script>

<style scoped>
.forward-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.forward-modal-content {
  width: 100%;
  max-width: 500px;
  background: #1a1a1a;
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.forward-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #2a2a2a;
}

.forward-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.forward-modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #aaa;
  font-size: 24px;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.forward-modal-close:hover {
  background: #2a2a2a;
  color: #fff;
}

.forward-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.forward-loading,
.forward-empty {
  text-align: center;
  padding: 40px 20px;
  color: #aaa;
  font-size: 14px;
}

.forward-friends-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.forward-friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.forward-friend-item:hover {
  background: #2a2a2a;
}

.forward-friend-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.forward-friend-name {
  font-size: 16px;
  color: #fff;
  font-weight: 500;
}

/* 滑动动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease-out;
}

.slide-up-enter-from {
  opacity: 0;
}

.slide-up-enter-from .forward-modal-content {
  transform: translateY(100%);
}

.slide-up-leave-to {
  opacity: 0;
}

.slide-up-leave-to .forward-modal-content {
  transform: translateY(100%);
}

.slide-up-enter-active .forward-modal-content,
.slide-up-leave-active .forward-modal-content {
  transition: transform 0.3s ease-out;
}
</style>

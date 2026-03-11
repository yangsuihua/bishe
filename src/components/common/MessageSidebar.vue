<template>
  <div class="message-sidebar">
    <div class="sidebar-header">
      <h3>消息</h3>
      <button class="close-btn" @click="$emit('close')">×</button>
    </div>
    
    <div class="friend-list">
      <div 
        v-for="friend in friendList" 
        :key="friend.id"
        class="friend-item"
        :class="{ active: activeChatFriend?.id === friend.id }"
        @click="selectFriend(friend)"
      >
        <img 
          :src="friend.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + friend.username" 
          class="friend-avatar" 
          alt="avatar"
        >
        <div class="friend-info">
          <div class="friend-name">{{ friend.nickname || friend.username }}</div>
          <div class="friend-username">@{{ friend.username }}</div>
        </div>
      </div>
      
      <div v-if="friendList.length === 0" class="empty-state">
        暂无好友
      </div>
    </div>
  </div>
</template>

<script>
import friendAPI from '@/api/friend'

export default {
  name: 'MessageSidebar',
  data() {
    return {
      friendList: []
    }
  },
  props: {
    activeChatFriend: {
      type: Object,
      default: null
    }
  },
  watch: {
    // 监听父组件触发刷新
    '$attrs.refresh': {
      handler() {
        this.loadFriendList()
      }
    }
  },
  async mounted() {
    await this.loadFriendList()
  },
  methods: {
    async loadFriendList() {
      try {
        const response = await friendAPI.getFriendList()
        if (response.data.code === 200) {
          this.friendList = response.data.data || []
        }
      } catch (error) {
        console.error('加载好友列表失败:', error)
      }
    },
    
    selectFriend(friend) {
      this.$emit('select-friend', friend)
    }
  }
}
</script>

<style scoped>
.message-sidebar {
  position: fixed;
  right: 400px;
  top: 60px;
  width: 300px;
  height: calc(100vh - 60px);
  background: var(--bg-color, #1a1a1a);
  border-left: 1px solid var(--border-color, #333);
  display: flex;
  flex-direction: column;
  z-index: 999;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid var(--border-color, #333);
}

.sidebar-header h3 {
  margin: 0;
  color: var(--text-color, #fff);
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-color, #fff);
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  line-height: 30px;
}

.friend-list {
  flex: 1;
  overflow-y: auto;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color, #333);
  transition: background 0.2s;
}

.friend-item:hover {
  background: var(--hover-bg, #2a2a2a);
}

.friend-item.active {
  background: var(--active-bg, #333);
}

.friend-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
}

.friend-info {
  flex: 1;
}

.friend-name {
  font-weight: 500;
  color: var(--text-color, #fff);
  margin-bottom: 4px;
}

.friend-username {
  font-size: 12px;
  color: #999;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: #999;
}
</style>

<template>
  <div id="app">
    <div v-if="!isBlankLayout">
      <Header />
      <div class="main-container">
        <Sidebar @open-messages="openMessages" />
        <main class="content" :class="{ 'with-chat': showChatWindow }">
          <router-view></router-view>
        </main>
        <MessageSidebar 
          ref="messageSidebar"
          v-if="showChatWindow"
          :activeChatFriend="activeChatFriend"
          @select-friend="selectFriend"
          @close="closeChat"
        />
        <ChatWindow 
          v-if="showChatWindow && activeChatFriend"
          :activeChatFriend="activeChatFriend"
          @close-chat="closeChat"
          @refresh-friends="refreshFriends"
        />
      </div>
    </div>
    <div v-else>
      <router-view v-if="$route.matched.length" />
    </div>
  </div>
</template>
  
  <script>
  import Header from './components/layout/Header.vue'
  import Sidebar from './components/layout/Sidebar.vue'
  import ChatWindow from './components/common/ChatWindow.vue'
  import MessageSidebar from './components/common/MessageSidebar.vue'
  import friendAPI from './api/friend'
  
  export default {
    name: 'App',
    components: {
      Header,
      Sidebar,
      ChatWindow,
      MessageSidebar
    },
    data() {
      return {
        showChatWindow: false,
        activeChatFriend: null,
        friendList: []
      }
    },
    computed: {
      isBlankLayout() {
        // 检查当前路由或其父路由是否有 blankLayout 元信息
        return this.$route.meta.blankLayout === true;
      }
    },
    methods: {
      async openMessages() {
        if (!this.showChatWindow) {
          this.showChatWindow = true
        } else {
          this.showChatWindow = false
          this.activeChatFriend = null
        }
      },
      
      selectFriend(friend) {
        this.activeChatFriend = friend
      },
      
      closeChat() {
        this.showChatWindow = false
        this.activeChatFriend = null
      },
      
      async refreshFriends() {
        // 触发 MessageSidebar 重新加载好友列表
        this.$refs.messageSidebar?.loadFriendList()
      }
    }
  }
  </script>
  
  <style>
  .content.with-chat {
    margin-right: 700px;
  }
  </style>